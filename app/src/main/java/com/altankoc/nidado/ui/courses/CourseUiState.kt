package com.altankoc.nidado.ui.courses

import com.altankoc.nidado.domain.model.Course

data class CourseUiState(
    val courses: List<Course> = emptyList(),
    val filteredCourses: List<Course> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val courseToDelete: Course? = null,
    val isSelectionMode: Boolean = false,
    val selectedCourseIds: Set<Int> = emptySet(),
    val coursesToDelete: List<Course>? = null
)