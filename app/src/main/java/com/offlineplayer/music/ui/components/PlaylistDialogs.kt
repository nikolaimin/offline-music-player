package com.offlineplayer.music.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offlineplayer.music.data.Track
import com.offlineplayer.music.ui.PlaylistUi
import com.offlineplayer.music.ui.theme.Accent
import com.offlineplayer.music.ui.theme.SecondaryText
import com.offlineplayer.music.ui.util.tracksCountText

internal val DialogColor = Color(0xFF1B1230)
private const val MaxNameLength = 40

@Composable
internal fun dialogFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedBorderColor = Accent,
    unfocusedBorderColor = SecondaryText,
    cursorColor = Accent
)

/** Диалог с полем названия: создание плейлиста или переименование. */
@Composable
fun PlaylistNameDialog(
    title: String,
    initialName: String,
    confirmText: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    val canSave = name.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DialogColor,
        title = { Text(title, color = Color.White) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(MaxNameLength) },
                singleLine = true,
                placeholder = { Text("Название плейлиста", color = SecondaryText) },
                colors = dialogFieldColors()
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = canSave) {
                Text(confirmText, color = if (canSave) Accent else SecondaryText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена", color = SecondaryText) }
        }
    )
}

/** Подтверждение необратимого действия (удаление плейлиста). */
@Composable
fun PlaylistConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DialogColor,
        title = { Text(title, color = Color.White) },
        text = { Text(message, color = SecondaryText) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmText, color = Accent) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена", color = SecondaryText) }
        }
    )
}

/** Выбор плейлистов для одного трека: нажатие добавляет трек в плейлист или убирает из него. */
@Composable
fun AddToPlaylistDialog(
    playlists: List<PlaylistUi>,
    trackId: Long,
    onToggle: (playlistId: Long) -> Unit,
    onCreateNew: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DialogColor,
        title = { Text("Добавить в плейлист", color = Color.White) },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                item(key = "new") {
                    DialogRow(
                        title = "＋ Новый плейлист",
                        subtitle = null,
                        mark = null,
                        highlight = true,
                        onClick = onCreateNew
                    )
                }
                items(playlists, key = { it.id }) { playlist ->
                    DialogRow(
                        title = playlist.name,
                        subtitle = tracksCountText(playlist.trackCount),
                        mark = if (trackId in playlist.trackIds) "✓" else null,
                        onClick = { onToggle(playlist.id) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Готово", color = Accent) }
        }
    )
}

/** Выбор треков для плейлиста: нажатие отмечает или снимает трек сразу, с поиском по списку. */
@Composable
fun PickTracksDialog(
    tracks: List<Track>,
    selectedIds: Set<Long>,
    onToggle: (trackId: Long) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val trimmed = query.trim()
    val shown = if (trimmed.isEmpty()) tracks else tracks.filter { it.matches(trimmed) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DialogColor,
        title = { Text("Добавить треки · ${selectedIds.size}", color = Color.White) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Поиск", color = SecondaryText) },
                    colors = dialogFieldColors()
                )
                Spacer(modifier = Modifier.padding(top = 8.dp))
                if (shown.isEmpty()) {
                    Text(
                        text = if (tracks.isEmpty()) "На телефоне нет треков" else "Ничего не найдено",
                        color = SecondaryText,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 340.dp)) {
                        items(shown, key = { it.id }) { track ->
                            DialogRow(
                                title = track.title,
                                subtitle = track.artist,
                                mark = if (track.id in selectedIds) "✓" else null,
                                onClick = { onToggle(track.id) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Готово", color = Accent) }
        }
    )
}

@Composable
private fun DialogRow(
    title: String,
    subtitle: String?,
    mark: String?,
    onClick: () -> Unit,
    highlight: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (highlight) Accent else Color.White,
                fontSize = 16.sp,
                fontWeight = if (highlight) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = SecondaryText,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (mark != null) {
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = mark, color = Accent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}
