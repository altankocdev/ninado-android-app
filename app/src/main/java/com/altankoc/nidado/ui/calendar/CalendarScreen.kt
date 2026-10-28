package com.altankoc.nidado.ui.calendar

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.altankoc.nidado.domain.model.TodoListWithItems
import com.altankoc.nidado.ui.components.common.DeleteConfirmDialog
import com.altankoc.nidado.ui.components.common.GlassCard
import com.altankoc.nidado.ui.components.common.luminance
import com.altankoc.nidado.ui.components.listcard.CalendarCard
import com.altankoc.nidado.ui.theme.*
import com.altankoc.nidado.util.UiEvent
import java.util.Calendar
import java.util.Locale

private val CalendarCyan = Color(0xFF06B6D4)
private val CalendarCyanLight = Color(0xFF22D3EE)

@Composable
fun CalendarScreen(
    navController: NavController,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentTime by viewModel.currentTime.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var listForAction by remember { mutableStateOf<TodoListWithItems?>(null) }
    var listToDelete by remember { mutableStateOf<TodoListWithItems?>(null) }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.Navigate -> navController.navigate(event.route)
                is UiEvent.ShowToast -> {
                    android.widget.Toast.makeText(context, event.message, android.widget.Toast.LENGTH_SHORT).show()
                }
                else -> Unit
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            CalendarHeader(
                viewMode = state.viewMode,
                onViewModeChanged = viewModel::onViewModeChanged
            )

            when (state.viewMode) {
                CalendarViewMode.GRID -> {
                    CalendarGridView(
                        displayedYear = state.displayedYear,
                        displayedMonth = state.displayedMonth,
                        allLists = state.allLists,
                        onPreviousMonth = viewModel::onPreviousMonth,
                        onNextMonth = viewModel::onNextMonth,
                        onDayClicked = viewModel::onDayClicked
                    )
                }
                CalendarViewMode.FEED -> {
                    CalendarFeedView(
                        lists = state.allLists,
                        currentTime = currentTime,
                        isLoading = state.isLoading,
                        onMuteToggle = { viewModel.onToggleMute(it.todoList) },
                        onItemToggle = viewModel::onToggleItemCompleted,
                        onViewAll = { viewModel.navigateToDetail(it.todoList.id) },
                        onSwipeOrLongPress = { listForAction = it }
                    )
                }
            }
        }

        // ── "Sil / Arşive At" seçenek dialog'u (Feed görünümü için) ──
        listForAction?.let { list ->
            CalendarActionDialog(
                title = list.todoList.title,
                onArchive = {
                    viewModel.onArchiveList(list.todoList)
                    listForAction = null
                },
                onDelete = {
                    listToDelete = list
                    listForAction = null
                },
                onDismiss = { listForAction = null }
            )
        }

        listToDelete?.let { list ->
            DeleteConfirmDialog(
                title = "Listeyi Sil",
                message = "\"${list.todoList.title}\" listesini silmek istiyor musun? Bu işlem geri alınamaz.",
                onConfirm = {
                    viewModel.onDeleteList(list.todoList)
                    listToDelete = null
                },
                onDismiss = { listToDelete = null }
            )
        }
    }

    // ── Gün detay paneli (Grid görünümünden tıklanınca) ──
    state.selectedDayMillis?.let { dayMillis ->
        DayDetailModal(
            dayMillis = dayMillis,
            existingList = state.selectedDayList,
            currentTime = currentTime,
            onDismiss = viewModel::onDayDetailDismiss,
            onCreateClick = {
                viewModel.onDayDetailDismiss()
                viewModel.navigateToCreateWithDate(dayMillis)
            },
            onMuteToggle = { viewModel.onToggleMute(it.todoList) },
            onItemToggle = viewModel::onToggleItemCompleted,
            onViewAll = {
                viewModel.onDayDetailDismiss()
                viewModel.navigateToDetail(it)
            }
        )
    }
}

// ─────────────────────────────────────────────
//  ÜST BAR — başlık + Grid/Feed toggle
// ─────────────────────────────────────────────
@Composable
private fun CalendarHeader(
    viewMode: CalendarViewMode,
    onViewModeChanged: (CalendarViewMode) -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(
                Brush.verticalGradient(
                    colors = if (isDark) {
                        listOf(
                            DeepOcean,
                            VibrantTeal,
                            SoftTeal
                        )
                    } else {
                        listOf(
                            DeepOcean,
                            VibrantTeal,
                            OffWhite
                        )
                    }
                )
            )
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Text(
                text = "Takvim",
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            )

            Spacer(Modifier.height(20.dp))

            // -- BUBBLE TOGGLE --
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color.White.copy(alpha = 0.15f))
                    .padding(4.dp)
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val containerWidth = maxWidth
                    val indicatorWidth = containerWidth / 2
                    val targetOffset = if (viewMode == CalendarViewMode.GRID) 0.dp else indicatorWidth
                    val animatedOffset by animateDpAsState(
                        targetValue = targetOffset,
                        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioLowBouncy),
                        label = "tab_offset"
                    )

                    // Bubble Indicator
                    Box(
                        modifier = Modifier
                            .offset(x = animatedOffset)
                            .width(indicatorWidth)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(22.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color.White, Color.White.copy(alpha = 0.9f))
                                )
                            )
                            .shadow(4.dp, RoundedCornerShape(22.dp))
                    )

                    Row(modifier = Modifier.fillMaxSize()) {
                        ViewModeTab(
                            label = "Takvim",
                            icon = Icons.Rounded.CalendarMonth,
                            isSelected = viewMode == CalendarViewMode.GRID,
                            onClick = { onViewModeChanged(CalendarViewMode.GRID) },
                            modifier = Modifier.weight(1f)
                        )
                        ViewModeTab(
                            label = "Planlar",
                            icon = Icons.AutoMirrored.Rounded.List,
                            isSelected = viewMode == CalendarViewMode.FEED,
                            onClick = { onViewModeChanged(CalendarViewMode.FEED) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ViewModeTab(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) VibrantTeal else Color.White.copy(alpha = 0.9f),
        animationSpec = tween(300),
        label = "tab_color"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(22.dp))
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                color = contentColor,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
            )
        }
    }
}

