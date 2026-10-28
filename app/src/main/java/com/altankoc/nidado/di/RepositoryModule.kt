package com.altankoc.nidado.di

import com.altankoc.nidado.data.repository.CourseRepositoryImpl
import com.altankoc.nidado.data.repository.TodoListRepositoryImpl
import com.altankoc.nidado.data.repository.UserRepositoryImpl
import com.altankoc.nidado.domain.repository.CourseRepository
import com.altankoc.nidado.domain.repository.TodoListRepository
import com.altankoc.nidado.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindCourseRepository(
        courseRepositoryImpl: CourseRepositoryImpl
    ): CourseRepository

    @Binds
    @Singleton
    abstract fun bindTodoListRepository(
        todoListRepositoryImpl: TodoListRepositoryImpl
    ): TodoListRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        userRepositoryImpl: UserRepositoryImpl
    ): UserRepository
}