package com.offlineplayer.music.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offlineplayer.music.ui.theme.Accent
import com.offlineplayer.music.ui.theme.CardColor
import com.offlineplayer.music.ui.theme.IconTile
import com.offlineplayer.music.ui.theme.SecondaryText

@Composable
fun LibraryItem(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(CardColor, RoundedCornerShape(16.dp))
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

        Column {
            Text(
                title,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
            Text(
                subtitle,
                color = SecondaryText,
                fontSize = 12.sp
            )
        }
    }
}
