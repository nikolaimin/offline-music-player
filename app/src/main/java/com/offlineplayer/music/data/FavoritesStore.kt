package com.offlineplayer.music.data

import android.content.Context

private const val KeyFavorites = "favorite_ids"

/** Хранит на телефоне, какие треки добавлены в избранное (набор id треков). */
class FavoritesStore(context: Context) {

    private val prefs = context.getSharedPreferences("favorites", Context.MODE_PRIVATE)

    fun loadAll(): Set<Long> =
        prefs.getStringSet(KeyFavorites, emptySet())
            .orEmpty()
            .mapNotNull { it.toLongOrNull() }
            .toSet()

    /** Добавляет трек в избранное или убирает его и возвращает новый набор. */
    fun setFavorite(trackId: Long, favorite: Boolean): Set<Long> {
        val updated = loadAll().toMutableSet()
        if (favorite) updated.add(trackId) else updated.remove(trackId)
        // В SharedPreferences нужно сохранять новый набор, а не менять прочитанный.
        prefs.edit().putStringSet(KeyFavorites, updated.map { it.toString() }.toSet()).apply()
        return updated
    }
}
