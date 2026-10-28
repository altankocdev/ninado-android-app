package com.altankoc.nidado.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "todo_lists")
data class TodoListEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val type: String,
    val isTimed: Boolean = false,
    val scheduledDate: Long? = null,
    val startTime: Long? = null,
    val endTime: Long? = null,
    val isCompleted: Boolean = false,
    val isExpired: Boolean = false,
    val isArchived: Boolean = false,
    val isMuted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val isAlarmEnabled: Boolean = false,

)