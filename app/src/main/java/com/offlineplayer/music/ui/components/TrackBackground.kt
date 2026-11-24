package com.offlineplayer.music.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.offlineplayer.music.R
import com.offlineplayer.music.data.ArtworkLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Маска фона: слева картинка видна, к середине строки плавно исчезает, правая часть чистая. */
private val RowFadeMask = Brush.horizontalGradient(
    colorStops = arrayOf(
        0.00f to Color.Black,
        0.12f to Color.Black.copy(alpha = 0.92f),
        0.30f to Color.Black.copy(alpha = 0.50f),
        0.42f to Color.Black.copy(alpha = 0.15f),
        0.52f to Color.Transparent,
        1.00f to Color.Transparent
    )
)

/** Результат загрузки обложки: [bitmap] == null значит, что обложки у трека нет. */
private class Loaded(val bitmap: ImageBitmap?)

/**
 * Фон строки трека: обложка альбома или, если обложки нет, логотип приложения
 * (res/drawable-nodpi/app_logo.webp). Пока обложка загружается, фон пустой,
 * чтобы логотип не мигал перед появлением настоящей обложки.
 */
@Composable
fun TrackBackground(trackId: Long, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    var loaded by remember(trackId) {
        mutableStateOf<Loaded?>(
            ArtworkLoader.cachedThumb(trackId)?.let { Loaded(it.asImageBitmap()) }
                ?: if (ArtworkLoader.knownWithoutThumb(trackId)) Loaded(null) else null
        )
    }

    LaunchedEffect(trackId) {
        if (loaded == null) {
            loaded = Loaded(
                withContext(Dispatchers.IO) {
                    ArtworkLoader.loadThumbCached(context, trackId)?.asImageBitmap()
                }
            )
        }
    }

    Box(
        modifier = modifier
            // Offscreen нужен, чтобы маска (DstIn) действовала только на этот слой.
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                drawRect(brush = RowFadeMask, blendMode = BlendMode.DstIn)
            }
    ) {
        val state = loaded ?: return@Box
        val cover = state.bitmap

        if (cover != null) {
            Image(
                bitmap = cover,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alignment = Alignment.Center
            )
            // Небольшое затемнение: текст читается даже на светлой обложке.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
            )
        } else {
            // Логотип квадратный: показываем целиком у левого края, а не растягиваем на всю строку.
            Image(
                painter = painterResource(R.drawable.app_logo),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                alignment = Alignment.CenterStart
            )
        }
    }
}
