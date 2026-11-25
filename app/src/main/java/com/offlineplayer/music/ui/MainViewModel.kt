package com.offlineplayer.music.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.offlineplayer.music.data.FavoritesStore
import com.offlineplayer.music.data.MusicRepository
import com.offlineplayer.music.data.PlayCountStore
import com.offlineplayer.music.data.Playlist
import com.offlineplayer.music.data.PlaylistStore
import com.offlineplayer.music.data.Track
import com.offlineplayer.music.playback.PlayerConnection
import com.offlineplayer.music.playback.PlayerState
import com.offlineplayer.music.playback.QueueState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Сколько треков хранят папки «Недавно добавленные» и «Часто прослушиваемые». */
private const val FolderLimit = 100

/** Сколько миллисекунд трек должен играть, чтобы считаться прослушанным. */
private const val ListenThresholdMs = 10_000L

/**
 * Для коротких треков (голосовые сообщения и т.п.) порог ниже: достаточно проиграть половину,
 * иначе такой трек закончится раньше порога и никогда не попадёт в «Часто прослушиваемые».
 */
private fun listenThresholdMs(durationMs: Long): Long =
    if (durationMs > 0L) minOf(ListenThresholdMs, maxOf(1_000L, durationMs / 2)) else ListenThresholdMs

/** Плейлист для показа на экране. */
data class PlaylistUi(
    val id: Long,
    val name: String,
    /** Сколько треков плейлиста сейчас есть на телефоне. */
    val trackCount: Int,
    /** Треки плейлиста в порядке добавления (при поиске фильтруются как библиотека). */
    val tracks: List<Track>,
    /** id всех существующих треков плейлиста (для отметок в диалогах). */
    val trackIds: Set<Long>,
    /** Трек, обложка которого служит обложкой плейлиста (первый существующий). */
    val coverTrackId: Long?
)

private const val MaxPlaylistNameLength = 40

