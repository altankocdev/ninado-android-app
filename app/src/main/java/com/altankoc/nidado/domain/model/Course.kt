package com.altankoc.nidado.domain.model

import com.altankoc.nidado.domain.model.enums.CourseYear

data class Course(
    val id: Int = 0,
    val name: String,
    val year: CourseYear,
    val difficulty: Float,
    val createdAt: Long = System.currentTimeMillis()
)