package com.altankoc.nidado.data.repository

import com.altankoc.nidado.data.dao.UserDao
import com.altankoc.nidado.data.mapper.toUser
import com.altankoc.nidado.data.mapper.toUserEntity
import com.altankoc.nidado.domain.model.User
import com.altankoc.nidado.domain.model.enums.ThemePreference
import com.altankoc.nidado.domain.repository.UserRepository
import com.altankoc.nidado.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao
) : UserRepository {

    override fun getUser(): Flow<User?> {
        return userDao.getUser().map { it?.toUser() }
    }

    override suspend fun getUserOnce(): User? {
        return userDao.getUserOnce()?.toUser()
    }

    override suspend fun insertUser(user: User): Resource<Unit> {
        return try {
            userDao.insertUser(user.toUserEntity())
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Kullanıcı oluşturulurken hata oluştu")
        }
    }

    override suspend fun updateName(name: String): Resource<Unit> {
        return try {
            userDao.updateName(name)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "İsim güncellenirken hata oluştu")
        }
    }

    override suspend fun updateAvatar(avatarId: Int): Resource<Unit> {
        return try {
            userDao.updateAvatar(avatarId)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Avatar güncellenirken hata oluştu")
        }
    }

    override suspend fun updateTheme(theme: ThemePreference): Resource<Unit> {
        return try {
            userDao.updateTheme(theme.name)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Tema güncellenirken hata oluştu")
        }
    }

    override suspend fun updateNotifications(enabled: Boolean): Resource<Unit> {
        return try {
            userDao.updateNotifications(enabled)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Bildirim ayarı güncellenirken hata oluştu")
        }
    }

    override suspend fun updateNotificationSound(soundId: String): Resource<Unit> {
        return try {
            userDao.updateNotificationSound(soundId)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Bildirim sesi güncellenirken hata oluştu")
        }
    }

    override suspend fun setFirstLaunchCompleted(): Resource<Unit> {
        return try {
            userDao.setFirstLaunchCompleted()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Hata oluştu")
        }
    }
}