package com.example.apphumor.viewmodel

import androidx.lifecycle.*
import com.example.apphumor.R
import com.example.apphumor.models.HumorNote
import com.example.apphumor.models.Insight
import com.example.apphumor.repository.DatabaseRepository
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

// --- AQUI ESTÁ A DEFINIÇÃO OFICIAL DO TIMERANGE ---
enum class TimeRange {
    LAST_7_DAYS,
    LAST_30_DAYS,
    CURRENT_MONTH
}

class InsightsViewModel(
    private val auth: FirebaseAuth,
    private val dbRepository: DatabaseRepository
) : ViewModel() {

    // 1. Inputs
    private val userIdLiveData = MutableLiveData<String?>()
    private val _timeRange = MutableLiveData<TimeRange>(TimeRange.CURRENT_MONTH)

    // 2. Estado de Carregamento
    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    // 3. Fonte de Dados
    private val allNotesLiveData: LiveData<List<HumorNote>> = userIdLiveData.switchMap { userId ->
        _loading.value = true
        if (userId.isNullOrBlank()) {
            _loading.value = false
            MutableLiveData(emptyList())
        } else {
            val source = dbRepository.getHumorNotesAsLiveData(userId)
            source.map {
                _loading.value = false
                it
            }
        }
    }

    // 4. Output
    val insights = MediatorLiveData<List<Insight>>().apply {
        addSource(allNotesLiveData) { notes ->
            value = calculateInsights(notes, _timeRange.value ?: TimeRange.CURRENT_MONTH)
        }
        addSource(_timeRange) { range ->
            value = calculateInsights(allNotesLiveData.value ?: emptyList(), range)
        }
    }

    init {
        userIdLiveData.value = auth.currentUser?.uid
    }

    fun setTimeRange(range: TimeRange) {
        _timeRange.value = range
    }

    private fun calculateInsights(notes: List<HumorNote>, range: TimeRange): List<Insight> {
        if (notes.isEmpty()) return emptyList()

        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis

        val startTime = when (range) {
            TimeRange.LAST_7_DAYS -> {
                val c = Calendar.getInstance()
                c.add(Calendar.DAY_OF_YEAR, -7)
                c.timeInMillis
            }
            TimeRange.LAST_30_DAYS -> {
                val c = Calendar.getInstance()
                c.add(Calendar.DAY_OF_YEAR, -30)
                c.timeInMillis
            }
            TimeRange.CURRENT_MONTH -> {
                val c = Calendar.getInstance()
                c.set(Calendar.DAY_OF_MONTH, 1)
                c.set(Calendar.HOUR_OF_DAY, 0)
                c.set(Calendar.MINUTE, 0)
                c.set(Calendar.SECOND, 0)
                c.timeInMillis
            }
        }

        val filteredNotes = notes.filter { it.timestamp in startTime..endTime }

        if (filteredNotes.isEmpty()) return listOf(createEmptyInsight("Nenhum registro no período."))

        val totalNotes = filteredNotes.size

        val humorCounts = filteredNotes
            .filter { it.humor != null }
            .groupingBy { it.humor!!.lowercase(Locale.ROOT) }
            .eachCount()

        val mostCommonEntry = humorCounts.maxByOrNull { it.value }
        val mostCommonHumor = mostCommonEntry?.key?.replaceFirstChar { it.titlecase() } ?: "Neutro"
        val (iconCommon, bgCommon) = getIconAndColorForHumor(mostCommonHumor)

        val bestDay = calculateBestDay(filteredNotes)

        return listOf(
            Insight(
                rotulo = "Humor Predominante",
                valor = mostCommonHumor,
                iconResId = iconCommon,
                backgroundColorResId = bgCommon
            ),
            Insight(
                rotulo = "Total de Registros",
                valor = "$totalNotes notas",
                iconResId = R.drawable.ic_active_days_24, // Use ic_launcher_foreground se não tiver este ícone
                backgroundColorResId = R.color.insight_neutral_bg
            ),
            Insight(
                rotulo = "Melhor Dia",
                valor = bestDay,
                iconResId = R.drawable.ic_energetic_24, // Use ic_launcher_foreground se não tiver este ícone
                backgroundColorResId = R.color.insight_energetic_bg
            )
        )
    }

    private fun calculateBestDay(notes: List<HumorNote>): String {
        val positiveHumors = listOf("calm", "energetic", "feliz", "happy", "calmo", "energético")
        val dayCounts = notes
            .filter { (it.humor?.lowercase() ?: "") in positiveHumors }
            .groupingBy {
                val c = Calendar.getInstance()
                c.timeInMillis = it.timestamp
                SimpleDateFormat("EEEE", Locale("pt", "BR")).format(c.time)
                    .replaceFirstChar { char -> char.titlecase() }
                    .split("-")[0]
            }
            .eachCount()
        return dayCounts.maxByOrNull { it.value }?.key ?: "N/A"
    }

    private fun getIconAndColorForHumor(humorType: String): Pair<Int, Int> {
        return when (humorType.lowercase(Locale.ROOT)) {
            "calm", "calmo" -> Pair(R.drawable.ic_calm_24, R.color.insight_calm_bg)
            "energetic", "energético" -> Pair(R.drawable.ic_energetic_24, R.color.insight_energetic_bg)
            "sad", "triste" -> Pair(R.drawable.ic_sad_24, R.color.insight_sad_bg)
            "angry", "irritado" -> Pair(R.drawable.ic_angry_24, R.color.insight_angry_bg)
            else -> Pair(R.drawable.ic_neutral_24, R.color.insight_neutral_bg)
        }
    }

    private fun createEmptyInsight(msg: String) = Insight("Status", msg, R.drawable.ic_neutral_24, R.color.insight_neutral_bg)
}

// --- A FACTORY TAMBÉM DEVE ESTAR AQUI ---
class InsightsViewModelFactory(
    private val auth: FirebaseAuth,
    private val dbRepository: DatabaseRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InsightsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return InsightsViewModel(auth, dbRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}