package com.altankoc.nidado.ui.courses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.altankoc.nidado.domain.model.Course
import com.altankoc.nidado.domain.usecase.course.DeleteCourseUseCase
import com.altankoc.nidado.domain.usecase.course.GetCoursesUseCase
import com.altankoc.nidado.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CourseViewModel @Inject constructor(
    private val getCoursesUseCase: GetCoursesUseCase,
    private val deleteCourseUseCase: DeleteCourseUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(CourseUiState())
    val state: StateFlow<CourseUiState> = _state.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        loadCourses()
    }

    private fun loadCourses() {
        viewModelScope.launch {
            getCoursesUseCase(ascending = true).collect { courses ->
                val sorted = courses.sortedBy { it.name }
                _state.update {
                    it.copy(
                        courses = sorted,
                        filteredCourses = applySearch(sorted, it.searchQuery),
                        isLoading = false
                    )
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _state.update {
            it.copy(
                searchQuery = query,
                filteredCourses = applySearch(it.courses, query)
            )
        }
    }

    private fun applySearch(courses: List<Course>, query: String): List<Course> {
        return if (query.isBlank()) courses
        else courses.filter {
            it.name.contains(query, ignoreCase = true)
        }
    }

    fun onDeleteCourse(course: Course) {
        viewModelScope.launch {
            deleteCourseUseCase(course)
            _state.update { it.copy(courseToDelete = null) }
        }
    }

    fun onDeleteCourses(courses: List<Course>) {
        viewModelScope.launch {
            courses.forEach { deleteCourseUseCase(it) }
            _state.update { 
                it.copy(
                    coursesToDelete = null,
                    isSelectionMode = false,
                    selectedCourseIds = emptySet()
                ) 
            }
        }
    }

    fun toggleSelectionMode() {
        _state.update { 
            it.copy(
                isSelectionMode = !it.isSelectionMode,
                selectedCourseIds = emptySet()
            ) 
        }
    }

    fun toggleCourseSelection(courseId: Int) {
        _state.update { state ->
            val newSelection = if (state.selectedCourseIds.contains(courseId)) {
                state.selectedCourseIds - courseId
            } else {
                state.selectedCourseIds + courseId
            }
            state.copy(selectedCourseIds = newSelection)
        }
    }

    fun selectAll() {
        _state.update { state ->
            val allIds = state.filteredCourses.map { it.id }.toSet()
            state.copy(selectedCourseIds = allIds)
        }
    }

    fun onDeleteSelectedRequest() {
        _state.update { state ->
            val selectedCourses = state.courses.filter { state.selectedCourseIds.contains(it.id) }
            state.copy(coursesToDelete = selectedCourses)
        }
    }

    fun onDeleteConfirmRequest(course: Course) {
        _state.update { it.copy(courseToDelete = course) }
    }

    fun onDeleteDismiss() {
        _state.update { it.copy(courseToDelete = null, coursesToDelete = null) }
    }

    fun navigateToEdit(courseId: Int) {
        viewModelScope.launch {
            _uiEvent.send(UiEvent.Navigate("edit_course/$courseId"))
        }
    }

    fun navigateToCreate() {
        viewModelScope.launch {
            _uiEvent.send(UiEvent.Navigate("create_course"))
        }
    }
}