package com.offlineplayer.music.data

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.LruCache
import android.util.Size
import java.util.Collections

/** Достаёт обложку трека. Вызывать только из фонового потока. */
object ArtworkLoader {

    private const val MAX_SIDE = 1024
    private const val CACHE_BYTES = 12 * 1024 * 1024

    // Кеш последних обложек и список треков без обложки, чтобы не читать файл повторно.
    private val cache = object : LruCache<Long, Bitmap>(CACHE_BYTES) {
        override fun sizeOf(key: Long, value: Bitmap): Int = value.byteCount
    }
    private val withoutArtwork: MutableSet<Long> = Collections.synchronizedSet(HashSet())

    /** То же, что [load], но запоминает результат. Вызывать только из фонового потока. */
    fun loadCached(context: Context, trackId: Long): Bitmap? {
        cache.get(trackId)?.let { return it }
        if (trackId in withoutArtwork) return null

        val bitmap = load(context, trackId)
        if (bitmap != null) cache.put(trackId, bitmap) else withoutArtwork.add(trackId)
        return bitmap
    }

    fun load(context: Context, trackId: Long): Bitmap? {
        val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, trackId)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                return context.contentResolver.loadThumbnail(uri, Size(640, 640), null)
            } catch (e: Exception) {
                // Нет системной миниатюры: пробуем достать обложку из тегов файла.
            }
        }
        return loadEmbedded(context, uri)
    }

    private fun loadEmbedded(context: Context, uri: Uri): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val bytes = retriever.embeddedPicture ?: return null
            decodeScaled(bytes)
        } catch (e: Exception) {
            null
        } finally {
            retriever.release()
        }
    }

    private fun decodeScaled(bytes: ByteArray): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)

        var sample = 1
        while (bounds.outWidth / sample > MAX_SIDE || bounds.outHeight / sample > MAX_SIDE) {
            sample *= 2
        }

        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }
}