// ─────────────────────────────────────────────
//  TAKVİM GRID GÖRÜNÜMÜ
// ─────────────────────────────────────────────
@Composable
private fun CalendarGridView(
    displayedYear: Int,
    displayedMonth: Int,
    allLists: List<TodoListWithItems>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDayClicked: (Long) -> Unit
) {
    val monthNames = listOf(
        "Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran",
        "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık"
    )
    val weekDays = listOf("Pt", "Sa", "Ça", "Pe", "Cu", "Ct", "Pa")

    val firstDayCal = remember(displayedYear, displayedMonth) {
        Calendar.getInstance().apply {
            set(displayedYear, displayedMonth, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
    val daysInMonth = firstDayCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    // Pazartesi = 0 ... Pazar = 6 olacak şekilde kaydır
    val firstDayOfWeek = (firstDayCal.get(Calendar.DAY_OF_WEEK) + 5) % 7

    val today = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    // O ayda listesi olan günleri işaretlemek için set
    val markedDays = remember(allLists, displayedYear, displayedMonth) {
        allLists.mapNotNull { it.todoList.scheduledDate }
            .filter { date ->
                val cal = Calendar.getInstance().apply { timeInMillis = date }
                cal.get(Calendar.YEAR) == displayedYear && cal.get(Calendar.MONTH) == displayedMonth
            }
            .map { date -> Calendar.getInstance().apply { timeInMillis = date }.get(Calendar.DAY_OF_MONTH) }
            .toSet()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        // Ay navigasyonu - GLASS STYLE
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPreviousMonth,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Rounded.ChevronLeft, contentDescription = null, tint = VibrantTeal)
            }
            
            Text(
                text = "${monthNames[displayedMonth]} $displayedYear",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            IconButton(
                onClick = onNextMonth,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = VibrantTeal)
            }
        }

        Spacer(Modifier.height(28.dp))

        // Hafta günleri başlığı
        Row(modifier = Modifier.fillMaxWidth()) {
            weekDays.forEach { day ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Gün grid'i
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            userScrollEnabled = false
        ) {
            items(firstDayOfWeek) {
                Box(modifier = Modifier.aspectRatio(1f))
            }

            items(daysInMonth) { index ->
                val day = index + 1
                val dayCal = Calendar.getInstance().apply {
                    set(displayedYear, displayedMonth, day, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val isToday = dayCal.timeInMillis == today.timeInMillis
                val isPast = dayCal.timeInMillis < today.timeInMillis
                val hasList = markedDays.contains(day)

                DayCell(
                    day = day,
                    isToday = isToday,
                    isPast = isPast,
                    hasList = hasList,
                    onClick = { onDayClicked(dayCal.timeInMillis) }
                )
            }
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    isToday: Boolean,
    isPast: Boolean,
    hasList: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(14.dp))
            .background(
                when {
                    hasList -> Brush.linearGradient(listOf(VibrantTeal, SoftTeal))
                    isToday -> Brush.linearGradient(
                        listOf(VibrantTeal.copy(alpha = 0.15f), VibrantTeal.copy(alpha = 0.05f))
                    )
                    else -> Brush.linearGradient(
                        listOf(Color.Transparent, Color.Transparent)
                    )
                }
            )
            .then(
                if (isToday && !hasList) {
                    Modifier.border(1.5.dp, VibrantTeal.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                } else Modifier
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = day.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (hasList || isToday) FontWeight.ExtraBold else FontWeight.Medium,
                color = when {
                    hasList -> Color.White
                    isPast -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f)
                    isToday -> VibrantTeal
                    else -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f)
                }
            )
            if (hasList) {
                Spacer(Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
//  FEED GÖRÜNÜMÜ — CalendarCard listesi
// ─────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarFeedView(
    lists: List<TodoListWithItems>,
    currentTime: Long,
    isLoading: Boolean,
    onMuteToggle: (TodoListWithItems) -> Unit,
    onItemToggle: (Int, Boolean) -> Unit,
    onViewAll: (TodoListWithItems) -> Unit,
    onSwipeOrLongPress: (TodoListWithItems) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 12.dp, bottom = 140.dp)
    ) {
        when {
            isLoading -> {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(320.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = CalendarCyan, strokeWidth = 2.dp)
                    }
                }
            }
            lists.isEmpty() -> {
                item { CalendarEmptyFeedState() }
            }
            else -> {
                items(lists, key = { it.todoList.id }) { listWithItems ->
                    SwipeableCalendarCard(
                        listWithItems = listWithItems,
                        currentTime = currentTime,
                        onMuteToggle = { onMuteToggle(listWithItems) },
                        onItemToggle = onItemToggle,
                        onViewAll = { onViewAll(listWithItems) },
                        onSwipeOrLongPress = { onSwipeOrLongPress(listWithItems) }
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
//  KAYDIRILABİLİR / UZUN BASILABİLİR TAKVİM KARTI
// ─────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableCalendarCard(
    listWithItems: TodoListWithItems,
    currentTime: Long,
    onMuteToggle: () -> Unit,
    onItemToggle: (Int, Boolean) -> Unit,
    onViewAll: () -> Unit,
    onSwipeOrLongPress: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onSwipeOrLongPress()
                false
            } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(CalendarCyan.copy(alpha = 0.18f)),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Rounded.MoreHoriz,
                    contentDescription = "Seçenekler",
                    tint = CalendarCyan,
                    modifier = Modifier.padding(end = 36.dp)
                )
            }
        }
    ) {
        Box(
            modifier = Modifier.pointerInput(Unit) {
                detectTapGestures(onLongPress = { onSwipeOrLongPress() })
            }
        ) {
            CalendarCard(
                listWithItems = listWithItems,
                currentTime = currentTime,
                onMuteToggle = onMuteToggle,
                onDelete = onSwipeOrLongPress,
                onViewAll = onViewAll,
                onItemToggle = onItemToggle
            )
        }
    }
}
@Composable
private fun CalendarEmptyFeedState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "📅", fontSize = 56.sp)
        Text(
            text = "Henüz bir planın yok",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Takvim görünümünden bir güne dokunarak\nplan oluşturabilirsin!",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
            textAlign = TextAlign.Center
        )
    }
}

