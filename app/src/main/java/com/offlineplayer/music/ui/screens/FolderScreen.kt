package com.offlineplayer.music.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offlineplayer.music.data.Track
import com.offlineplayer.music.ui.components.EmptyState
import com.offlineplayer.music.ui.components.ListScrollbar
import com.offlineplayer.music.ui.components.SectionTitle
import com.offlineplayer.music.ui.components.TrackCard
import com.offlineplayer.music.ui.util.formatDuration

/**
 * Список треков с заголовком: содержимое папки («Недавно добавленные», «Часто прослушиваемые»)
 * со стрелкой «назад» или, если [onBack] не задан, вкладка «Избранное».
 */
@Composable
fun FolderScreen(
    title: String,
    tracks: List<Track>,
    emptyText: String,
    currentTrackId: Long?,
    onTrackClick: (Track) -> Unit,
    favoriteIds: Set<Long> = emptySet(),
    onToggleFavorite: (Long) -> Unit = {},
    isPlaying: Boolean = false,
    onAddToPlaylist: ((Long) -> Unit)? = null,
    onRemoveFromPlaylist: ((Long) -> Unit)? = null,
    onEditTrack: ((Track) -> Unit)? = null,
    onDeleteTrack: ((Track) -> Unit)? = null,
    headerAction: (@Composable () -> Unit)? = null,
    onBack: (() -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Text("‹", color = Color.White, fontSize = 30.sp)
                }
                Spacer(modifier = Modifier.width(4.dp))
            }
            SectionTitle("$title · ${tracks.size}", modifier = Modifier.weight(1f, fill = false))
            if (headerAction != null) {
                Spacer(modifier = Modifier.weight(1f))
                headerAction()
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (tracks.isEmpty()) {
            EmptyState(emptyText)
        } else {
            val listState = rememberLazyListState()

            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    // Справа оставляем место под ползунок.
                    contentPadding = PaddingValues(end = 22.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
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
                            onRemoveFromPlaylist = onRemoveFromPlaylist?.let { remove -> { remove(track.id) } },
                            onEdit = onEditTrack?.let { edit -> { edit(track) } },
                            onDelete = onDeleteTrack?.let { delete -> { delete(track) } },
                            onClick = { onTrackClick(track) }
                        )
                    }
                }

                ListScrollbar(
                    state = listState,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }
        }
    }
}
