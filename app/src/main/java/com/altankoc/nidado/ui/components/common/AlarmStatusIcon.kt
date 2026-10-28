package com.altankoc.nidado.ui.components.common

import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.size
import androidx.compose.ui.res.stringResource
import com.altankoc.nidado.ui.theme.VibrantTeal

@Composable
fun AlarmStatusIcon(
    isAlarmEnabled: Boolean,
    isMuted: Boolean,
    onMuteToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    when {
        // Alarmsız — gri, dokunulamaz
        !isAlarmEnabled -> {
            Icon(
                imageVector = Icons.Rounded.NotificationsOff,
                contentDescription = "Alarmsız",
                tint = Color.Gray.copy(alpha = 0.4f),
                modifier = modifier.then(androidx.compose.ui.Modifier.size(20.dp))
            )
        }

        // Alarmlı + Sessiz
        isMuted -> {
            IconButton(
                onClick = onMuteToggle,
                modifier = modifier
            ) {
                Icon(
                    imageVector = Icons.Rounded.NotificationsOff,
                    contentDescription = "Sessiz",
                    tint = VibrantTeal.copy(alpha = 0.5f),
                    modifier = androidx.compose.ui.Modifier.size(20.dp)
                )
            }
        }

        // Alarmlı + Aktif
        else -> {
            IconButton(
                onClick = onMuteToggle,
                modifier = modifier
            ) {
                Icon(
                    imageVector = Icons.Rounded.Notifications,
                    contentDescription = "Aktif",
                    tint = VibrantTeal,
                    modifier = androidx.compose.ui.Modifier.size(20.dp)
                )
            }
        }
    }
}