package com.altankoc.nidado.di

import android.content.Context
import androidx.room.Room
import com.altankoc.nidado.data.dao.CourseDao
import com.altankoc.nidado.data.dao.ListItemDao
import com.altankoc.nidado.data.dao.TodoListDao
import com.altankoc.nidado.data.dao.UserDao
import com.altankoc.nidado.data.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "nidado_database"
        ).build()
    }

    @Provides
    @Singleton
    fun provideCourseDao(db: AppDatabase): CourseDao = db.courseDao()

    @Provides
    @Singleton
    fun provideTodoListDao(db: AppDatabase): TodoListDao = db.todoListDao()

    @Provides
    @Singleton
    fun provideListItemDao(db: AppDatabase): ListItemDao = db.listItemDao()

    @Provides
    @Singleton
    fun provideUserDao(db: AppDatabase): UserDao = db.userDao()
}