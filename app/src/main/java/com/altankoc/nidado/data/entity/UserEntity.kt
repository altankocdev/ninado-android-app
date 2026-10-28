package com.altankoc.nidado.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user")
data class UserEntity(
    @PrimaryKey
    val id: Int = 1,
    val name: String,
    val avatarId: Int = 1,
    val isFirstLaunch: Boolean = true,
    val themePreference: String = "SYSTEM",
    val notificationsEnabled: Boolean = true,
    val notificationSoundId: String = "bird",
    val totalListsCreated: Int = 0,
    val lockedCompletedTasks: Int = 0
)