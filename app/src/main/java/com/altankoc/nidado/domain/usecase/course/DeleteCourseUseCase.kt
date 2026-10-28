package com.altankoc.nidado.domain.usecase.course

import com.altankoc.nidado.domain.model.Course
import com.altankoc.nidado.domain.repository.CourseRepository
import com.altankoc.nidado.util.Resource
import javax.inject.Inject

class DeleteCourseUseCase @Inject constructor(
    private val repository: CourseRepository
) {
    suspend operator fun invoke(course: Course): Resource<Unit> {
        return repository.deleteCourse(course)
    }
}