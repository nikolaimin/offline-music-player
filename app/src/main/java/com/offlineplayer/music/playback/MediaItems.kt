package com.offlineplayer.music.playback

import android.content.ContentUris
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.offlineplayer.music.data.Track

private val AlbumArtBaseUri: Uri = Uri.parse("content://media/external/audio/albumart")

fun Track.toMediaItem(): MediaItem =
    MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .setArtworkUri(ContentUris.withAppendedId(AlbumArtBaseUri, albumId))
                .build()
        )
        .build()
