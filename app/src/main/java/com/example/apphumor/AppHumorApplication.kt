package com.example.apphumor

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.work.*
import com.example.apphumor.utils.ThemePreferences // Importante
import com.example.apphumor.worker.NotificationWorker
import com.google.firebase.database.FirebaseDatabase
import java.util.concurrent.TimeUnit

class AppHumorApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // 1. Aplica o tema salvo (Claro/Escuro) imediatamente
        ThemePreferences.applyTheme(this)

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
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val dailyWorkRequest = PeriodicWorkRequestBuilder<NotificationWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "DailyHumorReminder",
            ExistingPeriodicWorkPolicy.KEEP,
            dailyWorkRequest
        )
    }
}