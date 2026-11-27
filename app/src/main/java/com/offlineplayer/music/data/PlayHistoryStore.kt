package com.offlineplayer.music.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Одна запись истории: какой трек и когда был прослушан. */
data class PlayHistoryEntry(val trackId: Long, val playedAtMs: Long)

private const val KeyHistory = "history_json"
private const val KeyTotalPlays = "total_plays"

/** Сколько последних прослушиваний хранится: история не растёт бесконечно. */
const val MaxHistoryEntries = 50

/**
 * Хранит на телефоне историю прослушиваний (последние [MaxHistoryEntries]) и общее число прослушиваний.
 * Общее число ведётся отдельно от счётчиков треков: оно не уменьшается, когда трек удаляют.
 */
class PlayHistoryStore(private val context: Context) {

    private val prefs = context.getSharedPreferences("play_history", Context.MODE_PRIVATE)

    /** История от новых записей к старым. */
    fun loadAll(): List<PlayHistoryEntry> {
        val raw = prefs.getString(KeyHistory, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map { index ->
                val item = array.getJSONObject(index)
                PlayHistoryEntry(item.getLong("id"), item.getLong("t"))
            }
        } catch (e: Exception) {
            // Повреждённые данные не должны ронять приложение.
            emptyList()
        }
    }

    /**
     * Общее число прослушиваний. При первом обращении берётся сумма уже накопленных счётчиков треков,
     * чтобы у тех, кто слушал до появления профиля, число не началось с нуля.
     */
    fun totalPlays(): Int {
        if (!prefs.contains(KeyTotalPlays)) {
            val existing = PlayCountStore(context).loadAll().values.sum()
            prefs.edit().putInt(KeyTotalPlays, existing).apply()
            return existing
        }
        return prefs.getInt(KeyTotalPlays, 0)
    }

    /** Записывает одно прослушивание: увеличивает общее число и добавляет запись в историю. */
    fun recordPlay(trackId: Long, playedAtMs: Long) {
        val total = totalPlays() + 1
        val updated = (listOf(PlayHistoryEntry(trackId, playedAtMs)) + loadAll()).take(MaxHistoryEntries)
        prefs.edit()
            .putInt(KeyTotalPlays, total)
            .putString(KeyHistory, encode(updated))
            .apply()
    }

    /** Убирает трек из истории (например, после удаления файла). Общее число не меняется. */
    fun forgetTrack(trackId: Long) {
        val current = loadAll()
        val updated = current.filterNot { it.trackId == trackId }
        if (updated.size != current.size) {
            prefs.edit().putString(KeyHistory, encode(updated)).apply()
        }
    }

    private fun encode(entries: List<PlayHistoryEntry>): String {
        val array = JSONArray()
        entries.forEach { array.put(JSONObject().put("id", it.trackId).put("t", it.playedAtMs)) }
        return array.toString()
    }
}
