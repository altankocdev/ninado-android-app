package com.altankoc.nidado.ui.archive

import com.altankoc.nidado.domain.model.TodoListWithItems

data class ArchiveUiState(
    val archivedLists: List<TodoListWithItems> = emptyList(),
    val isLoading: Boolean = true,
    val selectedListForPreview: TodoListWithItems? = null,
    val listToDelete: TodoListWithItems? = null
)