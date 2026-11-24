package com.offlineplayer.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offlineplayer.music.data.Track
import com.offlineplayer.music.ui.theme.CardColor
import com.offlineplayer.music.ui.theme.TrackTitleFont

/** Папки на вкладке «Музыка». */
enum class LibraryFolder(val title: String) {
    Recent("Недавно добавленные"),
    Frequent("Часто прослушиваемые")
}

private val TileHeight = 56.dp
private val TextPadding = 10.dp
private const val MaxTitleSp = 14f
private const val MinTitleSp = 9f

/**
 * Две плитки-папки рядом: обложка слева, название справа.
 * [recentCover] и [frequentCover] — треки, чьи обложки показываются на плитках.
 */
@Composable
fun FolderTiles(
    recentCover: Track?,
    frequentCover: Track?,
    onOpen: (LibraryFolder) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FolderTile(
            title = LibraryFolder.Recent.title,
            coverTrackId = recentCover?.id,
            onClick = { onOpen(LibraryFolder.Recent) },
            modifier = Modifier.weight(1f)
        )
        FolderTile(
            title = LibraryFolder.Frequent.title,
            coverTrackId = frequentCover?.id,
            onClick = { onOpen(LibraryFolder.Frequent) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun FolderTile(
    title: String,
    coverTrackId: Long?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .height(TileHeight)
            .clip(RoundedCornerShape(12.dp))
            .background(CardColor)
            .clickable(onClick = onClick)
    ) {
        val textMeasurer = rememberTextMeasurer()
        val density = LocalDensity.current
        val textWidth = maxWidth - TileHeight - TextPadding * 2

        // Montserrat широкий, а плитка узкая: подбираем самый крупный размер,
        // при котором самое длинное слово названия не разрывается.
        val fontSize = remember(title, textWidth, density) {
            val widthPx = with(density) { textWidth.roundToPx() }
            val longestWord = title.split(' ').maxByOrNull { it.length } ?: title
            var size = MaxTitleSp
            while (size > MinTitleSp) {
                val measured = textMeasurer.measure(
                    text = longestWord,
                    style = TextStyle(
                        fontFamily = TrackTitleFont,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = size.sp
                    ),
                    softWrap = false,
                    maxLines = 1
                ).size.width
                if (measured <= widthPx) break
                size -= 0.5f
            }
            size
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Artwork(
                trackId = coverTrackId,
                modifier = Modifier.size(TileHeight),
                placeholderSize = 24.sp
            )
            Text(
                text = title,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = TextPadding),
                color = Color.White,
                fontFamily = TrackTitleFont,
                fontWeight = FontWeight.SemiBold,
                fontSize = fontSize.sp,
                lineHeight = (fontSize * 1.25f).sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
