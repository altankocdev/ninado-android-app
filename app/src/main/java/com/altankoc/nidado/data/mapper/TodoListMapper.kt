package com.altankoc.nidado.data.mapper

import com.altankoc.nidado.data.entity.TodoListEntity
import com.altankoc.nidado.data.entity.TodoListWithItemsEntity
import com.altankoc.nidado.domain.model.enums.ListType
import com.altankoc.nidado.domain.model.TodoList
import com.altankoc.nidado.domain.model.TodoListWithItems

fun TodoListEntity.toTodoList(): TodoList {
    return TodoList(
        id = id,
        title = title,
        type = ListType.valueOf(type),
        isTimed = isTimed,
        scheduledDate = scheduledDate,
        startTime = startTime,
        endTime = endTime,
        isCompleted = isCompleted,
        isExpired = isExpired,
        isArchived = isArchived,
        isMuted = isMuted,
        createdAt = createdAt,
        isAlarmEnabled = isAlarmEnabled,
    )
}

fun TodoList.toTodoListEntity(): TodoListEntity {
    return TodoListEntity(
        id = id,
        title = title,
        type = type.name,
        isTimed = isTimed,
        scheduledDate = scheduledDate,
        startTime = startTime,
        endTime = endTime,
        isCompleted = isCompleted,
        isExpired = isExpired,
        isArchived = isArchived,
        isMuted = isMuted,
        createdAt = createdAt,
        isAlarmEnabled = isAlarmEnabled,
    )
}

fun TodoListWithItemsEntity.toTodoListWithItems(): TodoListWithItems {
    return TodoListWithItems(
        todoList = todoList.toTodoList(),
        items = items.map { it.toListItem() }
    )
}