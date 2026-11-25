package com.offlineplayer.music.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.offlineplayer.music.data.Track
import com.offlineplayer.music.ui.components.LibraryFolder
import com.offlineplayer.music.ui.components.AddToPlaylistDialog
import com.offlineplayer.music.ui.components.MiniPlayer
import com.offlineplayer.music.ui.components.PickTracksDialog
import com.offlineplayer.music.ui.components.PlaylistConfirmDialog
import com.offlineplayer.music.ui.components.PlaylistNameDialog
import com.offlineplayer.music.ui.components.SearchField
import com.offlineplayer.music.ui.components.SectionTitle
import com.offlineplayer.music.ui.components.appBackground
import com.offlineplayer.music.ui.screens.FolderScreen
import com.offlineplayer.music.ui.screens.HeaderPillButton
import com.offlineplayer.music.ui.screens.LibraryScreen
import com.offlineplayer.music.ui.screens.NowPlayingScreen
import com.offlineplayer.music.ui.screens.PermissionScreen
import com.offlineplayer.music.ui.screens.PlaylistsScreen
import com.offlineplayer.music.ui.theme.Accent
import com.offlineplayer.music.ui.theme.Background
import com.offlineplayer.music.ui.theme.CardColor

/** Длительность анимации перехода между вкладками. */
private const val TabAnimationMs = 300

private data class Tab(val title: String, val icon: String)

/** Для чего открыто окно с названием плейлиста. */
private sealed interface PlaylistNameTarget {
    /** Новый плейлист; [firstTrackId] сразу попадёт в него (создание из меню трека). */
    data class Create(val firstTrackId: Long?) : PlaylistNameTarget
    data class Rename(val playlist: PlaylistUi) : PlaylistNameTarget
}

private val tabs = listOf(
    Tab("Музыка", "♫"),
    Tab("Плейлисты", "☷"),
    Tab("Избранное", "♥")
)

