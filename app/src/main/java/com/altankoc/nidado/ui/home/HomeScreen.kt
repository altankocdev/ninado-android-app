package com.altankoc.nidado.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.altankoc.nidado.domain.model.ListItem
import com.altankoc.nidado.domain.model.TodoListWithItems
import com.altankoc.nidado.domain.model.enums.ListType
import com.altankoc.nidado.ui.components.common.DeleteConfirmDialog
import com.altankoc.nidado.ui.components.listcard.EducationPersonalDailyCard
import com.altankoc.nidado.ui.components.listcard.ShoppingCard
import com.altankoc.nidado.ui.components.listcard.SingleTaskCard
import com.altankoc.nidado.ui.theme.*
import com.altankoc.nidado.util.Constants
import com.altankoc.nidado.util.UiEvent
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentTime by viewModel.currentTime.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var listForAction by remember { mutableStateOf<TodoListWithItems?>(null) }
    var listToDelete by remember { mutableStateOf<TodoListWithItems?>(null) }
    // Karta tıklanınca açılan mini önizleme paneli — artık sadece ID tutuluyor,
    // içerik her recompose'da state'ten canlı türetiliyor
    var previewListId by remember { mutableStateOf<Int?>(null) }

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
            FixedHeader(
                name = state.user?.name ?: "",
                avatarId = state.user?.avatarId ?: 1,
                selectedFilter = state.selectedFilter,
                onFilterSelected = viewModel::onFilterSelected
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(top = 12.dp, bottom = 140.dp)
            ) {
                when {
                    state.isLoading -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(320.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color       = VibrantTeal,
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    }

                    state.filteredLists.isEmpty() -> {
                        item { EmptyState() }
                    }

                    else -> {
                        items(
                            items = state.filteredLists,
                            key   = { it.todoList.id }
                        ) { listWithItems ->
                            SwipeableListCard(
                                listWithItems = listWithItems,
                                currentTime   = currentTime,
                                onMuteToggle  = { viewModel.onToggleMute(listWithItems.todoList) },
                                onItemToggle  = { itemId, isCompleted ->
                                    viewModel.onToggleItemCompleted(itemId, isCompleted)
                                },
                                onViewAll     = { previewListId = listWithItems.todoList.id },
                                onSwipeOrLongPress = { listForAction = listWithItems }
                            )
                        }
                    }
                }
            }
        }

        listForAction?.let { list ->
            ListActionDialog(
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
                title   = "Listeyi Sil",
                message = "\"${list.todoList.title}\" listesini silmek istiyor musun? Bu işlem geri alınamaz.",
                onConfirm = {
                    viewModel.onDeleteList(list.todoList)
                    listToDelete = null
                },
                onDismiss = { listToDelete = null }
            )
        }
    }

    // ── Liste Önizleme Paneli — canlı güncellenir ──
    previewListId?.let { id ->
        val liveList = state.filteredLists.find { it.todoList.id == id }
        if (liveList != null) {
            ListPreviewModal(
                listWithItems = liveList,
                onDismiss = { previewListId = null },
                onGoToDetail = {
                    previewListId = null
                    viewModel.navigateToDetail(id)
                },
                onItemToggle = viewModel::onToggleItemCompleted
            )
        } else {
            // Liste artık listede yok (silindi/arşivlendi) — modal'ı kapat
            previewListId = null
        }
    }
}

