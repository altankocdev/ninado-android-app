package com.altankoc.nidado.data.entity

import androidx.room.Embedded
import androidx.room.Relation

data class TodoListWithItemsEntity(
    @Embedded val todoList: TodoListEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "listId"
    )
    val items: List<ListItemEntity>
)