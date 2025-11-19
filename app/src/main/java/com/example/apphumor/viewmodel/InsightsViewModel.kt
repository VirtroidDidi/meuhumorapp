// ARQUIVO: app/src/main/java/com/example/apphumor/viewmodel/InsightsViewModel.kt

package com.example.apphumor.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.switchMap
import com.example.apphumor.R
import com.example.apphumor.models.HumorNote
import com.example.apphumor.models.Insight
import com.example.apphumor.repository.DatabaseRepository
import com.google.firebase.auth.FirebaseAuth
import java.util.*

/**
 * [InsightsViewModel]
 * Responsável por buscar as notas de humor, calcular e expor os insights prontos para a UI.
 */
class InsightsViewModel(
    private val auth: FirebaseAuth,
    private val dbRepository: DatabaseRepository
) : ViewModel() {

    companion object {
        private const val TAG = "InsightsViewModel"
    }

    // LiveData usado como gatilho para a busca/cálculo
    private val userIdLiveData = MutableLiveData<String?>()

    // LiveData que busca as notas em tempo real do Repositório
    private val allNotesLiveData: LiveData<List<HumorNote>> = userIdLiveData.switchMap { userId ->
        if (userId.isNullOrBlank()) {
            MutableLiveData(emptyList())
        } else {
            dbRepository.getHumorNotesAsLiveData(userId)
        }
    }

    /**
     * LiveData que transforma a lista bruta de notas nos Insights prontos para a UI.
     */
    val insights: LiveData<List<Insight>> = allNotesLiveData.switchMap { notes ->
        val result = MutableLiveData<List<Insight>>()
        result.value = calculateInsights(notes)
        result
    }

    // LiveData opcional para indicar estado de carregamento
    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading


    init {
        // Inicia o processo de busca
        val userId = auth.currentUser?.uid
        userIdLiveData.value = userId
    }

    /**
     * Lógica principal de cálculo dos insights a partir das notas de humor (Filtrando por Mês).
     */
    private fun calculateInsights(notes: List<HumorNote>): List<Insight> {
        if (notes.isEmpty()) return emptyList()

        // PASSO 1: Filtra as notas apenas para o MÊS ATUAL
        val currentCalendar = Calendar.getInstance()
        val currentMonth = currentCalendar.get(Calendar.MONTH)
        val currentYear = currentCalendar.get(Calendar.YEAR)

        val notesCurrentMonth = notes.filter { note ->
            val noteCalendar = Calendar.getInstance().apply {
                // CORREÇÃO CRÍTICA: Acessando 'timestamp' diretamente
                timeInMillis = note.timestamp
            }
            noteCalendar.get(Calendar.MONTH) == currentMonth &&
                    noteCalendar.get(Calendar.YEAR) == currentYear
        }

        if (notesCurrentMonth.isEmpty()) {
            return getEmptyMonthInsights()
        }

        val totalNotesInMonth = notesCurrentMonth.size

        // Agrupa e conta a frequência de humores APENAS para o mês atual
        val humorCounts = notesCurrentMonth
            .filter { it.humor != null }
            .groupingBy { it.humor!!.lowercase(Locale.ROOT) }
            .eachCount()


        // 1. Humor Mais Comum
        val mostCommonHumorEntry = humorCounts.maxByOrNull { it.value }
        val mostCommonHumor = mostCommonHumorEntry?.key?.replaceFirstChar { it.titlecase(Locale.ROOT) } ?: "Neutro"
        val countCommon = mostCommonHumorEntry?.value ?: 0
        val humorIconCommon = getIconAndColorForHumor(mostCommonHumor)

        val humorInsight = Insight(
            rotulo = "Humor Mais Comum (Mês)",
            valor = "$mostCommonHumor ($countCommon notas)",
            iconResId = humorIconCommon.first,
            backgroundColorResId = humorIconCommon.second
        )

        // 2. Dias de Registros Ativos (Total de Notas do Mês)
        val activeDaysInsight = Insight(
            rotulo = "Notas Registradas (Mês)",
            valor = "$totalNotesInMonth registros",
            iconResId = R.drawable.ic_active_days_24,
            backgroundColorResId = R.color.insight_calm_bg
        )

        // 3. Humor Menos Comum
        val leastCommonHumorEntry = humorCounts.minByOrNull { it.value }
        val leastCommonHumor = leastCommonHumorEntry?.key?.replaceFirstChar { it.titlecase(Locale.ROOT) } ?: "N/A"
        val countLeastCommon = leastCommonHumorEntry?.value ?: 0
        val humorIconLeastCommon = getIconAndColorForHumor(leastCommonHumor)

        val humorLeastCommonInsight = Insight(
            rotulo = "Humor Menos Comum (Mês)",
            valor = "$leastCommonHumor ($countLeastCommon notas)",
            iconResId = humorIconLeastCommon.first,
            backgroundColorResId = R.color.insight_angry_bg
        )

        return listOf(humorInsight, activeDaysInsight, humorLeastCommonInsight)
    }

    // Funções de Suporte (Movidas do Fragment)
    private fun getIconAndColorForHumor(humorType: String): Pair<Int, Int> {
        return when (humorType.lowercase(Locale.ROOT)) {
            "calmo" -> Pair(R.drawable.ic_calm_24, R.color.insight_calm_bg)
            "energetico" -> Pair(R.drawable.ic_energetic_24, R.color.insight_energetic_bg)
            "triste" -> Pair(R.drawable.ic_sad_24, R.color.insight_sad_bg)
            "irritado" -> Pair(R.drawable.ic_angry_24, R.color.insight_angry_bg)
            else -> Pair(R.drawable.ic_neutral_24, R.color.insight_neutral_bg)
        }
    }

    private fun getEmptyMonthInsights(): List<Insight> {
        return listOf(
            Insight(
                rotulo = "Dias Ativos (Mês)",
                valor = "0 dias",
                iconResId = R.drawable.ic_active_days_24,
                backgroundColorResId = R.color.insight_calm_bg
            ),
            Insight(
                rotulo = "Humor Mais Comum",
                valor = "Sem Registros",
                iconResId = R.drawable.ic_neutral_24,
                backgroundColorResId = R.color.insight_neutral_bg
            ),
            Insight(
                rotulo = "Humor Menos Comum",
                valor = "Sem Registros",
                iconResId = R.drawable.ic_neutral_24,
                backgroundColorResId = R.color.insight_neutral_bg
            )
        )
    }
}

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