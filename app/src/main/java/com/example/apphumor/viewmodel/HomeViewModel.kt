// ARQUIVO: app/src/main/java/com/example/apphumor/viewmodel/HomeViewModel.kt

package com.example.apphumor.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.switchMap
import com.example.apphumor.models.HumorNote
import com.example.apphumor.repository.DatabaseRepository
import com.google.firebase.auth.FirebaseAuth
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * [HomeViewModel]
 * Gerencia o estado da tela inicial e do histórico.
 * CORRIGIDO: Usa o campo 'timestamp' direto do HumorNote.
 */
class HomeViewModel(
    private val auth: FirebaseAuth,
    private val dbRepository: DatabaseRepository
) : ViewModel() {

    // LiveData que dispara a busca no repositório
    private val userIdLiveData = MutableLiveData<String?>()

    // LiveData que observa todas as notas em tempo real
    private val allNotesLiveData: LiveData<List<HumorNote>> = userIdLiveData.switchMap { userId ->
        if (userId.isNullOrBlank()) {
            MutableLiveData(emptyList())
        } else {
            dbRepository.getHumorNotesAsLiveData(userId)
        }
    }

    // -------------------------------------------------------------------
    // Dados de Saída
    // -------------------------------------------------------------------

    val allNotes: LiveData<List<HumorNote>> = allNotesLiveData

    val todayNotes: LiveData<List<HumorNote>> = allNotesLiveData.switchMap { notes ->
        MutableLiveData(filterTodayNotes(notes))
    }

    val dailyProgress: LiveData<Pair<Int, Long?>> = allNotesLiveData.switchMap { notes ->
        val sequence = calculateDailySequence(notes)
        // CORREÇÃO: Acessando .timestamp diretamente
        val lastRecordedTimestamp = notes.maxByOrNull { it.timestamp }?.timestamp
        MutableLiveData(Pair(sequence, lastRecordedTimestamp))
    }

    init {
        userIdLiveData.value = auth.currentUser?.uid
    }

    // --- Lógica de Suporte ---

    private fun getDayUnit(timestamp: Long): Long {
        return timestamp / TimeUnit.DAYS.toMillis(1)
    }

    private fun calculateDailySequence(notes: List<HumorNote>): Int {
        if (notes.isEmpty()) return 0

        // 1. Preparar os dias únicos e ordenados
        // CORREÇÃO: Acessando .timestamp diretamente
        val distinctRecordedDays = notes
            .map { getDayUnit(it.timestamp) }
            .distinct()
            .sortedDescending()

        if (distinctRecordedDays.isEmpty()) return 0

        // 2. Obter as datas de referência
        val todayDayUnit = getDayUnit(System.currentTimeMillis())
        val lastRecordedDayUnit = distinctRecordedDays.first()

        // 3. Verificação de Reset da Sequência
        val dayDifference = todayDayUnit - lastRecordedDayUnit

        if (dayDifference > 1) {
            return 0 // Reset
        }

        // 4. Lógica de Contagem
        var sequence = 0
        var expectedDay = lastRecordedDayUnit

        for (day in distinctRecordedDays) {
            if (day == expectedDay) {
                sequence++
                expectedDay--
            } else if (day < expectedDay) {
                break
            }
        }

        return sequence.coerceAtMost(7)
    }

    private fun filterTodayNotes(notes: List<HumorNote>): List<HumorNote> {
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val todayEnd = todayStart + TimeUnit.DAYS.toMillis(1) // 24 horas

        return notes.filter { note ->
            // CORREÇÃO: Acessando .timestamp diretamente
            val timestamp = note.timestamp
            timestamp in todayStart until todayEnd
        }
    }
}

class HomeViewModelFactory(
    private val auth: FirebaseAuth,
    private val dbRepository: DatabaseRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(auth, dbRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
