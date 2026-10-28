package com.altankoc.nidado.ui.listdetail

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.altankoc.nidado.domain.model.ListItem
import com.altankoc.nidado.domain.model.enums.ListType
import com.altankoc.nidado.ui.components.common.DeleteConfirmDialog
import com.altankoc.nidado.ui.theme.*
import com.altankoc.nidado.util.UiEvent
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun ListDetailScreen(
    navController: NavController,
    viewModel: ListDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.NavigateBack -> navController.popBackStack()
                is UiEvent.ShowToast -> {
                    android.widget.Toast.makeText(
                        context, event.message, android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
                else -> Unit
            }
        }
    }

    val todoList = state.todoList
    val completedCount = remember(state.items) { state.items.count { it.isCompleted } }
    val totalCount = remember(state.items) { state.items.size }
    val progress = if (totalCount == 0) 0f else completedCount.toFloat() / totalCount

    // Yeni görev eklenebilir mi? Liste expire olmamış ve arşivlenmemişse aktif
    val canAddTask = remember(todoList) {
        todoList != null && !todoList.isExpired && !todoList.isArchived && !todoList.isCompleted
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(colors = listOf(SurfaceDark, Color(0xFF04141A)))
            )
    ) {
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.TopCenter)
                .offset(y = (-100).dp)
                .background(VibrantTeal.copy(alpha = 0.15f), CircleShape)
                .alpha(0.5f)
        )

        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                DetailTopBar(
                    onBack = { navController.popBackStack() },
                    onDeleteList = viewModel::onDeleteListRequest,
                    isSelectionMode = state.isSelectionMode,
                    onSelectionModeToggle = viewModel::onSelectionModeToggle
                )
            }
        ) { paddingValues ->
            if (state.isLoading || todoList == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = VibrantTeal)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(bottom = 120.dp, top = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        ListHeroSection(
                            title = todoList.title,
                            type = todoList.type,
                            progress = progress,
                            completedCount = completedCount,
                            totalCount = totalCount,
                            onTitleClick = viewModel::onEditListTitleClick
                        )
                    }

                    item { Spacer(modifier = Modifier.height(24.dp)) }

                    if (state.items.isEmpty() && !state.isAddingTask) {
                        item { EmptyStateView() }
                    } else {
                        items(state.items, key = { it.id }) { item ->
                            ModernTaskItem(
                                item = item,
                                type = todoList.type,
                                isTimed = todoList.isTimed,
                                isPast = viewModel.isPast(item),
                                isSelectionMode = state.isSelectionMode,
                                isSelected = state.selectedItemIds.contains(item.id),
                                onToggleComplete = { viewModel.onToggleItemCompleted(item) },
                                onTitleClick = { viewModel.onEditItemClick(item) },
                                onTimeClick = { viewModel.onTimePickerOpen(item) },
                                onSelectToggle = { viewModel.onItemSelected(item) },
                                onDeleteSingle = { viewModel.onDeleteSingleItem(item) }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                        if (state.isAddingTask) {
                            AddTaskModernCard(
                                type = todoList.type,
                                isTimed = todoList.isTimed,
                                title = state.newTaskTitle,
                                time = state.newTaskTime,
                                onTitleChanged = viewModel::onNewTaskTitleChanged,
                                onConfirm = viewModel::onAddTaskConfirm,
                                onDismiss = viewModel::onAddTaskDismiss,
                                onTimeSelected = viewModel::onNewTaskTimeSelected
                            )
                        } else if (canAddTask) {
                            // Sadece liste aktifse + butonu göster
                            FloatingAddButton(onClick = viewModel::onAddTaskClick)
                        }
                    }
                }
            }
        }

        SelectionActionBar(
            visible = state.isSelectionMode && state.selectedItemIds.isNotEmpty(),
            selectedCount = state.selectedItemIds.size,
            onDelete = viewModel::onDeleteSelectedRequest
        )
    }

    // Mevcut görev saat değiştirme dialog'u
    state.timePickerItemId?.let {
        NinaDoTimePickerDialog(
            onConfirm = { hour, minute ->
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                }
                // Geçmiş saat kontrolü
                if (cal.timeInMillis <= System.currentTimeMillis()) {
                    "Bu saat geçmiş, geçerli olamaz"
                } else {
                    viewModel.onTimeSelected(cal.timeInMillis)
                    null
                }
            },
            onDismiss = viewModel::onTimePickerDismiss
        )
    }

    if (state.showDeleteListDialog) {
        DeleteConfirmDialog(
            title = "Listeyi Sil",
            message = "Bu listeyi tamamen silmek istediğinizden emin misiniz?",
            onConfirm = viewModel::onDeleteListConfirm,
            onDismiss = viewModel::onDeleteListDismiss
        )
    }

    if (state.showDeleteSelectedDialog) {
        DeleteConfirmDialog(
            title = "Seçilenleri Sil",
            message = "${state.selectedItemIds.size} görevi silmek istiyor musun?",
            onConfirm = viewModel::onDeleteSelectedConfirm,
            onDismiss = viewModel::onDeleteSelectedDismiss
        )
    }
}

