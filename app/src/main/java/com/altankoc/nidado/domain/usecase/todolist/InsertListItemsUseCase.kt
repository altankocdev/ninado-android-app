package com.altankoc.nidado.domain.usecase.todolist

import com.altankoc.nidado.domain.model.ListItem
import com.altankoc.nidado.domain.repository.TodoListRepository
import com.altankoc.nidado.util.Resource
import javax.inject.Inject

class InsertListItemsUseCase @Inject constructor(
    private val repository: TodoListRepository
) {
    suspend operator fun invoke(items: List<ListItem>): Resource<Unit> {
        if (items.isEmpty()) return Resource.Error("Liste boş olamaz")
        if (items.any { it.title.isBlank() }) return Resource.Error("Görev adı boş olamaz")
        return repository.insertItems(items)
    }
}