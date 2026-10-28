package com.altankoc.nidado

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.altankoc.nidado.domain.model.User
import com.altankoc.nidado.domain.usecase.user.GetUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    getUserUseCase: GetUserUseCase
) : ViewModel() {

    val user: StateFlow<User?> = getUserUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
}