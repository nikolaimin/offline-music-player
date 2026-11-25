package com.offlineplayer.music.data

import android.app.RecoverableSecurityException
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi

/**
 * Удаляет аудиофайл с телефона через MediaStore. Правила зависят от версии Android:
 *  - Android 11+: система сама показывает окно «Разрешить удалить?» и удаляет файл;
 *  - Android 10: файл чужого приложения удаляется после подтверждения (RecoverableSecurityException);
 *  - Android 9 и старше: удаляется сразу, нужно разрешение WRITE_EXTERNAL_STORAGE.
 * Вызывать только из фонового потока.
 */
object TrackDeleter {

    sealed interface Result {
        /** Файл удалён. */
        object Deleted : Result

        /** Удалить не удалось. */
        object Failed : Result

        /**
         * Android просит подтверждение у пользователя: [intentSender] нужно запустить.
         * Если [deleteAfterConsent] = true (Android 10), после «Разрешить» файл надо удалить
         * повторным вызовом; на Android 11+ его удаляет сама система.
         */
        class NeedsConsent(val intentSender: IntentSender, val deleteAfterConsent: Boolean) : Result
    }

    fun delete(context: Context, uri: Uri): Result {
        val resolver = context.contentResolver

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return try {
                val request = MediaStore.createDeleteRequest(resolver, listOf(uri))
                Result.NeedsConsent(request.intentSender, deleteAfterConsent = false)
            } catch (e: Exception) {
                Result.Failed
            }
        }

        return try {
            if (resolver.delete(uri, null, null) > 0) Result.Deleted else Result.Failed
        } catch (e: SecurityException) {
            val sender = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) consentSender(e) else null
            if (sender != null) Result.NeedsConsent(sender, deleteAfterConsent = true) else Result.Failed
        } catch (e: Exception) {
            Result.Failed
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun consentSender(e: SecurityException): IntentSender? =
        (e as? RecoverableSecurityException)?.userAction?.actionIntent?.intentSender
}
