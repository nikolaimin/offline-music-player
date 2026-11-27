package com.offlineplayer.music.ui.profile

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offlineplayer.music.data.Track
import com.offlineplayer.music.ui.components.TrackCover
import com.offlineplayer.music.ui.theme.Accent
import com.offlineplayer.music.ui.theme.IconTile
import com.offlineplayer.music.ui.theme.SecondaryText
import com.offlineplayer.music.ui.theme.TrackTitleFont
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

private val AchievementGradient = Brush.linearGradient(listOf(Color(0xFFD2C2FF), Color(0xFF8B5CF6)))

/**
 * Блоки профиля под шапкой: статистика, топ-5, любимые исполнитель и альбом, история, достижения.
 * Блоки плавно появляются по очереди.
 */
@Composable
fun ProfileSections(
    details: ProfileDetails,
    onPlay: (List<Track>, Track) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(26.dp)) {
        Appear(0) { StatsSection(details.stats) }
        Appear(1) { TopTracksSection(details.topTracks, onPlay) }
        Appear(2) { FavoritesSection(details.favoriteArtist, details.favoriteAlbum) }
        Appear(3) { RecentSection(details.recent, onPlay) }
        Appear(4) { AchievementsSection(details.achievements) }
    }
}

/** Плавное появление блока (прозрачность и лёгкий подъём) с задержкой по номеру блока. */
@Composable
private fun Appear(index: Int, content: @Composable () -> Unit) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 70L)
        shown = true
    }
    val progress by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(450),
        label = "profileAppear"
    )
    Box(
        modifier = Modifier.graphicsLayer {
            alpha = progress
            translationY = (1f - progress) * 24.dp.toPx()
        }
    ) {
        content()
    }
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        color = Accent,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp
    )
}

@Composable
private fun EmptyBlock(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassSurface(corner = 18.dp)
            .padding(horizontal = 16.dp, vertical = 18.dp)
    ) {
        Text(text = text, color = SecondaryText, fontSize = 13.sp)
    }
}

// ---------- Моя статистика ----------