// ─────────────────────────────────────────────
//  GÜN DETAY MODAL'I — boşsa "+" oluştur, doluysa önizleme
// ─────────────────────────────────────────────
@Composable
private fun DayDetailModal(
    dayMillis: Long,
    existingList: TodoListWithItems?,
    currentTime: Long,
    onDismiss: () -> Unit,
    onCreateClick: () -> Unit,
    onMuteToggle: (TodoListWithItems) -> Unit,
    onItemToggle: (Int, Boolean) -> Unit,
    onViewAll: (Int) -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val dateLabel = remember(dayMillis) {
        java.text.SimpleDateFormat("d MMMM EEEE", Locale("tr")).format(java.util.Date(dayMillis))
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(400)),
                exit = fadeOut(tween(300))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable { onDismiss() }
                )
            }

            AnimatedVisibility(
                visible = visible,
                enter = scaleIn(initialScale = 0.9f) + fadeIn(),
                exit = scaleOut(targetScale = 0.9f) + fadeOut(),
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .clickable(enabled = false) { },
                    cornerRadius = 32.dp
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = dateLabel,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Rounded.Close, null, tint = Color.White.copy(alpha = 0.6f))
                            }
                        }

                        Spacer(Modifier.height(24.dp))

                        if (existingList == null) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.05f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.EventNote,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.3f),
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    text = "Henüz bir planın yok",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(Modifier.height(32.dp))
                                Button(
                                    onClick = onCreateClick,
                                    shape = RoundedCornerShape(20.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = VibrantTeal,
                                        contentColor = Color.White
                                    ),
                                    contentPadding = PaddingValues(horizontal = 32.dp, vertical = 14.dp),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                                ) {
                                    Icon(Icons.Rounded.Add, null, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Plan Oluştur", fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            CalendarCard(
                                listWithItems = existingList,
                                currentTime = currentTime,
                                onMuteToggle = { onMuteToggle(existingList) },
                                onDelete = { },
                                onViewAll = { onViewAll(existingList.todoList.id) },
                                onItemToggle = onItemToggle,
                                modifier = Modifier.padding(horizontal = 0.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
//  "SİL / ARŞİVE AT" SEÇENEK DİALOĞU (Feed için)
// ─────────────────────────────────────────────
@Composable
private fun CalendarActionDialog(
    title: String,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1
                )
                Spacer(Modifier.height(12.dp))

                CalendarActionRow(
                    icon = Icons.Rounded.Archive,
                    label = "Arşive At",
                    tint = CalendarCyan,
                    onClick = onArchive
                )
                CalendarActionRow(
                    icon = Icons.Rounded.Delete,
                    label = "Sil",
                    tint = MaterialTheme.colorScheme.error,
                    onClick = onDelete
                )

                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Vazgeç", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                }
            }
        }
    }
}

@Composable
private fun CalendarActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(tint.copy(alpha = 0.08f))
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
        Text(text = label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = tint)
    }
}