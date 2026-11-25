package com.offlineplayer.music.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Читает список музыкальных файлов телефона через MediaStore.
 * Система сама сканирует память, поэтому новые файлы появляются при следующей загрузке.
 */
class MusicRepository(private val context: Context) {

    suspend fun loadTracks(): List<Track> = withContext(Dispatchers.IO) {
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED
        )

        // Файлы без длительности (служебные копии вроде AutoBackup) воспроизвести нельзя: скрываем их.
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND " +
            "${MediaStore.Audio.Media.DURATION} >= $MIN_DURATION_MS"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        val tracks = ArrayList<Track>()

        try {
            context.contentResolver.query(
                collection, projection, selection, null, sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    tracks.add(
                        Track(
                            id = id,
                            title = cursor.getString(titleCol).orEmpty().ifBlank { "Без названия" },
                            artist = cursor.getString(artistCol).cleanUnknown("Неизвестный исполнитель"),
                            album = cursor.getString(albumCol).cleanUnknown("Неизвестный альбом"),
                            albumId = cursor.getLong(albumIdCol),
                            durationMs = cursor.getLong(durationCol),
                            dateAddedSec = cursor.getLong(dateCol),
                            uri = ContentUris.withAppendedId(collection, id)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Нет доступа или система вернула ошибку: не роняем приложение.
            Log.e("MusicRepository", "Не удалось прочитать библиотеку", e)
        }

        tracks
    }

    private fun String?.cleanUnknown(fallback: String): String =
        if (this.isNullOrBlank() || this == MediaStore.UNKNOWN_STRING) fallback else this

    private companion object {
        const val MIN_DURATION_MS = 1000L
    }
}
