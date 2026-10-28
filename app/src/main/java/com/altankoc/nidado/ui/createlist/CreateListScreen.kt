package com.altankoc.nidado.ui.createlist

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.altankoc.nidado.domain.model.Course
import com.altankoc.nidado.domain.model.enums.CourseYear
import com.altankoc.nidado.domain.model.enums.ListType
import com.altankoc.nidado.ui.theme.*
import com.altankoc.nidado.util.UiEvent
import com.altankoc.nidado.util.toDifficultyColor
import java.text.SimpleDateFormat
import android.Manifest
import android.os.Build
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun CreateListScreen(
    navController: NavController,
    viewModel: CreateListViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val animatedHeaderColor by animateColorAsState(
        targetValue = when (state.selectedType) {
            ListType.PERSONAL -> Color(0xFF06B6D4)
            ListType.EDUCATION -> DeepOcean
            ListType.DAILY_PLAN -> Color(0xFFE91E63)
            ListType.SHOPPING -> Color(0xFFF59E0B)
            ListType.SINGLE_TASK -> Color(0xFF8B5CF6)
        },
        animationSpec = tween(600),
        label = "headerColor"
    )

    var showDatePicker by remember { mutableStateOf(false) }
    var showListTimePicker by remember { mutableStateOf(false) }
    var activeTaskTimeId by remember { mutableLongStateOf(-1L) }
    var activeCoursePickerId by remember { mutableLongStateOf(-1L) }

    val notificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    } else null

    val context = LocalContext.current

    var showExactAlarmDialog by remember { mutableStateOf(false) }

    val alarmManager = remember {
        context.getSystemService(AlarmManager::class.java)
    }

    fun hasExactAlarmPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager?.canScheduleExactAlarms() ?: true
        } else {
            true
        }
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.NavigateBack -> navController.popBackStack()
                is UiEvent.ShowToast -> {
                    android.widget.Toast.makeText(context, event.message, android.widget.Toast.LENGTH_SHORT).show()
                }
                else -> Unit
            }
        }
    }

    // Date picker — geçmiş tarihler seçilemez
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val today = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    return utcTimeMillis >= today.timeInMillis
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { utcMillis ->
                        // DatePicker UTC 00:00 döndürür, yerel saat dilimine normalize ediyoruz
                        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                            timeInMillis = utcMillis
                        }
                        val localCal = Calendar.getInstance().apply {
                            set(
                                utcCal.get(Calendar.YEAR),
                                utcCal.get(Calendar.MONTH),
                                utcCal.get(Calendar.DAY_OF_MONTH),
                                0, 0, 0
                            )
                            set(Calendar.MILLISECOND, 0)
                        }
                        viewModel.onDateSelected(localCal.timeInMillis)
                    }
                    showDatePicker = false
                }) { Text("Tamam", color = VibrantTeal) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("İptal")
                }
            }
        ) { DatePicker(state = datePickerState) }
    }

    // Liste saat picker (Shopping) — Takvim hariç geçmiş saat seçilemez
    if (showListTimePicker) {
        NinaDoTimePickerDialog(
            onConfirm = { hour, minute ->
                val cal = Calendar.getInstance()
                state.scheduledDate?.let { cal.timeInMillis = it }
                cal.set(Calendar.HOUR_OF_DAY, hour)
                cal.set(Calendar.MINUTE, minute)
                cal.set(Calendar.SECOND, 0)
                val candidate = cal.timeInMillis

                if (candidate < System.currentTimeMillis()) {
                    "Bu saat geçmiş, geçerli olamaz"
                } else {
                    viewModel.onTimeSelected(candidate)
                    null
                }
            },
            onDismiss = { showListTimePicker = false }
        )
    }

    // Görev saat picker — Takvim (PERSONAL) hariç geçmiş saat seçilemez
    if (activeTaskTimeId != -1L) {
        NinaDoTimePickerDialog(
            onConfirm = { hour, minute ->
                val cal = Calendar.getInstance()
                state.scheduledDate?.let { cal.timeInMillis = it }
                cal.set(Calendar.HOUR_OF_DAY, hour)
                cal.set(Calendar.MINUTE, minute)
                cal.set(Calendar.SECOND, 0)
                val candidate = cal.timeInMillis

                val isExemptFromPastCheck = state.selectedType == ListType.PERSONAL
                if (!isExemptFromPastCheck && candidate < System.currentTimeMillis()) {
                    "Bu saat geçmiş, geçerli olamaz"
                } else {
                    viewModel.onTaskTimeSelected(activeTaskTimeId, candidate)
                    null
                }
            },
            onDismiss = { activeTaskTimeId = -1L }
        )
    }

    // Ders picker
    if (activeCoursePickerId != -1L) {
        CoursePickerDialog(
            courses = state.availableCourses,
            onSelected = { course ->
                viewModel.onTaskCourseSelected(
                    activeCoursePickerId,
                    course.id,
                    course.name,
                    course.difficulty
                )
                activeCoursePickerId = -1L
            },
            onDismiss = { activeCoursePickerId = -1L }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            animatedHeaderColor.copy(alpha = 0.12f),
                            animatedHeaderColor.copy(alpha = 0.04f),
                            MaterialTheme.colorScheme.background
                        ),
                        endY = 1200f
                    )
                )
                .navigationBarsPadding()
                .imePadding()
        ) {
            // Header (Sabit)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Yeni Liste",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Tür seçici (Sabit)
            TypeSelector(
                selectedType = state.selectedType,
                onTypeSelected = viewModel::onTypeSelected,
                onSave = viewModel::onSave,
                isLoading = state.isLoading
            )

            Spacer(Modifier.height(12.dp))

            // Kaydırılabilir Alan
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 140.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Ayarlar Satırı — Alarm sadece Saatli açıkken kullanılabilir
                        // (Market hariç, onun kendi saati zaten var)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val canEnableAlarm = state.selectedType == ListType.SHOPPING || state.isTimed

                            MinimalToggle(
                                icon = if (state.isAlarmEnabled) Icons.Rounded.Notifications else Icons.Rounded.NotificationsOff,
                                label = "Alarm",
                                isSelected = state.isAlarmEnabled,
                                onClick = {
                                    if (!canEnableAlarm) {
                                        android.widget.Toast.makeText(
                                            context,
                                            "Alarm için önce Saatli seçmelisiniz",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        val turningOn = !state.isAlarmEnabled
                                        if (turningOn) {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                notificationPermission?.launchPermissionRequest()
                                            }
                                            if (!hasExactAlarmPermission()) {
                                                showExactAlarmDialog = true
                                            }
                                        }
                                        viewModel.onAlarmToggle(turningOn)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .alpha(if (canEnableAlarm) 1f else 0.45f)
                            )
                            if (state.selectedType != ListType.SHOPPING) {
                                MinimalToggle(
                                    icon = Icons.Rounded.Schedule,
                                    label = "Saatli",
                                    isSelected = state.isTimed,
                                    onClick = { viewModel.onTimedToggle(!state.isTimed) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Liste adı (SINGLE_TASK hariç)
                        AnimatedVisibility(
                            visible = state.selectedType != ListType.SINGLE_TASK,
                            enter = fadeIn(tween(200)) + expandVertically(),
                            exit = fadeOut(tween(200)) + shrinkVertically()
                        ) {
                            TitleField(
                                value = state.title,
                                onValueChange = viewModel::onTitleChanged,
                                error = state.titleError,
                                type = state.selectedType
                            )
                        }

                        // Shopping ve Personal (Takvim) için tarih/saat
                        AnimatedVisibility(
                            visible = state.selectedType == ListType.SHOPPING || state.selectedType == ListType.PERSONAL,
                            enter = fadeIn(tween(200)) + expandVertically(),
                            exit = fadeOut(tween(200)) + shrinkVertically()
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (state.selectedType == ListType.PERSONAL) {
                                    MinimalToggle(
                                        icon = Icons.Rounded.CalendarToday,
                                        label = state.scheduledDate?.let { formatDate(it) } ?: "Tarih Seç",
                                        isSelected = state.scheduledDate != null,
                                        onClick = { showDatePicker = true },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                } else {
                                    ShoppingTimeSection(
                                        scheduledDate = state.scheduledDate,
                                        startTime = state.startTime,
                                        onDateClick = { showDatePicker = true },
                                        onTimeClick = { showListTimePicker = true }
                                    )
                                }
                                state.dateError?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                                    )
                                }
                            }
                        }

                        // Bölüm başlığı
                        Text(
                            text = when (state.selectedType) {
                                ListType.SHOPPING -> "Alınacaklar"
                                ListType.SINGLE_TASK -> "Görev Detayı"
                                else -> "Görevler"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }

                // Görev inputları
                when (state.selectedType) {
                    ListType.SHOPPING -> {
                        items(
                            state.shoppingItems,
                            key = { it.id }
                        ) { item ->
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 20.dp, vertical = 5.dp)
                                    .animateItem()
                            ) {
                                ShoppingItemInput(
                                    item = item,
                                    onTitleChanged = { viewModel.onShoppingTitleChanged(item.id, it) },
                                    onQuantityChanged = { viewModel.onShoppingQuantityChanged(item.id, it) },
                                    onRemove = { viewModel.onRemoveShoppingItem(item.id) },
                                    canRemove = state.shoppingItems.size > 1
                                )
                            }
                        }
                        item(key = "add_shopping_button") {
                            Box(modifier = Modifier.padding(20.dp).animateItem()) {
                                AddItemButton(onClick = viewModel::onAddShoppingItem, label = "Ürün Ekle")
                            }
                        }
                    }

                    ListType.SINGLE_TASK -> {
                        items(
                            state.taskInputs.take(1),
                            key = { it.id }
                        ) { task ->
                            Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp).animateItem()) {
                                SingleTaskInput(
                                    task = task,
                                    isTimed = state.isTimed,
                                    onTitleChanged = { viewModel.onTaskTitleChanged(task.id, it) },
                                    onTimeClick = { activeTaskTimeId = task.id }
                                )
                            }
                        }
                    }

                    else -> {
                        items(
                            state.taskInputs,
                            key = { it.id }
                        ) { task ->
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 20.dp, vertical = 6.dp)
                                    .animateItem()
                            ) {
                                when (state.selectedType) {
                                    ListType.EDUCATION -> EducationTaskInput(
                                        task = task,
                                        isTimed = state.isTimed,
                                        onTitleChanged = { viewModel.onTaskTitleChanged(task.id, it) },
                                        onCourseClick = { activeCoursePickerId = task.id },
                                        onTimeClick = { activeTaskTimeId = task.id },
                                        onRemove = { viewModel.onRemoveTask(task.id) },
                                        canRemove = state.taskInputs.size > 1,
                                        isTitleReadOnly = true
                                    )
                                    ListType.DAILY_PLAN -> EducationTaskInput(
                                        task = task,
                                        isTimed = state.isTimed,
                                        onTitleChanged = { viewModel.onTaskTitleChanged(task.id, it) },
                                        onCourseClick = { activeCoursePickerId = task.id },
                                        onTimeClick = { activeTaskTimeId = task.id },
                                        onRemove = { viewModel.onRemoveTask(task.id) },
                                        canRemove = state.taskInputs.size > 1,
                                        isTitleReadOnly = false
                                    )
                                    else -> PersonalTaskInput(
                                        task = task,
                                        isTimed = state.isTimed,
                                        onTitleChanged = { viewModel.onTaskTitleChanged(task.id, it) },
                                        onTimeClick = { activeTaskTimeId = task.id },
                                        onRemove = { viewModel.onRemoveTask(task.id) },
                                        canRemove = state.taskInputs.size > 1
                                    )
                                }
                            }
                        }
                        item(key = "add_task_button") {
                            Box(modifier = Modifier.padding(20.dp).animateItem()) {
                                AddItemButton(
                                    onClick = viewModel::onAddTask,
                                    label = if (state.selectedType == ListType.EDUCATION) "Ders Ekle" else "Görev Ekle"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    if (showExactAlarmDialog) {
        ExactAlarmInfoDialog(
            onDismiss = { showExactAlarmDialog = false },
            onGoToSettings = {
                showExactAlarmDialog = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = android.net.Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                }
            }
        )
    }
}

// ─── Type Selector ───────────────────────────────────────────────────────────

@Composable
private fun TypeSelector(
    selectedType: ListType,
    onTypeSelected: (ListType) -> Unit,
    onSave: () -> Unit,
    isLoading: Boolean
) {
    // Sıra: Günlük, Eğitim, Tekli, Takvim, Market
    val items = listOf(
        Triple(ListType.DAILY_PLAN, Icons.Rounded.Person, "Günlük"),
        Triple(ListType.EDUCATION, Icons.AutoMirrored.Rounded.MenuBook, "Eğitim"),
        Triple(ListType.SINGLE_TASK, Icons.Rounded.TaskAlt, "Tekli"),
        Triple(ListType.PERSONAL, Icons.Rounded.CalendarToday, "Takvim"),
        Triple(ListType.SHOPPING, Icons.Rounded.ShoppingCart, "Market")
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "LİSTE TÜRÜ",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
            )

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        brush = Brush.linearGradient(listOf(VibrantTeal, DeepOcean)),
                        shape = CircleShape
                    )
                    .clip(CircleShape)
                    .clickable(enabled = !isLoading, onClick = onSave),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = "Kaydet",
                        tint = White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items.forEach { item ->
                val (type, icon, label) = item
                val isSelected = selectedType == type
                val typeColors = when (type) {
                    ListType.DAILY_PLAN -> listOf(Color(0xFFE91E63), Color(0xFFF44336))
                    ListType.EDUCATION -> listOf(DeepOcean, VibrantTeal)
                    ListType.SINGLE_TASK -> listOf(Color(0xFF8B5CF6), Color(0xFFC084FC))
                    ListType.PERSONAL -> listOf(Color(0xFF06B6D4), Color(0xFF22D3EE))
                    ListType.SHOPPING -> listOf(Color(0xFFF59E0B), Color(0xFFFFD60A))
                }

                val transition = updateTransition(isSelected, label = "TypeSelection")

                val animatedBgColor by transition.animateColor(label = "bg") { target ->
                    if (target) typeColors.first().copy(alpha = 0.12f) else Color.Transparent
                }

                val animatedContentColor by transition.animateColor(label = "content") { target ->
                    if (target) typeColors.first() else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                }

                val animatedBorderColor by transition.animateColor(label = "border") { target ->
                    if (target) typeColors.first().copy(alpha = 0.3f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                }

                val scale by transition.animateFloat(
                    label = "scale",
                    transitionSpec = {
                        if (targetState) {
                            spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow)
                        } else {
                            tween(200)
                        }
                    }
                ) { target -> if (target) 1.05f else 1f }

                Column(
                    modifier = Modifier
                        .width(82.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .clip(RoundedCornerShape(20.dp))
                        .background(animatedBgColor)
                        .border(1.5.dp, animatedBorderColor, RoundedCornerShape(20.dp))
                        .clickable { onTypeSelected(type) }
                        .padding(vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                if (isSelected) typeColors.first().copy(alpha = 0.1f) else Color.Transparent,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = animatedContentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                        color = animatedContentColor,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

// ─── Form Bileşenleri ────────────────────────────────────────────────────────

@Composable
private fun TitleField(
    value: String,
    onValueChange: (String) -> Unit,
    error: String?,
    type: ListType
) {
    val placeholder = when (type) {
        ListType.EDUCATION -> "Örn. Ders Planı"
        ListType.PERSONAL -> "Örn. Doktor Randevusu"
        ListType.DAILY_PLAN -> "Örn. Günlük Planım"
        ListType.SHOPPING -> "Örn. Market Alışverişi"
        else -> "Liste adı"
    }
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        placeholder = {
            Text(
                placeholder,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                fontWeight = FontWeight.Bold
            )
        },
        singleLine = true,
        isError = error != null,
        supportingText = error?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
        textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            errorContainerColor = Color.Transparent,
            focusedIndicatorColor = VibrantTeal,
            unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
            cursorColor = VibrantTeal
        )
    )
}

@Composable
private fun ShoppingTimeSection(
    scheduledDate: Long?,
    startTime: Long?,
    onDateClick: () -> Unit,
    onTimeClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MinimalToggle(
            icon = Icons.Rounded.CalendarToday,
            label = scheduledDate?.let { formatDate(it) } ?: "Tarih Seç",
            isSelected = scheduledDate != null,
            onClick = onDateClick,
            modifier = Modifier.weight(1f)
        )
        MinimalToggle(
            icon = Icons.Rounded.Schedule,
            label = startTime?.let { formatTime(it) } ?: "Saat Seç",
            isSelected = startTime != null,
            onClick = onTimeClick,
            modifier = Modifier.weight(1f)
        )
    }
}

// ─── Görev Inputları ─────────────────────────────────────────────────────────

@Composable
private fun PersonalTaskInput(
    task: TaskInput,
    isTimed: Boolean,
    onTitleChanged: (String) -> Unit,
    onTimeClick: () -> Unit,
    onRemove: () -> Unit,
    canRemove: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = task.title,
                    onValueChange = onTitleChanged,
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            "Görev yaz...",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    singleLine = true,
                    isError = task.titleError != null,
                    colors = taskFieldColors(),
                    shape = RoundedCornerShape(12.dp)
                )
                if (canRemove) {
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            if (isTimed) {
                TimeChip(reminderTime = task.reminderTime, onClick = onTimeClick)
            }
        }
    }
}

@Composable
private fun EducationTaskInput(
    task: TaskInput,
    isTimed: Boolean,
    onTitleChanged: (String) -> Unit,
    onCourseClick: () -> Unit,
    onTimeClick: () -> Unit,
    onRemove: () -> Unit,
    canRemove: Boolean,
    isTitleReadOnly: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = task.title,
                    onValueChange = onTitleChanged,
                    modifier = Modifier.weight(1f),
                    readOnly = isTitleReadOnly,
                    placeholder = {
                        Text(
                            if (isTitleReadOnly) "Ders seçerek başlayın..." else "Ödev veya görev yaz...",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    singleLine = true,
                    isError = task.titleError != null,
                    colors = taskFieldColors(),
                    shape = RoundedCornerShape(12.dp)
                )

                if (canRemove) {
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Ders seçici
                Surface(
                    onClick = onCourseClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = if (task.courseId != null) VibrantTeal.copy(alpha = 0.1f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, if (task.courseId != null) VibrantTeal.copy(alpha = 0.2f) else Color.Transparent)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.MenuBook,
                            null,
                            modifier = Modifier.size(16.dp),
                            tint = if (task.courseId != null) VibrantTeal else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                        Text(
                            text = task.courseName ?: "Ders Seç",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (task.courseId != null) FontWeight.Bold else FontWeight.Medium,
                            color = if (task.courseId != null) VibrantTeal else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }

                if (isTimed) {
                    Box(modifier = Modifier.weight(1f)) {
                        TimeChip(reminderTime = task.reminderTime, onClick = onTimeClick)
                    }
                }
            }
        }
    }
}

@Composable
private fun SingleTaskInput(
    task: TaskInput,
    isTimed: Boolean,
    onTitleChanged: (String) -> Unit,
    onTimeClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = task.title,
                onValueChange = onTitleChanged,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        "Ne yapacaksın?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                },
                singleLine = true,
                isError = task.titleError != null,
                colors = taskFieldColors(),
                shape = RoundedCornerShape(12.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
            )
            if (isTimed) {
                TimeChip(reminderTime = task.reminderTime, onClick = onTimeClick)
            }
        }
    }
}

