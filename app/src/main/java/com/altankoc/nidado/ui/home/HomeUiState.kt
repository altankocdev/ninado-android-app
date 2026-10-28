package com.altankoc.nidado.ui.home

import com.altankoc.nidado.domain.model.TodoListWithItems
import com.altankoc.nidado.domain.model.User
import com.altankoc.nidado.domain.model.enums.ListType

data class HomeUiState(
    val user: User? = null,
    val activeLists: List<TodoListWithItems> = emptyList(),
    val filteredLists: List<TodoListWithItems> = emptyList(),
    val selectedFilter: ListType? = null,
    val isLoading: Boolean = true
)