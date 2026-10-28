package com.altankoc.nidado.domain.usecase.todolist

import com.altankoc.nidado.domain.repository.TodoListRepository
import com.altankoc.nidado.util.Resource
import javax.inject.Inject

class ProcessAutoFinishedListsUseCase @Inject constructor(
    private val repository: TodoListRepository
) {
    suspend operator fun invoke(): Resource<Unit> {
        val cutoffTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000) // 24 saat önce
        return repository.processAutoFinishedLists(cutoffTime)
    }
}