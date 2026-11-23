package com.offlineplayer.music.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.paint
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.offlineplayer.music.R

/**
 * Рисует фирменный фон приложения (res/drawable-nodpi/app_background.webp)
 * на всю область элемента с обрезкой по краям, чтобы он подходил под любой экран.
 */
@Composable
fun Modifier.appBackground(): Modifier = this
    .clipToBounds()
    .paint(
        painter = painterResource(R.drawable.app_background),
        contentScale = ContentScale.Crop
    )
