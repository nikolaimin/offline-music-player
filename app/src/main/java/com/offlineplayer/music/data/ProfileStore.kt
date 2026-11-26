package com.offlineplayer.music.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

const val DefaultProfileName = "Слушатель"

const val MaxProfileNameLength = 30
const val MaxUsernameLength = 20
const val MaxStatusLength = 60

/** Размер стороны сохранённого аватара, пикселей. */
private const val AvatarSizePx = 512

private const val KeyId = "id"
private const val KeyName = "name"
private const val KeyUsername = "username"
private const val KeyStatus = "status"
private const val KeyAvatarVersion = "avatar_version"

/**
 * Локальный профиль пользователя. Регистрации и сервера нет: [id] и [username]
 * хранятся заранее, чтобы позже профиль можно было связать с онлайн-функциями.
 */
data class Profile(
    /** Постоянный случайный идентификатор профиля, создаётся при первом запуске. */
    val id: String,
    val name: String,
    /** Имя пользователя без «@»: латиница, цифры и «_». */
    val username: String,
    val status: String,
    /** Меняется при каждой смене фото, чтобы экран перечитал файл. 0 — фото нет. */
    val avatarVersion: Long,
    /** Путь к файлу аватара во внутренней памяти приложения или null, если фото нет. */
    val avatarPath: String?
)

/** Хранит профиль на телефоне: текстовые поля в SharedPreferences, фото отдельным файлом. */
class ProfileStore(private val context: Context) {

    private val prefs = context.getSharedPreferences("profile", Context.MODE_PRIVATE)
    private val avatarFile = File(context.filesDir, "profile_avatar.jpg")

    fun load(): Profile {
        var id = prefs.getString(KeyId, null)
        if (id.isNullOrEmpty()) {
            id = UUID.randomUUID().toString()
            prefs.edit().putString(KeyId, id).apply()
        }
        val version = prefs.getLong(KeyAvatarVersion, 0L)
        return Profile(
            id = id,
            name = prefs.getString(KeyName, null)?.takeIf { it.isNotBlank() } ?: DefaultProfileName,
            username = prefs.getString(KeyUsername, null)?.takeIf { it.isNotBlank() }
                ?: defaultUsername(id),
            status = prefs.getString(KeyStatus, null).orEmpty(),
            avatarVersion = version,
            avatarPath = if (version > 0L && avatarFile.exists()) avatarFile.absolutePath else null
        )
    }

    /** Сохраняет имя, имя пользователя и статус; пустые имя и имя пользователя не принимаются. */
    fun save(name: String, username: String, status: String): Profile {
        val editor = prefs.edit()
        name.trim().take(MaxProfileNameLength).takeIf { it.isNotEmpty() }
            ?.let { editor.putString(KeyName, it) }
        cleanUsername(username).takeIf { it.isNotEmpty() }
            ?.let { editor.putString(KeyUsername, it) }
        editor.putString(KeyStatus, status.trim().take(MaxStatusLength))
        editor.apply()
        return load()
    }

    /** Берёт картинку по [uri], обрезает до квадрата, уменьшает и сохраняет как аватар. */
    fun saveAvatarFrom(uri: Uri): Profile? {
        val bitmap = try {
            decodeSquare(uri)
        } catch (e: Exception) {
            null
        } ?: return null
        return try {
            FileOutputStream(avatarFile).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            prefs.edit().putLong(KeyAvatarVersion, System.currentTimeMillis()).apply()
            load()
        } catch (e: IOException) {
            null
        } finally {
            bitmap.recycle()
        }
    }

    fun removeAvatar(): Profile {
        avatarFile.delete()
        prefs.edit().remove(KeyAvatarVersion).apply()
        return load()
    }

    private fun decodeSquare(uri: Uri): Bitmap? {
        val resolver = context.contentResolver

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        // При inJustDecodeBounds decodeStream всегда возвращает null: размеры читаем из bounds.
        val opened = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds); true }
        if (opened != true || bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        // Грубое уменьшение при чтении, чтобы не держать в памяти фото на десятки мегабайт.
        var sample = 1
        while (minOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= AvatarSizePx) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: return null

        val side = minOf(decoded.width, decoded.height)
        val scale = minOf(1f, AvatarSizePx.toFloat() / side)
        val matrix = Matrix().apply {
            postRotate(readRotation(uri).toFloat())
            postScale(scale, scale)
        }
        val result = Bitmap.createBitmap(
            decoded,
            (decoded.width - side) / 2,
            (decoded.height - side) / 2,
            side,
            side,
            matrix,
            true
        )
        if (result !== decoded) decoded.recycle()
        return result
    }

    /** Поворот фото из метаданных камеры (на Android 6 не читается: нужен API 24). */
    private fun readRotation(uri: Uri): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return 0
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = android.media.ExifInterface(stream)
                when (exif.getAttributeInt(
                    android.media.ExifInterface.TAG_ORIENTATION,
                    android.media.ExifInterface.ORIENTATION_NORMAL
                )) {
                    android.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    android.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    android.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            } ?: 0
        } catch (e: Exception) {
            0
        }
    }

    private fun defaultUsername(id: String) = "user" + id.replace("-", "").take(6)
}

/** Оставляет в имени пользователя только латиницу, цифры и «_» (в нижнем регистре). */
fun cleanUsername(value: String): String =
    value.lowercase().filter { it in 'a'..'z' || it in '0'..'9' || it == '_' }.take(MaxUsernameLength)
