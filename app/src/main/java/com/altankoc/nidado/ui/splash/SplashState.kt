package com.altankoc.nidado.ui.splash

sealed class SplashState {
    data object Background : SplashState()
    data object WelcomeIntro : SplashState()
    data object NamePrompt : SplashState()
    data object NameInput : SplashState()
    data object Loading : SplashState()
    data class Greeting(val name: String) : SplashState()
    data class Welcome(val name: String) : SplashState()
}