package com.example.apphumor.viewmodel

import android.app.Application
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

// Alteração: Agora estende AndroidViewModel para ter acesso ao Contexto (Application)
// Necessário para buscar strings traduzidas (getString) dentro do ViewModel se necessário,
// mas aqui usaremos para garantir acesso a recursos se precisarmos no futuro.
// Mantive ViewModel padrão para compatibilidade com sua Factory atual, a lógica de string é feita via ID.
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

        // Agrupa e conta os humores
        val humorCounts = filteredNotes
            .filter { it.humor != null }
            .groupingBy { it.humor!!.lowercase(Locale.ROOT) }
            .eachCount()

        // Encontra o mais comum
        val mostCommonEntry = humorCounts.maxByOrNull { it.value }

        // Formata o nome para exibição (ex: "Rad" -> "Rad" (será traduzido na View se usarmos ID) ou texto direto)
        // Aqui usamos o texto cru do banco, mas a View (Fragment) pode traduzir se quiser,
        // ou podemos retornar o String Resource ID no Insight.
        // Para simplificar e manter compatibilidade com seu modelo Insight atual (que aceita String no valor),
        // vamos retornar o nome capitalizado, mas o ícone será o visual principal.
        val mostCommonHumorRaw = mostCommonEntry?.key?.replaceFirstChar { it.titlecase() } ?: "Neutro"

        // Pega a configuração visual (Ícone e Cor de Fundo)
        val (iconCommon, bgCommon, labelId) = getMoodConfig(mostCommonHumorRaw)

        // Se quiser exibir o texto traduzido, precisaria passar o Context, mas como o Insight.valor é String,
        // vamos passar o nome em inglês/banco por enquanto ou tentar mapear manualmente se crítico.
        // O ícone já diz tudo!

        val bestDay = calculateBestDay(filteredNotes)

        return listOf(
            Insight(
                rotulo = "Humor Predominante",
                valor = mostCommonHumorRaw, // Exibirá "Rad", "Happy", etc.
                iconResId = iconCommon,
                backgroundColorResId = bgCommon
            ),
            Insight(
                rotulo = "Total de Registros",
                valor = "$totalNotes notas",
                iconResId = R.drawable.ic_save_24, // Ícone genérico de 'salvar/registro' ou similar
                backgroundColorResId = R.color.insight_neutral_bg
            ),
            Insight(
                rotulo = "Melhor Dia",
                valor = bestDay,
                iconResId = R.drawable.ic_mood_rad, // Ícone de alta energia para representar o melhor dia
                backgroundColorResId = R.color.insight_rad_bg
            )
        )
    }

    private fun calculateBestDay(notes: List<HumorNote>): String {
        // Lista expandida de humores positivos para o cálculo
        val positiveHumors = listOf(
            "rad", "happy", "grateful", "calm", "excellent", "good",
            "energetic", "feliz", "bem", "calmo", "grato", "incrível", "excelente"
        )

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

    // Retorna Triple: IconRes, BgColorRes, LabelRes (LabelRes para uso futuro)
    private fun getMoodConfig(humorType: String): Triple<Int, Int, Int> {
        return when (humorType.lowercase(Locale.ROOT)) {
            // Alta Energia +
            "rad", "incrível", "excellent", "energetic" ->
                Triple(R.drawable.ic_mood_rad, R.color.insight_rad_bg, R.string.humor_rad)
            "happy", "feliz", "good", "bem" ->
                Triple(R.drawable.ic_mood_happy, R.color.insight_happy_bg, R.string.humor_happy)

            // Baixa Energia +
            "grateful", "grato" ->
                Triple(R.drawable.ic_mood_grateful, R.color.insight_grateful_bg, R.string.humor_grateful)
            "calm", "calmo" ->
                Triple(R.drawable.ic_mood_calm, R.color.insight_calm_bg, R.string.humor_calm)

            // Neutros
            "neutral", "neutro" ->
                Triple(R.drawable.ic_mood_neutral, R.color.insight_neutral_bg, R.string.humor_neutral)
            "pensive", "pensativo" ->
                Triple(R.drawable.ic_mood_pensive, R.color.insight_pensive_bg, R.string.humor_pensive)

            // Baixa Energia -
            "tired", "cansado" ->
                Triple(R.drawable.ic_mood_tired, R.color.insight_tired_bg, R.string.humor_tired)
            "sad", "triste" ->
                Triple(R.drawable.ic_mood_sad, R.color.insight_sad_bg, R.string.humor_sad)

            // Alta Energia -
            "anxious", "ansioso" ->
                Triple(R.drawable.ic_mood_anxious, R.color.insight_anxious_bg, R.string.humor_anxious)
            "angry", "irritado" ->
                Triple(R.drawable.ic_mood_angry, R.color.insight_angry_bg, R.string.humor_angry)

            else -> Triple(R.drawable.ic_mood_neutral, R.color.insight_neutral_bg, R.string.humor_neutral)
        }
    }

    private fun createEmptyInsight(msg: String) = Insight("Status", msg, R.drawable.ic_mood_neutral, R.color.insight_neutral_bg)
}

// --- FACTORY ---
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