@Composable
private fun StatsSection(stats: ProfileStats) {
    Column {
        SectionLabel("МОЯ СТАТИСТИКА")
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("▶", stats.totalPlays, "ПРОСЛУШИВАНИЙ", Modifier.weight(1f))
            StatCard("♫", stats.trackCount, "ТРЕКОВ", Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("♥", stats.favoriteCount, "ИЗБРАННОЕ", Modifier.weight(1f))
            StatCard("☷", stats.playlistCount, "ПЛЕЙЛИСТОВ", Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatCard(icon: String, value: Int, label: String, modifier: Modifier = Modifier) {
    // Число «досчитывается» от нуля до значения.
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(value) { shown = true }
    val shownValue by animateIntAsState(
        targetValue = if (shown) value else 0,
        animationSpec = tween(800),
        label = "statCount"
    )

    Row(
        modifier = modifier
            .glassSurface(corner = 20.dp)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(IconTile),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, color = Accent, fontSize = 18.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = shownValue.toString(),
                color = Color.White,
                fontFamily = TrackTitleFont,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                maxLines = 1
            )
            Text(
                text = label,
                color = SecondaryText,
                fontSize = 10.sp,
                letterSpacing = 1.sp,
                maxLines = 1
            )
        }
    }
}

// ---------- Топ-5 треков и недавно прослушанные ----------

@Composable
private fun TopTracksSection(top: List<TopTrack>, onPlay: (List<Track>, Track) -> Unit) {
    Column {
        SectionLabel("ТОП-5 ТРЕКОВ")
        Spacer(modifier = Modifier.height(12.dp))
        if (top.isEmpty()) {
            EmptyBlock("Пока нет прослушанных треков. Включите любой трек, и он появится здесь.")
        } else {
            val tracks = top.map { it.track }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                top.forEachIndexed { index, item ->
                    ProfileTrackRow(
                        lead = (index + 1).toString(),
                        track = item.track,
                        trailing = timesText(item.plays),
                        trailingColor = Accent,
                        onClick = { onPlay(tracks, item.track) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentSection(recent: List<RecentPlay>, onPlay: (List<Track>, Track) -> Unit) {
    Column {
        SectionLabel("НЕДАВНО ПРОСЛУШАНО")
        Spacer(modifier = Modifier.height(12.dp))
        if (recent.isEmpty()) {
            EmptyBlock("История пуста. Здесь появятся треки, которые вы слушали.")
        } else {
            val tracks = recent.map { it.track }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                recent.forEach { item ->
                    ProfileTrackRow(
                        lead = null,
                        track = item.track,
                        trailing = formatPlayedAt(item.playedAtMs),
                        trailingColor = SecondaryText,
                        onClick = { onPlay(tracks, item.track) }
                    )
                }
            }
        }
    }
}

/** Строка трека для профиля: номер (если есть), обложка, название, исполнитель и подпись справа. */
@Composable
private fun ProfileTrackRow(
    lead: String?,
    track: Track,
    trailing: String,
    trailingColor: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(120),
        label = "profileRowScale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .glassSurface(corner = 18.dp)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (lead != null) {
            Text(
                text = lead,
                color = Accent,
                fontFamily = TrackTitleFont,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(22.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        TrackCover(trackId = track.id, size = 48.dp, corner = 10.dp)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = Color.White,
                fontFamily = TrackTitleFont,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = track.artist,
                color = SecondaryText,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = trailing,
            color = trailingColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

// ---------- Любимые исполнитель и альбом ----------

@Composable
private fun FavoritesSection(artist: FavoriteArtist?, album: FavoriteAlbum?) {
    Column {
        SectionLabel("ЛЮБИМОЕ")
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FavoriteCard(
                caption = "ИСПОЛНИТЕЛЬ",
                name = artist?.name,
                subtitle = null,
                plays = artist?.plays ?: 0,
                coverTrackId = artist?.coverTrackId,
                circle = true,
                modifier = Modifier.weight(1f)
            )
            FavoriteCard(
                caption = "АЛЬБОМ",
                name = album?.name,
                subtitle = album?.artist,
                plays = album?.plays ?: 0,
                coverTrackId = album?.coverTrackId,
                circle = false,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun FavoriteCard(
    caption: String,
    name: String?,
    subtitle: String?,
    plays: Int,
    coverTrackId: Long?,
    circle: Boolean,
    modifier: Modifier = Modifier
) {
    val coverSize = 84.dp
    Column(
        modifier = modifier
            .glassSurface(corner = 22.dp)
            .padding(horizontal = 12.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (coverTrackId != null) {
            TrackCover(
                trackId = coverTrackId,
                size = coverSize,
                corner = if (circle) coverSize / 2 else 16.dp
            )
        } else {
            Box(
                modifier = Modifier
                    .size(coverSize)
                    .clip(if (circle) CircleShape else RoundedCornerShape(16.dp))
                    .background(IconTile),
                contentAlignment = Alignment.Center
            ) {
                Text("♫", color = Accent, fontSize = 30.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = caption,
            color = SecondaryText,
            fontSize = 10.sp,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        if (name == null) {
            Text(
                text = "Пока мало данных",
                color = SecondaryText,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        } else {
            Text(
                text = name,
                color = Color.White,
                fontFamily = TrackTitleFont,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = SecondaryText,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = timesText(plays),
                color = Accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ---------- Достижения ----------

@Composable
private fun AchievementsSection(items: List<AchievementUi>) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("ДОСТИЖЕНИЯ", Modifier.weight(1f))
            Text(
                text = "${items.count { it.unlockedAtMs != null }} из ${items.size}",
                color = SecondaryText,
                fontSize = 12.sp
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items.forEach { AchievementCard(it) }
        }
    }
}

@Composable
private fun AchievementCard(item: AchievementUi) {
    val unlocked = item.unlockedAtMs != null
    val achievement = item.achievement
    val shape = RoundedCornerShape(20.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            // Недоступное достижение приглушено целиком.
            .graphicsLayer { alpha = if (unlocked) 1f else 0.62f }
            .glassSurface(corner = 20.dp)
            .then(if (unlocked) Modifier.border(1.dp, Accent.copy(alpha = 0.55f), shape) else Modifier)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .then(
                    if (unlocked) Modifier.background(AchievementGradient)
                    else Modifier.background(IconTile)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = achievement.icon,
                color = if (unlocked) Color(0xFF1A1030) else SecondaryText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = achievement.title,
                color = Color.White,
                fontFamily = TrackTitleFont,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = achievement.description,
                color = SecondaryText,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (unlocked) {
                Text(
                    text = "Получено · ${formatDate(item.unlockedAtMs ?: 0L)}",
                    color = Accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            } else {
                ProgressBar(progress = item.progress, target = achievement.target)
            }
        }
    }
}

/** Полоса прогресса к достижению с подписью «37 / 100». */
@Composable
private fun ProgressBar(progress: Int, target: Int) {
    val fraction by animateFloatAsState(
        targetValue = if (target > 0) progress.toFloat() / target else 0f,
        animationSpec = tween(600),
        label = "achievementProgress"
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(5.dp)
                .clip(CircleShape)
                .background(IconTile)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(Accent)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(text = "$progress / $target", color = SecondaryText, fontSize = 11.sp)
    }
}

private fun formatDate(timeMs: Long): String =
    SimpleDateFormat("d MMM yyyy", Locale("ru")).format(Date(timeMs))
