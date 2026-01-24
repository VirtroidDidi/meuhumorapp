package com.example.apphumor.utils

import com.example.apphumor.R
import com.example.apphumor.models.HumorNote
import com.example.apphumor.models.HumorType // [NOVO]
import java.text.SimpleDateFormat
import java.util.*

data class InsightResult(
    val title: String,
    val message: String,
    val iconRes: Int,
    val colorRes: Int,
    val backgroundTint: Int
)

object InsightAnalysis {

    fun generateInsight(notes: List<HumorNote>): InsightResult {
        if (notes.isEmpty()) {
            return InsightResult(
                "Começando sua jornada",
                "Registre seu primeiro humor para desbloquear insights sobre você.",
                R.drawable.ic_mood_neutral,
                R.color.mood_neutral,
                R.color.insight_neutral_bg
            )
        }

        // 1. Filtrar últimos 30 dias
        val trintaDiasAtras = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -30) }.timeInMillis
        val recentNotes = notes.filter { it.timestamp >= trintaDiasAtras }
        val notesToAnalyze = if (recentNotes.isNotEmpty()) recentNotes else notes

        // 2. Estatística (Usando Enum para agrupar corretamente Legados e Atuais)
        val total = notesToAnalyze.size

        // Agrupa pelo Enum HumorType, não mais pela String bruta
        val grouped = notesToAnalyze.groupingBy { HumorType.fromKey(it.humor) }.eachCount()

        val dominantEntry = grouped.maxByOrNull { it.value }
        val dominantType = dominantEntry?.key ?: HumorType.NEUTRAL
        val count = dominantEntry?.value ?: 0
        val percentage = (count * 100) / total

        // Traduz para usar na frase (Pega do resources do Enum)
        // Precisamos de Contexto para getString, mas como aqui é Object,
        // vamos usar nomes genéricos ou passar contexto.
        // Para simplificar, manteremos a lógica de texto aqui baseada no ENUM.
        val moodNamePT = getMoodNamePT(dominantType)

        // 3. Melhor Dia
        val bestDay = calculateBestDay(notesToAnalyze)
        val bestDayText = if (bestDay != "N/A") "\n\n📅 Curiosidade: $bestDay costuma ser seu melhor dia!" else ""

        // 4. LÓGICA DE MENSAGENS (Baseada no Enum)
        return when (dominantType) {
            // --- POSITIVOS ---
            HumorType.RAD, HumorType.HAPPY, HumorType.GRATEFUL, HumorType.CALM -> {
                InsightResult(
                    title = "Onda Positiva! 🌟",
                    message = "Você está brilhando! $percentage% dos seus registros recentes são sobre '$moodNamePT'. Aproveite essa energia para criar.$bestDayText",
                    iconRes = dominantType.iconRes,
                    colorRes = dominantType.colorRes,
                    backgroundTint = dominantType.backgroundTint
                )
            }

            // --- TRISTEZA / BAIXA ENERGIA ---
            HumorType.SAD, HumorType.TIRED, HumorType.PENSIVE -> {
                InsightResult(
                    title = "Acolhimento 💙",
                    message = "Notamos que '$moodNamePT' apareceu em $percentage% das vezes. Respeite seu tempo. Um chá ou banho quente podem ajudar.$bestDayText",
                    iconRes = dominantType.iconRes,
                    colorRes = dominantType.colorRes,
                    backgroundTint = dominantType.backgroundTint
                )
            }

            // --- ANSIEDADE ---
            HumorType.ANXIOUS -> {
                InsightResult(
                    title = "Respire Fundo 🍃",
                    message = "A ansiedade esteve presente em $percentage% dos registros. Tente a técnica 4-7-8 agora: inspire 4s, segure 7s, solte 8s.$bestDayText",
                    iconRes = dominantType.iconRes,
                    colorRes = dominantType.colorRes,
                    backgroundTint = dominantType.backgroundTint
                )
            }

            // --- RAIVA ---
            HumorType.ANGRY -> {
                InsightResult(
                    title = "Pausa Necessária 🛑",
                    message = "Sentir raiva ou irritação é um sinal de limites. Tente se afastar do problema por 5 minutos e beber um copo d'água.$bestDayText",
                    iconRes = dominantType.iconRes,
                    colorRes = dominantType.colorRes,
                    backgroundTint = dominantType.backgroundTint
                )
            }

            // --- NEUTRO ---
            else -> {
                InsightResult(
                    title = "Equilíbrio ⚖️",
                    message = "Seus dias estão estáveis. É um ótimo momento para planejar os próximos passos sem pressão.$bestDayText",
                    iconRes = dominantType.iconRes,
                    colorRes = dominantType.colorRes,
                    backgroundTint = dominantType.backgroundTint
                )
            }
        }
    }

    private fun getMoodNamePT(type: HumorType): String {
        return when (type) {
            HumorType.ANGRY -> "Raiva"
            HumorType.ANXIOUS -> "Ansiedade"
            HumorType.SAD -> "Tristeza"
            HumorType.TIRED -> "Cansaço"
            HumorType.HAPPY -> "Felicidade"
            HumorType.RAD -> "Empolgação"
            HumorType.GRATEFUL -> "Gratidão"
            HumorType.CALM -> "Calma"
            HumorType.PENSIVE -> "Reflexão"
            else -> "Neutro"
        }
    }

    private fun calculateBestDay(notes: List<HumorNote>): String {
        // Define quais tipos contam como "Bons"
        val positiveTypes = listOf(
            HumorType.RAD, HumorType.HAPPY, HumorType.GRATEFUL, HumorType.CALM
        )

        val dayCounts = notes
            .filter { HumorType.fromKey(it.humor) in positiveTypes }
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
}