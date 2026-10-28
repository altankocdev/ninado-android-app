package com.altankoc.nidado.domain.usecase.todolist

import com.altankoc.nidado.domain.repository.TodoListRepository
import com.altankoc.nidado.util.Resource
import javax.inject.Inject

class UpdateFinishedAtUseCase @Inject constructor(
    private val repository: TodoListRepository
) {
    suspend operator fun invoke(listId: Int, finishedAt: Long?): Resource<Unit> {
        return repository.updateFinishedAt(listId, finishedAt)
    }
}