// ─── Top Bar ────────────────────────────────────────────────────────────────

@Composable
private fun DetailTopBar(
    onBack: () -> Unit,
    onDeleteList: () -> Unit,
    isSelectionMode: Boolean,
    onSelectionModeToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Surface(
            onClick = onBack,
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.08f),
            modifier = Modifier.size(44.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Rounded.ArrowBack,
                "Geri",
                tint = Color.White,
                modifier = Modifier.padding(12.dp)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(
                onClick = onSelectionModeToggle,
                shape = CircleShape,
                color = if (isSelectionMode) VibrantTeal else Color.White.copy(alpha = 0.08f),
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = if (isSelectionMode) Icons.Rounded.Close else Icons.Rounded.Checklist,
                    contentDescription = "Seç",
                    tint = Color.White,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Surface(
                onClick = onDeleteList,
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.08f),
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    Icons.Rounded.DeleteOutline,
                    "Sil",
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}

// ─── Hero Section ────────────────────────────────────────────────────────────

@Composable
private fun ListHeroSection(
    title: String,
    type: ListType,
    progress: Float,
    completedCount: Int,
    totalCount: Int,
    onTitleClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(180.dp)) {
            CircularProgressIndicator(
                progress = { 1f },
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.1f),
                color = Color.White,
                strokeWidth = 14.dp,
                strokeCap = StrokeCap.Round,
            )
            val animatedProgress by animateFloatAsState(
                targetValue = progress,
                animationSpec = tween(1200, easing = FastOutSlowInEasing),
                label = "progress"
            )
            CircularProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxSize(),
                color = VibrantTeal,
                strokeWidth = 14.dp,
                strokeCap = StrokeCap.Round,
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = "$completedCount/$totalCount Tamamlandı",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.4f)
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 32.sp,
                lineHeight = 40.sp
            ),
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.clickable { onTitleClick() }
        )

        Spacer(modifier = Modifier.height(12.dp))

        Surface(
            color = VibrantTeal.copy(alpha = 0.12f),
            shape = RoundedCornerShape(100.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, VibrantTeal.copy(alpha = 0.25f))
        ) {
            Text(
                text = typeLabel(type),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                color = SoftTeal,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ─── Task Item ───────────────────────────────────────────────────────────────

@Composable
private fun ModernTaskItem(
    item: ListItem,
    type: ListType,
    isTimed: Boolean,
    isPast: Boolean,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onToggleComplete: () -> Unit,
    onTitleClick: () -> Unit,
    onTimeClick: () -> Unit,
    onSelectToggle: () -> Unit,
    onDeleteSingle: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(32.dp),
        color = if (isSelected) VibrantTeal.copy(alpha = 0.18f)
        else Color.White.copy(alpha = 0.05f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) VibrantTeal.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.08f)
        ),
        onClick = { if (isSelectionMode) onSelectToggle() else onTitleClick() }
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        if (item.isCompleted) VibrantTeal
                        else Color.White.copy(alpha = 0.1f)
                    )
                    .clickable { onToggleComplete() },
                contentAlignment = Alignment.Center
            ) {
                if (item.isCompleted) {
                    Icon(
                        Icons.Rounded.Check,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (item.isCompleted) Color.White.copy(alpha = 0.35f) else Color.White,
                    textDecoration = if (item.isCompleted) TextDecoration.LineThrough else null,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (isTimed && item.reminderTime != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .clickable { onTimeClick() }
                    ) {
                        Icon(
                            Icons.Rounded.Schedule,
                            null,
                            tint = SoftTeal,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = formatTime(item.reminderTime),
                            style = MaterialTheme.typography.labelMedium,
                            color = SoftTeal
                        )
                    }
                }
            }

            if (!isSelectionMode) {
                IconButton(onClick = onDeleteSingle, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Rounded.Close,
                        null,
                        tint = Color.White.copy(alpha = 0.2f)
                    )
                }
            }
        }
    }
}

