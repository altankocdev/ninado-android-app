package com.altankoc.nidado.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.altankoc.nidado.domain.model.TodoList
import com.altankoc.nidado.domain.model.TodoListWithItems
import com.altankoc.nidado.domain.model.enums.ListType
import com.altankoc.nidado.domain.repository.ArchiveResult
import com.altankoc.nidado.domain.usecase.todolist.ArchiveTodoListUseCase
import com.altankoc.nidado.domain.usecase.todolist.DeleteTodoListUseCase
import com.altankoc.nidado.domain.usecase.todolist.GetActiveListsForHomeUseCase
import com.altankoc.nidado.domain.usecase.todolist.GetTodoListByIdUseCase
import com.altankoc.nidado.domain.usecase.todolist.ToggleItemCompletedUseCase
import com.altankoc.nidado.domain.usecase.todolist.UpdateTodoListUseCase
import com.altankoc.nidado.domain.usecase.user.GetUserUseCase
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getActiveListsForHomeUseCase: GetActiveListsForHomeUseCase,
    private val getUserUseCase: GetUserUseCase,
    private val deleteListUseCase: DeleteTodoListUseCase,
    private val updateTodoListUseCase: UpdateTodoListUseCase,
    private val toggleItemCompletedUseCase: ToggleItemCompletedUseCase,
    private val getTodoListByIdUseCase: GetTodoListByIdUseCase,
    private val taskReminderScheduler: TaskReminderScheduler,
    private val archiveTodoListUseCase: ArchiveTodoListUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

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
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                getUserUseCase(),
                getActiveListsForHomeUseCase()
            ) { user, lists ->
                val sorted = sortListsByNearestTask(lists)
                _state.update {
                    it.copy(
                        user = user,
                        activeLists = sorted,
                        filteredLists = applyFilter(sorted, it.selectedFilter),
                        isLoading = false
                    )
                }
            }.collect {}
        }
    }

    private fun sortListsByNearestTask(lists: List<TodoListWithItems>): List<TodoListWithItems> {
        val now = System.currentTimeMillis()
        return lists.sortedBy { listWithItems ->
            val nearestTime = when (listWithItems.todoList.type) {
                ListType.SHOPPING -> listWithItems.todoList.scheduledDate ?: Long.MAX_VALUE
                ListType.SINGLE_TASK -> {
                    val item = listWithItems.items.firstOrNull()
                    if (item != null && !item.isCompleted && item.reminderTime != null && item.reminderTime >= now) {
                        item.reminderTime
                    } else {
                        Long.MAX_VALUE
                    }
                }
                else -> listWithItems.items
                    .filterNot { it.isCompleted }
                    .mapNotNull { it.reminderTime }
                    .filter { it >= now }
                    .minOrNull() ?: Long.MAX_VALUE
            }
            nearestTime
        }
    }

    private fun applyFilter(
        lists: List<TodoListWithItems>,
        filter: ListType?
    ): List<TodoListWithItems> {
        return if (filter == null) lists
        else lists.filter { it.todoList.type == filter }
    }

    fun onFilterSelected(filter: ListType?) {
        _state.update {
            it.copy(
                selectedFilter = filter,
                filteredLists = applyFilter(it.activeLists, filter)
            )
        }
    }

    // Bir listenin tüm bekleyen bildirimlerini iptal eder. Market listesi
    // için öğe bazlı reminder yoktur, liste seviyesi sentinel (-listId)
    // bildirimi de ayrıca iptal edilir.
    private suspend fun cancelAllRemindersForList(listWithItems: TodoListWithItems) {
        listWithItems.items.forEach { item ->
            taskReminderScheduler.cancelReminder(item.id)
        }
        if (listWithItems.todoList.type == ListType.SHOPPING) {
            taskReminderScheduler.cancelReminder(-listWithItems.todoList.id)
        }
    }

    fun onDeleteList(todoList: TodoList) {
        viewModelScope.launch {
            val listWithItems = getTodoListByIdUseCase(todoList.id)
            listWithItems?.let { cancelAllRemindersForList(it) }
            deleteListUseCase(todoList)
        }
    }

    fun onToggleMute(todoList: TodoList) {
        viewModelScope.launch {
            val newMutedState = !todoList.isMuted
            updateTodoListUseCase(todoList.copy(isMuted = newMutedState))

            val listWithItems = getTodoListByIdUseCase(todoList.id) ?: return@launch
            val now = System.currentTimeMillis()

            if (newMutedState) {
                cancelAllRemindersForList(listWithItems)
            } else {
                if (todoList.isAlarmEnabled) {
                    if (todoList.type == ListType.SHOPPING) {
                        val date = todoList.scheduledDate
                        val time = todoList.startTime
                        if (date != null && time != null) {
                            val dateCal = java.util.Calendar.getInstance().apply { timeInMillis = date }
                            val timeCal = java.util.Calendar.getInstance().apply { timeInMillis = time }
                            val combined = java.util.Calendar.getInstance().apply {
                                set(
                                    dateCal.get(java.util.Calendar.YEAR),
                                    dateCal.get(java.util.Calendar.MONTH),
                                    dateCal.get(java.util.Calendar.DAY_OF_MONTH),
                                    timeCal.get(java.util.Calendar.HOUR_OF_DAY),
                                    timeCal.get(java.util.Calendar.MINUTE),
                                    0
                                )
                                set(java.util.Calendar.MILLISECOND, 0)
                            }.timeInMillis

                            if (combined > now) {
                                taskReminderScheduler.scheduleReminder(
                                    itemId = -todoList.id,
                                    listId = todoList.id,
                                    taskTitle = "Alışveriş zamanı geldi",
                                    reminderTime = combined
                                )
                            }
                        }
                    } else {
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
        }
    }

    fun onArchiveList(todoList: TodoList) {
        viewModelScope.launch {
            when (val result = archiveTodoListUseCase(todoList)) {
                is Resource.Success -> {
                    when (result.data) {
                        ArchiveResult.Success -> {
                            val listWithItems = getTodoListByIdUseCase(todoList.id)
                            listWithItems?.let { cancelAllRemindersForList(it) }
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
                is Resource.Error -> {
                    _uiEvent.send(UiEvent.ShowToast(result.message ?: "Hata oluştu"))
                }
                else -> Unit
            }
        }
    }

    fun onToggleItemCompleted(itemId: Int, isCompleted: Boolean) {
        viewModelScope.launch {
            toggleItemCompletedUseCase(itemId, isCompleted)
        }
    }

    fun navigateToDetail(listId: Int) {
        viewModelScope.launch {
            _uiEvent.send(UiEvent.Navigate("list_detail/$listId"))
        }
    }
}