@Composable
fun MusicPlayerApp(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val queueState by viewModel.queueState.collectAsStateWithLifecycle()

    var showPlayer by rememberSaveable { mutableStateOf(false) }
    var openFolder by rememberSaveable { mutableStateOf<LibraryFolder?>(null) }
    var openPlaylistId by rememberSaveable { mutableStateOf<Long?>(null) }
    // Трек, для которого выбирают плейлист («Добавить в плейлист»).
    var trackForPlaylist by remember { mutableStateOf<Long?>(null) }
    var nameDialog by remember { mutableStateOf<PlaylistNameTarget?>(null) }
    var deleteTarget by remember { mutableStateOf<PlaylistUi?>(null) }
    var pickingTracks by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchText by remember { mutableStateOf("") }
    var hasPermission by remember { mutableStateOf(hasAudioPermission(context)) }
    var askedForNotifications by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) viewModel.loadTracks()
    }

    // Запуск трека, который ждёт ответа на запрос разрешения уведомлений.
    var pendingStart by remember { mutableStateOf<(() -> Unit)?>(null) }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        // Музыка играет и без уведомления, поэтому результат не важен:
        // главное, что пользователь ответил, и можно начинать воспроизведение.
        pendingStart?.invoke()
        pendingStart = null
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

    // Android 13+: без этого разрешения уведомление с кнопками не покажется.
    // Если запрос нужен, музыка стартует только после ответа на него, иначе сразу.
    val startAfterNotificationPrompt: (() -> Unit) -> Unit = { start ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !askedForNotifications &&
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            askedForNotifications = true
            pendingStart = start
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            start()
        }
    }

    val onTrackClick: (Track) -> Unit = { track ->
        startAfterNotificationPrompt { viewModel.playTrack(track) }
    }

    val playerVisible = showPlayer && playerState.mediaId != null

    // Кнопка «Назад» закрывает экран плеера, а не приложение.
    BackHandler(enabled = playerVisible) { showPlayer = false }

    // Если открыта папка, «Назад» возвращает к библиотеке.
    val folderVisible = openFolder != null && !uiState.hasQuery
    BackHandler(enabled = folderVisible && !playerVisible) { openFolder = null }

    // Если открыт плейлист (его могли удалить), «Назад» возвращает к списку плейлистов.
    val openPlaylist = uiState.playlists.firstOrNull { it.id == openPlaylistId }
    BackHandler(enabled = openPlaylist != null && selectedTab == 1 && !playerVisible) {
        openPlaylistId = null
    }

    Box(modifier = Modifier.fillMaxSize()) {

    Scaffold(
        containerColor = Background,
        bottomBar = {
            Column {
                if (playerState.mediaId != null) {
                    MiniPlayer(
                        state = playerState,
                        onTogglePlay = viewModel::togglePlayPause,
                        onNext = viewModel::skipNext,
                        onOpen = { showPlayer = true }
                    )
                }
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
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .appBackground()
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

            // Отступ под поиском. Вместе с отступом внутри списка даёт прежний
            // зазор до заголовка, а при прокрутке треки уходят под строку поиска.
            Spacer(modifier = Modifier.height(16.dp))

            AnimatedContent(
                targetState = selectedTab,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    // Вкладка справа от текущей въезжает справа, слева: слева.
                    val direction = if (targetState > initialState) {
                        AnimatedContentTransitionScope.SlideDirection.Left
                    } else {
                        AnimatedContentTransitionScope.SlideDirection.Right
                    }
                    (slideIntoContainer(direction, tween(TabAnimationMs)) +
                        fadeIn(tween(TabAnimationMs))) togetherWith
                        (slideOutOfContainer(direction, tween(TabAnimationMs)) +
                            fadeOut(tween(TabAnimationMs)))
                },
                label = "tabContent"
            ) { tab ->
                when (tab) {
                    0 -> if (hasPermission) {
                        val folder = openFolder
                        if (folder != null && folderVisible) {
                            val folderTracks = when (folder) {
                                LibraryFolder.Recent -> uiState.recent
                                LibraryFolder.Frequent -> uiState.frequent
                            }
                            FolderScreen(
                                title = folder.title,
                                tracks = folderTracks,
                                emptyText = when (folder) {
                                    LibraryFolder.Recent -> "Нет добавленных треков"
                                    LibraryFolder.Frequent -> "Пока нет прослушанных треков"
                                },
                                currentTrackId = playerState.mediaId?.toLongOrNull(),
                                isPlaying = playerState.isPlaying,
                                favoriteIds = uiState.favoriteIds,
                                onToggleFavorite = viewModel::toggleFavorite,
                                onAddToPlaylist = { trackForPlaylist = it },
                                onBack = { openFolder = null },
                                onTrackClick = { track ->
                                    // Очередь = содержимое папки.
                                    startAfterNotificationPrompt {
                                        viewModel.playTrackFrom(folderTracks, track)
                                    }
                                }
                            )
                        } else {
                            // Заголовок «Ваша библиотека» лежит внутри списка и прокручивается с музыкой.
                            LibraryScreen(
                                title = "Ваша библиотека · ${uiState.totalCount}",
                                tracks = uiState.tracks,
                                isLoading = uiState.isLoading,
                                hasQuery = uiState.hasQuery,
                                currentTrackId = playerState.mediaId?.toLongOrNull(),
                                isPlaying = playerState.isPlaying,
                                onTrackClick = onTrackClick,
                                favoriteIds = uiState.favoriteIds,
                                onToggleFavorite = viewModel::toggleFavorite,
                                onAddToPlaylist = { trackForPlaylist = it },
                                recentCover = uiState.recent.firstOrNull(),
                                frequentCover = uiState.lastPlayed,
                                onOpenFolder = { openFolder = it }
                            )
                        }
                    } else {
                        Column {
                            Spacer(modifier = Modifier.height(10.dp))
                            SectionTitle("Ваша библиотека")
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
                    }

                    1 -> {
                        val playlist = openPlaylist
                        if (playlist != null) {
                            FolderScreen(
                                title = playlist.name,
                                tracks = playlist.tracks,
                                emptyText = if (uiState.hasQuery) {
                                    "Ничего не найдено"
                                } else {
                                    "Плейлист пуст. Добавьте треки кнопкой «＋ Треки»"
                                },
                                currentTrackId = playerState.mediaId?.toLongOrNull(),
                                isPlaying = playerState.isPlaying,
                                favoriteIds = uiState.favoriteIds,
                                onToggleFavorite = viewModel::toggleFavorite,
                                onAddToPlaylist = { trackForPlaylist = it },
                                onRemoveFromPlaylist = { viewModel.removeFromPlaylist(playlist.id, it) },
                                headerAction = {
                                    HeaderPillButton(text = "＋ Треки", onClick = { pickingTracks = true })
                                },
                                onBack = { openPlaylistId = null },
                                onTrackClick = { track ->
                                    // Очередь = содержимое плейлиста.
                                    startAfterNotificationPrompt {
                                        viewModel.playTrackFrom(playlist.tracks, track)
                                    }
                                }
                            )
                        } else {
                            PlaylistsScreen(
                                playlists = uiState.playlists,
                                onCreate = { nameDialog = PlaylistNameTarget.Create(null) },
                                onOpen = { openPlaylistId = it },
                                onRename = { nameDialog = PlaylistNameTarget.Rename(it) },
                                onDelete = { deleteTarget = it }
                            )
                        }
                    }

                    else -> FolderScreen(
                        title = "Любимые композиции",
                        tracks = uiState.favorites,
                        emptyText = if (uiState.hasQuery) {
                            "Ничего не найдено"
                        } else {
                            "Нажимайте ♥ у треков, чтобы добавить их сюда"
                        },
                        currentTrackId = playerState.mediaId?.toLongOrNull(),
                        isPlaying = playerState.isPlaying,
                        favoriteIds = uiState.favoriteIds,
                        onToggleFavorite = viewModel::toggleFavorite,
                        onAddToPlaylist = { trackForPlaylist = it },
                        onTrackClick = { track ->
                            // Очередь = избранное.
                            startAfterNotificationPrompt {
                                viewModel.playTrackFrom(uiState.favorites, track)
                            }
                        }
                    )
                }
            }
        }
    }

    AnimatedVisibility(
        visible = playerVisible,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it })
    ) {
        NowPlayingScreen(
            state = playerState,
            queue = queueState,
            getPositionMs = viewModel::positionMs,
            onClose = { showPlayer = false },
            onTogglePlay = viewModel::togglePlayPause,
            onNext = viewModel::skipNext,
            onPrevious = viewModel::skipPrevious,
            onSeek = viewModel::seekTo,
            onToggleShuffle = viewModel::toggleShuffle,
            onCycleRepeat = viewModel::cycleRepeat,
            onQueueItemClick = viewModel::playQueueItem,
            isFavorite = playerState.mediaId?.toLongOrNull()?.let { it in uiState.favoriteIds } == true,
            onToggleFavorite = {
                playerState.mediaId?.toLongOrNull()?.let(viewModel::toggleFavorite)
            }
        )
    }

    // Окна работы с плейлистами.
    nameDialog?.let { target ->
        val renaming = target as? PlaylistNameTarget.Rename
        PlaylistNameDialog(
            title = if (renaming != null) "Переименовать плейлист" else "Новый плейлист",
            initialName = renaming?.playlist?.name.orEmpty(),
            confirmText = if (renaming != null) "Сохранить" else "Создать",
            onConfirm = { name ->
                when (target) {
                    is PlaylistNameTarget.Rename -> viewModel.renamePlaylist(target.playlist.id, name)
                    is PlaylistNameTarget.Create -> viewModel.createPlaylist(name, target.firstTrackId)
                }
                nameDialog = null
            },
            onDismiss = { nameDialog = null }
        )
    }

    deleteTarget?.let { target ->
        PlaylistConfirmDialog(
            title = "Удалить плейлист?",
            message = "Плейлист «${target.name}» будет удалён. Сами треки останутся на телефоне.",
            confirmText = "Удалить",
            onConfirm = {
                viewModel.deletePlaylist(target.id)
                if (openPlaylistId == target.id) openPlaylistId = null
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null }
        )
    }

    trackForPlaylist?.let { trackId ->
        AddToPlaylistDialog(
            playlists = uiState.playlists,
            trackId = trackId,
            onToggle = { playlistId -> viewModel.togglePlaylistTrack(playlistId, trackId) },
            onCreateNew = {
                trackForPlaylist = null
                nameDialog = PlaylistNameTarget.Create(trackId)
            },
            onDismiss = { trackForPlaylist = null }
        )
    }

    if (pickingTracks && openPlaylist != null) {
        PickTracksDialog(
            tracks = uiState.library,
            selectedIds = openPlaylist.trackIds,
            onToggle = { trackId -> viewModel.togglePlaylistTrack(openPlaylist.id, trackId) },
            onDismiss = { pickingTracks = false }
        )
    }

    }
}
