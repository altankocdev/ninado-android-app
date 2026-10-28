package com.altankoc.nidado.ui.components.listcard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.altankoc.nidado.domain.model.ListItem
import com.altankoc.nidado.domain.model.TodoListWithItems
import com.altankoc.nidado.ui.components.common.GlassCard
import com.altankoc.nidado.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

// Görev saati geçtikten sonra "geçmiş" sayılmadan önceki tolerans süresi.
// Bu pencere içinde görev hâlâ next pozisyonda kalır ama daha güçlü parlar.
private const val DUE_NOW_GRACE_MS = 60_000L

@Composable
fun EducationPersonalDailyCard(
    listWithItems: TodoListWithItems,
    currentTime: Long,
    onMuteToggle: () -> Unit,
    onDelete: () -> Unit,
    onViewAll: () -> Unit,
    onItemToggle: (itemId: Int, isCompleted: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val todoList = listWithItems.todoList
    val items = remember(listWithItems.items) {
        listWithItems.items.sortedBy { it.reminderTime ?: Long.MAX_VALUE }
    }
    val now = currentTime

    // Artık sadece tik atılmışsa VEYA saatinden 1 dakikadan fazla geçmişse "past" sayılır
    fun ListItem.isPastOrDone() = isCompleted ||
            (reminderTime != null && (now - reminderTime) >= DUE_NOW_GRACE_MS)

    val isAllCompleted = items.isNotEmpty() && items.all { it.isCompleted }

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
                .clickable { onViewAll() }
                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 20.dp)
        ) {
            CardHeader(
                title          = todoList.title,
                createdAt      = todoList.createdAt,
                isAlarmEnabled = todoList.isAlarmEnabled,
                isMuted        = todoList.isMuted,
                isAllCompleted = isAllCompleted,
                onMuteToggle   = onMuteToggle
            )

            Spacer(Modifier.height(14.dp))

            Box(modifier = Modifier.fillMaxWidth()) {
                if (todoList.isTimed) {
                    TimedTaskContent(items = items, now = now, onToggle = onItemToggle)
                } else {
                    UntimedTaskContent(items = items, onToggle = onItemToggle)
                }
            }
        }
    }
}

@Composable
private fun CardHeader(
    title: String,
    createdAt: Long,
    isAlarmEnabled: Boolean,
    isMuted: Boolean,
    isAllCompleted: Boolean,
    onMuteToggle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text           = title,
                style          = MaterialTheme.typography.titleMedium,
                fontWeight     = FontWeight.Bold,
                color          = if (isAllCompleted) Color.White.copy(alpha = 0.45f) else Color.White,
                textDecoration = if (isAllCompleted) TextDecoration.LineThrough else null,
                maxLines       = 1,
                overflow       = TextOverflow.Ellipsis,
                modifier       = Modifier.weight(1f, fill = false)
            )
            Text(
                text  = formatListDate(createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.40f),
                maxLines = 1
            )
        }
        Spacer(Modifier.width(8.dp))
        AlarmIcon(isAlarmEnabled = isAlarmEnabled, isMuted = isMuted, onMuteToggle = onMuteToggle)
    }
}

@Composable
private fun TimedTaskContent(
    items: List<ListItem>,
    now: Long,
    onToggle: (Int, Boolean) -> Unit
) {
    if (items.isEmpty()) { EmptyTaskHint(); return }

    fun ListItem.isPastOrDone() = isCompleted ||
            (reminderTime != null && (now - reminderTime) >= DUE_NOW_GRACE_MS)

    val allPast = items.all { it.isPastOrDone() }

    if (allPast) {
        val showItems = items.takeLast(minOf(3, items.size))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            showItems.forEach { item ->
                SmallTaskRow(item = item, isPast = true, onToggle = onToggle)
            }
        }
    } else {
        val nextIndex = items.indexOfFirst { !it.isPastOrDone() }
        val prevItem  = if (nextIndex > 0) items[nextIndex - 1] else null
        val nextItem  = items.getOrNull(nextIndex)
        val afterItem = items.getOrNull(nextIndex + 1)

        // Next görev şu an "due now" mu? (saati geldi ama henüz 1 dk geçmedi)
        val isDueNow = nextItem?.reminderTime?.let { rt ->
            now >= rt && (now - rt) < DUE_NOW_GRACE_MS
        } ?: false

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            prevItem?.let {
                SmallTaskRow(item = it, isPast = true, onToggle = onToggle)
            }

            nextItem?.let {
                AnimatedContent(
                    targetState = it,
                    transitionSpec = {
                        (slideInVertically { height -> height / 3 } + fadeIn(tween(350))) togetherWith
                                (slideOutVertically { height -> -height / 3 } + fadeOut(tween(200)))
                    },
                    label = "next_task_transition"
                ) { animatedItem ->
                    NextTaskBox(
                        item = animatedItem,
                        isDueNow = isDueNow,
                        onToggle = onToggle
                    )
                }
            }

            afterItem?.let {
                SmallTaskRow(item = it, isPast = false, onToggle = onToggle)
            }
        }
    }
}

@Composable
private fun UntimedTaskContent(
    items: List<ListItem>,
    onToggle: (Int, Boolean) -> Unit
) {
    if (items.isEmpty()) { EmptyTaskHint(); return }

    LazyColumn(
        userScrollEnabled   = true,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier            = Modifier.heightIn(max = 280.dp)
    ) {
        items(items, key = { it.id }) { item ->
            UntimedTaskRow(item = item, onToggle = onToggle)
        }
    }
}

