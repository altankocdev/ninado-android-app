package com.altankoc.nidado.widget

import com.altankoc.nidado.domain.usecase.todolist.GetActiveListsForHomeUseCase
import com.altankoc.nidado.domain.usecase.todolist.GetCalendarListsUseCase
import com.altankoc.nidado.domain.usecase.todolist.GetTodoListByIdUseCase
import com.altankoc.nidado.domain.usecase.todolist.ToggleItemCompletedUseCase
import com.altankoc.nidado.domain.usecase.todolist.UpdateTodoListUseCase
import com.altankoc.nidado.domain.usecase.user.GetUserUseCase
import com.altankoc.nidado.util.notification.TaskReminderScheduler
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun getActiveListsForHomeUseCase(): GetActiveListsForHomeUseCase
    fun getCalendarListsUseCase(): GetCalendarListsUseCase
    fun getTodoListByIdUseCase(): GetTodoListByIdUseCase
    fun getUserUseCase(): GetUserUseCase
    fun toggleItemCompletedUseCase(): ToggleItemCompletedUseCase
    fun updateTodoListUseCase(): UpdateTodoListUseCase
    fun taskReminderScheduler(): TaskReminderScheduler
}