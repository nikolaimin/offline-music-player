package com.offlineplayer.music.data

import android.content.Context

/** Хранит на телефоне, какие достижения получены и когда (id достижения -> время в мс). */
class AchievementStore(context: Context) {

    private val prefs = context.getSharedPreferences("achievements", Context.MODE_PRIVATE)

    fun loadAll(): Map<String, Long> {
        val result = HashMap<String, Long>()
        for ((key, value) in prefs.all) {
            val time = value as? Long ?: continue
            result[key] = time
        }
        return result
    }

    /** Отмечает достижения полученными (уже полученные не трогает) и возвращает полный набор. */
    fun unlock(ids: Collection<String>, timeMs: Long): Map<String, Long> {
        val editor = prefs.edit()
        ids.filterNot { prefs.contains(it) }.forEach { editor.putLong(it, timeMs) }
        editor.apply()
        return loadAll()
    }
}
