package com.offlineplayer.music.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offlineplayer.music.data.MaxProfileNameLength
import com.offlineplayer.music.data.MaxStatusLength
import com.offlineplayer.music.data.Profile
import com.offlineplayer.music.data.cleanUsername
import com.offlineplayer.music.ui.components.UserAvatar
import com.offlineplayer.music.ui.components.dialogFieldColors
import com.offlineplayer.music.ui.theme.Accent
import com.offlineplayer.music.ui.theme.SecondaryText

private val PanelTop = Color(0xFF221440)
private val PanelBottom = Color(0xFF0F0820)
private val DangerColor = Color(0xFFFF8A80)

/** Насколько нужно сдвинуть палец влево, чтобы панель закрылась свайпом. */
private const val SwipeCloseDistancePx = 80f

/**
 * Боковая панель редактирования профиля: плавно выезжает слева поверх приложения.
 * Закрывается нажатием на затемнённую область, свайпом влево, крестиком или кнопкой «назад».
 * Поля заполняются заново при каждом открытии.
 */
@Composable
fun ProfileSidePanel(
    visible: Boolean,
    profile: Profile,
    onClose: () -> Unit,
    onSave: (name: String, username: String, status: String) -> Unit,
    onPhotoPicked: (Uri) -> Unit,
    onRemovePhoto: () -> Unit
) {
    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) onPhotoPicked(uri) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Затемнение: нажатие по нему закрывает панель.
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(260)),
            exit = fadeOut(tween(220))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClose
                    )
            )
        }

        AnimatedVisibility(
            visible = visible,
            enter = slideInHorizontally(tween(340, easing = FastOutSlowInEasing)) { -it } +
                fadeIn(tween(220)),
            exit = slideOutHorizontally(tween(260, easing = FastOutSlowInEasing)) { -it } +
                fadeOut(tween(220))
        ) {
            PanelContent(
                profile = profile,
                onClose = onClose,
                onSave = onSave,
                onPickPhoto = {
                    photoPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onRemovePhoto = onRemovePhoto
            )
        }
    }
}

@Composable
private fun PanelContent(
    profile: Profile,
    onClose: () -> Unit,
    onSave: (name: String, username: String, status: String) -> Unit,
    onPickPhoto: () -> Unit,
    onRemovePhoto: () -> Unit
) {
    var name by remember { mutableStateOf(profile.name) }
    var username by remember { mutableStateOf(profile.username) }
    var status by remember { mutableStateOf(profile.status) }
    val canSave = name.isNotBlank() && username.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(0.84f)
            .widthIn(max = 340.dp)
            .background(
                Brush.verticalGradient(listOf(PanelTop, PanelBottom)),
                RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)
            )
            // Касания внутри панели не должны доходить до затемнения и закрывать её.
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .pointerInput(Unit) {
                var total = 0f
                detectHorizontalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = { if (total < -SwipeCloseDistancePx) onClose() },
                    onHorizontalDrag = { _, delta -> total += delta }
                )
            }
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "ПРОФИЛЬ",
                    color = Accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Редактирование",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Text("✕", color = SecondaryText, fontSize = 18.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Фото
        Row(verticalAlignment = Alignment.CenterVertically) {
            UserAvatar(
                name = name,
                avatarPath = profile.avatarPath,
                avatarVersion = profile.avatarVersion,
                size = 84.dp
            )
            Spacer(modifier = Modifier.size(16.dp))
            Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                PanelButton(text = "Сменить фото", onClick = onPickPhoto)
                if (profile.avatarPath != null) {
                    PanelButton(text = "Удалить фото", color = DangerColor, onClick = onRemovePhoto)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(MaxProfileNameLength) },
            singleLine = true,
            label = { Text("Имя", color = SecondaryText) },
            colors = dialogFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = cleanUsername(it) },
            singleLine = true,
            prefix = { Text("@", color = Accent) },
            label = { Text("Имя пользователя", color = SecondaryText) },
            colors = dialogFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = status,
            onValueChange = { status = it.take(MaxStatusLength) },
            maxLines = 2,
            label = { Text("Статус", color = SecondaryText) },
            colors = dialogFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(if (canSave) Accent else Accent.copy(alpha = 0.3f))
                .clickable(enabled = canSave) {
                    onSave(name, username, status)
                    onClose()
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Сохранить",
                color = Color.Black,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun PanelButton(
    text: String,
    onClick: () -> Unit,
    color: Color = Color.White
) {
    Text(
        text = text,
        color = color,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .glassSurface(corner = 16.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp)
    )
}
