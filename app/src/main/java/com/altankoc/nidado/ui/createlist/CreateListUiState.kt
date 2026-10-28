package com.altankoc.nidado.ui.createlist

import com.altankoc.nidado.domain.model.Course
import com.altankoc.nidado.domain.model.enums.ListType

data class CreateListUiState(
    val selectedType: ListType = ListType.DAILY_PLAN,
    val title: String = "",
    val titleError: String? = null,
    val isTimed: Boolean = false,
    val scheduledDate: Long? = null,
    val dateError: String? = null,
    val startTime: Long? = null,

    // Görevler
    val taskInputs: List<TaskInput> = listOf(TaskInput()),

    // Eğitim modu için
    val availableCourses: List<Course> = emptyList(),

    // Alışveriş modu için
    val shoppingItems: List<ShoppingInput> = listOf(ShoppingInput()),

    val isLoading: Boolean = false,
    val isAlarmEnabled: Boolean = false
)

data class TaskInput(
    val id: Long = java.util.UUID.randomUUID().mostSignificantBits,
    val title: String = "",
    val courseId: Int? = null,
    val courseName: String? = null,
    val courseDifficulty: Float? = null,
    val reminderTime: Long? = null,
    val titleError: String? = null
)

data class ShoppingInput(
    val id: Long = java.util.UUID.randomUUID().mostSignificantBits,
    val title: String = "",
    val quantity: String = "",
    val titleError: String? = null
)