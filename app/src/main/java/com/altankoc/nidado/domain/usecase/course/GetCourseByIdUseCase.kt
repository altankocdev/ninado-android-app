package com.altankoc.nidado.domain.usecase.course

import com.altankoc.nidado.domain.model.Course
import com.altankoc.nidado.domain.repository.CourseRepository
import javax.inject.Inject

class GetCourseByIdUseCase @Inject constructor(
    private val repository: CourseRepository
) {
    suspend operator fun invoke(id: Int): Course? {
        return repository.getCourseById(id)
    }
}