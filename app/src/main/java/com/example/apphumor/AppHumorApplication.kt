package com.example.apphumor

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.work.*
import com.example.apphumor.di.AppContainer
import com.example.apphumor.worker.NotificationWorker
import com.google.firebase.database.FirebaseDatabase
import java.util.concurrent.TimeUnit

class AppHumorApplication : Application() {

    // Instância do Container acessível por todas as Activities/Fragments
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()

        // 1. Inicializa o Container (Injeção de Dependência)
        container = AppContainer()

        // 2. Configurações do Firebase (Persistência Offline)
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)

        // 3. Configurações de Notificação e Workers (Mantido do original)
        createNotificationChannel()
        setupDailyWorker()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.notification_channel_name) // Usando strings.xml se disponível
            val descriptionText = getString(R.string.notification_channel_desc)
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