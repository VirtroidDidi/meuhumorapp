package com.example.apphumor.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.example.apphumor.models.FilterState
import com.example.apphumor.models.FilterTimeRange
import com.example.apphumor.models.HumorNote
import com.example.apphumor.models.SortOrder
import com.example.apphumor.repository.DatabaseRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit

// --- NOVO: Classe auxiliar para o visual das bolinhas ---
data class DayStatus(
    val label: String,      // "S", "T", "Q"... ou "H" (Hoje)
    val hasEntry: Boolean,  // Se tem registro (cor verde)
    val isToday: Boolean,   // Se é hoje (borda brilhante)
    val isFuture: Boolean,  // Se é dia futuro (não usado na lógica de histórico, mas útil p/ expansão)
    val dateTimestamp: Long // Timestamp do dia (meia-noite)
)

class HomeViewModel(
    private val auth: FirebaseAuth,
    private val dbRepository: DatabaseRepository,
    private var lastDeletedNote: HumorNote? = null
) : ViewModel() {

    // 1. INPUTS
    private val userIdLiveData = MutableLiveData<String?>()

    // O estado atual dos filtros
    private val _filterState = MutableLiveData(FilterState())
    val filterState: LiveData<FilterState> = _filterState

    // 2. FONTE DE DADOS (Banco de Dados)
    // Consome Flow e converte para LiveData
    private val allNotesSource: LiveData<List<HumorNote>> = userIdLiveData.switchMap { userId ->
        if (userId.isNullOrBlank()) {
            MutableLiveData(emptyList())
        } else {
            dbRepository.getHumorNotesFlow(userId)
                .onStart { Log.d("HomeViewModel", "Iniciando coleta do Flow de notas") }
                .catch { e ->
                    Log.e("HomeViewModel", "Erro no Flow: ${e.message}")
                    emit(emptyList())
                }
                .asLiveData()
        }
    }

    // 3. SAÍDAS (Outputs para a UI)

    // --- NOVO: Lógica da Sequência de Dias (Streak) e Confetes ---
    private val _weekDays = MediatorLiveData<List<DayStatus>>()
    val weekDays: LiveData<List<DayStatus>> = _weekDays

    private val _showConfetti = MutableLiveData<Boolean>(false)
    val showConfetti: LiveData<Boolean> = _showConfetti

    private val _streakText = MediatorLiveData<String>()
    val streakText: LiveData<String> = _streakText

    // --- EXISTENTE: Lista Filtrada para o Histórico ---
    val filteredHistoryNotes = MediatorLiveData<List<HumorNote>>().apply {
        addSource(allNotesSource) { notes ->
            value = applyFilters(notes, _filterState.value ?: FilterState())
        }
        addSource(_filterState) { state ->
            value = applyFilters(allNotesSource.value ?: emptyList(), state)
        }
    }

    // --- EXISTENTE: Dados para a Home (Hoje e Progresso Simples) ---
    val todayNotes: LiveData<List<HumorNote>> = allNotesSource.switchMap { notes ->
        MutableLiveData(filterTodayNotes(notes))
    }

    val dailyProgress: LiveData<Pair<Int, Long?>> = allNotesSource.switchMap { notes ->
        val sequence = calculateDailySequence(notes)
        val lastRecordedTimestamp = notes.maxByOrNull { it.timestamp }?.timestamp
        MutableLiveData(Pair(sequence, lastRecordedTimestamp))
    }

    init {
        userIdLiveData.value = auth.currentUser?.uid
        setupWeekLogic() // Inicializa a lógica visual da semana
    }

    // --- NOVA LÓGICA: Cálculo Visual da Semana ---

    private fun setupWeekLogic() {
        // Sempre que a lista de notas mudar, recalculamos o visual das bolinhas
        _weekDays.addSource(allNotesSource) { notes ->
            calculateWeekStatus(notes)
        }
    }

    private fun calculateWeekStatus(notes: List<HumorNote>) {
        val calendar = Calendar.getInstance()
        // Zera hora/minuto para comparação justa de datas (meia-noite)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val todayMillis = calendar.timeInMillis
        val statusList = mutableListOf<DayStatus>()
        var daysWithContentCount = 0

        // Vamos gerar os últimos 6 dias + Hoje (Total 7 dias para exibir)
        // Loop de 6 até 0 (onde 0 é hoje)
        val daysToCheck = 6

        for (i in daysToCheck downTo 0) {
            val targetDay = Calendar.getInstance()
            targetDay.timeInMillis = todayMillis
            targetDay.add(Calendar.DAY_OF_YEAR, -i) // Volta 'i' dias

            val dayStart = targetDay.timeInMillis
            val dayEnd = dayStart + TimeUnit.DAYS.toMillis(1)

            // Verifica se existe alguma nota dentro desse intervalo de dia
            val hasNote = notes.any { it.timestamp in dayStart until dayEnd }

            // Define o Label (S, T, Q... ou H)
            val dayOfWeek = targetDay.get(Calendar.DAY_OF_WEEK)
            val isToday = (i == 0)
            val label = if (isToday) "H" else getDayLetter(dayOfWeek)

            statusList.add(
                DayStatus(
                    label = label,
                    hasEntry = hasNote,
                    isToday = isToday,
                    isFuture = false,
                    dateTimestamp = dayStart
                )
            )

            // Contagem para "Semana Perfeita" (quantos dias da visualização têm registro)
            if (hasNote) {
                daysWithContentCount++
            }
        }

        _weekDays.value = statusList
        _streakText.value = "$daysWithContentCount Dias" // Atualiza o badge

        // --- LÓGICA DO CONFETE ---
        // Se o usuário preencheu todos os 7 dias visíveis (Semana Perfeita)
        if (daysWithContentCount == 7) {
            if (_showConfetti.value == false) {
                triggerPerfectWeek()
            }
        }
    }

    private fun triggerPerfectWeek() {
        _showConfetti.value = true
        val userId = userIdLiveData.value ?: return
        viewModelScope.launch {
            // Nota: Certifique-se de ter criado este método 'incrementPerfectWeeks' no seu DatabaseRepository
            // Se não tiver, comente a linha abaixo para não dar erro de compilação
            try {
                // dbRepository.incrementPerfectWeeks(userId)
                Log.d("HomeViewModel", "Semana perfeita registrada!")
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Erro ao salvar semana perfeita: $e")
            }
        }
    }

    fun resetConfetti() {
        _showConfetti.value = false
    }

    private fun getDayLetter(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            Calendar.SUNDAY -> "D"
            Calendar.MONDAY -> "S"
            Calendar.TUESDAY -> "T"
            Calendar.WEDNESDAY -> "Q"
            Calendar.THURSDAY -> "Q"
            Calendar.FRIDAY -> "S"
            Calendar.SATURDAY -> "S"
            else -> "?"
        }
    }

    // --- AÇÕES EXISTENTES (Busca, Filtro, Delete) ---

    fun updateSearchQuery(query: String) {
        val current = _filterState.value ?: FilterState()
        if (current.query != query) {
            _filterState.value = current.copy(query = query)
        }
    }

    fun updateFilterState(newState: FilterState) {
        _filterState.value = newState
    }

    fun deleteNote(note: HumorNote) {
        val userId = userIdLiveData.value ?: return
        val noteId = note.id ?: return

        lastDeletedNote = note

        viewModelScope.launch {
            dbRepository.deleteHumorNote(userId, noteId)
        }
    }

    fun undoDelete() {
        val userId = userIdLiveData.value ?: return
        val noteToRestore = lastDeletedNote ?: return

        viewModelScope.launch {
            dbRepository.restoreHumorNote(userId, noteToRestore)
            lastDeletedNote = null
        }
    }

    // --- LÓGICA DE FILTRAGEM E SUPORTE (Mantido) ---

    private fun applyFilters(notes: List<HumorNote>, state: FilterState): List<HumorNote> {
        var result = notes

        // 1. Filtro de Texto
        if (state.query.isNotEmpty()) {
            result = result.filter {
                it.descricao?.contains(state.query, ignoreCase = true) == true ||
                        it.humor?.contains(state.query, ignoreCase = true) == true
            }
        }

        // 2. Filtro de Humor
        if (state.selectedHumors.isNotEmpty()) {
            result = result.filter { note ->
                val noteHumor = note.humor ?: ""
                state.selectedHumors.any { selected ->
                    selected.equals(noteHumor, ignoreCase = true)
                }
            }
        }

        // 3. Filtro de Conteúdo
        if (state.onlyWithNotes) {
            result = result.filter { !it.descricao.isNullOrEmpty() }
        }

        // 4. Filtro de Tempo
        val now = System.currentTimeMillis()
        val oneDay = TimeUnit.DAYS.toMillis(1)

        result = when (state.timeRange) {
            FilterTimeRange.LAST_7_DAYS -> {
                val limit = now - (7 * oneDay)
                result.filter { it.timestamp >= limit }
            }

            FilterTimeRange.LAST_30_DAYS -> {
                val limit = now - (30 * oneDay)
                result.filter { it.timestamp >= limit }
            }

            FilterTimeRange.ALL_TIME -> result
        }

        // 5. Ordenação
        result = when (state.sortOrder) {
            SortOrder.NEWEST -> result.sortedByDescending { it.timestamp }
            SortOrder.OLDEST -> result.sortedBy { it.timestamp }
        }

        return result
    }

    private fun getDayUnit(timestamp: Long): Long {
        return timestamp / TimeUnit.DAYS.toMillis(1)
    }

    // Calcula sequência consecutiva (lógica antiga de backup)
    private fun calculateDailySequence(notes: List<HumorNote>): Int {
        if (notes.isEmpty()) return 0
        val distinctRecordedDays = notes
            .map { getDayUnit(it.timestamp) }
            .distinct()
            .sortedDescending()

        if (distinctRecordedDays.isEmpty()) return 0

        val todayDayUnit = getDayUnit(System.currentTimeMillis())
        val lastRecordedDayUnit = distinctRecordedDays.first()
        val dayDifference = todayDayUnit - lastRecordedDayUnit

        if (dayDifference > 1) return 0

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
        val todayEnd = todayStart + TimeUnit.DAYS.toMillis(1)

        return notes.filter { note ->
            note.timestamp in todayStart until todayEnd
        }
    }
}