package com.example.apphumor.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import com.example.apphumor.models.HumorNote
import com.example.apphumor.repository.DatabaseRepository
import com.google.firebase.auth.FirebaseAuth
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * [HomeViewModel]
 * Gerencia o estado da tela inicial e do histórico: Notas registradas hoje,
 * lista completa de notas e cálculo da sequência diária.
 */
class HomeViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val dbRepository = DatabaseRepository()

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
    // Dados de Saída (Transformação dos dados brutos)
    // -------------------------------------------------------------------

    // NOVO: LiveData que expõe todas as notas para o HistoryFragment e InsightsFragment
    val allNotes: LiveData<List<HumorNote>> = allNotesLiveData

    // Transforma todas as notas na lista de notas para serem exibidas hoje
    val todayNotes: LiveData<List<HumorNote>> = allNotesLiveData.switchMap { notes ->
        MutableLiveData(filterTodayNotes(notes))
    }

    // Transforma todas as notas no estado de sequência diária (parâmetros para UI)
    val dailyProgress: LiveData<Pair<Int, Long?>> = allNotesLiveData.switchMap { notes ->
        val sequence = calculateDailySequence(notes)
        val lastRecordedTimestamp = notes.mapNotNull { it.data?.get("time") as? Long }.maxOrNull()
        MutableLiveData(Pair(sequence, lastRecordedTimestamp))
    }

    init {
        // Inicia a busca
        userIdLiveData.value = auth.currentUser?.uid
    }

    // --- Lógica de Suporte (Movida de HomeFragment) ---

    // Função utilitária para converter timestamp para a unidade de "dia"
    private fun getDayUnit(timestamp: Long): Long {
        return timestamp / TimeUnit.DAYS.toMillis(1)
    }

    /**
     * Calcula a sequência de dias consecutivos de registro de humor.
     * @param notes A lista completa de HumorNote do usuário.
     * @return O número de dias consecutivos (máximo 7).
     */
    private fun calculateDailySequence(notes: List<HumorNote>): Int {
        if (notes.isEmpty()) return 0

        // 1. Preparar os dias únicos e ordenados
        val distinctRecordedDays = notes
            .mapNotNull { it.data?.get("time") as? Long }
            .map { getDayUnit(it) }
            .distinct()
            .sortedDescending()

        if (distinctRecordedDays.isEmpty()) return 0

        // 2. Obter as datas de referência (D_Hoje e D_Última)
        val todayDayUnit = getDayUnit(System.currentTimeMillis())
        val lastRecordedDayUnit = distinctRecordedDays.first()

        // 3. Verificação de Reset da Sequência (Quebra se a diferença for > 1)
        val dayDifference = todayDayUnit - lastRecordedDayUnit

        if (dayDifference > 1) {
            return 0 // Executar Reset
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

        // 5. Garantir o limite máximo de 7 
        return sequence.coerceAtMost(7)
    }

    /**
     * Filtra as notas para incluir apenas aquelas registradas no dia de hoje.
     */
    private fun filterTodayNotes(notes: List<HumorNote>): List<HumorNote> {
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val todayEnd = todayStart + TimeUnit.DAYS.toMillis(1) // 24 horas

        return notes.filter { note ->
            val timestamp = note.data?.get("time") as? Long ?: 0L
            timestamp in todayStart until todayEnd
        }
    }
}