package com.offlineplayer.music.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.offlineplayer.music.data.Track
import com.offlineplayer.music.ui.components.EmptyState
import com.offlineplayer.music.ui.components.FolderTiles
import com.offlineplayer.music.ui.components.LibraryFolder
import com.offlineplayer.music.ui.components.ListScrollbar
import com.offlineplayer.music.ui.components.SectionTitle
import com.offlineplayer.music.ui.components.TrackCard
import com.offlineplayer.music.ui.util.formatDuration

@Composable
fun LibraryScreen(
    title: String,
    tracks: List<Track>,
    isLoading: Boolean,
    hasQuery: Boolean,
    currentTrackId: Long?,
    onTrackClick: (Track) -> Unit,
    favoriteIds: Set<Long> = emptySet(),
    onToggleFavorite: (Long) -> Unit = {},
    isPlaying: Boolean = false,
    onAddToPlaylist: ((Long) -> Unit)? = null,
    recentCover: Track? = null,
    frequentCover: Track? = null,
    onOpenFolder: (LibraryFolder) -> Unit = {}
) {
    if (tracks.isNotEmpty()) {
        val listState = rememberLazyListState()

        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                // Справа оставляем место под ползунок, чтобы он не заходил на карточки.
                contentPadding = PaddingValues(top = 10.dp, end = 22.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Две папки рядом; при поиске скрыты.
                if (!hasQuery) {
                    item(key = "folders") {
                        FolderTiles(
                            recentCover = recentCover,
                            frequentCover = frequentCover,
                            onOpen = onOpenFolder
                        )
                    }
                }

                // Заголовок — часть списка: прокручивается вместе с музыкой.
                item(key = "header") {
                    SectionTitle(title)
                }

                items(tracks, key = { it.id }) { track ->
                    TrackCard(
                        trackId = track.id,
                        title = track.title,
                        subtitle = "${track.artist} • ${track.album}",
                        duration = formatDuration(track.durationMs),
                        isCurrent = track.id == currentTrackId,
                        isPlaying = isPlaying,
                        isFavorite = track.id in favoriteIds,
                        onToggleFavorite = { onToggleFavorite(track.id) },
                        onAddToPlaylist = onAddToPlaylist?.let { add -> { add(track.id) } },
                        onClick = { onTrackClick(track) }
                    )
                }
            }

            ListScrollbar(
                state = listState,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
    } else {
        Column {
            Spacer(modifier = Modifier.height(10.dp))
            SectionTitle(title)
            Spacer(modifier = Modifier.height(14.dp))

            when {
                isLoading -> EmptyState("Загрузка библиотеки...")
                hasQuery -> EmptyState("Ничего не найдено")
                else -> EmptyState("Музыка на телефоне не найдена")
            }
        }
    }
}
