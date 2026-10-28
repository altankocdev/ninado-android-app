package com.altankoc.nidado.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.altankoc.nidado.domain.model.TodoList
import com.altankoc.nidado.domain.usecase.todolist.ArchiveTodoListUseCase
import com.altankoc.nidado.domain.usecase.todolist.DeleteTodoListUseCase
import com.altankoc.nidado.domain.usecase.todolist.GetCalendarListsUseCase
import com.altankoc.nidado.domain.usecase.todolist.GetTodoListByIdUseCase
import com.altankoc.nidado.domain.usecase.todolist.ToggleItemCompletedUseCase
import com.altankoc.nidado.domain.usecase.todolist.UpdateTodoListUseCase
import com.altankoc.nidado.domain.repository.ArchiveResult
import com.altankoc.nidado.util.Resource
import com.altankoc.nidado.util.UiEvent
import com.altankoc.nidado.util.notification.TaskReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val getCalendarListsUseCase: GetCalendarListsUseCase,
    private val getTodoListByIdUseCase: GetTodoListByIdUseCase,
    private val deleteTodoListUseCase: DeleteTodoListUseCase,
    private val updateTodoListUseCase: UpdateTodoListUseCase,
    private val toggleItemCompletedUseCase: ToggleItemCompletedUseCase,
    private val archiveTodoListUseCase: ArchiveTodoListUseCase,
    private val taskReminderScheduler: TaskReminderScheduler
) : ViewModel() {

    private val _state = MutableStateFlow(CalendarUiState())
    val state: StateFlow<CalendarUiState> = _state.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    val currentTime: StateFlow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(15_000)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = System.currentTimeMillis()
    )

    init {
        loadLists()
    }

    private fun loadLists() {
        viewModelScope.launch {
            getCalendarListsUseCase().collect { lists ->
                val sorted = lists.sortedBy { it.todoList.scheduledDate ?: Long.MAX_VALUE }
                _state.update { it.copy(allLists = sorted, isLoading = false) }
            }
        }
    }

    fun onViewModeChanged(mode: CalendarViewMode) {
        _state.update { it.copy(viewMode = mode) }
    }

    // ── Takvim grid navigasyonu ──
    fun onPreviousMonth() {
        _state.update {
            val cal = Calendar.getInstance().apply {
                set(it.displayedYear, it.displayedMonth, 1)
                add(Calendar.MONTH, -1)
            }
            it.copy(displayedYear = cal.get(Calendar.YEAR), displayedMonth = cal.get(Calendar.MONTH))
        }
    }

    fun onNextMonth() {
        _state.update {
            val cal = Calendar.getInstance().apply {
                set(it.displayedYear, it.displayedMonth, 1)
                add(Calendar.MONTH, 1)
            }
            it.copy(displayedYear = cal.get(Calendar.YEAR), displayedMonth = cal.get(Calendar.MONTH))
        }
    }

    // ── Gün seçimi (takvimde bir güne tıklanınca) ──
    fun onDayClicked(dayMillis: Long) {
        val existing = findListForDay(dayMillis)
        _state.update { it.copy(selectedDayMillis = dayMillis, selectedDayList = existing) }
    }

    fun onDayDetailDismiss() {
        _state.update { it.copy(selectedDayMillis = null, selectedDayList = null) }
    }

    private fun findListForDay(dayMillis: Long): com.altankoc.nidado.domain.model.TodoListWithItems? {
        val target = Calendar.getInstance().apply { timeInMillis = dayMillis }
        return _state.value.allLists.find { listWithItems ->
            val date = listWithItems.todoList.scheduledDate ?: return@find false
            val listCal = Calendar.getInstance().apply { timeInMillis = date }
            listCal.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
                    listCal.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)
        }
    }

    // ── Liste etkileşimleri (Feed ve gün detayı ortak kullanır) ──
    fun onToggleItemCompleted(itemId: Int, isCompleted: Boolean) {
        viewModelScope.launch {
            toggleItemCompletedUseCase(itemId, isCompleted)
        }
    }

    fun onToggleMute(todoList: TodoList) {
        viewModelScope.launch {
            val newMutedState = !todoList.isMuted
            updateTodoListUseCase(todoList.copy(isMuted = newMutedState))

            val listWithItems = getTodoListByIdUseCase(todoList.id) ?: return@launch
            val now = System.currentTimeMillis()

            if (newMutedState) {
                listWithItems.items.forEach { item -> taskReminderScheduler.cancelReminder(item.id) }
            } else if (todoList.isAlarmEnabled) {
                listWithItems.items
                    .filter { !it.isCompleted && it.reminderTime != null && it.reminderTime > now }
                    .forEach { item ->
                        taskReminderScheduler.scheduleReminder(
                            itemId = item.id,
                            listId = todoList.id,
                            taskTitle = item.title,
                            reminderTime = item.reminderTime!!
                        )
                    }
            }
        }
    }

    fun onDeleteList(todoList: TodoList) {
        viewModelScope.launch {
            val listWithItems = getTodoListByIdUseCase(todoList.id)
            listWithItems?.items?.forEach { item -> taskReminderScheduler.cancelReminder(item.id) }
            deleteTodoListUseCase(todoList)
        }
    }

    fun onArchiveList(todoList: TodoList) {
        viewModelScope.launch {
            when (val result = archiveTodoListUseCase(todoList)) {
                is Resource.Success -> {
                    when (result.data) {
                        ArchiveResult.Success -> {
                            val listWithItems = getTodoListByIdUseCase(todoList.id)
                            listWithItems?.items?.forEach { item -> taskReminderScheduler.cancelReminder(item.id) }
                            _uiEvent.send(UiEvent.ShowToast("Liste arşive taşındı"))
                        }
                        ArchiveResult.ArchiveFull -> {
                            _uiEvent.send(UiEvent.ShowToast("Arşiviniz dolu, önce bir liste silmelisiniz"))
                        }
                        ArchiveResult.NotFinished -> {
                            _uiEvent.send(UiEvent.ShowToast("Bu listenin süresi henüz geçmedi, arşivlenemez"))
                        }
                        else -> Unit
                    }
                }
                is Resource.Error -> _uiEvent.send(UiEvent.ShowToast(result.message ?: "Hata oluştu"))
                else -> Unit
            }
        }
    }

    fun navigateToCreateWithDate(dayMillis: Long) {
        viewModelScope.launch {
            _uiEvent.send(UiEvent.Navigate("create_list?date=$dayMillis"))
        }
    }

    fun navigateToDetail(listId: Int) {
        viewModelScope.launch {
            _uiEvent.send(UiEvent.Navigate("list_detail/$listId"))
        }
    }
}