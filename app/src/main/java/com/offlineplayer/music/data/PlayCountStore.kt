package com.offlineplayer.music.data

import android.content.Context

// Не число, поэтому loadAll() эту запись пропускает.
private const val KeyLastPlayed = "last_played"

/**
 * Хранит на телефоне, сколько раз каждый трек был прослушан (id трека -> число).
 * Android сам такую статистику не ведёт, поэтому приложение считает её само.
 */
class PlayCountStore(context: Context) {

    private val prefs = context.getSharedPreferences("play_counts", Context.MODE_PRIVATE)

    fun loadAll(): Map<Long, Int> {
        val result = HashMap<Long, Int>()
        for ((key, value) in prefs.all) {
            val id = key.toLongOrNull() ?: continue
            val count = value as? Int ?: continue
            result[id] = count
        }
        return result
    }

    /** Id трека, который играл последним, или null, если ничего ещё не играло. */
    fun lastPlayedId(): Long? =
        prefs.getLong(KeyLastPlayed, -1L).takeIf { it >= 0L }

    fun setLastPlayed(trackId: Long) {
        prefs.edit().putLong(KeyLastPlayed, trackId).apply()
    }

    /** Забывает трек (например, после удаления файла): счётчик и отметку «играл последним». */
    fun forget(trackId: Long) {
        val editor = prefs.edit().remove(trackId.toString())
        if (lastPlayedId() == trackId) editor.remove(KeyLastPlayed)
        editor.apply()
    }

    /** Добавляет одно прослушивание и возвращает новое значение. */
    fun increment(trackId: Long): Int {
        val key = trackId.toString()
        val next = prefs.getInt(key, 0) + 1
        prefs.edit().putInt(key, next).apply()
        return next
    }
}
