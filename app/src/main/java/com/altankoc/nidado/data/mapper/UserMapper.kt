package com.altankoc.nidado.data.mapper

import com.altankoc.nidado.data.entity.UserEntity
import com.altankoc.nidado.domain.model.User
import com.altankoc.nidado.domain.model.enums.ThemePreference

fun UserEntity.toUser(): User {
    return User(
        name = name,
        avatarId = avatarId,
        isFirstLaunch = isFirstLaunch,
        themePreference = ThemePreference.valueOf(themePreference),
        notificationsEnabled = notificationsEnabled,
        notificationSoundId = notificationSoundId,
        totalListsCreated = totalListsCreated,
        lockedCompletedTasks = lockedCompletedTasks
    )
}

fun User.toUserEntity(): UserEntity {
    return UserEntity(
        id = 1,
        name = name,
        avatarId = avatarId,
        isFirstLaunch = isFirstLaunch,
        themePreference = themePreference.name,
        notificationsEnabled = notificationsEnabled,
        notificationSoundId = notificationSoundId,
        totalListsCreated = totalListsCreated,
        lockedCompletedTasks = lockedCompletedTasks
    )
}