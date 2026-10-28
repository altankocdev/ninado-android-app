package com.altankoc.nidado.ui.courses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.altankoc.nidado.domain.model.Course
import com.altankoc.nidado.domain.model.enums.CourseYear
import com.altankoc.nidado.domain.usecase.course.DeleteCourseUseCase
import com.altankoc.nidado.domain.usecase.course.GetCourseByIdUseCase
import com.altankoc.nidado.domain.usecase.course.InsertCourseUseCase
import com.altankoc.nidado.domain.usecase.course.UpdateCourseUseCase
import com.altankoc.nidado.util.Resource
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

data class CreateEditCourseUiState(
    val name: String = "",
    val difficulty: Float = 2.5f,
    val year: CourseYear = CourseYear.FIRST,
    val nameError: String? = null,
    val showDeleteDialog: Boolean = false,
    val isLoading: Boolean = false
)

@HiltViewModel
class CreateEditCourseViewModel @Inject constructor(
    private val getCourseByIdUseCase: GetCourseByIdUseCase,
    private val insertCourseUseCase: InsertCourseUseCase,
    private val updateCourseUseCase: UpdateCourseUseCase,
    private val deleteCourseUseCase: DeleteCourseUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(CreateEditCourseUiState())
    val state: StateFlow<CreateEditCourseUiState> = _state.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    private var currentCourse: Course? = null

    fun loadCourse(id: Int) {
        viewModelScope.launch {
            val course = getCourseByIdUseCase(id)
            course?.let {
                currentCourse = it
                _state.update { state ->
                    state.copy(
                        name = it.name,
                        difficulty = it.difficulty.toFloat(),
                        year = it.year
                    )
                }
            }
        }
    }

    fun onNameChanged(name: String) {
        _state.update { it.copy(name = name, nameError = null) }
    }

    fun onDifficultyChanged(difficulty: Float) {
        _state.update { it.copy(difficulty = difficulty) }
    }

    fun onYearChanged(year: CourseYear) {
        _state.update { it.copy(year = year) }
    }

    fun onSave() {
        val name = _state.value.name.trim()
        if (name.isBlank()) {
            _state.update { it.copy(nameError = "Ders adı boş olamaz") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val course = Course(
                id = currentCourse?.id ?: 0,
                name = name,
                difficulty = _state.value.difficulty,
                year = _state.value.year
            )

            val result = if (currentCourse != null) {
                updateCourseUseCase(course)
            } else {
                insertCourseUseCase(course)
            }

            when (result) {
                is Resource.Success -> _uiEvent.send(UiEvent.NavigateBack)
                is Resource.Error -> _state.update {
                    it.copy(nameError = result.message, isLoading = false)
                }
                else -> Unit
            }
        }
    }

    fun onDeleteRequest() {
        _state.update { it.copy(showDeleteDialog = true) }
    }

    fun onDeleteConfirm() {
        viewModelScope.launch {
            currentCourse?.let { deleteCourseUseCase(it) }
            _uiEvent.send(UiEvent.NavigateBack)
        }
    }

    fun onDeleteDismiss() {
        _state.update { it.copy(showDeleteDialog = false) }
    }
}