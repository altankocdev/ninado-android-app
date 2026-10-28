package com.altankoc.nidado.data.repository

import com.altankoc.nidado.data.dao.ListItemDao
import com.altankoc.nidado.data.dao.TodoListDao
import com.altankoc.nidado.data.dao.UserDao
import com.altankoc.nidado.data.mapper.toListItem
import com.altankoc.nidado.data.mapper.toListItemEntity
import com.altankoc.nidado.data.mapper.toTodoList
import com.altankoc.nidado.data.mapper.toTodoListEntity
import com.altankoc.nidado.data.mapper.toTodoListWithItems
import com.altankoc.nidado.domain.model.ListItem
import com.altankoc.nidado.domain.model.TodoList
import com.altankoc.nidado.domain.model.TodoListWithItems
import com.altankoc.nidado.domain.repository.ArchiveResult
import com.altankoc.nidado.domain.repository.TodoListRepository
import com.altankoc.nidado.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TodoListRepositoryImpl @Inject constructor(
    private val todoListDao: TodoListDao,
    private val listItemDao: ListItemDao,
    private val userDao: UserDao
) : TodoListRepository {

    override fun getActiveLists(): Flow<List<TodoListWithItems>> {
        return todoListDao.getActiveLists().map { list ->
            list.map { it.toTodoListWithItems() }
        }
    }

    override fun getActiveListsForHome(): Flow<List<TodoListWithItems>> {
        return todoListDao.getActiveListsForHome().map { list ->
            list.map { it.toTodoListWithItems() }
        }
    }

    override fun getCalendarLists(): Flow<List<TodoListWithItems>> {
        return todoListDao.getCalendarLists().map { list ->
            list.map { it.toTodoListWithItems() }
        }
    }

    override suspend fun getListByDate(startOfDay: Long, endOfDay: Long): TodoListWithItems? {
        return todoListDao.getListByDate(startOfDay, endOfDay)?.toTodoListWithItems()
    }

    override fun getArchivedLists(): Flow<List<TodoListWithItems>> {
        return todoListDao.getArchivedLists().map { list ->
            list.map { it.toTodoListWithItems() }
        }
    }

    override suspend fun getTodoListById(id: Int): TodoListWithItems? {
        return todoListDao.getTodoListById(id)?.toTodoListWithItems()
    }

    // Liste oluşturulunca kalıcı sayaç +1 artar (silinse de geri alınmaz)
    override suspend fun insertTodoList(todoList: TodoList): Resource<Long> {
        return try {
            val id = todoListDao.insertTodoList(todoList.toTodoListEntity())
            userDao.incrementTotalListsCreated()
            Resource.Success(id)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Liste eklenirken hata oluştu")
        }
    }

    override suspend fun updateTodoList(todoList: TodoList): Resource<Unit> {
        return try {
            todoListDao.updateTodoList(todoList.toTodoListEntity())
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Liste güncellenirken hata oluştu")
        }
    }

    // Liste silinmeden önce, o anki tamamlanmış görev sayısı kalıcı
    // sayaca eklenir — liste silinince kaybolmasın.
    override suspend fun deleteTodoList(todoList: TodoList): Resource<Unit> {
        return try {
            val completedCount = listItemDao.getCompletedTaskCountForList(todoList.id)
            if (completedCount > 0) {
                userDao.addLockedCompletedTasks(completedCount)
            }
            todoListDao.deleteTodoList(todoList.toTodoListEntity())
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Liste silinirken hata oluştu")
        }
    }

    override suspend fun archiveTodoList(todoList: TodoList): Resource<ArchiveResult> {
        return try {
            val archivedCount = todoListDao.getArchivedCount()
            if (archivedCount >= 10) {
                Resource.Success(ArchiveResult.ArchiveFull)
            } else {
                todoListDao.archiveList(todoList.id)
                Resource.Success(ArchiveResult.Success)
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Liste arşivlenirken hata oluştu")
        }
    }

    override suspend fun getArchivedCount(): Int {
        return todoListDao.getArchivedCount()
    }

    override suspend fun updateFinishedAt(listId: Int, finishedAt: Long?): Resource<Unit> {
        return try {
            todoListDao.updateFinishedAt(listId, finishedAt)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Hata oluştu")
        }
    }

    // Saatsiz listeler için otomatik işleme. Liste KALICI olarak silinirse
    // (arşiv doluysa), o listenin tamamlanmış görev sayısı kalıcı sayaca
    // eklenir. Arşive taşınırsa (liste hâlâ var), hiçbir şey eklenmez —
    // çünkü liste hâlâ mevcut, canlı sayaçtan zaten sayılıyor.
    override suspend fun processAutoFinishedLists(cutoffTime: Long): Resource<Unit> {
        return try {
            val readyLists = todoListDao.getListsReadyForAutoProcessing(cutoffTime)

            readyLists.forEach { listWithItemsEntity ->
                val listId = listWithItemsEntity.todoList.id
                val archivedCount = todoListDao.getArchivedCount()

                if (archivedCount < 10) {
                    todoListDao.archiveList(listId)
                } else {
                    val completedCount = listItemDao.getCompletedTaskCountForList(listId)
                    if (completedCount > 0) {
                        userDao.addLockedCompletedTasks(completedCount)
                    }
                    todoListDao.deleteTodoList(listWithItemsEntity.todoList)
                }
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Otomatik işleme sırasında hata oluştu")
        }
    }

    override suspend fun processStaleTimedLists(graceMs: Long): Resource<Unit> {
        return try {
            val now = System.currentTimeMillis()
            val activeTimedLists = todoListDao.getActiveTimedLists()

            activeTimedLists.forEach { listWithItemsEntity ->
                val items = listWithItemsEntity.items
                val maxReminderTime = items.mapNotNull { it.reminderTime }.maxOrNull()

                if (maxReminderTime == null) return@forEach

                val timeSinceLastTask = now - maxReminderTime
                if (timeSinceLastTask >= graceMs) {
                    val listId = listWithItemsEntity.todoList.id
                    val archivedCount = todoListDao.getArchivedCount()

                    if (archivedCount < 10) {
                        todoListDao.archiveList(listId)
                    } else {
                        val completedCount = listItemDao.getCompletedTaskCountForList(listId)
                        if (completedCount > 0) {
                            userDao.addLockedCompletedTasks(completedCount)
                        }
                        todoListDao.deleteTodoList(listWithItemsEntity.todoList)
                    }
                }
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Otomatik işleme sırasında hata oluştu")
        }
    }

    override suspend fun insertItem(item: ListItem): Resource<Int> {
        return try {
            val id = listItemDao.insertItem(item.toListItemEntity())
            Resource.Success(id.toInt())
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Görev eklenirken hata oluştu")
        }
    }

    override suspend fun insertItems(items: List<ListItem>): Resource<Unit> {
        return try {
            listItemDao.insertItems(items.map { it.toListItemEntity() })
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Görevler eklenirken hata oluştu")
        }
    }

    override suspend fun updateItem(item: ListItem): Resource<Unit> {
        return try {
            listItemDao.updateItem(item.toListItemEntity())
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Görev güncellenirken hata oluştu")
        }
    }

    override suspend fun deleteItem(item: ListItem): Resource<Unit> {
        return try {
            listItemDao.deleteItem(item.toListItemEntity())
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Görev silinirken hata oluştu")
        }
    }

    override suspend fun toggleItemCompleted(itemId: Int, isCompleted: Boolean): Resource<Unit> {
        return try {
            listItemDao.toggleItemCompleted(itemId, isCompleted)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Görev güncellenirken hata oluştu")
        }
    }

    override suspend fun deleteItemsByListId(listId: Int): Resource<Unit> {
        return try {
            listItemDao.deleteItemsByListId(listId)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Görevler silinirken hata oluştu")
        }
    }

    override suspend fun getListIdForItem(itemId: Int): Int? {
        return listItemDao.getListIdForItem(itemId)
    }

    override suspend fun deleteItems(items: List<ListItem>): Resource<Unit> {
        return try {
            listItemDao.deleteItems(items.map { it.toListItemEntity() })
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Görevler silinirken hata oluştu")
        }
    }

    // ── İstatistikler ──
    override fun getLiveCompletedTaskCount(): Flow<Int> {
        return listItemDao.getLiveCompletedTaskCount()
    }
}