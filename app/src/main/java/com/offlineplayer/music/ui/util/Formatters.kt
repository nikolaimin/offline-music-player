package com.offlineplayer.music.ui.util

import java.util.Locale

/** 215000 мс -> "3:35" */
fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
}

/** 1 -> "1 трек", 3 -> "3 трека", 5 -> "5 треков" */
fun tracksCountText(count: Int): String {
    val lastTwo = count % 100
    val last = count % 10
    val word = when {
        lastTwo in 11..14 -> "треков"
        last == 1 -> "трек"
        last in 2..4 -> "трека"
        else -> "треков"
    }
    return "$count $word"
}
