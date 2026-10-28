package com.altankoc.nidado.ui.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.altankoc.nidado.domain.model.TodoListWithItems
import com.altankoc.nidado.domain.usecase.todolist.DeleteTodoListUseCase
import com.altankoc.nidado.domain.usecase.todolist.GetArchivedListsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArchiveViewModel @Inject constructor(
    private val getArchivedListsUseCase: GetArchivedListsUseCase,
    private val deleteTodoListUseCase: DeleteTodoListUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ArchiveUiState())
    val state: StateFlow<ArchiveUiState> = _state.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            // Repository zaten LIMIT 10 ile sınırlıyor (max 10 arşiv kuralı)
            getArchivedListsUseCase().collect { lists ->
                _state.update { it.copy(archivedLists = lists, isLoading = false) }
            }
        }
    }

    // Bir liste önizlemesi açma (içindeki görevleri göster)
    fun onItemClick(listWithItems: TodoListWithItems) {
        _state.update { it.copy(selectedListForPreview = listWithItems) }
    }

    fun onPreviewDismiss() {
        _state.update { it.copy(selectedListForPreview = null) }
    }

    // Silme isteği (swipe veya önizleme içindeki sil butonu)
    fun onDeleteRequest(listWithItems: TodoListWithItems) {
        _state.update { it.copy(listToDelete = listWithItems, selectedListForPreview = null) }
    }

    fun onDeleteConfirm() {
        val list = _state.value.listToDelete ?: return
        viewModelScope.launch {
            deleteTodoListUseCase(list.todoList)
            _state.update { it.copy(listToDelete = null) }
        }
    }

    fun onDeleteDismiss() {
        _state.update { it.copy(listToDelete = null) }
    }
}