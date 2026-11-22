
package com.offlineplayer.music

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Background = Color(0xFF101014)
private val CardColor = Color(0xFF1D1D25)
private val Accent = Color(0xFFB69CFF)
private val SecondaryText = Color(0xFF9999A8)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MusicPlayerApp()
        }
    }
}

@Composable
fun MusicPlayerApp() {
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchText by remember { mutableStateOf("") }

    val tabs = listOf("Музыка", "Плейлисты", "Избранное")

    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Background,
            surface = CardColor,
            primary = Accent
        )
    ) {
        Scaffold(
            containerColor = Background,
            bottomBar = {
                NavigationBar(
                    containerColor = CardColor
                ) {
                    tabs.forEachIndexed { index, title ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = {
                                Text(
                                    listOf("♫", "☷", "♥")[index],
                                    fontSize = 20.sp
                                )
                            },
                            label = { Text(title) }
                        )
                    }
                }
            }
        ) { padding ->

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "OFFLINE PLAYER",
                    color = Accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = tabs[selectedTab],
                    color = Color.White,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text("Поиск музыки...")
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Accent,
                        unfocusedBorderColor = CardColor,
                        focusedContainerColor = CardColor,
                        unfocusedContainerColor = CardColor
                    )
                )

                Spacer(modifier = Modifier.height(26.dp))

                Text(
                    text = when (selectedTab) {
                        0 -> "Ваша библиотека"
                        1 -> "Ваши подборки"
                        else -> "Любимые композиции"
                    },
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(14.dp))

                if (selectedTab == 0) {
                    MusicLibrary()
                } else {
                    EmptyLibrary(
                        text = if (selectedTab == 1)
                            "Здесь будут ваши плейлисты"
                        else
                            "Добавляйте любимые композиции"
                    )
                }
            }
        }
    }
}

@Composable
fun MusicLibrary() {
    val songs = listOf(
        "Музыкальная библиотека" to "Ваши локальные композиции",
        "Недавно добавленные" to "Новые треки",
        "Исполнители" to "Все исполнители",
        "Альбомы" to "Музыка по альбомам",
        "Папки" to "Обзор файлов"
    )

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(songs) { song ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        CardColor,
                        RoundedCornerShape(16.dp)
                    )
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(
                            Color(0xFF302A40),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "♫",
                        color = Accent,
                        fontSize = 24.sp
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        song.first,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                    Text(
                        song.second,
                        color = SecondaryText,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyLibrary(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = SecondaryText,
            fontSize = 14.sp
        )
    }
}

