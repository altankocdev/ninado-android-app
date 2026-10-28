package com.altankoc.nidado.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.altankoc.nidado.domain.model.enums.ListType
import com.altankoc.nidado.domain.usecase.todolist.GetActiveListsUseCase
import com.altankoc.nidado.util.notification.TaskReminderScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var getActiveListsUseCase: GetActiveListsUseCase

    @Inject
    lateinit var taskReminderScheduler: TaskReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                rescheduleAllAlarms()
            } finally {
                pendingResult.finish()
            }
        }
    }

    // AlarmManager'ın kesin alarmları, telefon yeniden başladığında
    // sistem tarafından silinir (WorkManager'ın aksine). Bu yüzden
    // reboot sonrası, tüm bekleyen alarmları manuel olarak yeniden kuruyoruz.
    private suspend fun rescheduleAllAlarms() {
        val now = System.currentTimeMillis()
        val activeLists = getActiveListsUseCase().first()

        activeLists.forEach { listWithItems ->
            val todoList = listWithItems.todoList

            if (!todoList.isAlarmEnabled || todoList.isMuted) return@forEach

            if (todoList.type == ListType.SHOPPING) {
                // Market: liste seviyesi sentinel reminder (-listId)
                val date = todoList.scheduledDate
                val time = todoList.startTime
                if (date != null && time != null) {
                    val dateCal = Calendar.getInstance().apply { timeInMillis = date }
                    val timeCal = Calendar.getInstance().apply { timeInMillis = time }
                    val combined = Calendar.getInstance().apply {
                        set(
                            dateCal.get(Calendar.YEAR),
                            dateCal.get(Calendar.MONTH),
                            dateCal.get(Calendar.DAY_OF_MONTH),
                            timeCal.get(Calendar.HOUR_OF_DAY),
                            timeCal.get(Calendar.MINUTE),
                            0
                        )
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis

                    if (combined > now) {
                        taskReminderScheduler.scheduleReminder(
                            itemId = -todoList.id,
                            listId = todoList.id,
                            taskTitle = "Alışveriş zamanı geldi",
                            reminderTime = combined
                        )
                    }
                }
            } else {
                listWithItems.items
                    .filter { !it.isCompleted && it.reminderTime != null && it.reminderTime > now }
                    .forEach { item ->
                        taskReminderScheduler.scheduleReminder(
                            itemId = item.id,
                            listId = todoList.id,
                            taskTitle = item.title,
                            reminderTime = item.reminderTime!!
                        )
                    }
            }
        }
    }
}