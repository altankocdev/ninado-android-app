package com.altankoc.nidado.domain.usecase.todolist

import com.altankoc.nidado.domain.model.TodoListWithItems
import com.altankoc.nidado.domain.repository.TodoListRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetActiveListsForHomeUseCase @Inject constructor(
    private val repository: TodoListRepository
) {
    operator fun invoke(): Flow<List<TodoListWithItems>> {
        return repository.getActiveListsForHome()
    }
}