package com.offlineplayer.music.data

import android.content.Context
import org.json.JSONObject

/** Свои название, исполнитель и альбом трека, заданные пользователем в приложении. */
data class TrackEdit(
    val title: String,
    val artist: String,
    val album: String
)

private const val KeyEdits = "edits_json"

/**
 * Хранит правки треков на телефоне (JSON в SharedPreferences: id трека -> правка).
 * Аудиофайл при этом не меняется, поэтому правки переживают пересканирование памяти.
 */
class TrackEditStore(context: Context) {

    private val prefs = context.getSharedPreferences("track_edits", Context.MODE_PRIVATE)

    fun loadAll(): Map<Long, TrackEdit> {
        val raw = prefs.getString(KeyEdits, null) ?: return emptyMap()
        return try {
            val root = JSONObject(raw)
            val result = HashMap<Long, TrackEdit>()
            for (key in root.keys()) {
                val id = key.toLongOrNull() ?: continue
                val item = root.getJSONObject(key)
                result[id] = TrackEdit(
                    title = item.getString("title"),
                    artist = item.getString("artist"),
                    album = item.getString("album")
                )
            }
            result
        } catch (e: Exception) {
            // Повреждённые данные не должны ронять приложение.
            emptyMap()
        }
    }

    fun saveAll(edits: Map<Long, TrackEdit>) {
        val root = JSONObject()
        edits.forEach { (id, edit) ->
            root.put(
                id.toString(),
                JSONObject()
                    .put("title", edit.title)
                    .put("artist", edit.artist)
                    .put("album", edit.album)
            )
        }
        prefs.edit().putString(KeyEdits, root.toString()).apply()
    }
}
