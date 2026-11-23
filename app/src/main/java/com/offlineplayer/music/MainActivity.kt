package com.offlineplayer.music

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.offlineplayer.music.ui.MusicPlayerApp
import com.offlineplayer.music.ui.SplashScreen
import com.offlineplayer.music.ui.theme.OfflinePlayerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            OfflinePlayerTheme {
                // Заставка на 2 секунды, затем основное приложение.
                var showSplash by rememberSaveable { mutableStateOf(true) }

                if (showSplash) {
                    SplashScreen(onFinished = { showSplash = false })
                } else {
                    MusicPlayerApp()
                }
            }
        }
    }
}
