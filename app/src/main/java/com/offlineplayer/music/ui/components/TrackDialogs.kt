package com.offlineplayer.music.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offlineplayer.music.data.Track
import com.offlineplayer.music.ui.theme.Accent
import com.offlineplayer.music.ui.theme.SecondaryText

private const val MaxTagLength = 100

/**
 * Правка названия, исполнителя и альбома трека. Данные хранятся в приложении, файл не меняется.
 * [isEdited] показывает кнопку возврата к исходным данным из файла.
 */
@Composable
fun TrackEditDialog(
    track: Track,
    isEdited: Boolean,
    onSave: (title: String, artist: String, album: String) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember(track.id) { mutableStateOf(track.title) }
    var artist by remember(track.id) { mutableStateOf(track.artist) }
    var album by remember(track.id) { mutableStateOf(track.album) }
    val canSave = title.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DialogColor,
        title = { Text("Изменить трек", color = Color.White) },
        text = {
            // Прокрутка нужна, когда экранная клавиатура закрывает часть окна.
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(MaxTagLength) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Название", color = SecondaryText) },
                    colors = dialogFieldColors()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it.take(MaxTagLength) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Исполнитель", color = SecondaryText) },
                    colors = dialogFieldColors()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = album,
                    onValueChange = { album = it.take(MaxTagLength) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Альбом", color = SecondaryText) },
                    colors = dialogFieldColors()
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Изменения хранятся в приложении, сам файл не меняется.",
                    color = SecondaryText,
                    fontSize = 12.sp
                )
                if (isEdited) {
                    TextButton(onClick = onReset) {
                        Text("Вернуть исходные данные", color = Accent)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(title, artist, album) }, enabled = canSave) {
                Text("Сохранить", color = if (canSave) Accent else SecondaryText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена", color = SecondaryText) }
        }
    )
}
