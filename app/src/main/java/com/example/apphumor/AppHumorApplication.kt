package com.example.apphumor

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.work.* // Import do WorkManager
import com.example.apphumor.worker.NotificationWorker
import com.google.firebase.database.FirebaseDatabase
import java.util.concurrent.TimeUnit

class AppHumorApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)

        createNotificationChannel()
        setupDailyWorker()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Lembrete Diário"
            val descriptionText = "Notificações para registrar seu humor"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("HUMOR_CHANNEL_ID", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun setupDailyWorker() {
        // Define restrições (ex: apenas se tiver bateria razoável)
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        // Configura para repetir a cada 24 horas
        val dailyWorkRequest = PeriodicWorkRequestBuilder<NotificationWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        // Enfileira o trabalho (KEEP garante que não criamos duplicatas ao abrir o app)
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "DailyHumorReminder",
            ExistingPeriodicWorkPolicy.KEEP,
            dailyWorkRequest
        )
    }
}