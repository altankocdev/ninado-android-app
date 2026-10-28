package com.altankoc.nidado.data.mapper

import com.altankoc.nidado.data.entity.ListItemEntity
import com.altankoc.nidado.domain.model.ListItem
import com.altankoc.nidado.domain.model.enums.ListItemType

fun ListItemEntity.toListItem(): ListItem {
    return ListItem(
        id = id,
        listId = listId,
        type = ListItemType.valueOf(type),
        title = title,
        isCompleted = isCompleted,
        courseId = courseId,
        courseName = courseName,
        courseDifficulty = courseDifficulty,
        quantity = quantity,
        reminderTime = reminderTime
    )
}

fun ListItem.toListItemEntity(): ListItemEntity {
    return ListItemEntity(
        id = id,
        listId = listId,
        type = type.name,
        title = title,
        isCompleted = isCompleted,
        courseId = courseId,
        courseName = courseName,
        courseDifficulty = courseDifficulty,
        quantity = quantity,
        reminderTime = reminderTime
    )
}