// ─── Add Button ──────────────────────────────────────────────────────────────

@Composable
private fun FloatingAddButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(72.dp),
        shape = CircleShape,
        color = VibrantTeal,
        shadowElevation = 12.dp
    ) {
        Icon(
            Icons.Rounded.Add,
            null,
            tint = Color.White,
            modifier = Modifier
                .padding(20.dp)
                .size(32.dp)
        )
    }
}

// ─── Add Task Card ───────────────────────────────────────────────────────────

@Composable
private fun AddTaskModernCard(
    type: ListType,
    isTimed: Boolean,
    title: String,
    time: Long?,
    onTitleChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onTimeSelected: (Long) -> Unit
) {
    var showTimePicker by remember { mutableStateOf(false) }
    var timeError by remember { mutableStateOf<String?>(null) }

    Surface(
        modifier = Modifier.padding(horizontal = 24.dp),
        shape = RoundedCornerShape(36.dp),
        color = Color.White.copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, Color.White.copy(alpha = 0.15f)
        )
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            TextField(
                value = title,
                onValueChange = onTitleChanged,
                placeholder = {
                    Text(
                        "Yeni bir görev...",
                        color = Color.White.copy(alpha = 0.3f)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    cursorColor = VibrantTeal,
                    focusedIndicatorColor = VibrantTeal,
                    unfocusedIndicatorColor = Color.White.copy(alpha = 0.2f)
                ),
                textStyle = MaterialTheme.typography.titleMedium
            )

            if (isTimed) {
                Column {
                    AssistChip(
                        onClick = { showTimePicker = true },
                        label = {
                            Text(
                                time?.let { formatTime(it) } ?: "Zaman Belirle",
                                color = if (timeError != null) MaterialTheme.colorScheme.error
                                else VibrantTeal
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Rounded.Schedule,
                                null,
                                modifier = Modifier.size(18.dp),
                                tint = if (timeError != null) MaterialTheme.colorScheme.error
                                else VibrantTeal
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            labelColor = VibrantTeal,
                            containerColor = VibrantTeal.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    timeError?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("Vazgeç", color = Color.White.copy(alpha = 0.6f))
                }
                Button(
                    onClick = {
                        // Saatli liste ise saat seçilmemişse hata göster
                        if (isTimed && time == null) {
                            timeError = "Lütfen saat seçin"
                            return@Button
                        }
                        timeError = null
                        onConfirm()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = VibrantTeal),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("Ekle", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showTimePicker) {
        NinaDoTimePickerDialog(
            onConfirm = { hour, minute ->
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                }
                if (cal.timeInMillis <= System.currentTimeMillis()) {
                    "Bu saat geçmiş, geçerli olamaz"
                } else {
                    timeError = null
                    onTimeSelected(cal.timeInMillis)
                    showTimePicker = false
                    null
                }
            },
            onDismiss = { showTimePicker = false }
        )
    }
}

// ─── Selection Bar ───────────────────────────────────────────────────────────

@Composable
private fun SelectionActionBar(
    visible: Boolean,
    selectedCount: Int,
    onDelete: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 50.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                onClick = onDelete,
                color = ErrorRed,
                shape = RoundedCornerShape(100.dp),
                shadowElevation = 16.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Rounded.Delete, null, tint = Color.White)
                    Text(
                        "$selectedCount Görevi Temizle",
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// ─── Empty State ─────────────────────────────────────────────────────────────

@Composable
private fun EmptyStateView() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Rounded.AutoAwesome,
            null,
            modifier = Modifier
                .size(56.dp)
                .alpha(0.1f),
            tint = Color.White
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Henüz görev yok.\nHaydi bir tane ekleyelim!",
            textAlign = TextAlign.Center,
            color = Color.White.copy(alpha = 0.3f),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

// ─── Dijital Saat Picker ─────────────────────────────────────────────────────
// onConfirm: null → başarılı, String → hata mesajı (dialog kapanmaz)

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun NinaDoTimePickerDialog(
    onConfirm: (hour: Int, minute: Int) -> String?,
    onDismiss: () -> Unit
) {
    val initialHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val initialMinute = Calendar.getInstance().get(Calendar.MINUTE)

    var selectedHour by remember { mutableIntStateOf(initialHour) }
    var selectedMinute by remember { mutableIntStateOf(initialMinute) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F3D52))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Saat Ayarla",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )

                Spacer(Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WheelPicker(
                        count = 24,
                        initialIndex = initialHour,
                        onSelectedIndex = { selectedHour = it; errorMessage = null },
                        label = { String.format(Locale.getDefault(), "%02d", it) }
                    )
                    Text(
                        ":",
                        style = MaterialTheme.typography.displayMedium,
                        modifier = Modifier.padding(horizontal = 12.dp),
                        color = VibrantTeal,
                        fontWeight = FontWeight.Light
                    )
                    WheelPicker(
                        count = 60,
                        initialIndex = initialMinute,
                        onSelectedIndex = { selectedMinute = it; errorMessage = null },
                        label = { String.format(Locale.getDefault(), "%02d", it) }
                    )
                }

                errorMessage?.let {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Vazgeç", color = Color.White.copy(alpha = 0.6f))
                    }
                    Button(
                        onClick = {
                            val error = onConfirm(selectedHour, selectedMinute)
                            if (error != null) errorMessage = error else onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VibrantTeal)
                    ) {
                        Text("Seç", color = White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun WheelPicker(
    count: Int,
    initialIndex: Int,
    onSelectedIndex: (Int) -> Unit,
    label: (Int) -> String
) {
    val state = rememberPagerState(initialPage = initialIndex + (count * 100)) { count * 200 }

    LaunchedEffect(state.currentPage) {
        onSelectedIndex(state.currentPage % count)
    }

    Box(
        modifier = Modifier
            .width(70.dp)
            .height(160.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(VibrantTeal.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                .border(1.dp, VibrantTeal.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
        )
        VerticalPager(
            state = state,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 56.dp)
        ) { page ->
            val index = page % count
            val isSelected = state.currentPage == page
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label(index),
                    style = if (isSelected) MaterialTheme.typography.displaySmall
                    else MaterialTheme.typography.titleLarge,
                    color = if (isSelected) VibrantTeal
                    else Color.White.copy(alpha = 0.3f),
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

// ─── Yardımcılar ─────────────────────────────────────────────────────────────

private fun typeLabel(type: ListType): String = when (type) {
    ListType.EDUCATION -> "Eğitim"
    ListType.PERSONAL -> "Kişisel"
    ListType.DAILY_PLAN -> "Günlük Plan"
    ListType.SHOPPING -> "Market"
    ListType.SINGLE_TASK -> "Tekli Görev"
}

private fun formatTime(timestamp: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))