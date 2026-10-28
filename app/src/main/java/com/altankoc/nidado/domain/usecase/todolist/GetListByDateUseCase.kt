package com.altankoc.nidado.domain.usecase.todolist

import com.altankoc.nidado.domain.model.TodoListWithItems
import com.altankoc.nidado.domain.repository.TodoListRepository
import java.util.Calendar
import javax.inject.Inject

class GetListByDateUseCase @Inject constructor(
    private val repository: TodoListRepository
) {
    suspend operator fun invoke(date: Long): TodoListWithItems? {
        val cal = Calendar.getInstance().apply { timeInMillis = date }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfDay = cal.timeInMillis

        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val endOfDay = cal.timeInMillis

        return repository.getListByDate(startOfDay, endOfDay)
    }
}