package com.altankoc.nidado.domain.model

data class TodoListWithItems(
    val todoList: TodoList,
    val items: List<ListItem> = emptyList()
)