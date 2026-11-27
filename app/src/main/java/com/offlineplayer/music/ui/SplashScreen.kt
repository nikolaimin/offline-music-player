package com.offlineplayer.music.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offlineplayer.music.R
import com.offlineplayer.music.ui.theme.Accent
import com.offlineplayer.music.ui.theme.Background
import com.offlineplayer.music.ui.theme.TrackTitleFont
import kotlinx.coroutines.delay

/** Время показа заставки при запуске приложения. */
private const val SplashDurationMs = 2000L

/**
 * Заставка с логотипом и названием Planix. Фон — облегчённая картинка без мелких
 * декоративных элементов (res/drawable-nodpi/app_background.webp), логотип и текст
 * рисуются поверх, поэтому заставка быстро открывается даже на слабых устройствах.
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
            painter = painterResource(R.drawable.app_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(R.drawable.planix_logo),
                contentDescription = null,
                modifier = Modifier.width(220.dp)
            )
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = "Planix",
                color = Color.White,
                fontFamily = TrackTitleFont,
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "PLAY  ·  ENJOY  ·  OFFLINE",
                color = Accent,
                fontSize = 12.sp,
                letterSpacing = 3.sp
            )
        }
    }
}
