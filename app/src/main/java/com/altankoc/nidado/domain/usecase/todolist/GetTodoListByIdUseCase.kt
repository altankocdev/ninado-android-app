package com.altankoc.nidado.domain.usecase.todolist

import com.altankoc.nidado.domain.model.TodoListWithItems
import com.altankoc.nidado.domain.repository.TodoListRepository
import javax.inject.Inject

class GetTodoListByIdUseCase @Inject constructor(
    private val repository: TodoListRepository
) {
    suspend operator fun invoke(id: Int): TodoListWithItems? {
        return repository.getTodoListById(id)
    }
}