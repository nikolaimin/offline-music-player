package com.offlineplayer.music.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.offlineplayer.music.data.MusicRepository
import com.offlineplayer.music.data.PlayCountStore
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
private const val ListenThresholdMs = 20_000L

data class LibraryUiState(
    val tracks: List<Track> = emptyList(),
    val totalCount: Int = 0,
    val hasQuery: Boolean = false,
    val isLoading: Boolean = false,
    val recent: List<Track> = emptyList(),
    val frequent: List<Track> = emptyList(),
    /** Трек, который играл последним (обложка папки «Часто прослушиваемые»). */
    val lastPlayed: Track? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MusicRepository(application.applicationContext)
    private val playerConnection = PlayerConnection(application.applicationContext)
    private val playCountStore = PlayCountStore(application.applicationContext)
    private val playCounts = MutableStateFlow(playCountStore.loadAll())
    private val lastPlayedId = MutableStateFlow(playCountStore.lastPlayedId())

    private val allTracks = MutableStateFlow<List<Track>>(emptyList())
    private val query = MutableStateFlow("")
    private val isLoading = MutableStateFlow(false)
    private var loadJob: Job? = null

    val playerState: StateFlow<PlayerState> = playerConnection.state
    val queueState: StateFlow<QueueState> = playerConnection.queue

    val uiState: StateFlow<LibraryUiState> =
        combine(allTracks, query, isLoading, playCounts, lastPlayedId) { all, q, loading, counts, lastId ->
            val trimmed = q.trim()
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
                lastPlayed = lastId?.let { id -> all.firstOrNull { it.id == id } }
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
                        delay(ListenThresholdMs)
                        counted = true
                        playCountStore.increment(trackId)
                        playCounts.value = playCountStore.loadAll()
                    }
                }
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
