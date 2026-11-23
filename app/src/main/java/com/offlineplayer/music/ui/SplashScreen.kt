package com.offlineplayer.music.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.offlineplayer.music.R
import com.offlineplayer.music.ui.theme.Background
import kotlinx.coroutines.delay

/** Время показа заставки при запуске приложения. */
private const val SplashDurationMs = 2000L

/**
 * Заставка с логотипом и названием. Использует облегчённую картинку
 * (res/drawable-nodpi/splash_screen.webp) без мелких декоративных элементов,
 * чтобы быстро открываться даже на слабых устройствах.
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val currentOnFinished by rememberUpdatedState(onFinished)

    LaunchedEffect(Unit) {
        delay(SplashDurationMs)
        currentOnFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Image(
            painter = painterResource(R.drawable.splash_screen),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}
