package com.altankoc.nidado.domain.usecase.todolist

import com.altankoc.nidado.domain.model.TodoList
import com.altankoc.nidado.domain.repository.ArchiveResult
import com.altankoc.nidado.domain.repository.TodoListRepository
import com.altankoc.nidado.util.Resource
import javax.inject.Inject

class ArchiveTodoListUseCase @Inject constructor(
    private val repository: TodoListRepository
) {
    suspend operator fun invoke(todoList: TodoList): Resource<ArchiveResult> {
        // Taze veriyi çek — finishedAt alanına güvenmiyoruz, anlık hesaplıyoruz.
        val fresh = repository.getTodoListById(todoList.id)
            ?: return Resource.Error("Liste bulunamadı")

        val isReadyToArchive = isReady(fresh.todoList, fresh.items.map { it })

        if (!isReadyToArchive) {
            return Resource.Success(ArchiveResult.NotFinished)
        }

        return repository.archiveTodoList(fresh.todoList)
    }

    private fun isReady(
        todoList: TodoList,
        items: List<com.altankoc.nidado.domain.model.ListItem>
    ): Boolean {
        // Saatsiz liste: tamamlanma şartı yok, istediği zaman arşivlenebilir.
        if (!todoList.isTimed) return true

        // Saatli liste: tamamlanma durumuna bakılmaksızın,
        // TÜM görevlerin saati geçmiş olmalı.
        val now = System.currentTimeMillis()
        if (items.isEmpty()) return false
        return items.all { it.reminderTime != null && it.reminderTime < now }
    }
}