@Composable
private fun ToggleCircle(
    isCompleted: Boolean,
    onClick: () -> Unit,
    size: Dp = 22.dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (isCompleted) VibrantTeal else Color.Transparent)
            .border(
                width = 1.5.dp,
                color = if (isCompleted) VibrantTeal else Color.White.copy(alpha = 0.32f),
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
private fun NextTaskBox(
    item: ListItem,
    isDueNow: Boolean,
    onToggle: (Int, Boolean) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "next_glow")

    // Normal durumda yumuşak glow, "due now" durumunda çok daha yoğun ve hızlı parlama
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue  = if (isDueNow) 0.65f else 0.45f,
        targetValue   = 1.0f,
        animationSpec = infiniteRepeatable(
            animation  = tween(if (isDueNow) 700 else 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    // "Due now" iken arka plan rengi de daha canlı parlasın
    val bgPulse by infiniteTransition.animateFloat(
        initialValue = 0.20f,
        targetValue  = if (isDueNow) 0.45f else 0.25f,
        animationSpec = infiniteRepeatable(
            animation  = tween(if (isDueNow) 700 else 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bg_pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(VibrantTeal.copy(alpha = bgPulse), DeepOcean.copy(alpha = 0.12f))
                )
            )
            .drawWithContent {
                drawContent()
                val r = 18.dp.toPx()
                val path = Path().apply {
                    moveTo(r, 0f); lineTo(size.width - r, 0f)
                    quadraticTo(size.width, 0f, size.width, r)
                    lineTo(size.width, size.height - r)
                    quadraticTo(size.width, size.height, size.width - r, size.height)
                    lineTo(r, size.height)
                    quadraticTo(0f, size.height, 0f, size.height - r)
                    lineTo(0f, r); quadraticTo(0f, 0f, r, 0f); close()
                }
                drawPath(
                    path  = path,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            VibrantTeal.copy(alpha = glowAlpha),
                            Color.White.copy(alpha = glowAlpha * 0.55f),
                            SoftTeal.copy(alpha = glowAlpha * 0.65f),
                            Color.Transparent
                        )
                    ),
                    style = Stroke(width = if (isDueNow) 2.2.dp.toPx() else 1.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = if (isDueNow) VibrantTeal.copy(alpha = 0.40f) else VibrantTeal.copy(alpha = 0.22f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isDueNow) Icons.Rounded.Bolt else Icons.Rounded.Star,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(10.dp)
                        )
                        Text(
                            text = if (isDueNow) "Şimdi!" else "Sıradaki Görev",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.3.sp
                        )
                    }
                }

                ToggleCircle(
                    isCompleted = item.isCompleted,
                    onClick = { onToggle(item.id, !item.isCompleted) }
                )
            }

            Text(
                text       = item.title,
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color      = Color.White,
                maxLines   = 2,
                overflow   = TextOverflow.Ellipsis
            )

            if (item.courseName != null && item.courseName != item.title) {
                Text(item.courseName, style = MaterialTheme.typography.labelMedium, color = SoftTeal.copy(alpha = 0.85f), fontWeight = FontWeight.Medium)
            }

            item.reminderTime?.let { ts ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Icon(Icons.Rounded.Schedule, null, tint = Color.White.copy(alpha = 0.50f), modifier = Modifier.size(14.dp))
                    Text(formatTime(ts), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.75f), fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun SmallTaskRow(
    item: ListItem,
    isPast: Boolean,
    onToggle: (Int, Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text           = item.title,
            style          = MaterialTheme.typography.bodySmall,
            color          = if (isPast) Color.White.copy(alpha = 0.32f) else Color.White.copy(alpha = 0.62f),
            textDecoration = if (isPast) TextDecoration.LineThrough else null,
            fontWeight     = FontWeight.Medium,
            maxLines       = 1,
            overflow       = TextOverflow.Ellipsis,
            modifier       = Modifier.weight(1f)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item.reminderTime?.let { ts ->
                Text(
                    text       = formatTime(ts),
                    style      = MaterialTheme.typography.labelSmall,
                    color      = if (isPast) Color.White.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.40f),
                    fontWeight = FontWeight.Medium
                )
            }
            ToggleCircle(
                isCompleted = item.isCompleted,
                onClick = { onToggle(item.id, !item.isCompleted) },
                size = 18.dp
            )
        }
    }
}

@Composable
private fun UntimedTaskRow(
    item: ListItem,
    onToggle: (Int, Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(0.5.dp, Color.White.copy(alpha = 0.09f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(if (item.isCompleted) VibrantTeal else Color.White.copy(alpha = 0.25f)))
            Text(
                text           = item.title,
                style          = MaterialTheme.typography.bodySmall,
                color          = if (item.isCompleted) Color.White.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.85f),
                textDecoration = if (item.isCompleted) TextDecoration.LineThrough else null,
                maxLines       = 1,
                overflow       = TextOverflow.Ellipsis,
                modifier       = Modifier.weight(1f)
            )
            if (item.courseName != null && item.courseName != item.title) {
                Surface(color = VibrantTeal.copy(alpha = 0.12f), shape = RoundedCornerShape(6.dp)) {
                    Text(item.courseName, style = MaterialTheme.typography.labelSmall, color = SoftTeal, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                }
            }
            ToggleCircle(
                isCompleted = item.isCompleted,
                onClick = { onToggle(item.id, !item.isCompleted) },
                size = 18.dp
            )
        }
    }
}

@Composable
private fun EmptyTaskHint() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Henüz görev eklenmedi", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.30f))
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

private fun formatTime(timestamp: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))