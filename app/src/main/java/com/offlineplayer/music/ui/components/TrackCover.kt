package com.offlineplayer.music.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.offlineplayer.music.data.ArtworkLoader
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Результат загрузки обложки: [bitmap] == null значит, что у трека обложки нет. */
private class Loaded(val bitmap: ImageBitmap?)

/**
 * Квадратная обложка трека с закруглёнными углами. Берётся встроенная обложка файла;
 * если её нет, рисуется абстрактная фиолетовая обложка, своя для каждого трека.
 * Настоящая обложка плавно проявляется поверх абстрактной.
 */
@Composable
fun TrackCover(
    trackId: Long,
    size: Dp,
    modifier: Modifier = Modifier,
    showArtwork: Boolean = true,
    corner: Dp = 16.dp
) {
    val context = LocalContext.current

    var loaded by remember(trackId) {
        mutableStateOf<Loaded?>(
            if (!showArtwork) Loaded(null)
            else ArtworkLoader.cachedThumb(trackId)?.let { Loaded(it.asImageBitmap()) }
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

    val cover = loaded?.bitmap
    // Если обложка уже в кеше, она видна сразу; если загрузилась позже, проявляется за 300 мс.
    val coverAlpha by animateFloatAsState(
        targetValue = if (cover != null) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "coverAlpha"
    )

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(corner))
    ) {
        GeneratedCover(trackId = trackId, modifier = Modifier.fillMaxSize())

        if (cover != null) {
            Image(
                bitmap = cover,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = coverAlpha },
                contentScale = ContentScale.Crop
            )
        }
    }
}

/** Параметры абстрактной обложки. Считаются из id трека, поэтому у одного трека всегда одинаковые. */
private class CoverSpec(seed: Long) {
    private val random = Random(seed)
    private val hue = 255f + random.nextFloat() * 40f // только фиолетовые оттенки

    val top = hsv(hue, 0.70f, 0.50f + random.nextFloat() * 0.25f)
    val bottom = hsv((hue + 18f) % 360f, 0.85f, 0.12f + random.nextFloat() * 0.10f)

    val glowColor = hsv(hue - 12f, 0.40f, 1f).copy(alpha = 0.34f)
    val glowX = 0.20f + random.nextFloat() * 0.60f
    val glowY = 0.15f + random.nextFloat() * 0.45f
    val glowRadius = 0.30f + random.nextFloat() * 0.20f

    val ringColor = hsv(hue + 8f, 0.30f, 1f).copy(alpha = 0.28f)
    val ringX = 0.25f + random.nextFloat() * 0.50f
    val ringY = 0.45f + random.nextFloat() * 0.40f
    val ringRadius = 0.18f + random.nextFloat() * 0.16f

    val dotColor = hsv(hue, 0.20f, 1f).copy(alpha = 0.55f)
    val dotX = 0.15f + random.nextFloat() * 0.70f
    val dotY = 0.10f + random.nextFloat() * 0.35f

    private fun hsv(h: Float, s: Float, v: Float): Color =
        Color(android.graphics.Color.HSVToColor(floatArrayOf((h + 360f) % 360f, s, v)))
}

@Composable
private fun GeneratedCover(trackId: Long, modifier: Modifier = Modifier) {
    val spec = remember(trackId) { CoverSpec(trackId) }

    Canvas(modifier = modifier) {
        val side = size.minDimension

        drawRect(brush = Brush.linearGradient(listOf(spec.top, spec.bottom)))
        drawCircle(
            color = spec.glowColor,
            radius = side * spec.glowRadius,
            center = Offset(size.width * spec.glowX, size.height * spec.glowY)
        )
        drawCircle(
            color = spec.ringColor,
            radius = side * spec.ringRadius,
            center = Offset(size.width * spec.ringX, size.height * spec.ringY),
            style = Stroke(width = side * 0.035f)
        )
        drawCircle(
            color = spec.dotColor,
            radius = side * 0.04f,
            center = Offset(size.width * spec.dotX, size.height * spec.dotY)
        )
    }
}
