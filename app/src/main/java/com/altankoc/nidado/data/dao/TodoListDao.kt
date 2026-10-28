package com.altankoc.nidado.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.altankoc.nidado.data.entity.ListItemEntity
import com.altankoc.nidado.data.entity.TodoListEntity
import com.altankoc.nidado.data.entity.TodoListWithItemsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoListDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodoList(todoList: TodoListEntity): Long

    @Update
    suspend fun updateTodoList(todoList: TodoListEntity)

    @Delete
    suspend fun deleteTodoList(todoList: TodoListEntity)

    @Transaction
    @Query("SELECT * FROM todo_lists WHERE isArchived = 0 ORDER BY createdAt DESC")
    fun getActiveLists(): Flow<List<TodoListWithItemsEntity>>

    @Transaction
    @Query("SELECT * FROM todo_lists WHERE isArchived = 0 AND type != 'PERSONAL' ORDER BY createdAt DESC")
    fun getActiveListsForHome(): Flow<List<TodoListWithItemsEntity>>

    @Transaction
    @Query("SELECT * FROM todo_lists WHERE isArchived = 0 AND type = 'PERSONAL' ORDER BY scheduledDate ASC")
    fun getCalendarLists(): Flow<List<TodoListWithItemsEntity>>

    @Transaction
    @Query("""
        SELECT * FROM todo_lists 
        WHERE isArchived = 0 
        AND type = 'PERSONAL' 
        AND scheduledDate BETWEEN :startOfDay AND :endOfDay 
        LIMIT 1
    """)
    suspend fun getListByDate(startOfDay: Long, endOfDay: Long): TodoListWithItemsEntity?

    @Transaction
    @Query("SELECT * FROM todo_lists WHERE isArchived = 1 ORDER BY createdAt DESC LIMIT 10")
    fun getArchivedLists(): Flow<List<TodoListWithItemsEntity>>

    @Transaction
    @Query("SELECT * FROM todo_lists WHERE id = :id")
    suspend fun getTodoListById(id: Int): TodoListWithItemsEntity?

    @Query("SELECT COUNT(*) FROM todo_lists WHERE isArchived = 1")
    suspend fun getArchivedCount(): Int

    @Query("UPDATE todo_lists SET isMuted = :isMuted WHERE id = :listId")
    suspend fun updateMuted(listId: Int, isMuted: Boolean)

    // ── 24 saat otomatik işleme için (SAATSİZ listeler, finishedAt'e bağlı) ──

    @Transaction
    @Query("""
        SELECT * FROM todo_lists 
        WHERE isArchived = 0 
        AND endTime IS NOT NULL 
        AND endTime <= :cutoffTime
    """)
    suspend fun getListsReadyForAutoProcessing(cutoffTime: Long): List<TodoListWithItemsEntity>

    // ── SAATLİ listeler için: finishedAt'e bağlı değil, görev saatlerinden
    //    doğrudan hesaplanacak. Tüm aktif saatli listeleri getir. ──
    @Transaction
    @Query("SELECT * FROM todo_lists WHERE isArchived = 0 AND isTimed = 1")
    suspend fun getActiveTimedLists(): List<TodoListWithItemsEntity>

    @Query("UPDATE todo_lists SET isArchived = 1 WHERE id = :listId")
    suspend fun archiveList(listId: Int)

    @Query("UPDATE todo_lists SET endTime = :finishedAt WHERE id = :listId")
    suspend fun updateFinishedAt(listId: Int, finishedAt: Long?)

    @Delete
    suspend fun deleteItems(items: List<ListItemEntity>)
}