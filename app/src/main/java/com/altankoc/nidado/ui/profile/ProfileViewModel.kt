package com.altankoc.nidado.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.altankoc.nidado.domain.model.TodoList
import com.altankoc.nidado.domain.model.enums.ThemePreference
import com.altankoc.nidado.domain.usecase.todolist.DeleteTodoListUseCase
import com.altankoc.nidado.domain.usecase.todolist.GetArchivedListsUseCase
import com.altankoc.nidado.domain.usecase.todolist.GetTotalCompletedTasksUseCase
import com.altankoc.nidado.domain.usecase.user.GetUserUseCase
import com.altankoc.nidado.domain.usecase.user.UpdateAvatarUseCase
import com.altankoc.nidado.domain.usecase.user.UpdateNameUseCase
import com.altankoc.nidado.domain.usecase.user.UpdateNotificationSoundUseCase
import com.altankoc.nidado.domain.usecase.user.UpdateNotificationsUseCase
import com.altankoc.nidado.domain.usecase.user.UpdateThemeUseCase
import com.altankoc.nidado.util.Resource
import com.altankoc.nidado.util.UiEvent
import com.altankoc.nidado.util.permission.PermissionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getUserUseCase: GetUserUseCase,
    private val getArchivedListsUseCase: GetArchivedListsUseCase,
    private val getTotalCompletedTasksUseCase: GetTotalCompletedTasksUseCase,
    private val deleteTodoListUseCase: DeleteTodoListUseCase,
    private val updateNameUseCase: UpdateNameUseCase,
    private val updateAvatarUseCase: UpdateAvatarUseCase,
    private val updateThemeUseCase: UpdateThemeUseCase,
    private val updateNotificationsUseCase: UpdateNotificationsUseCase,
    private val updateNotificationSoundUseCase: UpdateNotificationSoundUseCase,
    private val permissionManager: PermissionManager
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        loadData()
        checkNotificationPermission()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                getUserUseCase(),
                getArchivedListsUseCase(),
                getTotalCompletedTasksUseCase()
            ) { user, archivedLists, totalCompletedTasks ->
                _state.update {
                    it.copy(
                        user = user,
                        archivedLists = archivedLists,
                        totalCompletedTasks = totalCompletedTasks,
                        totalListsCreated = user?.totalListsCreated ?: 0,
                        isLoading = false,
                        notificationPermissionGranted = permissionManager.isNotificationGranted()
                                && (user?.notificationsEnabled ?: true)
                    )
                }
            }.collect {}
        }
    }

    fun checkNotificationPermission() {
        viewModelScope.launch {
            val systemGranted = permissionManager.isNotificationGranted()
            val userPref = _state.value.user?.notificationsEnabled ?: true
            _state.update {
                it.copy(notificationPermissionGranted = systemGranted && userPref)
            }
        }
    }

    // Avatar
    fun onAvatarClick() = _state.update { it.copy(isAvatarPickerVisible = true) }

    fun onAvatarSelected(avatarId: Int) {
        viewModelScope.launch {
            updateAvatarUseCase(avatarId)
            _state.update { it.copy(isAvatarPickerVisible = false) }
        }
    }

    fun onAvatarPickerDismiss() = _state.update { it.copy(isAvatarPickerVisible = false) }

    // İsim
    fun onEditNameClick() = _state.update {
        it.copy(isEditingName = true, nameInput = it.user?.name ?: "", nameError = null)
    }

    fun onNameInputChanged(name: String) = _state.update { it.copy(nameInput = name, nameError = null) }

    fun onNameSave() {
        val name = _state.value.nameInput.trim()
        when {
            name.isBlank() -> _state.update { it.copy(nameError = "İsim boş olamaz") }
            name.contains(" ") -> _state.update { it.copy(nameError = "İsim tek kelime olmalı") }
            !name.all { it.isLetter() } -> _state.update { it.copy(nameError = "İsim sadece harf içermeli") }
            else -> viewModelScope.launch {
                when (val result = updateNameUseCase(name)) {
                    is Resource.Success -> _state.update { it.copy(isEditingName = false, nameError = null) }
                    is Resource.Error -> _state.update { it.copy(nameError = result.message) }
                    else -> Unit
                }
            }
        }
    }

    fun onNameEditDismiss() = _state.update { it.copy(isEditingName = false, nameError = null) }

    // Geçmiş silme
    fun onDeleteArchivedList(todoList: TodoList) {
        viewModelScope.launch { deleteTodoListUseCase(todoList) }
    }

    // Tema
    fun onThemeChanged(theme: ThemePreference) {
        viewModelScope.launch { updateThemeUseCase(theme) }
    }

    // Bildirimler
    fun onNotificationToggle(enabled: Boolean) {
        viewModelScope.launch {
            updateNotificationsUseCase(enabled)
            _state.update { it.copy(notificationPermissionGranted = enabled) }
        }
    }

    // Bildirim sesi seçimi
    fun onSoundSelected(soundId: String) {
        viewModelScope.launch {
            updateNotificationSoundUseCase(soundId)
        }
    }
}