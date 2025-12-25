package com.example.apphumor.viewmodel

import androidx.lifecycle.*
import com.example.apphumor.R
import com.example.apphumor.models.HumorNote
import com.example.apphumor.models.Insight
import com.example.apphumor.repository.DatabaseRepository
import com.example.apphumor.utils.HumorUtils
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

enum class TimeRange {
    LAST_7_DAYS, LAST_30_DAYS, CURRENT_MONTH
}

class InsightsViewModel(
    private val auth: FirebaseAuth,
    private val dbRepository: DatabaseRepository
) : ViewModel() {

    private val userIdLiveData = MutableLiveData<String?>()
    private val _timeRange = MutableLiveData<TimeRange>(TimeRange.CURRENT_MONTH)

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    init {
        userIdLiveData.value = auth.currentUser?.uid
    }

    private val allNotesLiveData: LiveData<List<HumorNote>> = userIdLiveData.switchMap { userId ->
        if (userId.isNullOrBlank()) {
            MutableLiveData(emptyList())
        } else {
            dbRepository.getHumorNotesAsLiveData(userId)
        }
    }

    val insights = MediatorLiveData<List<Insight>>().apply {
        addSource(allNotesLiveData) { notes ->
            value = combineLatestData(notes, _timeRange.value)
        }
        addSource(_timeRange) { range ->
            value = combineLatestData(allNotesLiveData.value, range)
        }
    }

    fun setTimeRange(range: TimeRange) {
        _timeRange.value = range
    }

    private fun combineLatestData(notes: List<HumorNote>?, range: TimeRange?): List<Insight> {
        _loading.postValue(false)
        if (notes.isNullOrEmpty() || range == null) return emptyList()

        val filteredNotes = filterNotesByRange(notes, range)
        if (filteredNotes.isEmpty()) return emptyList()

        return calculateInsights(filteredNotes)
    }

    private fun filterNotesByRange(notes: List<HumorNote>, range: TimeRange): List<HumorNote> {
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis

        val startTime = when (range) {
            TimeRange.LAST_7_DAYS -> now - (7 * 24 * 60 * 60 * 1000L)
            TimeRange.LAST_30_DAYS -> now - (30 * 24 * 60 * 60 * 1000L)
            TimeRange.CURRENT_MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.timeInMillis
            }
        }
        return notes.filter { it.timestamp >= startTime }
    }

    private fun calculateInsights(notes: List<HumorNote>): List<Insight> {
        val totalNotes = notes.size
        val predominantMood = notes.groupingBy { it.humor?.lowercase() ?: "neutral" }
            .eachCount()
            .maxByOrNull { it.value }?.key ?: "neutral"

        val moodStyle = HumorUtils.getMoodStyle(predominantMood)
        val bestDay = calculateBestDay(notes)

        return listOf(
            Insight(
                rotulo = "Humor Predominante",
                valor = predominantMood.replaceFirstChar { it.uppercase() },
                iconResId = moodStyle.iconRes,
                backgroundColorResId = moodStyle.backgroundColorRes
            ),
            Insight(
                rotulo = "Total de Registros",
                valor = "$totalNotes notas",
                iconResId = R.drawable.ic_save_24,
                backgroundColorResId = R.color.insight_neutral_bg
            ),
            Insight(
                rotulo = "Melhor Dia",
                valor = bestDay,
                iconResId = R.drawable.ic_mood_rad,
                backgroundColorResId = R.color.insight_rad_bg
            )
        )
    }

    private fun calculateBestDay(notes: List<HumorNote>): String {
        val positiveHumors = listOf("rad", "happy", "grateful", "calm", "excellent", "good", "energetic")
        val dayCounts = notes
            .filter { (it.humor?.lowercase() ?: "") in positiveHumors }
            .groupingBy {
                val c = Calendar.getInstance()
                c.timeInMillis = it.timestamp
                val sdf = SimpleDateFormat("EEEE", Locale("pt", "BR"))
                sdf.format(c.time).split("-")[0].replaceFirstChar { char -> char.uppercase() }
            }
            .eachCount()

        return dayCounts.maxByOrNull { it.value }?.key ?: "N/A"
    }
}