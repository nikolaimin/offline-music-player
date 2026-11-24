package com.offlineplayer.music.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.offlineplayer.music.R

/**
 * Шрифт названий песен: Montserrat (SIL Open Font License, текст лицензии в assets/licenses).
 * Файлы урезаны до латиницы и кириллицы, поэтому весят около 58 КБ каждый.
 */
val TrackTitleFont = FontFamily(
    Font(R.font.montserrat_semibold, FontWeight.SemiBold),
    Font(R.font.montserrat_bold, FontWeight.Bold)
)
