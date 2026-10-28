package com.altankoc.nidado.data.mapper

import com.altankoc.nidado.data.entity.CourseEntity
import com.altankoc.nidado.domain.model.Course
import com.altankoc.nidado.domain.model.enums.CourseYear

fun CourseEntity.toCourse(): Course {
    return Course(
        id = id,
        name = name,
        year = CourseYear.valueOf(year),
        difficulty = difficulty,
        createdAt = createdAt
    )
}

fun Course.toCourseEntity(): CourseEntity {
    return CourseEntity(
        id = id,
        name = name,
        year = year.name,
        difficulty = difficulty,
        createdAt = createdAt
    )
}