package com.example.apphumor.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.example.apphumor.models.FilterState
import com.example.apphumor.models.FilterTimeRange
import com.example.apphumor.models.HumorNote
import com.example.apphumor.models.SortOrder
import com.example.apphumor.models.User
import com.example.apphumor.repository.DatabaseRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit

// --- Classe auxiliar para o visual das bolinhas ---
data class DayStatus(
    val label: String,      // "S", "T", "Q"... ou "H" (Hoje)
    val hasEntry: Boolean,  // Se tem registro (cor verde)
    val isToday: Boolean,   // Se é hoje (borda brilhante)
    val isFuture: Boolean,  // Se é dia futuro
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

    // Dados do Usuário (Para o Cabeçalho)
    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> = _currentUser

    // Variável para evitar "farmar" semanas perfeitas na mesma sessão
    private var savedPerfectWeekThisSession = false

    // 2. FONTE DE DADOS (Banco de Dados)
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

    // --- A. Lógica da Sequência de Dias (Streak) e Confetes ---
    private val _weekDays = MediatorLiveData<List<DayStatus>>()
    val weekDays: LiveData<List<DayStatus>> = _weekDays

    private val _showConfetti = MutableLiveData<Boolean>(false)
    val showConfetti: LiveData<Boolean> = _showConfetti

    private val _streakText = MediatorLiveData<String>()
    val streakText: LiveData<String> = _streakText

    // --- B. Lista Filtrada para o Histórico (ESTAVA FALTANDO ISTO!) ---
    val filteredHistoryNotes = MediatorLiveData<List<HumorNote>>().apply {
        addSource(allNotesSource) { notes ->
            value = applyFilters(notes, _filterState.value ?: FilterState())
        }
        addSource(_filterState) { state ->
            value = applyFilters(allNotesSource.value ?: emptyList(), state)
        }
    }

    // --- C. Dados para a Home (Hoje) ---
    val todayNotes: LiveData<List<HumorNote>> = allNotesSource.switchMap { notes ->
        MutableLiveData(filterTodayNotes(notes))
    }
    val totalNotesCount: LiveData<Int> = allNotesSource.map { it.size }

    init {
        userIdLiveData.value = auth.currentUser?.uid
        setupWeekLogic()
        loadUserData() // Carrega nome/foto ao iniciar
    }

    // --- FUNÇÕES DE CARREGAMENTO ---

    fun loadUserData() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val result = dbRepository.getUser(uid)
            if (result is DatabaseRepository.Result.Success) {
                _currentUser.value = result.data
            }
        }
    }

    // --- LÓGICA DE GAMIFICAÇÃO ---

    private fun setupWeekLogic() {
        _weekDays.addSource(allNotesSource) { notes ->
            calculateGamification(notes)
        }
    }

    private fun calculateGamification(notes: List<HumorNote>) {
        val currentStreak = calculateCurrentStreak(notes)
        _streakText.value = "$currentStreak Dias"

        // Lógica de "Limpeza Visual" (Apaga bolinhas se quebrou a sequência)
        val uniqueDates = notes
            .map { normalizeToMidnight(it.timestamp) }
            .distinct()
            .sortedDescending()

        var streakStartDate: Long = Long.MAX_VALUE

        if (currentStreak > 0 && uniqueDates.isNotEmpty()) {
            val lastValidDay = uniqueDates.first()
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = lastValidDay
            calendar.add(Calendar.DAY_OF_YEAR, -(currentStreak - 1))
            streakStartDate = normalizeToMidnight(calendar.timeInMillis)
        }

        val calendar = Calendar.getInstance()
        val todayMillis = normalizeToMidnight(calendar.timeInMillis)

        val statusList = mutableListOf<DayStatus>()
        var daysWithContentLastWeek = 0

        for (i in 6 downTo 0) {
            val targetDay = Calendar.getInstance()
            targetDay.timeInMillis = todayMillis
            targetDay.add(Calendar.DAY_OF_YEAR, -i)

            val dayStart = normalizeToMidnight(targetDay.timeInMillis)
            val dayEnd = dayStart + TimeUnit.DAYS.toMillis(1) - 1

            val hasPhysicalNote = notes.any {
                val noteTime = it.timestamp
                noteTime in dayStart..dayEnd
            }

            // Pinta apenas se faz parte da sequência atual
            val isPartOfStreak = hasPhysicalNote && (dayStart >= streakStartDate)

            val dayOfWeek = targetDay.get(Calendar.DAY_OF_WEEK)
            val isToday = (i == 0)
            val label = if (isToday) "H" else getDayLetter(dayOfWeek)

            statusList.add(
                DayStatus(
                    label = label,
                    hasEntry = isPartOfStreak,
                    isToday = isToday,
                    isFuture = false,
                    dateTimestamp = dayStart
                )
            )

            if (isPartOfStreak) {
                daysWithContentLastWeek++
            }
        }

        _weekDays.value = statusList

        if (daysWithContentLastWeek == 7 && !savedPerfectWeekThisSession) {
            triggerPerfectWeek()
        }
    }

    private fun triggerPerfectWeek() {
        if (_showConfetti.value == true) return
        _showConfetti.value = true
        savedPerfectWeekThisSession = true

        val userId = userIdLiveData.value ?: return
        viewModelScope.launch {
            try {
                dbRepository.incrementPerfectWeeks(userId)
            } catch (e: Exception) {
                savedPerfectWeekThisSession = false
            }
        }
    }

    fun resetConfetti() {
        _showConfetti.value = false
    }

    private fun calculateCurrentStreak(notes: List<HumorNote>): Int {
        if (notes.isEmpty()) return 0
        val uniqueActiveDays = notes
            .map { normalizeToMidnight(it.timestamp) }
            .distinct()
            .sortedDescending()

        if (uniqueActiveDays.isEmpty()) return 0

        val today = normalizeToMidnight(System.currentTimeMillis())
        val yesterday = Calendar.getInstance().apply {
            timeInMillis = today
            add(Calendar.DAY_OF_YEAR, -1)
        }.timeInMillis

        val lastPostDate = uniqueActiveDays.first()

        if (lastPostDate < yesterday) {
            return 0
        }

        var streak = 0
        var currentTargetDate = lastPostDate

        for (day in uniqueActiveDays) {
            if (day == currentTargetDate) {
                streak++
                val c = Calendar.getInstance()
                c.timeInMillis = currentTargetDate
                c.add(Calendar.DAY_OF_YEAR, -1)
                currentTargetDate = c.timeInMillis
            } else {
                break
            }
        }
        return streak
    }

    // --- FUNÇÕES DE FILTRO E UTILITÁRIOS ---

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

    private fun applyFilters(notes: List<HumorNote>, state: FilterState): List<HumorNote> {
        var result = notes

        // Filtro por Texto (Busca)
        if (state.query.isNotEmpty()) {
            result = result.filter {
                it.descricao?.contains(state.query, ignoreCase = true) == true ||
                        it.humor?.contains(state.query, ignoreCase = true) == true
            }
        }

        // Filtro por Humor
        if (state.selectedHumors.isNotEmpty()) {
            result = result.filter { note ->
                val noteHumor = note.humor ?: ""
                state.selectedHumors.any { selected -> selected.equals(noteHumor, ignoreCase = true) }
            }
        }

        // Filtro "Apenas com anotações"
        if (state.onlyWithNotes) {
            result = result.filter { !it.descricao.isNullOrEmpty() }
        }

        // Filtro por Data (7 dias, 30 dias, Tudo)
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

        // Ordenação
        result = when (state.sortOrder) {
            SortOrder.NEWEST -> result.sortedByDescending { it.timestamp }
            SortOrder.OLDEST -> result.sortedBy { it.timestamp }
        }
        return result
    }

    private fun filterTodayNotes(notes: List<HumorNote>): List<HumorNote> {
        val todayStart = normalizeToMidnight(System.currentTimeMillis())
        val todayEnd = todayStart + TimeUnit.DAYS.toMillis(1)
        return notes.filter { note ->
            note.timestamp in todayStart until todayEnd
        }
    }

    private fun normalizeToMidnight(timestamp: Long): Long {
        val c = Calendar.getInstance()
        c.timeInMillis = timestamp
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
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
}