package com.altankoc.nidado.ui.courses

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.altankoc.nidado.domain.model.enums.CourseYear
import com.altankoc.nidado.ui.components.common.DeleteConfirmDialog
import com.altankoc.nidado.ui.theme.*
import com.altankoc.nidado.util.UiEvent
import com.altankoc.nidado.util.toDifficultyColor
import com.altankoc.nidado.util.toTitleCase

@Composable
fun CreateEditCourseScreen(
    courseId: Int? = null,
    navController: NavController,
    viewModel: CreateEditCourseViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isEditMode = courseId != null

    LaunchedEffect(courseId) {
        courseId?.let { viewModel.loadCourse(it) }
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.NavigateBack -> navController.popBackStack()
                else -> Unit
            }
        }
    }

    Scaffold(
        topBar = {
            TopBar(
                isEditMode = isEditMode,
                onBack = { navController.popBackStack() },
                onDelete = if (isEditMode) ({ viewModel.onDeleteRequest() }) else null
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                // Ders adı
                NameField(
                    value = state.name,
                    onValueChange = viewModel::onNameChanged,
                    error = state.nameError
                )

                // Sınıf seçimi
                YearSelector(
                    selectedYear = state.year,
                    onYearSelected = viewModel::onYearChanged
                )

                // Zorluk slider
                DifficultySlider(
                    difficulty = state.difficulty,
                    onDifficultyChanged = viewModel::onDifficultyChanged
                )

                // Kaydet butonu
                Button(
                    onClick = viewModel::onSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VibrantTeal,
                        contentColor = White
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 2.dp,
                        pressedElevation = 8.dp
                    )
                ) {
                    Icon(
                        imageVector = if (isEditMode) Icons.Rounded.CheckCircleOutline else Icons.Rounded.AddCircleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = if (isEditMode) "Değişiklikleri Kaydet" else "Dersi Hemen Oluştur",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                
                Spacer(Modifier.height(16.dp))
            }

            // Silme dialog
            if (state.showDeleteDialog) {
                DeleteConfirmDialog(
                    title = "Dersi Sil",
                    message = "\"${state.name}\" dersini silmek istediğinizden emin misiniz?",
                    onConfirm = viewModel::onDeleteConfirm,
                    onDismiss = viewModel::onDeleteDismiss
                )
            }
        }
    }
}

@Composable
private fun TopBar(
    isEditMode: Boolean,
    onBack: () -> Unit,
    onDelete: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            onClick = onBack,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBackIosNew,
                    contentDescription = "Geri",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Text(
            text = if (isEditMode) "Dersi Düzenle" else "Yeni Ders",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            textAlign = TextAlign.Center
        )

        if (onDelete != null) {
            Surface(
                onClick = onDelete,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Sil",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        } else {
            Spacer(Modifier.size(44.dp))
        }
    }
}


@Composable
private fun NameField(
    value: String,
    onValueChange: (String) -> Unit,
    error: String?
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Dersin Adı",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 4.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(it.toTitleCase()) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = "Örn. Medeni Hukuk",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            },
            singleLine = true,
            isError = error != null,
            supportingText = error?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = VibrantTeal,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                errorContainerColor = MaterialTheme.colorScheme.surface,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                cursorColor = VibrantTeal
            ),
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
        )
    }
}

@Composable
private fun DifficultySlider(
    difficulty: Float,
    onDifficultyChanged: (Float) -> Unit
) {
    val difficultyColor = difficulty.toDifficultyColor()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(28.dp))
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Ders Zorluğu",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = when {
                        difficulty < 2.0f -> "Çok Rahat"
                        difficulty < 3.0f -> "İdeal Seviye"
                        difficulty < 4.0f -> "Biraz Zorlayıcı"
                        else -> "Oldukça Zor"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = difficultyColor,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = difficultyColor,
                shadowElevation = 8.dp
            ) {
                Text(
                    text = String.format("%.1f", difficulty.toDouble()),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = White,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            contentAlignment = Alignment.Center
        ) {
            // Enhanced Gradient Track
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF4CAF50),
                                Color(0xFF8BC34A),
                                Color(0xFFFFEB3B),
                                Color(0xFFFF9800),
                                Color(0xFFF44336)
                            )
                        )
                    )
            )

            Slider(
                value = difficulty,
                onValueChange = { onDifficultyChanged((it * 10).toInt() / 10f) },
                valueRange = 1f..5f,
                steps = 39,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = White,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent
                )
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            DifficultyLabel("Kolay", Color(0xFF4CAF50))
            DifficultyLabel("Orta", Color(0xFFFFEB3B))
            DifficultyLabel("Zor", Color(0xFFF44336))
        }
    }
}

@Composable
private fun DifficultyLabel(text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun YearSelector(
    selectedYear: CourseYear,
    onYearSelected: (CourseYear) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Kaçıncı Sınıf?",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 4.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            CourseYear.entries.forEach { year ->
                val isSelected = selectedYear == year
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) VibrantTeal else Color.Transparent,
                    animationSpec = tween(300),
                    label = "year_bg"
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    animationSpec = tween(300),
                    label = "year_text"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(bgColor)
                        .clickable { onYearSelected(year) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (year) {
                            CourseYear.FIRST -> "1"
                            CourseYear.SECOND -> "2"
                            CourseYear.THIRD -> "3"
                            CourseYear.FOURTH -> "4"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                        color = textColor
                    )
                }
            }
        }
    }
}
