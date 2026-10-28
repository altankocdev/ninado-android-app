package com.altankoc.nidado.domain.repository

import com.altankoc.nidado.domain.model.ListItem
import com.altankoc.nidado.domain.model.TodoList
import com.altankoc.nidado.domain.model.TodoListWithItems
import com.altankoc.nidado.util.Resource
import kotlinx.coroutines.flow.Flow

sealed class ArchiveResult {
    data object Success : ArchiveResult()
    data object ArchiveFull : ArchiveResult()
    data object NotFinished : ArchiveResult()
}

interface TodoListRepository {

    fun getActiveLists(): Flow<List<TodoListWithItems>>
    fun getActiveListsForHome(): Flow<List<TodoListWithItems>>
    fun getCalendarLists(): Flow<List<TodoListWithItems>>
    suspend fun getListByDate(startOfDay: Long, endOfDay: Long): TodoListWithItems?
    fun getArchivedLists(): Flow<List<TodoListWithItems>>
    suspend fun getTodoListById(id: Int): TodoListWithItems?
    suspend fun insertTodoList(todoList: TodoList): Resource<Long>
    suspend fun updateTodoList(todoList: TodoList): Resource<Unit>
    suspend fun deleteTodoList(todoList: TodoList): Resource<Unit>

    suspend fun archiveTodoList(todoList: TodoList): Resource<ArchiveResult>
    suspend fun getArchivedCount(): Int

    suspend fun updateFinishedAt(listId: Int, finishedAt: Long?): Resource<Unit>

    suspend fun processAutoFinishedLists(cutoffTime: Long): Resource<Unit>

    suspend fun processStaleTimedLists(graceMs: Long): Resource<Unit>

    suspend fun insertItem(item: ListItem): Resource<Int>
    suspend fun insertItems(items: List<ListItem>): Resource<Unit>
    suspend fun updateItem(item: ListItem): Resource<Unit>
    suspend fun deleteItem(item: ListItem): Resource<Unit>
    suspend fun toggleItemCompleted(itemId: Int, isCompleted: Boolean): Resource<Unit>
    suspend fun deleteItemsByListId(listId: Int): Resource<Unit>

    suspend fun getListIdForItem(itemId: Int): Int?

    suspend fun deleteItems(items: List<ListItem>): Resource<Unit>

    // ── İstatistikler ──
    fun getLiveCompletedTaskCount(): Flow<Int>
}