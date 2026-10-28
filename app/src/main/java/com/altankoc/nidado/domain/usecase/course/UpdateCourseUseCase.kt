package com.altankoc.nidado.domain.usecase.course

import com.altankoc.nidado.domain.model.Course
import com.altankoc.nidado.domain.repository.CourseRepository
import com.altankoc.nidado.util.Resource
import javax.inject.Inject

class UpdateCourseUseCase @Inject constructor(
    private val repository: CourseRepository
) {
    suspend operator fun invoke(course: Course): Resource<Unit> {
        if (course.name.isBlank()) return Resource.Error("Ders adı boş olamaz")
        if (course.difficulty !in 1.0f..5.0f) return Resource.Error("Zorluk 1-5 arasında olmalı")
        return repository.updateCourse(course)
    }
}