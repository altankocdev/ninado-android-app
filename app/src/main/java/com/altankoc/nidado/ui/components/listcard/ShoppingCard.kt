package com.altankoc.nidado.ui.components.listcard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
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
import androidx.compose.ui.unit.dp
import com.altankoc.nidado.domain.model.ListItem
import com.altankoc.nidado.domain.model.TodoListWithItems
import com.altankoc.nidado.ui.components.common.GlassCard
import com.altankoc.nidado.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ShoppingCard(
    listWithItems: TodoListWithItems,
    onMuteToggle: () -> Unit,
    onDelete: () -> Unit,
    onViewAll: () -> Unit,
    onItemToggle: (itemId: Int, isCompleted: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val todoList = listWithItems.todoList
    val items = listWithItems.items
    val isAllCompleted = items.isNotEmpty() && items.all { it.isCompleted }
    val completedCount = items.count { it.isCompleted }
    val progress = if (items.isNotEmpty()) completedCount.toFloat() / items.size else 0f

    GlassCard(
        modifier = modifier
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .wrapContentHeight(),
        isAllCompleted = isAllCompleted
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(22.dp)
        ) {
            // ── Header ──
            val displayTimestamp = remember(todoList.scheduledDate, todoList.startTime) {
                val date = todoList.scheduledDate
                val time = todoList.startTime
                when {
                    date == null -> null
                    time == null -> date
                    else -> {
                        val dateCal = Calendar.getInstance().apply { timeInMillis = date }
                        val timeCal = Calendar.getInstance().apply { timeInMillis = time }
                        Calendar.getInstance().apply {
                            set(
                                dateCal.get(Calendar.YEAR),
                                dateCal.get(Calendar.MONTH),
                                dateCal.get(Calendar.DAY_OF_MONTH),
                                timeCal.get(Calendar.HOUR_OF_DAY),
                                timeCal.get(Calendar.MINUTE),
                                0
                            )
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                    }
                }
            }

            ShoppingHeader(
                title = todoList.title,
                scheduledDate = displayTimestamp,
                isAlarmEnabled = todoList.isAlarmEnabled,
                isMuted = todoList.isMuted,
                onMuteToggle = onMuteToggle
            )

            Spacer(Modifier.height(16.dp))

            // ── İlerleme ──
            ShoppingProgress(
                completedCount = completedCount,
                totalCount = items.size,
                progress = progress
            )

            Spacer(Modifier.height(16.dp))

            // ── Ürün Listesi (içerik kadar büyür, çok ürün olursa kaydırılabilir) ──
            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Henüz ürün eklenmedi",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.30f)
                    )
                }
            } else {
                LazyColumn(
                    userScrollEnabled = true,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        ShoppingItemRow(
                            item = item,
                            onToggle = { onItemToggle(item.id, !item.isCompleted) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Listeyi Aç Butonu ──
            Surface(
                onClick = onViewAll,
                color = VibrantTeal.copy(alpha = 0.12f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Listeyi Aç",
                        style = MaterialTheme.typography.labelLarge,
                        color = VibrantTeal,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = VibrantTeal,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
//  HEADER
// ─────────────────────────────────────────────
@Composable
private fun ShoppingHeader(
    title: String,
    scheduledDate: Long?,
    isAlarmEnabled: Boolean,
    isMuted: Boolean,
    onMuteToggle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            scheduledDate?.let { ts ->
                Surface(
                    color = VibrantTeal.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Rounded.Schedule, null, tint = VibrantTeal, modifier = Modifier.size(14.dp))
                        Text(
                            text = formatDateTime(ts),
                            style = MaterialTheme.typography.labelMedium,
                            color = VibrantTeal,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            AlarmIcon(isAlarmEnabled = isAlarmEnabled, isMuted = isMuted, onMuteToggle = onMuteToggle)
        }
    }
}

// ─────────────────────────────────────────────
//  İLERLEME BARI
// ─────────────────────────────────────────────
@Composable
private fun ShoppingProgress(completedCount: Int, totalCount: Int, progress: Float) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "$completedCount / $totalCount tamamlandı",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.50f),
                fontWeight = FontWeight.Medium
            )
            if (progress == 1f) {
                Text(
                    text = "✓ Tamamlandı",
                    style = MaterialTheme.typography.labelMedium,
                    color = VibrantTeal,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(3.dp).clip(CircleShape),
            color = VibrantTeal,
            trackColor = Color.White.copy(alpha = 0.10f)
        )
    }
}

// ─────────────────────────────────────────────
//  ÜRÜN SATIRI
// ─────────────────────────────────────────────
@Composable
private fun ShoppingItemRow(item: ListItem, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (item.isCompleted) Color.White.copy(alpha = 0.03f) else Color.White.copy(alpha = 0.06f))
            .border(0.5.dp, if (item.isCompleted) Color.White.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Checkbox(
            checked = item.isCompleted,
            onCheckedChange = { onToggle() },
            modifier = Modifier.size(22.dp),
            colors = CheckboxDefaults.colors(
                checkedColor = VibrantTeal,
                uncheckedColor = Color.White.copy(alpha = 0.30f),
                checkmarkColor = White
            )
        )
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (item.isCompleted) Color.White.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.88f),
            textDecoration = if (item.isCompleted) TextDecoration.LineThrough else null,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        item.quantity?.let { qty ->
            Surface(color = VibrantTeal.copy(alpha = 0.14f), shape = RoundedCornerShape(6.dp)) {
                Text(
                    text = qty,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = SoftTeal,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
//  ALARM İKONU — 3 durum
// ─────────────────────────────────────────────
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

// ─────────────────────────────────────────────
//  TARİH + SAAT FORMATLAMA
// ─────────────────────────────────────────────
private fun formatDateTime(timestamp: Long): String {
    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))

    val today = Calendar.getInstance()
    val target = Calendar.getInstance().apply { timeInMillis = timestamp }

    val isToday = today.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
            today.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)

    return if (isToday) {
        "Bugün, $time"
    } else {
        val datePart = SimpleDateFormat("d MMM EEE", Locale("tr")).format(Date(timestamp))
        "$datePart, $time"
    }
}