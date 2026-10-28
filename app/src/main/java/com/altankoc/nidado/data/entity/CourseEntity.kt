package com.altankoc.nidado.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val year: String,
    val difficulty: Float,
    val createdAt: Long = System.currentTimeMillis()
)