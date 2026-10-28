package com.altankoc.nidado.domain.usecase.todolist

import com.altankoc.nidado.domain.repository.TodoListRepository
import com.altankoc.nidado.util.Resource
import javax.inject.Inject

class ToggleItemCompletedUseCase @Inject constructor(
    private val repository: TodoListRepository
) {
    suspend operator fun invoke(itemId: Int, isCompleted: Boolean): Resource<Unit> {
        // 1) Görevi güncelle
        val toggleResult = repository.toggleItemCompleted(itemId, isCompleted)
        if (toggleResult is Resource.Error) return toggleResult

        // 2) Bu görevin ait olduğu listeyi bul
        val listId = repository.getListIdForItem(itemId) ?: return Resource.Success(Unit)
        val listWithItems = repository.getTodoListById(listId) ?: return Resource.Success(Unit)

        // 3) Liste artık tamamen "bitti" mi? (her görev tik atılmış VEYA saati geçmiş)
        val now = System.currentTimeMillis()
        val isFinished = listWithItems.items.isNotEmpty() && listWithItems.items.all {
            it.isCompleted || (it.reminderTime != null && it.reminderTime < now)
        }

        val currentFinishedAt = listWithItems.todoList.endTime

        // 4) finishedAt'i duruma göre set et veya sıfırla
        return when {
            isFinished && currentFinishedAt == null -> {
                repository.updateFinishedAt(listId, now)
            }
            !isFinished && currentFinishedAt != null -> {
                repository.updateFinishedAt(listId, null)
            }
            else -> Resource.Success(Unit)
        }
    }
}