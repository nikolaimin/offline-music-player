package com.offlineplayer.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offlineplayer.music.data.Track
import com.offlineplayer.music.ui.theme.Accent
import com.offlineplayer.music.ui.theme.CardColor
import com.offlineplayer.music.ui.theme.SecondaryText
import com.offlineplayer.music.ui.theme.TrackTitleFont

/** Горизонтальная полка с заголовком и карточками треков («Недавно добавленные» и т.п.). */
@Composable
fun TrackShelf(
    title: String,
    tracks: List<Track>,
    currentTrackId: Long?,
    onTrackClick: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        SectionTitle(title)
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(tracks, key = { it.id }) { track ->
                TrackCard(
                    track = track,
                    isCurrent = track.id == currentTrackId,
                    onClick = { onTrackClick(track) }
                )
            }
        }
    }
}

@Composable
private fun TrackCard(
    track: Track,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(128.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(CardColor)
            .clickable(onClick = onClick)
            .padding(10.dp)
    ) {
        Artwork(
            trackId = track.id,
            modifier = Modifier
                .size(108.dp)
                .clip(RoundedCornerShape(12.dp)),
            placeholderSize = 40.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            track.title,
            color = if (isCurrent) Accent else Color.White,
            fontFamily = TrackTitleFont,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            track.artist,
            color = SecondaryText,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
