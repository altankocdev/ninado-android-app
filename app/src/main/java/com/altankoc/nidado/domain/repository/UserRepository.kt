package com.altankoc.nidado.domain.repository

import com.altankoc.nidado.domain.model.User
import com.altankoc.nidado.domain.model.enums.ThemePreference
import com.altankoc.nidado.util.Resource
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getUser(): Flow<User?>
    suspend fun getUserOnce(): User?
    suspend fun insertUser(user: User): Resource<Unit>
    suspend fun updateName(name: String): Resource<Unit>
    suspend fun updateAvatar(avatarId: Int): Resource<Unit>
    suspend fun updateTheme(theme: ThemePreference): Resource<Unit>
    suspend fun updateNotifications(enabled: Boolean): Resource<Unit>
    suspend fun updateNotificationSound(soundId: String): Resource<Unit>
    suspend fun setFirstLaunchCompleted(): Resource<Unit>
}