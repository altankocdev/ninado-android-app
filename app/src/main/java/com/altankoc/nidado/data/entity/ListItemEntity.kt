package com.altankoc.nidado.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "list_items",
    foreignKeys = [
        ForeignKey(
            entity = TodoListEntity::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["listId"])]
)
data class ListItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val listId: Int,
    val type: String,             // ListItemType enum → string
    val title: String,
    val isCompleted: Boolean = false,

    // Sadece COURSE_TASK için
    val courseId: Int? = null,
    val courseName: String? = null,
    val courseDifficulty: Int? = null,

    // Sadece SHOPPING_ITEM için
    val quantity: String? = null,

    // Opsiyonel hatırlatıcı
    val reminderTime: Long? = null
)