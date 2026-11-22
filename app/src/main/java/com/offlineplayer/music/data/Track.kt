package com.offlineplayer.music.data

import android.net.Uri

data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val dateAddedSec: Long,
    val uri: Uri
) {
    /** Совпадение по названию, исполнителю или альбому (без учёта регистра). */
    fun matches(query: String): Boolean =
        title.contains(query, ignoreCase = true) ||
            artist.contains(query, ignoreCase = true) ||
            album.contains(query, ignoreCase = true)
}
