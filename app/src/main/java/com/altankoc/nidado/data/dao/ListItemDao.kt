package com.altankoc.nidado.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.altankoc.nidado.data.entity.ListItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ListItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ListItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ListItemEntity>)

    @Update
    suspend fun updateItem(item: ListItemEntity)

    @Delete
    suspend fun deleteItem(item: ListItemEntity)

    @Query("SELECT * FROM list_items WHERE listId = :listId")
    fun getItemsByListId(listId: Int): Flow<List<ListItemEntity>>

    @Query("UPDATE list_items SET isCompleted = :isCompleted WHERE id = :itemId")
    suspend fun toggleItemCompleted(itemId: Int, isCompleted: Boolean)

    @Query("DELETE FROM list_items WHERE listId = :listId")
    suspend fun deleteItemsByListId(listId: Int)

    @Query("SELECT listId FROM list_items WHERE id = :itemId")
    suspend fun getListIdForItem(itemId: Int): Int?

    @Delete
    suspend fun deleteItems(items: List<ListItemEntity>)

    // ── İstatistikler için ──

    // Canlı (silinmemiş) tüm listelerdeki tamamlanmış görev sayısı.
    // Bir liste silinince (FK cascade ile) ona ait item'lar da silindiği
    // için bu sayı otomatik olarak doğru kalır.
    @Query("SELECT COUNT(*) FROM list_items WHERE isCompleted = 1")
    fun getLiveCompletedTaskCount(): Flow<Int>

    // Belirli bir listenin tamamlanmış görev sayısı — liste silinmeden
    // önce, o anki değeri kalıcı sayaca eklemek için kullanılır.
    @Query("SELECT COUNT(*) FROM list_items WHERE listId = :listId AND isCompleted = 1")
    suspend fun getCompletedTaskCountForList(listId: Int): Int
}