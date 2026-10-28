package com.altankoc.nidado.domain.model

import com.altankoc.nidado.domain.model.enums.ThemePreference

data class User(
    val name: String,
    val avatarId: Int = 1,
    val isFirstLaunch: Boolean = true,
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
    val notificationsEnabled: Boolean = true,
    val notificationSoundId: String = "bird",
    val totalListsCreated: Int = 0,
    val lockedCompletedTasks: Int = 0
)