data class LibraryUiState(
    val tracks: List<Track> = emptyList(),
    val totalCount: Int = 0,
    val hasQuery: Boolean = false,
    val isLoading: Boolean = false,
    val recent: List<Track> = emptyList(),
    val frequent: List<Track> = emptyList(),
    /** Трек, который играл последним (обложка папки «Часто прослушиваемые»). */
    val lastPlayed: Track? = null,
    /** Избранные треки (при поиске фильтруются так же, как библиотека). */
    val favorites: List<Track> = emptyList(),
    val favoriteIds: Set<Long> = emptySet(),
    val playlists: List<PlaylistUi> = emptyList(),
    /** Вся библиотека без учёта поиска (выбор треков для плейлиста). */
    val library: List<Track> = emptyList()
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MusicRepository(application.applicationContext)
    private val playerConnection = PlayerConnection(application.applicationContext)
    private val playCountStore = PlayCountStore(application.applicationContext)
    private val playCounts = MutableStateFlow(playCountStore.loadAll())
    private val lastPlayedId = MutableStateFlow(playCountStore.lastPlayedId())
    private val favoritesStore = FavoritesStore(application.applicationContext)
    private val favoriteIds = MutableStateFlow(favoritesStore.loadAll())
    private val playlistStore = PlaylistStore(application.applicationContext)
    private val playlists = MutableStateFlow(playlistStore.loadAll())

    private val allTracks = MutableStateFlow<List<Track>>(emptyList())
    private val query = MutableStateFlow("")
    private val isLoading = MutableStateFlow(false)
    private var loadJob: Job? = null

    val playerState: StateFlow<PlayerState> = playerConnection.state
    val queueState: StateFlow<QueueState> = playerConnection.queue

    private class Extras(
        val counts: Map<Long, Int>,
        val lastId: Long?,
        val favIds: Set<Long>,
        val playlists: List<Playlist>
    )

    // Статистика, избранное и плейлисты собраны в один поток: combine принимает не больше пяти потоков.
    private val extras = combine(playCounts, lastPlayedId, favoriteIds, playlists) { counts, lastId, favIds, lists ->
        Extras(counts, lastId, favIds, lists)
    }

    val uiState: StateFlow<LibraryUiState> =
        combine(allTracks, query, isLoading, extras) { all, q, loading, extra ->
            val counts = extra.counts
            val lastId = extra.lastId
            val favIds = extra.favIds
            val trimmed = q.trim()
            val byId = all.associateBy { it.id }
            val shown = if (trimmed.isEmpty()) all else all.filter { it.matches(trimmed) }
            // Папки показываем только когда поиск пуст.
            val browsing = trimmed.isEmpty()
            LibraryUiState(
                tracks = shown,
                totalCount = all.size,
                hasQuery = trimmed.isNotEmpty(),
                isLoading = loading,
                recent = if (browsing) {
                    all.sortedByDescending { it.dateAddedSec }.take(FolderLimit)
                } else emptyList(),
                frequent = if (browsing) {
                    all.filter { (counts[it.id] ?: 0) > 0 }
                        .sortedByDescending { counts[it.id] ?: 0 }
                        .take(FolderLimit)
                } else emptyList(),
                lastPlayed = lastId?.let { id -> all.firstOrNull { it.id == id } },
                favorites = shown.filter { it.id in favIds },
                favoriteIds = favIds,
                playlists = extra.playlists.map { playlist ->
                    val existing = playlist.trackIds.mapNotNull { byId[it] }
                    PlaylistUi(
                        id = playlist.id,
                        name = playlist.name,
                        trackCount = existing.size,
                        tracks = if (trimmed.isEmpty()) existing else existing.filter { it.matches(trimmed) },
                        trackIds = existing.mapTo(HashSet()) { it.id },
                        coverTrackId = existing.firstOrNull()?.id
                    )
                },
                library = all
            )
        }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    init {
        // Прослушивание засчитывается, когда трек непрерывно играет ListenThresholdMs.
        // Пауза до этого момента сбрасывает отсчёт; один и тот же трек за раз считается один раз.
        viewModelScope.launch {
            var currentId: String? = null
            var counted = false
            playerState
                .map { it.mediaId to it.isPlaying }
                .distinctUntilChanged()
                .collectLatest { (mediaId, playing) ->
                    if (mediaId != currentId) {
                        currentId = mediaId
                        counted = false
                    }
                    val trackId = mediaId?.toLongOrNull()
                    // Обложка папки «Часто прослушиваемые» меняется, как только трек заиграл.
                    if (trackId != null && playing && lastPlayedId.value != trackId) {
                        playCountStore.setLastPlayed(trackId)
                        lastPlayedId.value = trackId
                    }
                    if (trackId != null && playing && !counted) {
                        delay(listenThresholdMs(playerState.value.durationMs))
                        counted = true
                        playCountStore.increment(trackId)
                        playCounts.value = playCountStore.loadAll()
                    }
                }
        }
    }

    /** Добавляет трек в избранное или убирает из него. */
    fun toggleFavorite(trackId: Long) {
        favoriteIds.value = favoritesStore.setFavorite(trackId, trackId !in favoriteIds.value)
    }

    private fun updatePlaylists(transform: (List<Playlist>) -> List<Playlist>) {
        val updated = transform(playlists.value)
        playlistStore.saveAll(updated)
        playlists.value = updated
    }

    private fun cleanName(name: String): String = name.trim().take(MaxPlaylistNameLength)

    /** Создаёт плейлист (с первым треком, если он задан) и возвращает его id; пустое имя не принимается. */
    fun createPlaylist(name: String, firstTrackId: Long? = null): Long? {
        val clean = cleanName(name)
        if (clean.isEmpty()) return null
        val id = maxOf(System.currentTimeMillis(), (playlists.value.maxOfOrNull { it.id } ?: 0L) + 1)
        updatePlaylists { it + Playlist(id, clean, listOfNotNull(firstTrackId)) }
        return id
    }

    fun renamePlaylist(playlistId: Long, name: String) {
        val clean = cleanName(name)
        if (clean.isEmpty()) return
        updatePlaylists { list -> list.map { if (it.id == playlistId) it.copy(name = clean) else it } }
    }

    fun deletePlaylist(playlistId: Long) {
        updatePlaylists { list -> list.filterNot { it.id == playlistId } }
    }

    /** Добавляет трек в плейлист, а если он там уже есть, убирает. */
    fun togglePlaylistTrack(playlistId: Long, trackId: Long) {
        updatePlaylists { list ->
            list.map { playlist ->
                when {
                    playlist.id != playlistId -> playlist
                    trackId in playlist.trackIds ->
                        playlist.copy(trackIds = playlist.trackIds - trackId)
                    else -> playlist.copy(trackIds = playlist.trackIds + trackId)
                }
            }
        }
    }

    fun removeFromPlaylist(playlistId: Long, trackId: Long) {
        updatePlaylists { list ->
            list.map { if (it.id == playlistId) it.copy(trackIds = it.trackIds - trackId) else it }
        }
    }

    fun onQueryChange(newQuery: String) {
        query.value = newQuery
    }

    /** Читает библиотеку заново. Повторный вызов во время загрузки игнорируется. */
    fun loadTracks() {
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            isLoading.value = true
            try {
                allTracks.value = repository.loadTracks()
            } finally {
                isLoading.value = false
            }
        }
    }

    /** Играет выбранный трек; очередью становится список, который сейчас на экране. */
    fun playTrack(track: Track) {
        val list = uiState.value.tracks
        val index = list.indexOfFirst { it.id == track.id }
        if (index >= 0) playerConnection.playTracks(list, index)
    }

    /** Играет [track]; очередью становится [list] (например, содержимое открытой папки). */
    fun playTrackFrom(list: List<Track>, track: Track) {
        val index = list.indexOfFirst { it.id == track.id }
        if (index >= 0) playerConnection.playTracks(list, index)
    }

    fun togglePlayPause() = playerConnection.togglePlayPause()

    fun skipNext() = playerConnection.skipNext()

    fun skipPrevious() = playerConnection.skipPrevious()

    fun seekTo(positionMs: Long) = playerConnection.seekTo(positionMs)

    fun positionMs(): Long = playerConnection.positionMs()

    fun toggleShuffle() = playerConnection.toggleShuffle()

    fun cycleRepeat() = playerConnection.cycleRepeat()

    fun playQueueItem(index: Int) = playerConnection.playQueueItem(index)

    override fun onCleared() {
        playerConnection.release()
        super.onCleared()
    }
}
