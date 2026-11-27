package com.offlineplayer.music.ui.profile

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val Minute = 60_000L
private const val Hour = 60 * Minute
private const val Day = 24 * Hour

/** «только что», «5 мин назад», «3 ч назад», «вчера», затем дата вида «27 нояб.». */
fun formatPlayedAt(playedAtMs: Long, nowMs: Long = System.currentTimeMillis()): String {
    val diff = (nowMs - playedAtMs).coerceAtLeast(0L)
    return when {
        diff < Minute -> "только что"
        diff < Hour -> "${diff / Minute} мин назад"
        diff < Day -> "${diff / Hour} ч назад"
        diff < 2 * Day -> "вчера"
        else -> SimpleDateFormat("d MMM", Locale("ru")).format(Date(playedAtMs))
    }
}

/** 1 -> «раз», 2 -> «раза», 5 -> «раз»: для подписей вида «12 раз». */
fun timesText(count: Int): String {
    val lastTwo = count % 100
    val last = count % 10
    val word = when {
        lastTwo in 11..14 -> "раз"
        last in 2..4 -> "раза"
        else -> "раз"
    }
    return "$count $word"
}
