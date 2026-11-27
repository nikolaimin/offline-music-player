package com.offlineplayer.music.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offlineplayer.music.data.Profile
import com.offlineplayer.music.data.Track
import com.offlineplayer.music.ui.components.ListScrollbar
import com.offlineplayer.music.ui.components.MenuIcon
import com.offlineplayer.music.ui.components.UserAvatar
import com.offlineplayer.music.ui.profile.ProfileDetails
import com.offlineplayer.music.ui.profile.profileSections
import com.offlineplayer.music.ui.profile.glassSurface
import com.offlineplayer.music.ui.theme.Accent
import com.offlineplayer.music.ui.theme.SecondaryText

/**
 * Профиль пользователя. Кнопка «три полоски» слева сверху открывает боковую панель
 * с редактированием; сама панель лежит поверх всего приложения (см. MusicPlayerApp).
 */
@Composable
fun ProfileScreen(
    profile: Profile,
    onOpenMenu: () -> Unit,
    details: ProfileDetails = ProfileDetails(),
    onPlayTrack: (List<Track>, Track) -> Unit = { _, _ -> }
) {
    val listState = rememberLazyListState()

    // Как в списках треков: справа оставляем место под ползунок прокрутки.
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(end = 22.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(26.dp)
        ) {
            item(key = "header") {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .glassSurface(corner = 22.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onOpenMenu),
                            contentAlignment = Alignment.Center
                        ) {
                            MenuIcon(color = Color.White, size = 22.dp)
                        }
                        Text(
                            text = "ПРОФИЛЬ",
                            color = Accent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                        // Пустое место справа, чтобы заголовок оставался по центру.
                        Spacer(modifier = Modifier.size(44.dp))
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top
                    ) {
                        UserAvatar(
                            name = profile.name,
                            avatarPath = profile.avatarPath,
                            avatarVersion = profile.avatarVersion,
                            size = 132.dp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = profile.name,
                            color = Color.White,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "@${profile.username}",
                            color = Accent,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = profile.status.ifEmpty { "Добавьте статус в меню слева" },
                            color = if (profile.status.isEmpty()) SecondaryText else Color.White,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .glassSurface(corner = 16.dp)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // Статистика, топ-5, любимые исполнитель и альбом, история и достижения.
            profileSections(details = details, onPlay = onPlayTrack)
        }

        ListScrollbar(
            state = listState,
            modifier = Modifier.align(Alignment.CenterEnd)
        )
    }
}
