package com.offlineplayer.music.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.offlineplayer.music.data.ArtworkLoader
import com.offlineplayer.music.ui.theme.Accent
import com.offlineplayer.music.ui.theme.IconTile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Обложка трека. Пока обложка грузится или её нет, показывается значок ноты. */
@Composable
fun Artwork(
    trackId: Long?,
    modifier: Modifier = Modifier,
    placeholderSize: TextUnit = 72.sp
) {
    val context = LocalContext.current

    val bitmap by produceState<ImageBitmap?>(initialValue = null, trackId) {
        value = null
        if (trackId != null) {
            value = withContext(Dispatchers.IO) {
                ArtworkLoader.load(context, trackId)?.asImageBitmap()
            }
        }
    }

    Box(
        modifier = modifier.background(IconTile),
        contentAlignment = Alignment.Center
    ) {
        val image = bitmap
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text("♫", color = Accent, fontSize = placeholderSize)
        }
    }
}
