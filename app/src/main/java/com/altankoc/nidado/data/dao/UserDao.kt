package com.altankoc.nidado.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.altankoc.nidado.data.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("SELECT * FROM user WHERE id = 1")
    fun getUser(): Flow<UserEntity?>

    @Query("SELECT * FROM user WHERE id = 1")
    suspend fun getUserOnce(): UserEntity?

    @Query("UPDATE user SET isFirstLaunch = 0 WHERE id = 1")
    suspend fun setFirstLaunchCompleted()

    @Query("UPDATE user SET name = :name WHERE id = 1")
    suspend fun updateName(name: String)

    @Query("UPDATE user SET avatarId = :avatarId WHERE id = 1")
    suspend fun updateAvatar(avatarId: Int)

    @Query("UPDATE user SET themePreference = :theme WHERE id = 1")
    suspend fun updateTheme(theme: String)

    @Query("UPDATE user SET notificationsEnabled = :enabled WHERE id = 1")
    suspend fun updateNotifications(enabled: Boolean)

    @Query("UPDATE user SET notificationSoundId = :soundId WHERE id = 1")
    suspend fun updateNotificationSound(soundId: String)

    // Kalıcı sayaçlar — silinse/arşivlense de geri alınmaz
    @Query("UPDATE user SET totalListsCreated = totalListsCreated + 1 WHERE id = 1")
    suspend fun incrementTotalListsCreated()

    @Query("UPDATE user SET lockedCompletedTasks = lockedCompletedTasks + :count WHERE id = 1")
    suspend fun addLockedCompletedTasks(count: Int)
}