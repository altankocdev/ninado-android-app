package com.altankoc.nidado.domain.usecase.todolist

import com.altankoc.nidado.domain.model.TodoList
import com.altankoc.nidado.domain.repository.TodoListRepository
import com.altankoc.nidado.util.Resource
import javax.inject.Inject

class DeleteTodoListUseCase @Inject constructor(
    private val repository: TodoListRepository
) {
    suspend operator fun invoke(todoList: TodoList): Resource<Unit> {
        return repository.deleteTodoList(todoList)
    }
}