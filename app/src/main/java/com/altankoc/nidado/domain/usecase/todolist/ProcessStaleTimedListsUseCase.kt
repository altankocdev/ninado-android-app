package com.altankoc.nidado.domain.usecase.todolist

import com.altankoc.nidado.domain.repository.TodoListRepository
import com.altankoc.nidado.util.Resource
import javax.inject.Inject

class ProcessStaleTimedListsUseCase @Inject constructor(
    private val repository: TodoListRepository
) {
    suspend operator fun invoke(graceMs: Long): Resource<Unit> {
        return repository.processStaleTimedLists(graceMs)
    }
}