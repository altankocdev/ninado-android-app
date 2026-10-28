package com.altankoc.nidado.domain.model

import com.altankoc.nidado.domain.model.enums.ListType

data class TodoList(
    val id: Int = 0,
    val title: String,
    val type: ListType,
    val isTimed: Boolean = false,
    val isAlarmEnabled: Boolean = false,  // ← kullanıcı alarm seçti mi
    val isMuted: Boolean = false,          // ← alarm sessiz mi
    val scheduledDate: Long? = null,
    val startTime: Long? = null,
    val endTime: Long? = null,
    val isCompleted: Boolean = false,
    val isExpired: Boolean = false,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)