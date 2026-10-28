package com.altankoc.nidado.ui.createlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.altankoc.nidado.domain.model.ListItem
import com.altankoc.nidado.domain.model.TodoList
import com.altankoc.nidado.domain.model.enums.ListItemType
import com.altankoc.nidado.domain.model.enums.ListType
import com.altankoc.nidado.domain.usecase.course.GetCoursesUseCase
import com.altankoc.nidado.domain.usecase.todolist.GetListByDateUseCase
import com.altankoc.nidado.domain.usecase.todolist.GetTodoListByIdUseCase
import com.altankoc.nidado.domain.usecase.todolist.InsertTodoListUseCase
import com.altankoc.nidado.util.Resource
import com.altankoc.nidado.util.UiEvent
import com.altankoc.nidado.util.notification.TaskReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class CreateListViewModel @Inject constructor(
    private val insertTodoListUseCase: InsertTodoListUseCase,
    private val getCoursesUseCase: GetCoursesUseCase,
    private val getTodoListByIdUseCase: GetTodoListByIdUseCase,
    private val getListByDateUseCase: GetListByDateUseCase,
    private val taskReminderScheduler: TaskReminderScheduler
) : ViewModel() {

    private val _state = MutableStateFlow(CreateListUiState())
    val state: StateFlow<CreateListUiState> = _state.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    fun setPreselectedDate(date: Long?) {
        if (date != null) {
            _state.update {
                it.copy(
                    scheduledDate = date,
                    selectedType = ListType.PERSONAL,
                    title = "",
                    taskInputs = listOf(TaskInput()),
                    shoppingItems = listOf(ShoppingInput())
                )
            }
        }
    }

    init {
        loadCourses()
    }

    private fun loadCourses() {
        viewModelScope.launch {
            getCoursesUseCase(ascending = true).collect { courses ->
                _state.update { it.copy(availableCourses = courses) }
            }
        }
    }

    fun onTypeSelected(type: ListType) {
        _state.update {
            it.copy(
                selectedType = type,
                title = if (type == ListType.SINGLE_TASK) "Tekli Görev" else "",
                taskInputs = listOf(TaskInput()),
                shoppingItems = listOf(ShoppingInput()),
                dateError = null
            )
        }
    }

    fun onTitleChanged(title: String) {
        _state.update { it.copy(title = title, titleError = null) }
    }

    fun onTimedToggle(enabled: Boolean) {
        _state.update {
            it.copy(
                isTimed = enabled,
                isAlarmEnabled = if (!enabled && it.selectedType != ListType.SHOPPING) false else it.isAlarmEnabled
            )
        }
    }

    fun onDateSelected(date: Long) {
        _state.update { it.copy(scheduledDate = date, dateError = null) }
    }

    fun onTimeSelected(time: Long) {
        _state.update { it.copy(startTime = time) }
    }

    fun onTaskTitleChanged(id: Long, title: String) {
        _state.update { current ->
            val tasks = current.taskInputs.map {
                if (it.id == id) it.copy(title = title, titleError = null) else it
            }
            current.copy(taskInputs = tasks)
        }
    }

    fun onTaskTimeSelected(id: Long, time: Long) {
        _state.update { current ->
            val tasks = current.taskInputs.map {
                if (it.id == id) it.copy(reminderTime = time) else it
            }
            val sortedTasks = tasks.sortedBy { it.reminderTime ?: Long.MAX_VALUE }
            current.copy(taskInputs = sortedTasks)
        }
    }

    fun onTaskCourseSelected(id: Long, courseId: Int, courseName: String, courseDifficulty: Float) {
        _state.update { current ->
            val tasks = current.taskInputs.map {
                if (it.id == id) it.copy(
                    title = courseName,
                    courseId = courseId,
                    courseName = courseName,
                    courseDifficulty = courseDifficulty
                ) else it
            }
            current.copy(taskInputs = tasks)
        }
    }

    fun onAddTask() {
        _state.update { it.copy(taskInputs = it.taskInputs + TaskInput()) }
    }

    fun onRemoveTask(id: Long) {
        _state.update { current ->
            if (current.taskInputs.size > 1) {
                current.copy(taskInputs = current.taskInputs.filter { it.id != id })
            } else current
        }
    }

    fun onShoppingTitleChanged(id: Long, title: String) {
        _state.update { current ->
            val items = current.shoppingItems.map {
                if (it.id == id) it.copy(title = title, titleError = null) else it
            }
            current.copy(shoppingItems = items)
        }
    }

    fun onShoppingQuantityChanged(id: Long, quantity: String) {
        _state.update { current ->
            val items = current.shoppingItems.map {
                if (it.id == id) it.copy(quantity = quantity) else it
            }
            current.copy(shoppingItems = items)
        }
    }

    fun onAddShoppingItem() {
        _state.update { it.copy(shoppingItems = it.shoppingItems + ShoppingInput()) }
    }

    fun onRemoveShoppingItem(id: Long) {
        _state.update { current ->
            if (current.shoppingItems.size > 1) {
                current.copy(shoppingItems = current.shoppingItems.filter { it.id != id })
            } else current
        }
    }

    fun onSave() {
        val state = _state.value

        if (state.selectedType != ListType.SINGLE_TASK && state.title.isBlank()) {
            _state.update { it.copy(titleError = "Liste adı boş olamaz") }
            return
        }

        // PERSONAL (Takvim) için tarih zorunlu, geçmiş tarih olamaz
        if (state.selectedType == ListType.PERSONAL) {
            val date = state.scheduledDate
            if (date == null) {
                _state.update { it.copy(dateError = "Bir tarih seçmelisiniz") }
                return
            }
            if (isPastDate(date)) {
                _state.update { it.copy(dateError = "Geçmiş bir tarihe liste oluşturulamaz") }
                return
            }
        }

        val taskCount = when (state.selectedType) {
            ListType.SHOPPING -> state.shoppingItems.count { it.title.isNotBlank() }
            ListType.SINGLE_TASK -> state.taskInputs.take(1).count { it.title.isNotBlank() }
            ListType.PERSONAL -> state.taskInputs.count { it.title.isNotBlank() }
            ListType.EDUCATION -> state.taskInputs.count { it.courseId != null }
            ListType.DAILY_PLAN -> state.taskInputs.count { it.title.isNotBlank() }
        }

        if (taskCount == 0) {
            viewModelScope.launch {
                _uiEvent.send(UiEvent.ShowToast("Boş liste oluşturulamaz"))
            }
            return
        }

        // Minimum 2 görev kuralı SADECE Eğitim ve Günlük için geçerli.
        // Takvim ve Market tek elemanla da kaydedilebilir.
        if (taskCount == 1 && (state.selectedType == ListType.EDUCATION || state.selectedType == ListType.DAILY_PLAN)) {
            viewModelScope.launch {
                _uiEvent.send(UiEvent.ShowToast("Bir görev ekleyecekseniz tekli görev'i kullanmalısınız"))
            }
            return
        }

        if (state.isTimed && state.selectedType != ListType.SHOPPING) {
            val missingTime = if (state.selectedType == ListType.SINGLE_TASK) {
                state.taskInputs.take(1).any { it.reminderTime == null }
            } else {
                state.taskInputs.any { it.reminderTime == null }
            }

            if (missingTime) {
                viewModelScope.launch {
                    _uiEvent.send(UiEvent.ShowToast("Saatli listede tüm görevlerin saatini girmelisiniz"))
                }
                return
            }
        }

        viewModelScope.launch {
            if (state.selectedType == ListType.PERSONAL) {
                val existing = getListByDateUseCase(state.scheduledDate!!)
                if (existing != null) {
                    _state.update { it.copy(dateError = "Bu tarihe zaten bir liste oluşturulmuş") }
                    return@launch
                }
            }

            _state.update { it.copy(isLoading = true) }

            val todoList = TodoList(
                title = state.title,
                type = state.selectedType,
                isTimed = state.isTimed,
                isAlarmEnabled = state.isAlarmEnabled,
                scheduledDate = state.scheduledDate,
                startTime = state.startTime
            )

            val items = when (state.selectedType) {
                ListType.SHOPPING -> state.shoppingItems.filter { it.title.isNotBlank() }.map { item ->
                    ListItem(
                        listId = 0,
                        type = ListItemType.SHOPPING_ITEM,
                        title = item.title,
                        quantity = item.quantity.ifBlank { null }
                    )
                }
                ListType.SINGLE_TASK -> state.taskInputs.take(1).filter { it.title.isNotBlank() }.map { task ->
                    ListItem(
                        listId = 0,
                        type = ListItemType.PERSONAL_TASK,
                        title = task.title,
                        reminderTime = task.reminderTime
                    )
                }
                ListType.EDUCATION -> state.taskInputs.filter { it.courseId != null }.map { task ->
                    ListItem(
                        listId = 0,
                        type = ListItemType.COURSE_TASK,
                        title = task.title,
                        courseId = task.courseId,
                        courseName = task.courseName,
                        courseDifficulty = task.courseDifficulty?.toInt(),
                        reminderTime = task.reminderTime
                    )
                }
                ListType.DAILY_PLAN -> state.taskInputs.filter { it.title.isNotBlank() }.map { task ->
                    if (task.courseId != null) {
                        ListItem(
                            listId = 0,
                            type = ListItemType.COURSE_TASK,
                            title = task.title,
                            courseId = task.courseId,
                            courseName = task.courseName,
                            courseDifficulty = task.courseDifficulty?.toInt(),
                            reminderTime = task.reminderTime
                        )
                    } else {
                        ListItem(
                            listId = 0,
                            type = ListItemType.PERSONAL_TASK,
                            title = task.title,
                            reminderTime = task.reminderTime
                        )
                    }
                }
                else -> state.taskInputs.filter { it.title.isNotBlank() }.map { task ->
                    ListItem(
                        listId = 0,
                        type = ListItemType.PERSONAL_TASK,
                        title = task.title,
                        reminderTime = task.reminderTime
                    )
                }
            }

            when (val result = insertTodoListUseCase(todoList, items)) {
                is Resource.Success -> {
                    val listId = result.data ?: 0
                    if (state.isAlarmEnabled) {
                        scheduleRemindersForList(listId)
                    }
                    _uiEvent.send(UiEvent.NavigateBack)
                }
                is Resource.Error -> {
                    _state.update { it.copy(isLoading = false, titleError = result.message) }
                }
                else -> Unit
            }
        }
    }

    private fun isPastDate(date: Long): Boolean {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val target = Calendar.getInstance().apply {
            timeInMillis = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return target.timeInMillis < today.timeInMillis
    }

    private suspend fun scheduleRemindersForList(listId: Int) {
        val listWithItems = getTodoListByIdUseCase(listId) ?: return
        val todoList = listWithItems.todoList

        if (todoList.type == ListType.SHOPPING) {
            // Market: saat öğelerde değil, listenin kendisinde (scheduledDate + startTime).
            // Tek bir "liste seviyesi" bildirim kuruluyor, sentinel negatif ID ile.
            val date = todoList.scheduledDate
            val time = todoList.startTime
            if (date != null && time != null) {
                val dateCal = Calendar.getInstance().apply { timeInMillis = date }
                val timeCal = Calendar.getInstance().apply { timeInMillis = time }
                val combined = Calendar.getInstance().apply {
                    set(
                        dateCal.get(Calendar.YEAR),
                        dateCal.get(Calendar.MONTH),
                        dateCal.get(Calendar.DAY_OF_MONTH),
                        timeCal.get(Calendar.HOUR_OF_DAY),
                        timeCal.get(Calendar.MINUTE),
                        0
                    )
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                taskReminderScheduler.scheduleReminder(
                    itemId = -listId,
                    listId = listId,
                    taskTitle = "Alışveriş zamanı!",
                    reminderTime = combined
                )
            }
        } else {
            listWithItems.items.forEach { item ->
                item.reminderTime?.let { time ->
                    taskReminderScheduler.scheduleReminder(
                        itemId = item.id,
                        listId = listId,
                        taskTitle = item.title,
                        reminderTime = time
                    )
                }
            }
        }
    }

    fun onAlarmToggle(enabled: Boolean) {
        _state.update { it.copy(isAlarmEnabled = enabled) }
    }
}