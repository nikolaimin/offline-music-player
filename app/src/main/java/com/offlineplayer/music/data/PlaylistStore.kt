package com.offlineplayer.music.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Плейлист пользователя: название и id треков в порядке добавления. */
data class Playlist(
    val id: Long,
    val name: String,
    val trackIds: List<Long>
)

private const val KeyPlaylists = "playlists_json"

/** Хранит плейлисты на телефоне (JSON в SharedPreferences). */
class PlaylistStore(context: Context) {

    private val prefs = context.getSharedPreferences("playlists", Context.MODE_PRIVATE)

    fun loadAll(): List<Playlist> {
        val raw = prefs.getString(KeyPlaylists, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map { index ->
                val item = array.getJSONObject(index)
                val ids = item.getJSONArray("tracks")
                Playlist(
                    id = item.getLong("id"),
                    name = item.getString("name"),
                    trackIds = (0 until ids.length()).map { ids.getLong(it) }
                )
            }
        } catch (e: Exception) {
            // Повреждённые данные не должны ронять приложение.
            emptyList()
        }
    }

    fun saveAll(playlists: List<Playlist>) {
        val array = JSONArray()
        playlists.forEach { playlist ->
            array.put(
                JSONObject()
                    .put("id", playlist.id)
                    .put("name", playlist.name)
                    .put("tracks", JSONArray(playlist.trackIds))
            )
        }
        prefs.edit().putString(KeyPlaylists, array.toString()).apply()
    }
}
