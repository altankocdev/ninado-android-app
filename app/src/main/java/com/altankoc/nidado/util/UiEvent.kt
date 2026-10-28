package com.altankoc.nidado.util

sealed class UiEvent {
    data object NavigateBack : UiEvent()
    data object TaskAdded : UiEvent()
    data class ShowSnackbar(val message: String) : UiEvent()
    data class ShowToast(val message: String) : UiEvent()
    data class Navigate(val route: String) : UiEvent()
}