package com.example.apphumor.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.example.apphumor.models.FilterState
import com.example.apphumor.models.FilterTimeRange
import com.example.apphumor.models.HumorNote
import com.example.apphumor.models.SortOrder
import com.example.apphumor.repository.DatabaseRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit

class HomeViewModel(
    private val auth: FirebaseAuth,
    private val dbRepository: DatabaseRepository,
    private var lastDeletedNote: HumorNote? = null

) : ViewModel() {

    // 1. INPUTS
    private val userIdLiveData = MutableLiveData<String?>()

    // O estado atual dos filtros (começa padrão)
    private val _filterState = MutableLiveData(FilterState())
    val filterState: LiveData<FilterState> = _filterState

    // 2. FONTE DE DADOS (Banco de Dados)
    private val allNotesSource: LiveData<List<HumorNote>> = userIdLiveData.switchMap { userId ->
        if (userId.isNullOrBlank()) {
            MutableLiveData(emptyList())
        } else {
            dbRepository.getHumorNotesAsLiveData(userId)
        }
    }

    // 3. SAÍDAS (Outputs para a UI)

    // A. Lista Filtrada para o Histórico (A MÁGICA ACONTECE AQUI)
    val filteredHistoryNotes = MediatorLiveData<List<HumorNote>>().apply {
        // Se a lista do banco mudar, recalcula
        addSource(allNotesSource) { notes ->
            value = applyFilters(notes, _filterState.value ?: FilterState())
        }
        // Se o filtro mudar, recalcula
        addSource(_filterState) { state ->
            value = applyFilters(allNotesSource.value ?: emptyList(), state)
        }
    }

    // B. Dados para a Home (Mantido como estava, mas seguro)
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
    }

    // --- AÇÕES ---

    // Chamado quando o usuário digita na barra de busca
    fun updateSearchQuery(query: String) {
        val current = _filterState.value ?: FilterState()
        if (current.query != query) {
            _filterState.value = current.copy(query = query)
        }
    }

    // Chamado quando o usuário clica em "Aplicar" no BottomSheet
    fun updateFilterState(newState: FilterState) {
        _filterState.value = newState
    }

    // --- LÓGICA DE FILTRAGEM ---

    private fun applyFilters(notes: List<HumorNote>, state: FilterState): List<HumorNote> {
        var result = notes

        // 1. Filtro de Texto (Case insensitive)
        if (state.query.isNotEmpty()) {
            result = result.filter {
                it.descricao?.contains(state.query, ignoreCase = true) == true ||
                        it.humor?.contains(state.query, ignoreCase = true) == true
            }
        }

        // 2. Filtro de Humor (Multisseleção)
        if (state.selectedHumors.isNotEmpty()) {
            result = result.filter { note ->
                // Normaliza para comparar (ex: banco tem "Sad", filtro tem "Sad")
                // Se seu banco tem strings em português e o filtro em inglês, precisaria converter aqui.
                // Assumindo que o FilterBottomSheet já entrega as strings certas.
                val noteHumor = note.humor ?: ""

                // Verifica se o humor da nota está na lista de selecionados
                // Dica: Usamos lowercase para garantir que "Sad" bata com "sad"
                state.selectedHumors.any { selected ->
                    selected.equals(noteHumor, ignoreCase = true)
                }
            }
        }

        // 3. Filtro de Conteúdo (Apenas notas com texto)
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

    // --- LÓGICA DE SUPORTE (MANTIDA DO ORIGINAL) ---

    private fun getDayUnit(timestamp: Long): Long {
        return timestamp / TimeUnit.DAYS.toMillis(1)
    }

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

    fun deleteNote(note: HumorNote) {
        val userId = userIdLiveData.value ?: return
        val noteId = note.id ?: return

        // 1. Guarda na memória para possível Undo
        lastDeletedNote = note

        // 2. Remove do Banco
        viewModelScope.launch {
            dbRepository.deleteHumorNote(userId, noteId)
            // O LiveData allNotesSource atualizará a UI automaticamente via Realtime Database
        }
    }

    fun undoDelete() {
        val userId = userIdLiveData.value ?: return
        val noteToRestore = lastDeletedNote ?: return

        viewModelScope.launch {
            // Chama a função específica de restauração que criamos
            dbRepository.restoreHumorNote(userId, noteToRestore)

            // Limpa a memória após restaurar
            lastDeletedNote = null
        }
    }
}
