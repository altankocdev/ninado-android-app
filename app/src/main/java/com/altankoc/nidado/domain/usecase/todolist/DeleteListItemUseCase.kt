package com.altankoc.nidado.domain.usecase.todolist

import com.altankoc.nidado.domain.model.ListItem
import com.altankoc.nidado.domain.repository.TodoListRepository
import com.altankoc.nidado.util.Resource
import javax.inject.Inject

class DeleteListItemUseCase @Inject constructor(
    private val repository: TodoListRepository
) {
    suspend operator fun invoke(item: ListItem): Resource<Unit> {
        return repository.deleteItem(item)
    }
}