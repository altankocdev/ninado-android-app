package com.altankoc.nidado.domain.usecase.todolist

import com.altankoc.nidado.domain.repository.TodoListRepository
import com.altankoc.nidado.util.Resource
import javax.inject.Inject

class DeleteItemsByListIdUseCase @Inject constructor(
    private val repository: TodoListRepository
) {
    suspend operator fun invoke(listId: Int): Resource<Unit> {
        return repository.deleteItemsByListId(listId)
    }
}