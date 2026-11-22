package com.offlineplayer.music.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
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
    val isPlaying: Boolean = false
)

/**
 * Мост между интерфейсом и PlaybackService: подключается к сессии через MediaController
 * и публикует текущее состояние плеера.
 */
class PlayerConnection(context: Context) {

    private val appContext = context.applicationContext
    private var controller: MediaController? = null
    private var pendingAction: (() -> Unit)? = null

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            publish(player)
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
            isPlaying = player.isPlaying
        )
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

    fun release() {
        controller?.removeListener(listener)
        controller = null
        MediaController.releaseFuture(controllerFuture)
    }
}
