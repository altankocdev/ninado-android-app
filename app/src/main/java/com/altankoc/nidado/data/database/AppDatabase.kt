package com.altankoc.nidado.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.altankoc.nidado.data.dao.CourseDao
import com.altankoc.nidado.data.dao.ListItemDao
import com.altankoc.nidado.data.dao.TodoListDao
import com.altankoc.nidado.data.dao.UserDao
import com.altankoc.nidado.data.entity.CourseEntity
import com.altankoc.nidado.data.entity.ListItemEntity
import com.altankoc.nidado.data.entity.TodoListEntity
import com.altankoc.nidado.data.entity.UserEntity

@Database(
    entities = [
        CourseEntity::class,
        TodoListEntity::class,
        ListItemEntity::class,
        UserEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun courseDao(): CourseDao
    abstract fun todoListDao(): TodoListDao
    abstract fun listItemDao(): ListItemDao
    abstract fun userDao(): UserDao
}