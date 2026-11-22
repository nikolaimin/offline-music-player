package com.offlineplayer.music.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/** Android 13+ использует READ_MEDIA_AUDIO, более старые версии - READ_EXTERNAL_STORAGE. */
val AudioPermission: String
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

fun hasAudioPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, AudioPermission) == PackageManager.PERMISSION_GRANTED
