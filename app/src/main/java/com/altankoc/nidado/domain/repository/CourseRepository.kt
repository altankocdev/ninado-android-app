package com.altankoc.nidado.domain.repository

import com.altankoc.nidado.domain.model.Course
import com.altankoc.nidado.util.Resource
import kotlinx.coroutines.flow.Flow

interface CourseRepository {
    fun getAllCoursesDesc(): Flow<List<Course>>
    fun getAllCoursesAsc(): Flow<List<Course>>
    suspend fun getCourseById(id: Int): Course?
    suspend fun insertCourse(course: Course): Resource<Unit>
    suspend fun updateCourse(course: Course): Resource<Unit>
    suspend fun deleteCourse(course: Course): Resource<Unit>
}