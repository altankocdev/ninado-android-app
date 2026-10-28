package com.altankoc.nidado.ui.calendar

import com.altankoc.nidado.domain.model.TodoListWithItems
import java.util.Calendar

enum class CalendarViewMode {
    GRID, FEED
}

data class CalendarUiState(
    val viewMode: CalendarViewMode = CalendarViewMode.GRID,
    val allLists: List<TodoListWithItems> = emptyList(),
    val currentTime: Long = System.currentTimeMillis(),
    val isLoading: Boolean = true,

    // Takvim grid navigasyonu — şu an gösterilen ay/yıl
    val displayedYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val displayedMonth: Int = Calendar.getInstance().get(Calendar.MONTH), // 0-11

    // Gün detay paneli — boş günde "+" ile oluştur, dolu günde önizleme
    val selectedDayMillis: Long? = null,
    val selectedDayList: TodoListWithItems? = null
)