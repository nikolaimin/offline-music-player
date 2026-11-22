package com.offlineplayer.music.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.offlineplayer.music.ui.components.EmptyState
import com.offlineplayer.music.ui.components.SearchField
import com.offlineplayer.music.ui.screens.LibraryScreen
import com.offlineplayer.music.ui.screens.PermissionScreen
import com.offlineplayer.music.ui.theme.Accent
import com.offlineplayer.music.ui.theme.Background
import com.offlineplayer.music.ui.theme.CardColor

private data class Tab(val title: String, val icon: String)

private val tabs = listOf(
    Tab("Музыка", "♫"),
    Tab("Плейлисты", "☷"),
    Tab("Избранное", "♥")
)

@Composable
fun MusicPlayerApp(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    var searchText by remember { mutableStateOf("") }
    var hasPermission by remember { mutableStateOf(hasAudioPermission(context)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) viewModel.loadTracks()
    }

    // При каждом возвращении в приложение: перепроверяем доступ и обновляем библиотеку.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        hasPermission = hasAudioPermission(context)
        if (hasPermission) viewModel.loadTracks()
    }

    // Первый запуск: сразу просим доступ к музыке.
    LaunchedEffect(Unit) {
        if (!hasAudioPermission(context)) permissionLauncher.launch(AudioPermission)
    }

    Scaffold(
        containerColor = Background,
        bottomBar = {
            NavigationBar(containerColor = CardColor) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Text(tab.icon, fontSize = 20.sp) },
                        label = { Text(tab.title) }
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
                text = tabs[selectedTab].title,
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(20.dp))

            SearchField(
                value = searchText,
                onValueChange = {
                    searchText = it
                    viewModel.onQueryChange(it)
                }
            )

            Spacer(modifier = Modifier.height(26.dp))

            Text(
                text = when (selectedTab) {
                    0 -> if (hasPermission) "Ваша библиотека · ${uiState.totalCount}" else "Ваша библиотека"
                    1 -> "Ваши подборки"
                    else -> "Любимые композиции"
                },
                color = Color.White,
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(14.dp))

            when (selectedTab) {
                0 -> if (hasPermission) {
                    LibraryScreen(
                        tracks = uiState.tracks,
                        isLoading = uiState.isLoading,
                        hasQuery = uiState.hasQuery
                    )
                } else {
                    PermissionScreen(
                        onRequest = { permissionLauncher.launch(AudioPermission) },
                        onOpenSettings = {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", context.packageName, null)
                                )
                            )
                        }
                    )
                }

                1 -> EmptyState("Здесь будут ваши плейлисты")
                else -> EmptyState("Добавляйте любимые композиции")
            }
        }
    }
}
