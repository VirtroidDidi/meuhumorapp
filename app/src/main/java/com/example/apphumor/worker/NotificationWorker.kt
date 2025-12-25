package com.example.apphumor.worker

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.apphumor.AppHumorApplication
import com.example.apphumor.MainActivity
import com.example.apphumor.R
import java.util.*

class NotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // CORREÇÃO: Acessa as dependências via AppContainer (Application)
        // em vez do antigo DependencyProvider estático.
        val application = applicationContext as AppHumorApplication
        val container = application.container

        val auth = container.auth
        val repository = container.databaseRepository

        val userId = auth.currentUser?.uid ?: return Result.success()

        // Lógica original mantida: Busca notas e verifica se já registrou hoje
        try {
            val notes = repository.getHumorNotesOnce(userId)
            val lastNoteTimestamp = notes.maxByOrNull { it.timestamp }?.timestamp ?: 0L

            if (!isToday(lastNoteTimestamp)) {
                sendNotification()
            }
        } catch (e: Exception) {
            // Em caso de erro na busca, retornamos falha ou retry
            return Result.failure()
        }

        return Result.success()
    }

    private fun isToday(timestamp: Long): Boolean {
        if (timestamp == 0L) return false
        val calendarNote = Calendar.getInstance().apply { timeInMillis = timestamp }
        val calendarToday = Calendar.getInstance()

        return calendarNote.get(Calendar.YEAR) == calendarToday.get(Calendar.YEAR) &&
                calendarNote.get(Calendar.DAY_OF_YEAR) == calendarToday.get(Calendar.DAY_OF_YEAR)
    }

    @SuppressLint("MissingPermission")
    private fun sendNotification() {
        // Verifica permissão para Android 13+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(applicationContext, "HUMOR_CHANNEL_ID")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🔥 Não quebre sua sequência!")
            .setContentText("Você ainda não registrou seu humor hoje. Entre agora para manter seu histórico!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(applicationContext)) {
            notify(1001, builder.build())
        }
    }
}