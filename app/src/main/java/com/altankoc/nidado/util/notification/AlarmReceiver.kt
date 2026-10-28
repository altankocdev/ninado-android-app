package com.altankoc.nidado.util.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.updateAll
import com.altankoc.nidado.widget.NinaDoWidget
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var getTodoListByIdUseCase: com.altankoc.nidado.domain.usecase.todolist.GetTodoListByIdUseCase

    @Inject
    lateinit var getUserOnceUseCase: com.altankoc.nidado.domain.usecase.user.GetUserOnceUseCase

    @Inject
    lateinit var notificationHelper: NotificationHelper

    override fun onReceive(context: Context, intent: Intent) {
        val listId = intent.getIntExtra(TaskReminderScheduler.KEY_LIST_ID, -1)
        val itemId = intent.getIntExtra(TaskReminderScheduler.KEY_ITEM_ID, -1)
        val taskTitle = intent.getStringExtra(TaskReminderScheduler.KEY_TASK_TITLE) ?: "Görev"

        if (listId == -1) return

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                handleAlarm(listId, itemId, taskTitle)
                NinaDoWidget().updateAll(context)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleAlarm(listId: Int, itemId: Int, taskTitle: String) {
        val user = getUserOnceUseCase()
        if (user?.notificationsEnabled == false) return

        val listWithItems = getTodoListByIdUseCase(listId) ?: return
        val todoList = listWithItems.todoList

        if (!todoList.isAlarmEnabled || todoList.isMuted) return

        val isListLevelReminder = itemId < 0
        if (isListLevelReminder) {
            val allCompleted = listWithItems.items.isNotEmpty() && listWithItems.items.all { it.isCompleted }
            if (allCompleted) return
        } else {
            val item = listWithItems.items.find { it.id == itemId }
            if (item == null || item.isCompleted) return
        }

        val avatarId = user?.avatarId ?: 1
        val soundId = user?.notificationSoundId ?: "bird"
        val notificationId = if (isListLevelReminder) -itemId else itemId

        notificationHelper.showTaskReminder(
            notificationId = notificationId,
            listTitle = todoList.title,
            taskTitle = taskTitle,
            listId = listId,
            avatarId = avatarId,
            soundId = soundId
        )
    }
}