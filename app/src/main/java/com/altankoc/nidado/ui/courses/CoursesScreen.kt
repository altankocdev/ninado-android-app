package com.altankoc.nidado.ui.courses

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.altankoc.nidado.domain.model.Course
import com.altankoc.nidado.domain.model.enums.CourseYear
import com.altankoc.nidado.ui.components.common.DeleteConfirmDialog
import com.altankoc.nidado.ui.components.common.luminance
import com.altankoc.nidado.ui.navigation.Screen
import com.altankoc.nidado.ui.theme.*
import com.altankoc.nidado.util.UiEvent
import com.altankoc.nidado.util.toDifficultyColor
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CoursesScreen(
    navController: NavController,
    viewModel: CourseViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.Navigate -> navController.navigate(event.route)
                else -> Unit
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (state.isSelectionMode) "Seçildi: ${state.selectedCourseIds.size}" else "Derslerim",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                actions = {
                    if (state.isSelectionMode) {
                        TextButton(onClick = { viewModel.selectAll() }) {
                            Text("Tümünü Seç", color = VibrantTeal, fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = { viewModel.toggleSelectionMode() }) {
                            Icon(Icons.Rounded.Close, contentDescription = "İptal")
                        }
                    } else {
                        IconButton(
                            onClick = { navController.navigate(Screen.CreateCourse.route) }
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = VibrantTeal,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = "Ders Ekle",
                                    tint = Color.White,
                                    modifier = Modifier.padding(6.dp)
                                )
                            }
                        }
                        
                        var showMenu by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Rounded.MoreVert, contentDescription = "Menü")
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Seç") },
                                    onClick = {
                                        showMenu = false
                                        viewModel.toggleSelectionMode()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Rounded.Checklist, contentDescription = null)
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = state.isSelectionMode && state.selectedCourseIds.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.onDeleteSelectedRequest() },
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                    icon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
                    text = { Text("Sil (${state.selectedCourseIds.size})") },
                    modifier = Modifier.padding(bottom = 80.dp) // Avoid overlap with bottom bar
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                // Arama
                item {
                    SearchBar(
                        query = state.searchQuery,
                        onQueryChanged = viewModel::onSearchQueryChanged
                    )
                }

                if (state.isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = VibrantTeal,
                                strokeWidth = 3.dp
                            )
                        }
                    }
                } else if (state.filteredCourses.isEmpty() && state.searchQuery.isEmpty()) {
                    item { EmptyCourses() }
                } else if (state.filteredCourses.isEmpty() && state.searchQuery.isNotEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Arama sonucu bulunamadı",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    val grouped = state.filteredCourses.groupBy {
                        it.name.first().uppercaseChar()
                    }.toSortedMap()

                    grouped.forEach { (letter, courses) ->
                        stickyHeader {
                            LetterHeader(letter = letter.toString())
                        }
                        items(courses, key = { it.id }) { course ->
                            CourseCard(
                                course = course,
                                isSelectionMode = state.isSelectionMode,
                                isSelected = state.selectedCourseIds.contains(course.id),
                                onClick = {
                                    if (state.isSelectionMode) {
                                        viewModel.toggleCourseSelection(course.id)
                                    } else {
                                        navController.navigate(Screen.EditCourse.createRoute(course.id))
                                    }
                                },
                                onLongPress = {
                                    if (!state.isSelectionMode) {
                                        viewModel.toggleSelectionMode()
                                        viewModel.toggleCourseSelection(course.id)
                                    }
                                },
                                onDelete = { viewModel.onDeleteConfirmRequest(course) }
                            )
                        }
                    }
                }
            }

            state.courseToDelete?.let { course ->
                DeleteConfirmDialog(
                    title = "Dersi Sil",
                    message = "\"${course.name}\" dersini silmek istediğinizden emin misiniz?",
                    onConfirm = { viewModel.onDeleteCourse(course) },
                    onDismiss = { viewModel.onDeleteDismiss() }
                )
            }

            state.coursesToDelete?.let { courses ->
                DeleteConfirmDialog(
                    title = "Dersleri Sil",
                    message = "${courses.size} dersi silmek istediğinizden emin misiniz? Bu işlem geri alınamaz.",
                    onConfirm = { viewModel.onDeleteCourses(courses) },
                    onDismiss = { viewModel.onDeleteDismiss() }
                )
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChanged: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChanged,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = "Ders ara...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = VibrantTeal.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                AnimatedVisibility(
                    visible = query.isNotEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    IconButton(onClick = { onQueryChanged("") }) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Temizle",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = VibrantTeal
            ),
            textStyle = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun LetterHeader(letter: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Text(
            text = letter,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = VibrantTeal.copy(alpha = 0.6f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CourseCard(
    course: Course,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onDelete: () -> Unit
) {
    val difficultyColor = course.difficulty.toDifficultyColor()
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                false
            } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val color = animateColorAsState(
                if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart)
                    MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                else Color.Transparent,
                label = "dismiss_bg"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(color.value),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (dismissState.progress > 0.1f) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Sil",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(end = 24.dp)
                    )
                }
            }
        }
    ) {
        val glassBrush = if (isLight) {
            Brush.linearGradient(
                colors = listOf(
                    FrostBlue,
                    SoftTeal.copy(alpha = 0.3f),
                    VibrantTeal.copy(alpha = 0.1f)
                ),
                start = androidx.compose.ui.geometry.Offset(0f, 0f),
                end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    DeepOceanDark.copy(alpha = 0.9f),
                    SurfaceDark.copy(alpha = 0.95f),
                    VibrantTealDark.copy(alpha = 0.4f)
                ),
                start = androidx.compose.ui.geometry.Offset(0f, 0f),
                end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
            )
        }

        val borderBrush = if (isLight) {
            Brush.linearGradient(
                colors = listOf(
                    if (isSelected) VibrantTeal else VibrantTeal.copy(alpha = 0.5f),
                    SoftTeal.copy(alpha = 0.2f),
                    Color.White.copy(alpha = 0.7f)
                )
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    if (isSelected) VibrantTeal else VibrantTeal.copy(alpha = 0.6f),
                    DeepOcean.copy(alpha = 0.3f),
                    Color.White.copy(alpha = 0.2f)
                )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(glassBrush)
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    brush = borderBrush,
                    shape = RoundedCornerShape(20.dp)
                )
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onClick() },
                        onLongPress = { onLongPress() }
                    )
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Stylish difficulty indicator gradient
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(6.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    difficultyColor,
                                    difficultyColor.copy(alpha = 0.4f)
                                )
                            )
                        )
                )

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (isSelectionMode) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onClick() },
                            colors = CheckboxDefaults.colors(
                                checkedColor = VibrantTeal,
                                uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        )
                    } else {
                        // Modern Icon Container with glowing effect
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = difficultyColor.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, difficultyColor.copy(alpha = 0.25f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.MenuBook,
                                    contentDescription = null,
                                    tint = difficultyColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = course.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = when (course.year) {
                                    CourseYear.FIRST -> "1. Sınıf"
                                    CourseYear.SECOND -> "2. Sınıf"
                                    CourseYear.THIRD -> "3. Sınıf"
                                    CourseYear.FOURTH -> "4. Sınıf"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = VibrantTeal,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Box(
                                modifier = Modifier
                                    .size(3.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                            )

                            Text(
                                text = "Zorluk: ${String.format(Locale.getDefault(), "%.1f", course.difficulty.toDouble())}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    if (!isSelectionMode) {
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyCourses() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 100.dp)
            .padding(horizontal = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.MenuBook,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = VibrantTeal.copy(alpha = 0.4f)
                )
            }
        }
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Henüz ders eklenmemiş",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Sağ üstteki + butonuna basarak yeni bir ders ekleyebilirsin.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}
