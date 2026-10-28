package com.altankoc.nidado.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.altankoc.nidado.domain.model.User
import com.altankoc.nidado.domain.usecase.user.GetUserOnceUseCase
import com.altankoc.nidado.domain.usecase.user.InsertUserUseCase
import com.altankoc.nidado.domain.usecase.user.SetFirstLaunchCompletedUseCase
import com.altankoc.nidado.util.Resource
import com.altankoc.nidado.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val getUserOnceUseCase: GetUserOnceUseCase,
    private val insertUserUseCase: InsertUserUseCase,
    private val setFirstLaunchCompletedUseCase: SetFirstLaunchCompletedUseCase
) : ViewModel() {

    private val _state = MutableStateFlow<SplashState>(SplashState.Background)
    val state: StateFlow<SplashState> = _state.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    private val _nameInput = MutableStateFlow("")
    val nameInput: StateFlow<String> = _nameInput.asStateFlow()

    private val _nameError = MutableStateFlow<String?>(null)
    val nameError: StateFlow<String?> = _nameError.asStateFlow()

    init {
        viewModelScope.launch {
            delay(1500)                              // arka plan
            val user = getUserOnceUseCase()
            if (user != null) {
                _state.value = SplashState.Greeting(user.name)
                delay(3000)
                _state.value = SplashState.Background
                delay(800)
                _uiEvent.send(UiEvent.Navigate("home"))
            } else {
                _state.value = SplashState.WelcomeIntro
                delay(3000)
                _state.value = SplashState.Background  // nefes
                delay(1500)
                _state.value = SplashState.NamePrompt
                delay(2500)
                _state.value = SplashState.Background  // nefes
                delay(1500)
                _state.value = SplashState.NameInput
            }
        }
    }

    fun onNameChanged(name: String) {
        _nameInput.value = name
        _nameError.value = null
    }

    fun onNameConfirmed() {
        val name = _nameInput.value.trim()
        when {
            name.isBlank() -> _nameError.value = "İsim boş olamaz"
            name.contains(" ") -> _nameError.value = "İsim tek kelime olmalı"
            !name.all { it.isLetter() } -> _nameError.value = "İsim sadece harf içermeli"
            else -> {
                viewModelScope.launch {
                    _state.value = SplashState.Loading
                    val result = insertUserUseCase(
                        User(name = name, avatarId = 0, isFirstLaunch = true)
                    )
                    when (result) {
                        is Resource.Success -> {
                            setFirstLaunchCompletedUseCase()
                            delay(1500)
                            _state.value = SplashState.Background  // nefes
                            delay(800)
                            _state.value = SplashState.Welcome(name)
                            delay(2500)
                            _state.value = SplashState.Background  // nefes
                            delay(800)
                            _uiEvent.send(UiEvent.Navigate("home"))
                        }
                        is Resource.Error -> {
                            _state.value = SplashState.NameInput
                            _nameError.value = result.message
                        }
                        else -> Unit
                    }
                }
            }
        }
    }
}