@Composable
private fun ShoppingItemInput(
    item: ShoppingInput,
    onTitleChanged: (String) -> Unit,
    onQuantityChanged: (String) -> Unit,
    onRemove: () -> Unit,
    canRemove: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = item.title,
                onValueChange = onTitleChanged,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        "Ürün adı",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                singleLine = true,
                isError = item.titleError != null,
                colors = taskFieldColors(),
                shape = RoundedCornerShape(10.dp)
            )
            OutlinedTextField(
                value = item.quantity,
                onValueChange = onQuantityChanged,
                modifier = Modifier.width(80.dp),
                placeholder = {
                    Text(
                        "Adet",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                singleLine = true,
                colors = taskFieldColors(),
                shape = RoundedCornerShape(10.dp),
                textStyle = MaterialTheme.typography.bodySmall.copy(textAlign = TextAlign.Center)
            )
            if (canRemove) {
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .size(32.dp)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.08f), CircleShape)
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        null,
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// ─── Ortak Bileşenler ────────────────────────────────────────────────────────

@Composable
private fun TimeChip(reminderTime: Long?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (reminderTime != null) VibrantTeal.copy(alpha = 0.1f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            Icons.Rounded.Schedule,
            null,
            tint = if (reminderTime != null) VibrantTeal
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = reminderTime?.let { formatTime(it) } ?: "Saat Seç",
            style = MaterialTheme.typography.bodySmall,
            color = if (reminderTime != null) VibrantTeal
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            fontWeight = if (reminderTime != null) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun AddItemButton(onClick: () -> Unit, label: String) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = Color.Transparent,
            modifier = Modifier.height(44.dp)
        ) {
            Row(
                modifier = Modifier
                    .background(
                        brush = Brush.linearGradient(listOf(VibrantTeal.copy(alpha = 0.15f), VibrantTeal.copy(alpha = 0.05f))),
                        shape = CircleShape
                    )
                    .border(1.dp, VibrantTeal.copy(alpha = 0.2f), CircleShape)
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Rounded.Add, null, tint = VibrantTeal, modifier = Modifier.size(20.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = VibrantTeal,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun taskFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
    disabledContainerColor = Color.Transparent,
    errorContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.05f),
    focusedBorderColor = VibrantTeal.copy(alpha = 0.5f),
    unfocusedBorderColor = Color.Transparent,
    cursorColor = VibrantTeal,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
)

// ─── Dialoglar ───────────────────────────────────────────────────────────────

@Composable
private fun MinimalToggle(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val alpha by animateFloatAsState(if (isSelected) 1f else 0.4f, label = "alpha")
    val bgColor by animateColorAsState(
        if (isSelected) VibrantTeal.copy(alpha = 0.12f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        label = "bg"
    )
    val contentColor by animateColorAsState(
        if (isSelected) VibrantTeal else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        label = "content"
    )

    Surface(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        border = BorderStroke(1.5.dp, if (isSelected) VibrantTeal.copy(alpha = 0.3f) else Color.Transparent)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                null,
                modifier = Modifier.size(16.dp).graphicsLayer(alpha = alpha),
                tint = contentColor
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                color = contentColor.copy(alpha = if (isSelected) 1f else 0.6f)
            )
        }
    }
}

// ─── Modern Digital Time Picker ─────────────────────────────────────────────
// onConfirm: hour, minute alır; başarılıysa null, hata varsa hata mesajı döner.
// Hata dönerse dialog kapanmaz, içeride hata gösterilir.

@OptIn(ExperimentalFoundationApi::class)
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
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Saat Ayarla",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
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
                        Text("Vazgeç", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                    Button(
                        onClick = {
                            val error = onConfirm(selectedHour, selectedMinute)
                            if (error != null) {
                                errorMessage = error
                            } else {
                                onDismiss()
                            }
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

@OptIn(ExperimentalFoundationApi::class)
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
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun CoursePickerDialog(
    courses: List<Course>,
    onSelected: (Course) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredCourses = remember(searchQuery, courses) {
        val baseList = if (searchQuery.isBlank()) courses
        else courses.filter { it.name.contains(searchQuery, ignoreCase = true) }
        baseList.sortedBy { it.name }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(
                                brush = Brush.linearGradient(listOf(VibrantTeal, DeepOcean)),
                                shape = RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.MenuBook,
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Ders Seç",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Planına uygun dersi seçebilirsin",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    placeholder = {
                        Text(
                            "Ders ara...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Rounded.Search,
                            null,
                            tint = VibrantTeal,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Rounded.Close, null, modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = VibrantTeal
                    )
                )

                Spacer(Modifier.height(20.dp))

                if (filteredCourses.isEmpty()) {
                    EmptyCourseState(isSearching = searchQuery.isNotEmpty())
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .heightIn(max = 400.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(filteredCourses, key = { it.id }) { course ->
                            CoursePickerItem(course = course, onClick = { onSelected(course) })
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    elevation = ButtonDefaults.buttonElevation(0.dp)
                ) {
                    Text(
                        "Kapat",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyCourseState(isSearching: Boolean = false) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                if (isSearching) Icons.Rounded.SearchOff else Icons.AutoMirrored.Rounded.MenuBook,
                null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                modifier = Modifier.size(80.dp)
            )
            Text(
                if (isSearching) "Aradığın ders bulunamadı" else "Henüz ders eklemedin",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CoursePickerItem(course: Course, onClick: () -> Unit) {
    val difficultyColor = course.difficulty.toDifficultyColor()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        onClick = onClick,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(difficultyColor.copy(alpha = 0.15f))
                    .border(1.dp, difficultyColor.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = course.name.first().uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = difficultyColor
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = course.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Layers,
                            null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                        Text(
                            text = when (course.year) {
                                CourseYear.FIRST -> "1. Sınıf"
                                CourseYear.SECOND -> "2. Sınıf"
                                CourseYear.THIRD -> "3. Sınıf"
                                CourseYear.FOURTH -> "4. Sınıf"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }

                    Surface(
                        color = difficultyColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "Zorluk: ${course.difficulty}",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = difficultyColor
                        )
                    }
                }
            }
            Icon(
                Icons.Rounded.ChevronRight,
                null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun ExactAlarmInfoDialog(
    onDismiss: () -> Unit,
    onGoToSettings: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(VibrantTeal.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AlarmOn,
                        contentDescription = null,
                        tint = VibrantTeal,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Text(
                    text = "Tam Zamanında Bildirim",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Bildirimlerin tam zamanında gelmesi için telefon ayarlarından \"Alarm ve Hatırlatıcılar\" iznini vermen gerekiyor. Vermezsen bildirimler hâlâ gelir ama gecikebilir.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Daha Sonra", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                    Button(
                        onClick = onGoToSettings,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VibrantTeal)
                    ) {
                        Text("Ayarlara Git", color = White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ─── Yardımcılar ─────────────────────────────────────────────────────────────

private fun formatTime(timestamp: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))

private fun formatDate(timestamp: Long): String =
    SimpleDateFormat("dd MMM", Locale.forLanguageTag("tr")).format(Date(timestamp))