package com.altankoc.nidado.domain.usecase.todolist

import com.altankoc.nidado.domain.model.ListItem
import com.altankoc.nidado.domain.model.TodoList
import com.altankoc.nidado.domain.repository.TodoListRepository
import com.altankoc.nidado.util.Resource
import javax.inject.Inject

class InsertTodoListUseCase @Inject constructor(
    private val repository: TodoListRepository
) {
    suspend operator fun invoke(todoList: TodoList, items: List<ListItem>): Resource<Int> {
        if (todoList.title.isBlank() && todoList.type != com.altankoc.nidado.domain.model.enums.ListType.SINGLE_TASK) {
            return Resource.Error("Liste adı boş olamaz")
        }
        if (items.isEmpty()) return Resource.Error("Liste en az bir görev içermeli")

        return when (val result = repository.insertTodoList(todoList)) {
            is Resource.Success -> {
                val listId = result.data?.toInt() ?: return Resource.Error("Liste ID alınamadı")
                val itemsWithId = items.map { it.copy(listId = listId) }
                when (val insertItemsResult = repository.insertItems(itemsWithId)) {
                    is Resource.Success -> Resource.Success(listId)
                    is Resource.Error -> Resource.Error(insertItemsResult.message ?: "Görevler eklenirken hata oluştu")
                    is Resource.Loading -> Resource.Error("Hata oluştu")
                }
            }
            is Resource.Error -> Resource.Error(result.message ?: "Hata oluştu")
            is Resource.Loading -> Resource.Error("Hata oluştu")
        }
    }
}