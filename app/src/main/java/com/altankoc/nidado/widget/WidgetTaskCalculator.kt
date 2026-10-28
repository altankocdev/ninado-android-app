package com.altankoc.nidado.widget

import com.altankoc.nidado.domain.model.ListItem
import com.altankoc.nidado.domain.model.TodoListWithItems
import com.altankoc.nidado.domain.model.enums.ListType

enum class WidgetCategory {
    OTHERS, CALENDAR
}

data class WidgetPageData(
    val category: WidgetCategory,
    val listId: Int,
    val listTitle: String,
    val listType: ListType,
    val item: ListItem,
    val displayTime: Long?,
    val isAlarmEnabled: Boolean,
    val isMuted: Boolean,
    val isLive: Boolean // "şimdi" penceresinde mi (son 60 saniye içinde başladı)
)

object WidgetTaskCalculator {
    private const val BLINK_WINDOW_MS = 60_000L
    private val OTHER_TYPES = setOf(ListType.DAILY_PLAN, ListType.EDUCATION, ListType.SINGLE_TASK)

    // Sayfa sırası sabit: Diğerleri > Takvim. Sadece görevi olan kategoriler
    // sayfa olarak döner, boş kategori listeden düşer. Market widget'a dahil değil.
    fun computePages(
        homeLists: List<TodoListWithItems>,
        calendarLists: List<TodoListWithItems>,
        now: Long = System.currentTimeMillis()
    ): List<WidgetPageData> {
        val others = homeLists.filter { it.todoList.type in OTHER_TYPES }

        val pages = mutableListOf<WidgetPageData>()
        computeNextTaskPage(others, WidgetCategory.OTHERS, now)?.let { pages += it }
        computeNextTaskPage(calendarLists, WidgetCategory.CALENDAR, now)?.let { pages += it }
        return pages
    }

    private fun computeNextTaskPage(
        lists: List<TodoListWithItems>,
        category: WidgetCategory,
        now: Long
    ): WidgetPageData? {
        data class Candidate(val list: TodoListWithItems, val item: ListItem)

        val candidates = lists.flatMap { list ->
            list.items.filter { !it.isCompleted }.map { Candidate(list, it) }
        }
        if (candidates.isEmpty()) return null

        val timed = candidates.filter { it.item.reminderTime != null }
        val untimed = candidates.filter { it.item.reminderTime == null }

        val live = timed.filter { now - it.item.reminderTime!! in 0 until BLINK_WINDOW_MS }
        val future = timed.filter { it.item.reminderTime!! > now }
        val overdue = timed.filter { now - it.item.reminderTime!! >= BLINK_WINDOW_MS }

        val chosen = when {
            live.isNotEmpty() -> live.minByOrNull { it.item.reminderTime!! }
            future.isNotEmpty() -> future.minByOrNull { it.item.reminderTime!! }
            untimed.isNotEmpty() -> untimed.minByOrNull { it.item.id }
            else -> null
        } ?: return null

        val isLive = chosen.item.reminderTime?.let { now - it in 0 until BLINK_WINDOW_MS } ?: false

        return WidgetPageData(
            category = category,
            listId = chosen.list.todoList.id,
            listTitle = chosen.list.todoList.title,
            listType = chosen.list.todoList.type,
            item = chosen.item,
            displayTime = chosen.item.reminderTime,
            isAlarmEnabled = chosen.list.todoList.isAlarmEnabled,
            isMuted = chosen.list.todoList.isMuted,
            isLive = isLive
        )
    }
}