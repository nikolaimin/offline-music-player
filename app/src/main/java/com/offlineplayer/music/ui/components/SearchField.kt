package com.offlineplayer.music.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.remember
import com.offlineplayer.music.ui.theme.Accent
import com.offlineplayer.music.ui.theme.CardColor
import com.offlineplayer.music.ui.theme.SecondaryText

/**
 * Поле поиска. Сразу получает фокус и открывает клавиатуру; крестик справа вызывает [onClose]
 * (очистить запрос и свернуть поле).
 */
@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboard?.show()
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        placeholder = { Text("Поиск музыки...") },
        leadingIcon = { SearchIcon(color = Accent, size = 22.dp) },
        trailingIcon = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Text("✕", color = SecondaryText, fontSize = 16.sp)
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = Accent,
            unfocusedBorderColor = CardColor,
            focusedContainerColor = CardColor,
            unfocusedContainerColor = CardColor
        )
    )
}

/** Значок лупы. Рисуется сам, без набора иконок. */
@Composable
fun SearchIcon(
    modifier: Modifier = Modifier,
    color: Color,
    size: Dp = 24.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val stroke = w * 0.09f
        drawCircle(
            color = color,
            radius = w * 0.28f,
            center = Offset(w * 0.43f, w * 0.43f),
            style = Stroke(width = stroke)
        )
        drawLine(
            color = color,
            start = Offset(w * 0.64f, w * 0.64f),
            end = Offset(w * 0.86f, w * 0.86f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}