// ─────────────────────────────────────────────
//  LİSTE ÖNİZLEME PANELİ — ortalanmış, kenarlarda
//  boşluklu, scale+fade ile beliren modern kart
// ─────────────────────────────────────────────
@Composable
private fun ListPreviewModal(
    listWithItems: TodoListWithItems,
    onDismiss: () -> Unit,
    onGoToDetail: () -> Unit,
    onItemToggle: (Int, Boolean) -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { visible = true }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Dim arka plan
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(250)),
                exit = fadeOut(tween(200))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.15f))
                        .background(Color.Black.copy(alpha = 0.35f))
                        .clickable { onDismiss() }
                )
            }

            // Ortalanmış glass kart — kenarlarda boşluk, scale+fade ile belirir
            AnimatedVisibility(
                visible = visible,
                enter = scaleIn(
                    initialScale = 0.9f,
                    animationSpec = tween(400, easing = FastOutSlowInEasing)
                ) + fadeIn(tween(300)),
                exit = scaleOut(
                    targetScale = 0.9f,
                    animationSpec = tween(250, easing = FastOutSlowInEasing)
                ) + fadeOut(tween(200)),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp, vertical = 40.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 600.dp)
                        .shadow(
                            elevation = 30.dp,
                            shape = RoundedCornerShape(32.dp),
                            ambientColor = Color.Black,
                            spotColor = VibrantTeal.copy(alpha = 0.3f)
                        )
                        .clip(RoundedCornerShape(32.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.75f),
                                    Color.White.copy(alpha = 0.35f)
                                )
                            )
                        )
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    VibrantTeal.copy(alpha = 0.15f),
                                    SoftTeal.copy(alpha = 0.25f),
                                    Color.White.copy(alpha = 0.4f)
                                ),
                                start = Offset(0f, 0f),
                                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                            )
                        )
                        .border(
                            width = 2.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.9f),
                                    Color.White.copy(alpha = 0.2f),
                                    VibrantTeal.copy(alpha = 0.5f),
                                    Color.White.copy(alpha = 0.4f)
                                )
                            ),
                            shape = RoundedCornerShape(32.dp)
                        )
                        .clickable(enabled = false) { }
                ) {
                    // Dekoratif Glow
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .offset(x = (-50).dp, y = (-50).dp)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        VibrantTeal.copy(alpha = 0.15f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Başlık Bölümü
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.03f))
                                .padding(horizontal = 24.dp, vertical = 24.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = listWithItems.todoList.title,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = DeepOcean,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Surface(
                                        color = VibrantTeal,
                                        shape = RoundedCornerShape(14.dp),
                                        shadowElevation = 8.dp,
                                        border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.3f))
                                    ) {
                                        Text(
                                            text = "${listWithItems.items.size} GÖREV",
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White,
                                            letterSpacing = 1.2.sp
                                        )
                                    }
                                }
                                
                                IconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(DeepOcean.copy(alpha = 0.08f), CircleShape)
                                        .border(1.dp, DeepOcean.copy(alpha = 0.1f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Kapat",
                                        tint = DeepOcean.copy(alpha = 0.6f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        val sortedItems = remember(listWithItems.items) {
                            listWithItems.items.sortedBy { it.reminderTime ?: Long.MAX_VALUE }
                        }
// Kaydırılabilir görev listesi
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .heightIn(max = 360.dp),
                            contentPadding = PaddingValues(24.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (listWithItems.items.isEmpty()) {
                                item {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 40.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.List,
                                            contentDescription = null,
                                            tint = DeepOcean.copy(alpha = 0.15f),
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(Modifier.height(12.dp))
                                        Text(
                                            text = "Henüz görev eklenmedi",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = DeepOcean.copy(alpha = 0.4f)
                                        )
                                    }
                                }
                            } else {
                                items(sortedItems, key = { it.id }) { item ->
                                    PreviewTaskRow(
                                        item = item,
                                        type = listWithItems.todoList.type,
                                        onToggle = { onItemToggle(item.id, !item.isCompleted) }
                                    )
                                }
                            }
                        }

                        // Alt "Detaya Git" butonu
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                        ) {
                            Button(
                                onClick = onGoToDetail,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .shadow(
                                        elevation = 12.dp,
                                        shape = RoundedCornerShape(18.dp),
                                        spotColor = VibrantTeal.copy(alpha = 0.5f)
                                    ),
                                shape = RoundedCornerShape(18.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent
                                ),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(VibrantTeal, SoftTeal)
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "Detayları Görüntüle",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable
private fun PreviewTaskRow(item: ListItem, type: ListType, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.45f))
            .border(
                width = 1.5.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.8f),
                        Color.White.copy(alpha = 0.2f)
                    )
                ),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(
                    if (item.isCompleted) VibrantTeal.copy(alpha = 0.2f)
                    else DeepOcean.copy(alpha = 0.05f)
                )
                .border(
                    1.dp,
                    if (item.isCompleted) VibrantTeal.copy(alpha = 0.5f)
                    else DeepOcean.copy(alpha = 0.15f),
                    CircleShape
                )
                .clickable { onToggle() },
            contentAlignment = Alignment.Center
        ) {
            if (item.isCompleted) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = VibrantTeal,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (item.isCompleted) FontWeight.Medium else FontWeight.SemiBold,
                color = if (item.isCompleted) DeepOcean.copy(alpha = 0.4f) else DeepOcean,
                textDecoration = if (item.isCompleted) TextDecoration.LineThrough else null,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (type != ListType.SHOPPING && item.reminderTime != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AccessTime,
                        contentDescription = null,
                        tint = VibrantTeal.copy(alpha = 0.6f),
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = formatPreviewTime(item.reminderTime),
                        style = MaterialTheme.typography.labelSmall,
                        color = VibrantTeal.copy(alpha = 0.7f)
                    )
                }
            }
        }

        if (type == ListType.SHOPPING && item.quantity != null) {
            Surface(
                color = VibrantTeal.copy(alpha = 0.12f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(0.5.dp, VibrantTeal.copy(alpha = 0.2f))
            ) {
                Text(
                    text = item.quantity,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = SoftTeal,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun formatPreviewTime(timestamp: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))

// ─────────────────────────────────────────────
//  SABİT ÜST BLOK — Logo + Selamlama + Filtre
//  Hepsi tek gradient/glow kapsayıcı içinde,
//  scroll'da kaybolmaz
// ─────────────────────────────────────────────
@Composable
private fun FixedHeader(
    name: String,
    avatarId: Int,
    selectedFilter: ListType?,
    onFilterSelected: (ListType?) -> Unit
) {

    val filters = listOf(
        null to "Hepsi",
        ListType.EDUCATION to "Eğitim",
        ListType.DAILY_PLAN to "Günlük",
        ListType.SHOPPING to "Market",
        ListType.SINGLE_TASK to "Tekli"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(
                RoundedCornerShape(
                    bottomStart = 28.dp,
                    bottomEnd = 28.dp
                )
            )
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        DeepOcean,
                        VibrantTeal,
                        SoftTeal
                    )
                )
            )
    ) {

        Box(
            modifier = Modifier
                .size(220.dp)
                .offset(x = 180.dp, y = (-70).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 10.dp, bottom = 16.dp)
        ) {

            Text(
                text = "NinaDo",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                letterSpacing = 0.5.sp
            )

            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Color.White.copy(alpha = 0.12f)
                    )
                    .border(
                        1.dp,
                        Color.White.copy(alpha = 0.15f),
                        RoundedCornerShape(24.dp)
                    )
                    .padding(
                        horizontal = 16.dp,
                        vertical = 14.dp
                    )
            ) {

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {

                        Text(
                            text = "Merhaba $name",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold
                        )

                        Spacer(Modifier.height(2.dp))

                        Text(
                            text = "Bugün planlarında neler var?",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 13.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(
                                Color.White.copy(alpha = 0.15f)
                            )
                            .border(
                                1.dp,
                                Color.White.copy(alpha = 0.15f),
                                CircleShape
                            )
                            .padding(3.dp),
                        contentAlignment = Alignment.Center
                    ) {

                        Image(
                            painter = painterResource(
                                Constants.AVATARS.getOrElse(
                                    avatarId - 1
                                ) {
                                    Constants.AVATARS[0]
                                }
                            ),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        Color.White.copy(alpha = 0.12f)
                    )
                    .border(
                        1.dp,
                        Color.White.copy(alpha = 0.12f),
                        RoundedCornerShape(26.dp)
                    )
            ) {

                val itemWidth = maxWidth / filters.size

                val selectedIndex =
                    filters.indexOfFirst { it.first == selectedFilter }
                        .coerceAtLeast(0)

                val animatedOffset by animateDpAsState(
                    targetValue = itemWidth * selectedIndex,
                    animationSpec = spring(
                        dampingRatio = 0.75f,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "bubble"
                )

                Box(
                    modifier = Modifier
                        .offset(x = animatedOffset)
                        .width(itemWidth)
                        .fillMaxHeight()
                        .padding(4.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(22.dp)
                        )
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color.White)
                )

                Row(
                    modifier = Modifier.fillMaxSize()
                ) {

                    filters.forEach { (type, label) ->

                        val selected =
                            selectedFilter == type

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .pointerInput(Unit) {
                                    detectTapGestures {
                                        onFilterSelected(type)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {

                            Text(
                                text = label,
                                color =
                                    if (selected)
                                        DeepOcean
                                    else
                                        Color.White,
                                fontWeight =
                                    if (selected)
                                        FontWeight.Bold
                                    else
                                        FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
//  KAYDIRILABİLİR / UZUN BASILABİLİR LİSTE KARTI
// ─────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableListCard(
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
                    .background(VibrantTeal.copy(alpha = 0.18f)),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Rounded.MoreHoriz,
                    contentDescription = "Seçenekler",
                    tint = VibrantTeal,
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
            when (listWithItems.todoList.type) {
                ListType.SHOPPING -> ShoppingCard(
                    listWithItems = listWithItems,
                    onMuteToggle  = onMuteToggle,
                    onDelete      = onSwipeOrLongPress,
                    onViewAll     = onViewAll,
                    onItemToggle  = onItemToggle
                )
                ListType.SINGLE_TASK -> SingleTaskCard(
                    listWithItems = listWithItems,
                    onMuteToggle  = onMuteToggle,
                    onItemToggle  = onItemToggle
                )
                else -> EducationPersonalDailyCard(
                    listWithItems = listWithItems,
                    currentTime   = currentTime,
                    onMuteToggle  = onMuteToggle,
                    onDelete      = onSwipeOrLongPress,
                    onViewAll     = onViewAll,
                    onItemToggle  = onItemToggle
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
//  "SİL / ARŞİVE AT" SEÇENEK DİALOĞU
// ─────────────────────────────────────────────
@Composable
private fun ListActionDialog(
    title: String,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.background
            )
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

                ActionRow(
                    icon = Icons.Rounded.Archive,
                    label = "Arşive At",
                    tint = VibrantTeal,
                    onClick = onArchive
                )
                ActionRow(
                    icon = Icons.Rounded.Delete,
                    label = "Sil",
                    tint = MaterialTheme.colorScheme.error,
                    onClick = onDelete
                )

                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Vazgeç", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                }
            }
        }
    }
}

@Composable
private fun ActionRow(
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
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onClick() })
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = tint
        )
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 80.dp),
        horizontalAlignment   = Alignment.CenterHorizontally,
        verticalArrangement   = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "✨", fontSize = 56.sp)
        Text(
            text       = "Henüz bir listen yok",
            style      = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text      = "Aşağıdaki + butonuna basarak\nilk listeni oluştur!",
            style     = MaterialTheme.typography.bodyLarge,
            color     = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
            textAlign = TextAlign.Center
        )
    }
}