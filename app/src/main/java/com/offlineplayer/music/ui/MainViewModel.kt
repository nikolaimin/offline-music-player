package com.offlineplayer.music.ui

import android.app.Application
import android.content.IntentSender
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.offlineplayer.music.data.FavoritesStore
import com.offlineplayer.music.data.MusicRepository
import com.offlineplayer.music.data.PlayCountStore
import com.offlineplayer.music.data.Playlist
import com.offlineplayer.music.data.PlaylistStore
import com.offlineplayer.music.data.Profile
import com.offlineplayer.music.data.ProfileStore
import com.offlineplayer.music.data.Track
import com.offlineplayer.music.data.TrackDeleter
import com.offlineplayer.music.data.TrackEdit
import com.offlineplayer.music.data.TrackEditStore
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
import kotlinx.coroutines.withContext

private const val MaxTagLength = 100

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
    val library: List<Track> = emptyList(),
    /** id треков, у которых данные изменены пользователем. */
    val editedIds: Set<Long> = emptySet()
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
    private val trackEditStore = TrackEditStore(application.applicationContext)
    private val trackEdits = MutableStateFlow(trackEditStore.loadAll())

    private val profileStore = ProfileStore(application.applicationContext)
    private val _profile = MutableStateFlow(profileStore.load())
    val profile: StateFlow<Profile> = _profile

    private val allTracks = MutableStateFlow<List<Track>>(emptyList())
    private val query = MutableStateFlow("")
    private val isLoading = MutableStateFlow(false)
    private var loadJob: Job? = null

    // Треки с учётом правок пользователя: всё приложение работает с этим списком.
    // MediaStore отдаёт треки по названию, поэтому после правки порядок пересчитываем.
    private val displayTracks = combine(allTracks, trackEdits) { all, edits ->
        if (edits.isEmpty()) {
            all
        } else {
            all.map { track ->
                edits[track.id]?.let { track.copy(title = it.title, artist = it.artist, album = it.album) }
                    ?: track
            }.sortedBy { it.title.lowercase() }
        }
    }

    // Плеер хранит название из момента постановки в очередь, поэтому подставляем правки сами.
    val playerState: StateFlow<PlayerState> =
        combine(playerConnection.state, trackEdits) { state, edits ->
            val edit = state.mediaId?.toLongOrNull()?.let { edits[it] }
            if (edit == null) state else state.copy(title = edit.title, artist = edit.artist, album = edit.album)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, playerConnection.state.value)

    val queueState: StateFlow<QueueState> =
        combine(playerConnection.queue, trackEdits) { queue, edits ->
            if (edits.isEmpty()) {
                queue
            } else {
                queue.copy(items = queue.items.map { item ->
                    val edit = item.mediaId.toLongOrNull()?.let { edits[it] }
                    if (edit == null) item else item.copy(title = edit.title, artist = edit.artist)
                })
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, playerConnection.queue.value)

    private class Extras(
        val counts: Map<Long, Int>,
        val lastId: Long?,
        val favIds: Set<Long>,
        val playlists: List<Playlist>,
        val editedIds: Set<Long>
    )

    // Статистика, избранное, плейлисты и правки собраны в один поток: combine принимает не больше пяти потоков.
    private val extras = combine(
        playCounts, lastPlayedId, favoriteIds, playlists, trackEdits
    ) { counts, lastId, favIds, lists, edits ->
        Extras(counts, lastId, favIds, lists, edits.keys)
    }

    val uiState: StateFlow<LibraryUiState> =
        combine(displayTracks, query, isLoading, extras) { all, q, loading, extra ->
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
                library = all,
                editedIds = extra.editedIds
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

    /** Сохраняет имя, имя пользователя и статус профиля. */
    fun saveProfile(name: String, username: String, status: String) {
        _profile.value = profileStore.save(name, username, status)
    }

    /** Ставит выбранную картинку аватаром; если прочитать её не вышло, вызывается [onFailed]. */
    fun setAvatar(uri: Uri, onFailed: () -> Unit) {
        viewModelScope.launch {
            val updated = withContext(Dispatchers.IO) { profileStore.saveAvatarFrom(uri) }
            if (updated != null) _profile.value = updated else onFailed()
        }
    }

    fun removeAvatar() {
        viewModelScope.launch {
            _profile.value = withContext(Dispatchers.IO) { profileStore.removeAvatar() }
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

    private fun updateEdits(transform: (Map<Long, TrackEdit>) -> Map<Long, TrackEdit>) {
        val updated = transform(trackEdits.value)
        trackEditStore.saveAll(updated)
        trackEdits.value = updated
    }

    /** Сохраняет свои название, исполнителя и альбом трека. Пустое название не принимается. */
    fun saveTrackEdit(trackId: Long, title: String, artist: String, album: String) {
        val cleanTitle = title.trim().take(MaxTagLength)
        if (cleanTitle.isEmpty()) return
        val edit = TrackEdit(
            title = cleanTitle,
            artist = artist.trim().take(MaxTagLength).ifEmpty { "Неизвестный исполнитель" },
            album = album.trim().take(MaxTagLength).ifEmpty { "Неизвестный альбом" }
        )
        updateEdits { it + (trackId to edit) }
    }

    /** Возвращает трек к данным из файла. */
    fun resetTrackEdit(trackId: Long) {
        updateEdits { it - trackId }
    }

    /** Запрос подтверждения у системы перед удалением файла. */
    class DeleteRequest(
        val track: Track,
        val intentSender: IntentSender,
        val deleteAfterConsent: Boolean
    )

    /**
     * Удаляет файл трека с телефона. Если Android просит подтверждение, вызывается [onNeedsConsent]
     * (окно нужно показать пользователю); если удалить не вышло, вызывается [onFailed].
     */
    fun deleteTrack(track: Track, onNeedsConsent: (DeleteRequest) -> Unit, onFailed: () -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                TrackDeleter.delete(getApplication<Application>(), track.uri)
            }
            when (result) {
                is TrackDeleter.Result.Deleted -> forgetTrack(track.id)
                is TrackDeleter.Result.Failed -> onFailed()
                is TrackDeleter.Result.NeedsConsent ->
                    onNeedsConsent(DeleteRequest(track, result.intentSender, result.deleteAfterConsent))
            }
        }
    }

    /** Вызывается, когда пользователь нажал «Разрешить» в системном окне удаления. */
    fun finishDeleteAfterConsent(request: DeleteRequest, onFailed: () -> Unit) {
        if (!request.deleteAfterConsent) {
            // Android 11+: файл уже удалён системой.
            forgetTrack(request.track.id)
            return
        }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                TrackDeleter.delete(getApplication<Application>(), request.track.uri)
            }
            if (result is TrackDeleter.Result.Deleted) forgetTrack(request.track.id) else onFailed()
        }
    }

    /** Убирает удалённый трек отовсюду: из списка, очереди, избранного, плейлистов, статистики и правок. */
    private fun forgetTrack(trackId: Long) {
        allTracks.value = allTracks.value.filterNot { it.id == trackId }

        playerConnection.removeFromQueue(trackId.toString())

        if (trackId in favoriteIds.value) {
            favoriteIds.value = favoritesStore.setFavorite(trackId, false)
        }
        if (playlists.value.any { trackId in it.trackIds }) {
            updatePlaylists { list ->
                list.map { if (trackId in it.trackIds) it.copy(trackIds = it.trackIds - trackId) else it }
            }
        }
        playCountStore.forget(trackId)
        playCounts.value = playCountStore.loadAll()
        lastPlayedId.value = playCountStore.lastPlayedId()

        if (trackId in trackEdits.value) resetTrackEdit(trackId)

        // Сверяемся с MediaStore: библиотека должна совпадать с тем, что реально на телефоне.
        loadTracks()
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
