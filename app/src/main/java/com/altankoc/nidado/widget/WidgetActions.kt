package com.altankoc.nidado.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first

private fun entryPoint(context: Context) =
    EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)

val ITEM_ID_KEY = ActionParameters.Key<Int>("item_id")
val LIST_ID_KEY = ActionParameters.Key<Int>("list_id")
val DIRECTION_KEY = ActionParameters.Key<Int>("direction")
val GRADIENT_VALUE_KEY = ActionParameters.Key<String>("gradient_value")

class CompleteTaskAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val itemId = parameters[ITEM_ID_KEY] ?: return

        // 1) Anında görsel geri bildirim — yeşil tik göster
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[WidgetPrefKeys.LAST_COMPLETED_ITEM] = itemId
        }
        NinaDoWidget().update(context, glanceId)

        // 2) DB güncelle — uygulama ile eş zamanlı
        entryPoint(context).toggleItemCompletedUseCase()(itemId, true)

        // 3) updateAll çağırmıyoruz — görev tik ile görünür kalır,
        //    bir sonraki göreve geçiş refresh butonuyla veya periyodik
        //    güncellemeyle olur. Böylece "hemen kaybolma" sorunu çözülür.
    }
}

class ToggleMuteAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val listId = parameters[LIST_ID_KEY] ?: return
        val ep = entryPoint(context)
        val listWithItems = ep.getTodoListByIdUseCase()(listId) ?: return
        val todoList = listWithItems.todoList
        val newMuted = !todoList.isMuted

        // 1) Anında görsel geri bildirim
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[WidgetPrefKeys.LAST_MUTED_LIST] = listId
            prefs[WidgetPrefKeys.LAST_MUTED_STATE] = newMuted
        }
        NinaDoWidget().update(context, glanceId)

        // 2) DB güncelle
        ep.updateTodoListUseCase()(todoList.copy(isMuted = newMuted))

        val now = System.currentTimeMillis()
        if (newMuted) {
            listWithItems.items.forEach { item ->
                ep.taskReminderScheduler().cancelReminder(item.id)
            }
        } else if (todoList.isAlarmEnabled) {
            listWithItems.items
                .filter { !it.isCompleted && it.reminderTime != null && it.reminderTime > now }
                .forEach { item ->
                    ep.taskReminderScheduler().scheduleReminder(
                        itemId = item.id,
                        listId = todoList.id,
                        taskTitle = item.title,
                        reminderTime = item.reminderTime!!
                    )
                }
        }

        // 3) Gerçek veriyle tam yenileme
        NinaDoWidget().updateAll(context)
    }
}

class ChangePageAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val direction = parameters[DIRECTION_KEY] ?: 1
        val ep = entryPoint(context)
        val homeLists = ep.getActiveListsForHomeUseCase()().first()
        val calendarLists = ep.getCalendarListsUseCase()().first()
        val totalPages = WidgetTaskCalculator.computePages(homeLists, calendarLists).size

        updateAppWidgetState(context, glanceId) { prefs ->
            if (totalPages > 0) {
                val current = prefs[WidgetPrefKeys.SELECTED_PAGE] ?: 0
                val next = (current + direction).mod(totalPages)
                prefs[WidgetPrefKeys.SELECTED_PAGE] = next
            }
        }
        NinaDoWidget().update(context, glanceId)
    }
}

class ChangeGradientAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val gradientId = parameters[GRADIENT_VALUE_KEY] ?: return
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[WidgetPrefKeys.GRADIENT_ID] = gradientId
        }
        NinaDoWidget().update(context, glanceId)
    }
}

class RefreshAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        NinaDoWidget().updateAll(context)
    }
}