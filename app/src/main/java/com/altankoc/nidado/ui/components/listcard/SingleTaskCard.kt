package com.altankoc.nidado.ui.components.listcard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.altankoc.nidado.domain.model.TodoListWithItems
import com.altankoc.nidado.ui.components.common.GlassCard
import com.altankoc.nidado.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

private val UrgentCoral = Color(0xFFFF6B5B)
private val UrgentAmber = Color(0xFFFFA552)

@Composable
fun SingleTaskCard(
    listWithItems: TodoListWithItems,
    onMuteToggle: () -> Unit,
    onItemToggle: (Int, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val todoList    = listWithItems.todoList
    val item        = listWithItems.items.firstOrNull()
    val now         = System.currentTimeMillis()
    val isCompleted = item?.isCompleted == true
    val isPast      = (item?.reminderTime ?: Long.MAX_VALUE) < now

    GlassCard(
        modifier = modifier
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .wrapContentHeight(),
        isAllCompleted = isCompleted
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 18.dp)
        ) {
            // ── Header ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = UrgentCoral.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(Icons.Rounded.Bolt, null, tint = UrgentCoral, modifier = Modifier.size(13.dp))
                            Text(
                                text = "Tekli Görev",
                                style = MaterialTheme.typography.labelMedium,
                                color = UrgentCoral,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.3.sp
                            )
                        }
                    }
                    Text(
                        text  = formatListDate(todoList.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.40f),
                        maxLines = 1
                    )
                }
                AlarmIcon(
                    isAlarmEnabled = todoList.isAlarmEnabled,
                    isMuted        = todoList.isMuted,
                    onMuteToggle   = onMuteToggle
                )
            }

            Spacer(Modifier.height(12.dp))

            if (item != null) {
                TaskRow(
                    title        = item.title,
                    reminderTime = item.reminderTime,
                    isCompleted  = isCompleted,
                    isPast       = isPast,
                    onToggle     = { onItemToggle(item.id, !isCompleted) }
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text  = "Görev bulunamadı",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.30f)
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskRow(
    title: String,
    reminderTime: Long?,
    isCompleted: Boolean,
    isPast: Boolean,
    onToggle: () -> Unit
) {
    val accentColor = when {
        isCompleted -> Color(0xFF4CAF50)
        isPast      -> UrgentAmber
        else        -> UrgentCoral
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(accentColor.copy(alpha = 0.20f), accentColor.copy(alpha = 0.06f))
                )
            )
            .border(
                width = 1.dp,
                color = accentColor.copy(alpha = 0.30f),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = when {
                    isCompleted -> Icons.Rounded.CheckCircle
                    isPast      -> Icons.Rounded.Schedule
                    else        -> Icons.Rounded.PlayArrow
                },
                contentDescription = null,
                tint     = accentColor,
                modifier = Modifier.size(20.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text           = title,
                    style          = MaterialTheme.typography.bodyMedium,
                    fontWeight     = FontWeight.SemiBold,
                    color          = if (isCompleted) Color.White.copy(alpha = 0.40f) else Color.White,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else null,
                    maxLines       = 1,
                    overflow       = TextOverflow.Ellipsis
                )
                reminderTime?.let { ts ->
                    Text(
                        text       = formatTime(ts),
                        style      = MaterialTheme.typography.labelSmall,
                        color      = accentColor.copy(alpha = 0.80f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            ToggleCircle(
                isCompleted = isCompleted,
                onClick = onToggle,
                accentColor = accentColor
            )
        }
    }
}

@Composable
private fun ToggleCircle(
    isCompleted: Boolean,
    onClick: () -> Unit,
    accentColor: Color,
    size: Dp = 22.dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (isCompleted) accentColor else Color.Transparent)
            .border(
                width = 1.5.dp,
                color = if (isCompleted) accentColor else Color.White.copy(alpha = 0.32f),
                shape = CircleShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isCompleted) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = "Tamamlandı",
                tint = Color.White,
                modifier = Modifier.size(size * 0.6f)
            )
        }
    }
}

@Composable
private fun AlarmIcon(isAlarmEnabled: Boolean, isMuted: Boolean, onMuteToggle: () -> Unit) {
    val iconColor = if (isAlarmEnabled) VibrantTeal else Color.White.copy(alpha = 0.28f)
    val bgColor   = if (isAlarmEnabled) VibrantTeal.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.04f)
    val showLine  = isMuted || !isAlarmEnabled

    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(bgColor)
            .then(if (isAlarmEnabled) Modifier.border(0.5.dp, VibrantTeal.copy(alpha = 0.28f), CircleShape) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        IconButton(onClick = { if (isAlarmEnabled) onMuteToggle() }, enabled = isAlarmEnabled, modifier = Modifier.size(36.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.NotificationsActive, null, tint = iconColor, modifier = Modifier.size(18.dp))
                if (showLine) {
                    Canvas(modifier = Modifier.size(18.dp)) {
                        drawLine(color = iconColor, start = Offset(0f, size.height), end = Offset(size.width, 0f), strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
                    }
                }
            }
        }
    }
}

private fun formatTime(timestamp: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))

private fun formatListDate(createdAt: Long): String {
    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply { timeInMillis = createdAt }

    val isToday = now.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)

    if (isToday) return "Bugün"

    val diffDays = (now.timeInMillis - createdAt) / (1000 * 60 * 60 * 24)

    return if (diffDays < 7) {
        SimpleDateFormat("d MMM EEE", Locale("tr")).format(Date(createdAt))
    } else {
        SimpleDateFormat("d MMM yyyy", Locale("tr")).format(Date(createdAt))
    }
}