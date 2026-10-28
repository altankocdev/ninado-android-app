package com.altankoc.nidado.ui.profile

import com.altankoc.nidado.domain.model.TodoListWithItems
import com.altankoc.nidado.domain.model.User
import com.altankoc.nidado.domain.model.enums.ThemePreference

data class ProfileUiState(
    val user: User? = null,
    val archivedLists: List<TodoListWithItems> = emptyList(),
    val totalCompletedTasks: Int = 0,
    val totalListsCreated: Int = 0,
    val isLoading: Boolean = false,
    val isEditingName: Boolean = false,
    val nameInput: String = "",
    val nameError: String? = null,
    val isAvatarPickerVisible: Boolean = false,
    val notificationPermissionGranted: Boolean = false
)