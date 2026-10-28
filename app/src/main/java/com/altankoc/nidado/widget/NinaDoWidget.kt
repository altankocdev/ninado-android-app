package com.altankoc.nidado.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.altankoc.nidado.MainActivity
import com.altankoc.nidado.R
import com.altankoc.nidado.domain.model.enums.ListType
import com.altankoc.nidado.util.Constants
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val GRADIENTS = listOf("ocean", "sunset", "night")

private fun solidColor(color: Color) = ColorProvider(day = color, night = color)

private fun gradientDrawable(id: String): Int = when (id) {
    "sunset" -> R.drawable.widget_gradient_sunset
    "night" -> R.drawable.widget_gradient_night
    else -> R.drawable.widget_gradient_ocean
}

private fun typeLabel(type: ListType): String = when (type) {
    ListType.DAILY_PLAN -> "Günlük"
    ListType.EDUCATION -> "Eğitim"
    ListType.SINGLE_TASK -> "Tekli"
    ListType.PERSONAL -> "Takvim"
    ListType.SHOPPING -> "Market"
}

private fun formatTaskTime(timestamp: Long): String {
    val now = Calendar.getInstance()
    val task = Calendar.getInstance().apply { timeInMillis = timestamp }
    val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
    val isToday = now.get(Calendar.DAY_OF_YEAR) == task.get(Calendar.DAY_OF_YEAR) &&
            now.get(Calendar.YEAR) == task.get(Calendar.YEAR)
    val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
    val isTomorrow = tomorrow.get(Calendar.DAY_OF_YEAR) == task.get(Calendar.DAY_OF_YEAR) &&
            tomorrow.get(Calendar.YEAR) == task.get(Calendar.YEAR)
    return when {
        isToday -> "Bugün · $timeStr"
        isTomorrow -> "Yarın · $timeStr"
        else -> {
            val dateStr = SimpleDateFormat("dd MMM", Locale.forLanguageTag("tr"))
                .format(Date(timestamp))
            "$dateStr · $timeStr"
        }
    }
}

val WIDGET_NAVIGATE_KEY = ActionParameters.Key<String>("widget_navigate")

class NinaDoWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context, WidgetEntryPoint::class.java
        )
        val homeLists = entryPoint.getActiveListsForHomeUseCase()().first()
        val calendarLists = entryPoint.getCalendarListsUseCase()().first()
        val user = entryPoint.getUserUseCase()().first()
        val pages = WidgetTaskCalculator.computePages(homeLists, calendarLists)

        provideContent {
            val prefs = currentState<Preferences>()
            val gradientId = prefs[WidgetPrefKeys.GRADIENT_ID] ?: "ocean"
            val storedPage = prefs[WidgetPrefKeys.SELECTED_PAGE] ?: 0
            val lastCompletedItem = prefs[WidgetPrefKeys.LAST_COMPLETED_ITEM] ?: -1
            val lastMutedList = prefs[WidgetPrefKeys.LAST_MUTED_LIST] ?: -1
            val lastMutedState = prefs[WidgetPrefKeys.LAST_MUTED_STATE] ?: false
            val selectedPage = if (pages.isEmpty()) 0
            else storedPage.coerceIn(0, pages.size - 1)

            WidgetRoot(
                pages = pages,
                selectedPage = selectedPage,
                gradientId = gradientId,
                userName = user?.name ?: "",
                avatarId = user?.avatarId ?: 1,
                lastCompletedItem = lastCompletedItem,
                lastMutedList = lastMutedList,
                lastMutedState = lastMutedState
            )
        }
    }
}

