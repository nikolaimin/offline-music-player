package com.offlineplayer.music.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offlineplayer.music.ui.PlaylistUi
import com.offlineplayer.music.ui.components.EmptyState
import com.offlineplayer.music.ui.components.ListScrollbar
import com.offlineplayer.music.ui.components.MoreDots
import com.offlineplayer.music.ui.components.SectionTitle
import com.offlineplayer.music.ui.components.TrackCover
import com.offlineplayer.music.ui.components.drawCardBackground
import com.offlineplayer.music.ui.theme.Accent
import com.offlineplayer.music.ui.theme.SecondaryText
import com.offlineplayer.music.ui.theme.TrackTitleFont
import com.offlineplayer.music.ui.util.tracksCountText

private val MenuColor = Color(0xFF1B1230)

/** Вкладка «Плейлисты»: список плейлистов с кнопкой создания. */
@Composable
fun PlaylistsScreen(
    playlists: List<PlaylistUi>,
    onCreate: () -> Unit,
    onOpen: (Long) -> Unit,
    onRename: (PlaylistUi) -> Unit,
    onDelete: (PlaylistUi) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionTitle("Ваши подборки · ${playlists.size}")
            Spacer(modifier = Modifier.weight(1f))
            HeaderPillButton(text = "＋ Создать", onClick = onCreate)
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (playlists.isEmpty()) {
            EmptyState("Плейлистов пока нет. Нажмите «＋ Создать»")
        } else {
            val listState = rememberLazyListState()

            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    // Справа оставляем место под ползунок.
                    contentPadding = PaddingValues(end = 22.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(playlists, key = { it.id }) { playlist ->
                        PlaylistCard(
                            playlist = playlist,
                            onClick = { onOpen(playlist.id) },
                            onRename = { onRename(playlist) },
                            onDelete = { onDelete(playlist) }
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

/** Небольшая кнопка-«таблетка» в заголовке списка. */
@Composable
fun HeaderPillButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Accent.copy(alpha = 0.16f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = Accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Карточка плейлиста в стиле карточек треков: обложка, название, число треков, меню. */
@Composable
private fun PlaylistCard(
    playlist: PlaylistUi,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "playlistScale"
    )
    var menuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .drawBehind { drawCardBackground(0f) }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Обложка первого трека; у пустого плейлиста рисуется абстрактная фиолетовая.
        TrackCover(
            trackId = playlist.coverTrackId ?: playlist.id,
            size = 68.dp,
            showArtwork = playlist.coverTrackId != null
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.name,
                color = Color.White,
                fontFamily = TrackTitleFont,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = tracksCountText(playlist.trackCount),
                color = SecondaryText,
                fontSize = 12.sp
            )
        }

        Box {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable { menuOpen = true },
                contentAlignment = Alignment.Center
            ) {
                MoreDots(color = SecondaryText)
            }

            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
                shape = RoundedCornerShape(16.dp),
                containerColor = MenuColor
            ) {
                DropdownMenuItem(
                    text = { Text("Переименовать", color = Color.White) },
                    leadingIcon = { Text("✎", color = SecondaryText, fontSize = 18.sp) },
                    onClick = {
                        menuOpen = false
                        onRename()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Удалить", color = Color.White) },
                    leadingIcon = { Text("✕", color = SecondaryText, fontSize = 18.sp) },
                    onClick = {
                        menuOpen = false
                        onDelete()
                    }
                )
            }
        }
    }
}
