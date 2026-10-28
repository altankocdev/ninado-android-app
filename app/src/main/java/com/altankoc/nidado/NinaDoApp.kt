package com.altankoc.nidado

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.altankoc.nidado.worker.ReminderWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class NinaDoApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        scheduleAutoArchiveWorker()
    }

    private fun scheduleAutoArchiveWorker() {
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(
            15, TimeUnit.MINUTES // TEST: minimum değer. Gerçek değer: 6 saat
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "auto_archive_worker",
            ExistingPeriodicWorkPolicy.REPLACE, // KEEP yerine REPLACE — eski zamanlamayı günceller
            request
        )
    }
}