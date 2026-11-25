package com.offlineplayer.music.playback

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.offlineplayer.music.data.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlayerState(
    val mediaId: String? = null,
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val isPlaying: Boolean = false,
    val durationMs: Long = 0L,
    val shuffle: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val status: String = "",
    val error: String? = null
)

data class QueueItem(
    val mediaId: String,
    val title: String,
    val artist: String
)

data class QueueState(
    val items: List<QueueItem> = emptyList(),
    val currentIndex: Int = -1
)

/**
 * Мост между интерфейсом и PlaybackService: подключается к сессии через MediaController
 * и публикует текущее состояние плеера и очередь.
 */
class PlayerConnection(context: Context) {

    private val appContext = context.applicationContext
    private var controller: MediaController? = null
    private var pendingAction: (() -> Unit)? = null

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private val _queue = MutableStateFlow(QueueState())
    val queue: StateFlow<QueueState> = _queue.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            publish(player)
            if (events.contains(Player.EVENT_TIMELINE_CHANGED)) {
                publishQueue(player)
            } else if (events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)) {
                _queue.value = _queue.value.copy(currentIndex = player.currentMediaItemIndex)
            }
        }
    }

    private val controllerFuture = MediaController.Builder(
        appContext,
        SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java))
    ).buildAsync()

    init {
        controllerFuture.addListener(
            {
                val connected = try {
                    controllerFuture.get()
                } catch (e: Exception) {
                    null
                }
                if (connected != null) {
                    controller = connected
                    connected.addListener(listener)
                    publish(connected)
                    publishQueue(connected)
                    pendingAction?.invoke()
                    pendingAction = null
                }
            },
            ContextCompat.getMainExecutor(appContext)
        )
    }

    private fun publish(player: Player) {
        val item = player.currentMediaItem
        _state.value = PlayerState(
            mediaId = item?.mediaId,
            title = item?.mediaMetadata?.title?.toString().orEmpty(),
            artist = item?.mediaMetadata?.artist?.toString().orEmpty(),
            album = item?.mediaMetadata?.albumTitle?.toString().orEmpty(),
            isPlaying = player.isPlaying,
            durationMs = player.duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: 0L,
            shuffle = player.shuffleModeEnabled,
            repeatMode = player.repeatMode,
            status = when (player.playbackState) {
                Player.STATE_IDLE -> "IDLE"
                Player.STATE_BUFFERING -> "BUFFERING"
                Player.STATE_READY -> "READY"
                Player.STATE_ENDED -> "ENDED"
                else -> "?"
            },
            error = player.playerError?.let {
                Log.e("PlayerConnection", "Ошибка воспроизведения", it)
                "${it.errorCodeName}: ${it.cause?.message ?: it.message}"
            }
        )
    }

    private fun publishQueue(player: Player) {
        val items = (0 until player.mediaItemCount).map { index ->
            val media = player.getMediaItemAt(index)
            QueueItem(
                mediaId = media.mediaId,
                title = media.mediaMetadata.title?.toString().orEmpty(),
                artist = media.mediaMetadata.artist?.toString().orEmpty()
            )
        }
        _queue.value = QueueState(items, player.currentMediaItemIndex)
    }

    /** Ставит список в очередь и начинает играть с трека под номером [startIndex]. */
    fun playTracks(tracks: List<Track>, startIndex: Int) {
        val c = controller
        if (c == null) {
            // Соединение ещё устанавливается: выполним, как только оно будет готово.
            pendingAction = { playTracks(tracks, startIndex) }
            return
        }
        c.setMediaItems(tracks.map { it.toMediaItem() }, startIndex, 0L)
        c.prepare()
        c.play()
    }

    fun togglePlayPause() {
        controller?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    fun skipNext() {
        controller?.seekToNext()
    }

    /** Если трек играет дольше нескольких секунд, возвращает в его начало, иначе на прошлый. */
    fun skipPrevious() {
        controller?.seekToPrevious()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
    }

    fun positionMs(): Long = controller?.currentPosition?.coerceAtLeast(0L) ?: 0L

    fun toggleShuffle() {
        controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled }
    }

    /** Повтор: выкл -> весь список -> один трек -> выкл. */
    fun cycleRepeat() {
        controller?.let {
            it.repeatMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }

    /**
     * Убирает трек из очереди (например, после удаления файла). Если он играл,
     * плеер сам переходит к следующему треку.
     */
    fun removeFromQueue(mediaId: String) {
        val c = controller ?: return
        for (index in c.mediaItemCount - 1 downTo 0) {
            if (c.getMediaItemAt(index).mediaId == mediaId) c.removeMediaItem(index)
        }
    }

    fun playQueueItem(index: Int) {
        controller?.let {
            if (index in 0 until it.mediaItemCount) {
                it.seekTo(index, 0L)
                it.play()
            }
        }
    }

    fun release() {
        controller?.removeListener(listener)
        controller = null
        MediaController.releaseFuture(controllerFuture)
    }
}
