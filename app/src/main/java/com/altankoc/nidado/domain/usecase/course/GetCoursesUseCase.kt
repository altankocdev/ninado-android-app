package com.altankoc.nidado.domain.usecase.course

import com.altankoc.nidado.domain.model.Course
import com.altankoc.nidado.domain.repository.CourseRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCoursesUseCase @Inject constructor(
    private val repository: CourseRepository
) {
    operator fun invoke(ascending: Boolean = false): Flow<List<Course>> {
        return if (ascending) {
            repository.getAllCoursesAsc()
        } else {
            repository.getAllCoursesDesc()
        }
    }
}