@Composable
private fun WidgetRoot(
    pages: List<WidgetPageData>,
    selectedPage: Int,
    gradientId: String,
    userName: String,
    avatarId: Int,
    lastCompletedItem: Int,
    lastMutedList: Int,
    lastMutedState: Boolean
) {
    val compact = pages.size > 1

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(gradientDrawable(gradientId)))
            .cornerRadius(28.dp)
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = if (compact) 8.dp else 12.dp)
        ) {

            // ── HEADER ──
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sol: logo + isim → Home açar
                Row(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .clickable(
                            actionStartActivity<MainActivity>(
                                actionParametersOf(WIDGET_NAVIGATE_KEY to "home")
                            )
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.splash_icon),
                        contentDescription = null,
                        modifier = GlanceModifier.size(if (compact) 24.dp else 30.dp)
                    )
                    Spacer(modifier = GlanceModifier.width(8.dp))
                    Column {
                        if (!compact) {
                            Text(
                                text = "Merhaba",
                                style = TextStyle(
                                    color = solidColor(Color.White.copy(alpha = 0.6f)),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            )
                        }
                        Text(
                            text = if (compact) "Merhaba $userName" else userName,
                            style = TextStyle(
                                color = solidColor(Color.White),
                                fontWeight = FontWeight.Bold,
                                fontSize = if (compact) 13.sp else 16.sp
                            ),
                            maxLines = 1
                        )
                    }
                }

                // Sağ: avatar + gradient seçici + refresh
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = GlanceModifier
                        .background(ImageProvider(R.drawable.widget_pill_bg))
                        .padding(horizontal = 6.dp, vertical = 5.dp)
                ) {
                    // Avatar → Profile açar
                    Box(
                        modifier = GlanceModifier
                            .size(40.dp)
                            .background(ImageProvider(R.drawable.widget_circle_white_mid))
                            .clickable(
                                actionStartActivity<MainActivity>(
                                    actionParametersOf(WIDGET_NAVIGATE_KEY to "profile")
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            provider = ImageProvider(
                                Constants.AVATARS.getOrElse(avatarId - 1) { Constants.AVATARS[0] }
                            ),
                            contentDescription = null,
                            modifier = GlanceModifier.size(34.dp).cornerRadius(17.dp)
                        )
                    }

                    Spacer(modifier = GlanceModifier.width(4.dp))

                    // Gradient noktaları — 44dp dokunma alanı
                    GRADIENTS.forEach { gid ->
                        val isSelected = gid == gradientId
                        Box(
                            modifier = GlanceModifier
                                .size(44.dp)
                                .clickable(
                                    actionRunCallback<ChangeGradientAction>(
                                        actionParametersOf(GRADIENT_VALUE_KEY to gid)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                // Seçili: beyaz halka + küçük iç dot
                                Box(
                                    modifier = GlanceModifier
                                        .size(28.dp)
                                        .background(ImageProvider(R.drawable.widget_circle_selected)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = GlanceModifier
                                            .size(18.dp)
                                            .background(ImageProvider(gradientDrawable(gid)))
                                            .cornerRadius(9.dp)
                                    ) {}
                                }
                            } else {
                                // Seçili değil: düz dot
                                Box(
                                    modifier = GlanceModifier
                                        .size(22.dp)
                                        .background(ImageProvider(gradientDrawable(gid)))
                                        .cornerRadius(11.dp)
                                ) {}
                            }
                        }
                    }

                    Spacer(modifier = GlanceModifier.width(2.dp))

                    // Refresh — 40dp yuvarlak
                    Box(
                        modifier = GlanceModifier
                            .size(40.dp)
                            .background(ImageProvider(R.drawable.widget_circle_white_mid))
                            .clickable(actionRunCallback<RefreshAction>()),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "↺",
                            style = TextStyle(
                                color = solidColor(Color.White),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            Spacer(modifier = GlanceModifier.height(if (compact) 6.dp else 10.dp))

            // ── GLASS CARD → Home açar ──
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .defaultWeight()
                    .background(ImageProvider(R.drawable.widget_glass_card))
                    .cornerRadius(20.dp)
                    .clickable(
                        actionStartActivity<MainActivity>(
                            actionParametersOf(WIDGET_NAVIGATE_KEY to "home")
                        )
                    )
                    .padding(if (compact) 10.dp else 14.dp)
            ) {
                if (pages.isEmpty()) {
                    EmptyContent()
                } else {
                    TaskPage(
                        page = pages[selectedPage],
                        lastCompletedItem = lastCompletedItem,
                        lastMutedList = lastMutedList,
                        lastMutedState = lastMutedState,
                        compact = compact
                    )
                }
            }

            // ── OK TUŞLARI ──
            if (pages.size > 1) {
                Spacer(modifier = GlanceModifier.height(4.dp))
                PageNavRow(currentPage = selectedPage, totalPages = pages.size)
            }
        }
    }
}

@Composable
private fun EmptyContent() {
    Column(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Her şey yolunda ✨",
            style = TextStyle(
                color = solidColor(Color.White),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(modifier = GlanceModifier.height(4.dp))
        Text(
            text = "Yeni bir görev ekleyerek başla",
            style = TextStyle(
                color = solidColor(Color.White.copy(alpha = 0.5f)),
                fontSize = 12.sp
            )
        )
        Spacer(modifier = GlanceModifier.height(12.dp))
        Box(
            modifier = GlanceModifier
                .size(52.dp)
                .background(ImageProvider(R.drawable.widget_circle_white_mid))
                .clickable(
                    actionStartActivity<MainActivity>(
                        actionParametersOf(WIDGET_NAVIGATE_KEY to "create_list")
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "＋",
                style = TextStyle(
                    color = solidColor(Color.White),
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp
                )
            )
        }
    }
}

@Composable
private fun TaskPage(
    page: WidgetPageData,
    lastCompletedItem: Int,
    lastMutedList: Int,
    lastMutedState: Boolean,
    compact: Boolean = false
) {
    val isJustCompleted = page.item.id == lastCompletedItem
    val isMuted = if (page.listId == lastMutedList) lastMutedState else page.isMuted
    val timeText = page.displayTime?.let { formatTaskTime(it) }

    Column(modifier = GlanceModifier.fillMaxWidth()) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = GlanceModifier
                    .background(ImageProvider(R.drawable.widget_circle_white_low))
                    .cornerRadius(12.dp)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${page.listTitle} • ${typeLabel(page.listType)}",
                    style = TextStyle(
                        color = solidColor(Color.White.copy(alpha = 0.9f)),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1
                )
            }
            Spacer(modifier = GlanceModifier.defaultWeight())
            if (page.isAlarmEnabled) {
                Box(
                    modifier = GlanceModifier
                        .size(36.dp)
                        .background(ImageProvider(R.drawable.widget_circle_white_low))
                        .clickable(
                            actionRunCallback<ToggleMuteAction>(
                                actionParametersOf(LIST_ID_KEY to page.listId)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isMuted) "🔕" else "🔔",
                        style = TextStyle(fontSize = 15.sp)
                    )
                }
            }
        }

        Spacer(modifier = GlanceModifier.height(if (compact) 8.dp else 12.dp))

        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tik butonu
            Box(
                modifier = GlanceModifier
                    .size(if (compact) 36.dp else 44.dp)
                    .background(
                        ImageProvider(
                            if (isJustCompleted) R.drawable.widget_circle_green
                            else R.drawable.widget_circle_white_low
                        )
                    )
                    .clickable(
                        actionRunCallback<CompleteTaskAction>(
                            actionParametersOf(ITEM_ID_KEY to page.item.id)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isJustCompleted) {
                    Text(
                        text = "✓",
                        style = TextStyle(
                            color = solidColor(Color.White),
                            fontWeight = FontWeight.Bold,
                            fontSize = if (compact) 16.sp else 20.sp
                        )
                    )
                }
            }

            Spacer(modifier = GlanceModifier.width(12.dp))

            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = page.item.title,
                    style = TextStyle(
                        color = solidColor(
                            if (isJustCompleted) Color.White.copy(alpha = 0.3f)
                            else Color.White
                        ),
                        fontWeight = FontWeight.Bold,
                        fontSize = if (compact) 17.sp else 20.sp
                    ),
                    maxLines = 1
                )
                if (timeText != null) {
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (page.isLive) {
                            Box(
                                modifier = GlanceModifier
                                    .size(8.dp)
                                    .background(ImageProvider(R.drawable.widget_circle_yellow))
                            ) {}
                            Spacer(modifier = GlanceModifier.width(6.dp))
                        }
                        Text(
                            text = if (page.isLive) {
                                val rawTime = page.displayTime?.let {
                                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(it))
                                } ?: ""
                                "Şu an • $rawTime"
                            } else timeText,
                            style = TextStyle(
                                color = solidColor(
                                    if (page.isLive) Color(0xFFFFD60A)
                                    else Color.White.copy(alpha = 0.5f)
                                ),
                                fontWeight = if (page.isLive) FontWeight.Bold
                                else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PageNavRow(currentPage: Int, totalPages: Int) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = GlanceModifier
                .size(36.dp)
                .background(ImageProvider(R.drawable.widget_circle_white_low))
                .clickable(
                    actionRunCallback<ChangePageAction>(
                        actionParametersOf(DIRECTION_KEY to -1)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "←",
                style = TextStyle(
                    color = solidColor(Color.White),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
        Spacer(modifier = GlanceModifier.width(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${currentPage + 1}",
                style = TextStyle(
                    color = solidColor(Color.White),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            )
            Text(
                text = " / $totalPages",
                style = TextStyle(
                    color = solidColor(Color.White.copy(alpha = 0.4f)),
                    fontSize = 13.sp
                )
            )
        }
        Spacer(modifier = GlanceModifier.width(16.dp))
        Box(
            modifier = GlanceModifier
                .size(36.dp)
                .background(ImageProvider(R.drawable.widget_circle_white_low))
                .clickable(
                    actionRunCallback<ChangePageAction>(
                        actionParametersOf(DIRECTION_KEY to 1)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "→",
                style = TextStyle(
                    color = solidColor(Color.White),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}