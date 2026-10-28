package com.altankoc.nidado.domain.model

import com.altankoc.nidado.domain.model.enums.ListItemType

data class ListItem(
    val id: Int = 0,
    val listId: Int,
    val type: ListItemType,
    val title: String,
    val isCompleted: Boolean = false,

    // Sadece COURSE_TASK için
    val courseId: Int? = null,
    val courseName: String? = null,       // snapshot — ders silinse bile kalır
    val courseDifficulty: Int? = null,    // snapshot

    // Sadece SHOPPING_ITEM için
    val quantity: String? = null,

    // Hatırlatıcı — tüm tipler için opsiyonel
    val reminderTime: Long? = null
)