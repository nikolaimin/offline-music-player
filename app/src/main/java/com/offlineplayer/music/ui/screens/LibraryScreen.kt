package com.offlineplayer.music.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.offlineplayer.music.ui.components.LibraryItem

// Временные данные-заглушки. Будут заменены реальными треками из MediaStore.
private val placeholders = listOf(
    "Музыкальная библиотека" to "Ваши локальные композиции",
    "Недавно добавленные" to "Новые треки",
    "Исполнители" to "Все исполнители",
    "Альбомы" to "Музыка по альбомам",
    "Папки" to "Обзор файлов"
)

@Composable
fun LibraryScreen() {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(placeholders) { (title, subtitle) ->
            LibraryItem(title = title, subtitle = subtitle)
        }
    }
}
