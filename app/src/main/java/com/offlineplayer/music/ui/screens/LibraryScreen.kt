package com.offlineplayer.music.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.offlineplayer.music.data.Track
import com.offlineplayer.music.ui.components.EmptyState
import com.offlineplayer.music.ui.components.LibraryItem
import com.offlineplayer.music.ui.util.formatDuration

@Composable
fun LibraryScreen(
    tracks: List<Track>,
    isLoading: Boolean,
    hasQuery: Boolean
) {
    when {
        tracks.isNotEmpty() -> LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(tracks, key = { it.id }) { track ->
                LibraryItem(
                    title = track.title,
                    subtitle = "${track.artist} • ${track.album}",
                    trailing = formatDuration(track.durationMs)
                )
            }
        }

        isLoading -> EmptyState("Загрузка библиотеки...")
        hasQuery -> EmptyState("Ничего не найдено")
        else -> EmptyState("Музыка на телефоне не найдена")
    }
}
