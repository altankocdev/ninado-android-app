package com.altankoc.nidado.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.altankoc.nidado.domain.usecase.todolist.ProcessAutoFinishedListsUseCase
import com.altankoc.nidado.domain.usecase.todolist.ProcessStaleTimedListsUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

private const val GRACE_PERIOD_MS = 24 * 60 * 60 * 1000L // 24 saat

@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val processAutoFinishedListsUseCase: ProcessAutoFinishedListsUseCase,
    private val processStaleTimedListsUseCase: ProcessStaleTimedListsUseCase
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            // Saatsiz listeler: finishedAt'e bağlı (reaktif), mevcut mantık
            processAutoFinishedListsUseCase()

            // Saatli listeler: finishedAt'e bağlı değil, görev saatlerinden
            // doğrudan hesaplanır (self-healing, kullanıcı etkileşimine bağlı değil)
            processStaleTimedListsUseCase(GRACE_PERIOD_MS)

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}