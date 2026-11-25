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

    // Маленькие обложки для строк списка: отдельный кеш, чтобы не держать в памяти крупные картинки.
    private const val THUMB_SIDE = 256
    private const val THUMB_CACHE_BYTES = 6 * 1024 * 1024
    private val thumbCache = object : LruCache<Long, Bitmap>(THUMB_CACHE_BYTES) {
        override fun sizeOf(key: Long, value: Bitmap): Int = value.byteCount
    }
    private val withoutThumb: MutableSet<Long> = Collections.synchronizedSet(HashSet())

    /** Уже загруженная маленькая обложка или null. Читает только кеш, можно из UI-потока. */
    fun cachedThumb(trackId: Long): Bitmap? = thumbCache.get(trackId)

    /** Известно, что у трека нет обложки. Читает только кеш, можно из UI-потока. */
    fun knownWithoutThumb(trackId: Long): Boolean = trackId in withoutThumb

    /** Маленькая обложка для списка с кешем. Вызывать только из фонового потока. */
    fun loadThumbCached(context: Context, trackId: Long): Bitmap? {
        thumbCache.get(trackId)?.let { return it }
        if (trackId in withoutThumb) return null

        val bitmap = load(context, trackId, THUMB_SIDE, THUMB_SIDE)
        if (bitmap != null) thumbCache.put(trackId, bitmap) else withoutThumb.add(trackId)
        return bitmap
    }

    fun load(context: Context, trackId: Long): Bitmap? = load(context, trackId, 640, MAX_SIDE)

    private fun load(context: Context, trackId: Long, thumbSide: Int, decodeSide: Int): Bitmap? {
        val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, trackId)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                return context.contentResolver.loadThumbnail(uri, Size(thumbSide, thumbSide), null)
            } catch (e: Exception) {
                // Нет системной миниатюры: пробуем достать обложку из тегов файла.
            }
        }
        return loadEmbedded(context, uri, decodeSide)
    }

    private fun loadEmbedded(context: Context, uri: Uri, maxSide: Int): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val bytes = retriever.embeddedPicture ?: return null
            decodeScaled(bytes, maxSide)
        } catch (e: Exception) {
            null
        } finally {
            retriever.release()
        }
    }

    private fun decodeScaled(bytes: ByteArray, maxSide: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)

        var sample = 1
        while (bounds.outWidth / sample > maxSide || bounds.outHeight / sample > maxSide) {
            sample *= 2
        }

        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }
}
