package com.altankoc.nidado.domain.usecase.todolist

import com.altankoc.nidado.domain.repository.TodoListRepository
import com.altankoc.nidado.domain.usecase.user.GetUserUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class GetTotalCompletedTasksUseCase @Inject constructor(
    private val todoListRepository: TodoListRepository,
    private val getUserUseCase: GetUserUseCase
) {
    operator fun invoke(): Flow<Int> {
        return combine(
            todoListRepository.getLiveCompletedTaskCount(),
            getUserUseCase()
        ) { liveCount, user ->
            liveCount + (user?.lockedCompletedTasks ?: 0)
        }
    }
}