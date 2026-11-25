package com.offlineplayer.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offlineplayer.music.ui.theme.Accent
import com.offlineplayer.music.ui.theme.CardColor
import com.offlineplayer.music.ui.theme.IconTile
import com.offlineplayer.music.ui.theme.SecondaryText
import com.offlineplayer.music.ui.theme.TrackTitleFont

/** Простая строка списка (используется в очереди воспроизведения). */
@Composable
fun LibraryItem(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    isCurrent: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(16.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(CardColor)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .background(IconTile, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("♫", color = Accent, fontSize = 24.sp)
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                color = if (isCurrent) Accent else Color.White,
                fontFamily = TrackTitleFont,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                subtitle,
                color = SecondaryText,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (trailing != null) {
            Spacer(modifier = Modifier.width(12.dp))
            Text(trailing, color = SecondaryText, fontSize = 12.sp)
        }
    }
}
