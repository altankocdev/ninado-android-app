package com.altankoc.nidado.ui.listdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.altankoc.nidado.domain.model.ListItem
import com.altankoc.nidado.domain.model.TodoList
import com.altankoc.nidado.domain.model.enums.ListItemType
import com.altankoc.nidado.domain.usecase.todolist.DeleteListItemUseCase
import com.altankoc.nidado.domain.usecase.todolist.DeleteListItemsUseCase
import com.altankoc.nidado.domain.usecase.todolist.DeleteTodoListUseCase
import com.altankoc.nidado.domain.usecase.todolist.GetTodoListByIdUseCase
import com.altankoc.nidado.domain.usecase.todolist.InsertListItemUseCase
import com.altankoc.nidado.domain.usecase.todolist.ToggleItemCompletedUseCase
import com.altankoc.nidado.domain.usecase.todolist.UpdateFinishedAtUseCase
import com.altankoc.nidado.domain.usecase.todolist.UpdateListItemUseCase
import com.altankoc.nidado.domain.usecase.todolist.UpdateTodoListUseCase
import com.altankoc.nidado.util.Resource
import com.altankoc.nidado.util.UiEvent
import com.altankoc.nidado.util.notification.TaskReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val DUE_GRACE_MS = 60_000L

@HiltViewModel
class ListDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getTodoListByIdUseCase: GetTodoListByIdUseCase,
    private val updateTodoListUseCase: UpdateTodoListUseCase,
    private val updateListItemUseCase: UpdateListItemUseCase,
    private val insertListItemUseCase: InsertListItemUseCase,
    private val deleteListItemUseCase: DeleteListItemUseCase,
    private val deleteListItemsUseCase: DeleteListItemsUseCase,
    private val deleteTodoListUseCase: DeleteTodoListUseCase,
    private val toggleItemCompletedUseCase: ToggleItemCompletedUseCase,
    private val updateFinishedAtUseCase: UpdateFinishedAtUseCase,
    private val taskReminderScheduler: TaskReminderScheduler
) : ViewModel() {

    private val listId: Int = savedStateHandle.get<Int>("listId") ?: -1

    private val _state = MutableStateFlow(ListDetailUiState())
    val state: StateFlow<ListDetailUiState> = _state.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        loadList()
        startTicker()
    }

    private fun startTicker() {
        viewModelScope.launch {
            while (true) {
                delay(15_000)
                _state.update { it.copy(currentTime = System.currentTimeMillis()) }
            }
        }
    }

    private fun loadList() {
        viewModelScope.launch {
            val result = getTodoListByIdUseCase(listId)
            if (result == null) {
                _uiEvent.send(UiEvent.NavigateBack)
                return@launch
            }
            _state.update {
                it.copy(
                    todoList = result.todoList,
                    items = result.items.sortedBy { item -> item.reminderTime ?: Long.MAX_VALUE },
                    listTitleInput = result.todoList.title,
                    isLoading = false
                )
            }
        }
    }

    fun isPast(item: ListItem): Boolean {
        val now = _state.value.currentTime
        return item.reminderTime != null && (now - item.reminderTime) >= DUE_GRACE_MS
    }

    // ── Liste adı düzenleme ──
    fun onEditListTitleClick() {
        _state.update { it.copy(isEditingListTitle = true, listTitleInput = it.todoList?.title ?: "") }
    }

    fun onListTitleChanged(title: String) {
        _state.update { it.copy(listTitleInput = title, listTitleError = null) }
    }

    fun onListTitleConfirm() {
        val title = _state.value.listTitleInput.trim()
        if (title.isBlank()) {
            _state.update { it.copy(listTitleError = "Liste adı boş olamaz") }
            return
        }
        val current = _state.value.todoList ?: return
        viewModelScope.launch {
            when (val result = updateTodoListUseCase(current.copy(title = title))) {
                is Resource.Success -> {
                    _state.update { it.copy(isEditingListTitle = false, todoList = current.copy(title = title)) }
                }
                is Resource.Error -> _state.update { it.copy(listTitleError = result.message) }
                else -> Unit
            }
        }
    }

    fun onListTitleDismiss() {
        _state.update { it.copy(isEditingListTitle = false, listTitleError = null) }
    }

    // ── Görev adı düzenleme ──
    fun onEditItemClick(item: ListItem) {
        if (isPast(item)) return
        _state.update { it.copy(editingItemId = item.id, itemTitleInput = item.title, itemTitleError = null) }
    }

    fun onItemTitleChanged(title: String) {
        _state.update { it.copy(itemTitleInput = title, itemTitleError = null) }
    }

    fun onItemTitleConfirm() {
        val itemId = _state.value.editingItemId ?: return
        val title = _state.value.itemTitleInput.trim()
        if (title.isBlank()) {
            _state.update { it.copy(itemTitleError = "Görev adı boş olamaz") }
            return
        }
        val item = _state.value.items.find { it.id == itemId } ?: return
        viewModelScope.launch {
            when (val result = updateListItemUseCase(item.copy(title = title))) {
                is Resource.Success -> {
                    _state.update { s ->
                        s.copy(
                            editingItemId = null,
                            items = s.items.map { if (it.id == itemId) it.copy(title = title) else it }
                        )
                    }
                    // Bildirimin görüntülenen başlığı da güncellensin
                    rescheduleIfNeeded(item.copy(title = title))
                }
                is Resource.Error -> _state.update { it.copy(itemTitleError = result.message) }
                else -> Unit
            }
        }
    }

    fun onItemTitleDismiss() {
        _state.update { it.copy(editingItemId = null, itemTitleError = null) }
    }

    // ── Saat değiştirme ──
    fun onTimePickerOpen(item: ListItem) {
        if (isPast(item)) return
        _state.update { it.copy(timePickerItemId = item.id) }
    }

    fun onTimeSelected(newTime: Long) {
        val itemId = _state.value.timePickerItemId ?: return
        val item = _state.value.items.find { it.id == itemId } ?: return
        viewModelScope.launch {
            val updated = item.copy(reminderTime = newTime)
            when (val result = updateListItemUseCase(updated)) {
                is Resource.Success -> {
                    _state.update { s ->
                        s.copy(
                            timePickerItemId = null,
                            items = s.items
                                .map { if (it.id == itemId) updated else it }
                                .sortedBy { it.reminderTime ?: Long.MAX_VALUE }
                        )
                    }
                    rescheduleIfNeeded(updated)
                    recomputeFinishedAt()
                }
                is Resource.Error -> {
                    _uiEvent.send(UiEvent.ShowToast(result.message ?: "Hata oluştu"))
                    _state.update { it.copy(timePickerItemId = null) }
                }
                else -> Unit
            }
        }
    }

    fun onTimePickerDismiss() {
        _state.update { it.copy(timePickerItemId = null) }
    }

    // ── Miktar değiştirme (Shopping) ──
    fun onEditQuantityClick(item: ListItem) {
        _state.update { it.copy(editingQuantityItemId = item.id, quantityInput = item.quantity ?: "") }
    }

    fun onQuantityChanged(quantity: String) {
        _state.update { it.copy(quantityInput = quantity) }
    }

    fun onQuantityConfirm() {
        val itemId = _state.value.editingQuantityItemId ?: return
        val item = _state.value.items.find { it.id == itemId } ?: return
        val quantity = _state.value.quantityInput.trim().ifBlank { null }
        viewModelScope.launch {
            val updated = item.copy(quantity = quantity)
            when (updateListItemUseCase(updated)) {
                is Resource.Success -> {
                    _state.update { s ->
                        s.copy(
                            editingQuantityItemId = null,
                            items = s.items.map { if (it.id == itemId) updated else it }
                        )
                    }
                }
                else -> Unit
            }
        }
    }

    fun onQuantityDismiss() {
        _state.update { it.copy(editingQuantityItemId = null) }
    }

    // ── Tik atma ──
    fun onToggleItemCompleted(item: ListItem) {
        viewModelScope.launch {
            val newCompleted = !item.isCompleted
            toggleItemCompletedUseCase(item.id, newCompleted)
            _state.update { s ->
                s.copy(items = s.items.map { if (it.id == item.id) it.copy(isCompleted = newCompleted) else it })
            }
            // toggleItemCompletedUseCase zaten finishedAt'i kendi içinde güncelliyor.
            // Local todoList state'ini de senkronize edelim:
            loadListSilently()
        }
    }

    // ── Çoklu seçim modu ──
    fun onSelectionModeToggle() {
        _state.update {
            it.copy(isSelectionMode = !it.isSelectionMode, selectedItemIds = emptySet())
        }
    }

    fun onItemSelected(item: ListItem) {
        if (isPast(item)) return // geçmiş görevler seçilemez
        _state.update { s ->
            val newSelection = if (s.selectedItemIds.contains(item.id)) {
                s.selectedItemIds - item.id
            } else {
                s.selectedItemIds + item.id
            }
            s.copy(selectedItemIds = newSelection)
        }
    }

    fun onDeleteSelectedRequest() {
        if (_state.value.selectedItemIds.isEmpty()) return
        _state.update { it.copy(showDeleteSelectedDialog = true) }
    }

    fun onDeleteSelectedConfirm() {
        val selectedIds = _state.value.selectedItemIds
        val itemsToDelete = _state.value.items.filter { selectedIds.contains(it.id) }
        viewModelScope.launch {
            when (deleteListItemsUseCase(itemsToDelete)) {
                is Resource.Success -> {
                    itemsToDelete.forEach { taskReminderScheduler.cancelReminder(it.id) }
                    _state.update { s ->
                        s.copy(
                            items = s.items.filterNot { selectedIds.contains(it.id) },
                            selectedItemIds = emptySet(),
                            isSelectionMode = false,
                            showDeleteSelectedDialog = false
                        )
                    }
                    recomputeFinishedAt()
                }
                else -> _state.update { it.copy(showDeleteSelectedDialog = false) }
            }
        }
    }

    fun onDeleteSelectedDismiss() {
        _state.update { it.copy(showDeleteSelectedDialog = false) }
    }

    // ── Tek görev silme (geçmiş değilse) ──
    fun onDeleteSingleItem(item: ListItem) {
        if (isPast(item)) return
        viewModelScope.launch {
            when (deleteListItemUseCase(item)) {
                is Resource.Success -> {
                    taskReminderScheduler.cancelReminder(item.id)
                    _state.update { s -> s.copy(items = s.items.filterNot { it.id == item.id }) }
                    recomputeFinishedAt()
                }
                else -> Unit
            }
        }
    }

    // ── Yeni görev ekleme ──
    fun onAddTaskClick() {
        _state.update { it.copy(isAddingTask = true, newTaskTitle = "", newTaskTime = null, newTaskQuantity = "") }
    }

    fun onNewTaskTitleChanged(title: String) {
        _state.update { it.copy(newTaskTitle = title) }
    }

    fun onNewTaskTimeSelected(time: Long) {
        _state.update { it.copy(newTaskTime = time) }
    }

    fun onNewTaskQuantityChanged(quantity: String) {
        _state.update { it.copy(newTaskQuantity = quantity) }
    }

    fun onAddTaskConfirm() {
        val s = _state.value
        val todoList = s.todoList ?: return
        val title = s.newTaskTitle.trim()
        if (title.isBlank()) {
            viewModelScope.launch { _uiEvent.send(UiEvent.ShowToast("Görev adı boş olamaz")) }
            return
        }

        // Saatli listede görev saati zorunlu
        if (todoList.isTimed && s.newTaskTime == null) {
            viewModelScope.launch { _uiEvent.send(UiEvent.ShowToast("Bu liste saatli, görevin saatini seçmelisiniz")) }
            return
        }

        val newItem = ListItem(
            listId = todoList.id,
            type = if (todoList.type.name == "SHOPPING") ListItemType.SHOPPING_ITEM else ListItemType.PERSONAL_TASK,
            title = title,
            reminderTime = if (todoList.isTimed) s.newTaskTime else null,
            quantity = s.newTaskQuantity.trim().ifBlank { null }
        )

        viewModelScope.launch {
            when (val result = insertListItemUseCase(newItem)) {
                is Resource.Success -> {
                    val insertedId = result.data ?: 0
                    val insertedItem = newItem.copy(id = insertedId)

                    _state.update { it.copy(isAddingTask = false, newTaskTitle = "", newTaskTime = null, newTaskQuantity = "") }
                    loadList()

                    rescheduleIfNeeded(insertedItem)
                    recomputeFinishedAt()
                }
                is Resource.Error -> _uiEvent.send(UiEvent.ShowToast(result.message ?: "Hata oluştu"))
                else -> Unit
            }
        }
    }

    fun onAddTaskDismiss() {
        _state.update { it.copy(isAddingTask = false) }
    }

    // ── Listeyi sil ──
    fun onDeleteListRequest() {
        _state.update { it.copy(showDeleteListDialog = true) }
    }

    fun onDeleteListConfirm() {
        val todoList = _state.value.todoList ?: return
        viewModelScope.launch {
            _state.value.items.forEach { taskReminderScheduler.cancelReminder(it.id) }
            deleteTodoListUseCase(todoList)
            _uiEvent.send(UiEvent.NavigateBack)
        }
    }

    fun onDeleteListDismiss() {
        _state.update { it.copy(showDeleteListDialog = false) }
    }

    // ── Yardımcılar ──

    private fun loadListSilently() {
        viewModelScope.launch {
            val result = getTodoListByIdUseCase(listId) ?: return@launch
            _state.update { it.copy(todoList = result.todoList) }
        }
    }

    private suspend fun rescheduleIfNeeded(item: ListItem) {
        val todoList = _state.value.todoList ?: return
        taskReminderScheduler.cancelReminder(item.id)
        if (todoList.isAlarmEnabled && !todoList.isMuted && !item.isCompleted) {
            item.reminderTime?.let { time ->
                if (time > System.currentTimeMillis()) {
                    taskReminderScheduler.scheduleReminder(
                        itemId = item.id,
                        listId = todoList.id,
                        taskTitle = item.title,
                        reminderTime = time
                    )
                }
            }
        }
    }

    private fun recomputeFinishedAt() {
        viewModelScope.launch {
            val todoList = _state.value.todoList ?: return@launch
            val items = _state.value.items
            val now = System.currentTimeMillis()

            val isFinished = items.isNotEmpty() && items.all {
                it.isCompleted || (it.reminderTime != null && it.reminderTime < now)
            }

            val currentFinishedAt = todoList.endTime
            when {
                isFinished && currentFinishedAt == null -> updateFinishedAtUseCase(todoList.id, now)
                !isFinished && currentFinishedAt != null -> updateFinishedAtUseCase(todoList.id, null)
            }
            loadListSilently()
        }
    }
}