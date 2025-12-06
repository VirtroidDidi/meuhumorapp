package com.example.apphumor.utils

import android.content.Context
import androidx.work.*
import com.example.apphumor.worker.NotificationWorker
import java.util.*
import java.util.concurrent.TimeUnit

object NotificationScheduler {

    fun scheduleDailyReminder(context: Context, hour: Int, minute: Int, isEnabled: Boolean) {
        val workManager = WorkManager.getInstance(context)
        val uniqueWorkName = "DailyHumorReminder"

        // 1. Se o usuário desativou, cancelamos o agendamento existente
        if (!isEnabled) {
            workManager.cancelUniqueWork(uniqueWorkName)
            return
        }

        // 2. Calcula o tempo até o horário escolhido
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }

        // Se o horário já passou hoje, agenda para amanhã
        if (target.before(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }

        val initialDelay = target.timeInMillis - now.timeInMillis

        // 3. Configura o Worker
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val dailyWorkRequest = PeriodicWorkRequestBuilder<NotificationWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .addTag("notification_work")
            .build()

        // 4. Enfileira (UPDATE substitui o antigo pelo novo horário)
        workManager.enqueueUniquePeriodicWork(
            uniqueWorkName,
            ExistingPeriodicWorkPolicy.UPDATE,
            dailyWorkRequest
        )
    }
}