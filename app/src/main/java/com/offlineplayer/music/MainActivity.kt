package com.offlineplayer.music

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.offlineplayer.music.ui.MusicPlayerApp
import com.offlineplayer.music.ui.theme.OfflinePlayerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            OfflinePlayerTheme {
                MusicPlayerApp()
            }
        }
    }
}
