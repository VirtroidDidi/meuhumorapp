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
import com.example.apphumor.MainActivity
import com.example.apphumor.R
import com.example.apphumor.di.DependencyProvider
import java.util.*

class NotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val auth = DependencyProvider.auth
        val repository = DependencyProvider.databaseRepository
        val userId = auth.currentUser?.uid ?: return Result.success()

        // Se não tem usuário logado, não faz nada

        // Busca notas (One-Shot)
        val notes = repository.getHumorNotesOnce(userId)

        // Verifica se existe alguma nota de HOJE
        val lastNoteTimestamp = notes.maxByOrNull { it.timestamp }?.timestamp ?: 0L

        if (!isToday(lastNoteTimestamp)) {
            sendNotification()
        }

        return Result.success()
    }

    private fun isToday(timestamp: Long): Boolean {
        if (timestamp == 0L) return false
        val calendarNote = Calendar.getInstance().apply { timeInMillis = timestamp }
        val calendarNow = Calendar.getInstance()

        return calendarNote.get(Calendar.DAY_OF_YEAR) == calendarNow.get(Calendar.DAY_OF_YEAR) &&
                calendarNote.get(Calendar.YEAR) == calendarNow.get(Calendar.YEAR)
    }

    // A anotação abaixo remove o erro vermelho do notify, pois já fazemos a checagem manual
    @SuppressLint("MissingPermission")
    private fun sendNotification() {
        // Verifica permissão (Android 13+)
        if (ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Sem permissão, aborta silenciosamente
            return
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        // ===================================
        // CÓDIGO ATUALIZADO
        // ===================================
        val builder = NotificationCompat.Builder(applicationContext, "HUMOR_CHANNEL_ID")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            // TÍTULO DE ALERTA ATUALIZADO
            .setContentTitle("🔥 Não quebre sua sequência!")
            // TEXTO PERSUASIVO ATUALIZADO
            .setContentText("Você ainda não registrou seu humor hoje. Entre agora para manter seu histórico!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
        // ===================================

        // O erro deve sumir agora com o @SuppressLint acima
        with(NotificationManagerCompat.from(applicationContext)) {
            notify(1001, builder.build())
        }
    }
}