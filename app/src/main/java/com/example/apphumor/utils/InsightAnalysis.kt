package com.example.apphumor.utils

import com.example.apphumor.R
import com.example.apphumor.models.HumorNote
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

        // 2. Estatística
        val total = notesToAnalyze.size
        val grouped = notesToAnalyze.groupingBy { it.humor ?: "Neutro" }.eachCount()

        val dominantEntry = grouped.maxByOrNull { it.value }
        val dominantMoodRaw = dominantEntry?.key ?: "Neutro"
        val count = dominantEntry?.value ?: 0
        val percentage = (count * 100) / total

        // Traduz para usar na frase
        val moodNamePT = translateMoodName(dominantMoodRaw)

        // 3. Melhor Dia
        val bestDay = calculateBestDay(notesToAnalyze)
        val bestDayText = if (bestDay != "N/A") "\n\n📅 Curiosidade: $bestDay costuma ser seu melhor dia!" else ""

        // 4. LÓGICA DE MENSAGENS (AGORA SEPARADA)
        return when (dominantMoodRaw) {
            // --- POSITIVOS ---
            "Rad", "Happy", "Feliz", "Incrível", "Bem", "Energético", "Grateful", "Grato", "Excellent", "Excelente" -> {
                InsightResult(
                    title = "Onda Positiva! 🌟",
                    message = "Você está brilhando! $percentage% dos seus registros recentes são sobre '$moodNamePT'. Aproveite essa energia para criar.$bestDayText",
                    iconRes = R.drawable.ic_mood_rad,
                    colorRes = R.color.mood_rad,
                    backgroundTint = R.color.insight_rad_bg
                )
            }

            // --- TRISTEZA / BAIXA ENERGIA ---
            "Sad", "Triste", "Tired", "Cansado", "Chateado", "Pensive", "Pensativo" -> {
                InsightResult(
                    title = "Acolhimento 💙",
                    message = "Notamos que '$moodNamePT' apareceu em $percentage% das vezes. Respeite seu tempo. Um chá ou banho quente podem ajudar.$bestDayText",
                    iconRes = R.drawable.ic_mood_sad,
                    colorRes = R.color.mood_sad,
                    backgroundTint = R.color.insight_sad_bg
                )
            }

            // --- ANSIEDADE (Tensão) ---
            "Anxious", "Ansioso" -> {
                InsightResult(
                    title = "Respire Fundo 🍃",
                    // Ajustei o texto para fazer sentido com "Ansiedade"
                    message = "A ansiedade esteve presente em $percentage% dos registros. Tente a técnica 4-7-8 agora: inspire 4s, segure 7s, solte 8s.$bestDayText",
                    iconRes = R.drawable.ic_mood_anxious,
                    colorRes = R.color.mood_anxious,
                    backgroundTint = R.color.insight_anxious_bg
                )
            }

            // --- RAIVA (Irritação) - BLOCO NOVO ---
            "Angry", "Irritado", "Raiva" -> {
                InsightResult(
                    title = "Pausa Necessária 🛑",
                    // Texto específico para Raiva
                    message = "Sentir raiva ou irritação é um sinal de limites. Tente se afastar do problema por 5 minutos e beber um copo d'água.$bestDayText",
                    iconRes = R.drawable.ic_mood_angry,
                    colorRes = R.color.mood_angry,
                    backgroundTint = R.color.insight_angry_bg
                )
            }

            // --- NEUTRO ---
            else -> {
                InsightResult(
                    title = "Equilíbrio ⚖️",
                    message = "Seus dias estão estáveis. É um ótimo momento para planejar os próximos passos sem pressão.$bestDayText",
                    iconRes = R.drawable.ic_mood_neutral,
                    colorRes = R.color.mood_neutral,
                    backgroundTint = R.color.insight_neutral_bg
                )
            }
        }
    }

    private fun translateMoodName(englishName: String): String {
        return when (englishName) {
            "Angry", "Irritado" -> "Raiva"
            "Anxious", "Ansioso" -> "Ansiedade"
            "Sad", "Triste" -> "Tristeza"
            "Tired", "Cansado" -> "Cansaço"
            "Happy", "Feliz", "Bem", "Good" -> "Felicidade"
            "Rad", "Incrível", "Excellent", "Energetic" -> "Empolgação"
            "Grateful", "Grato" -> "Gratidão"
            "Calm", "Calmo" -> "Calma"
            "Pensive", "Pensativo" -> "Reflexão"
            else -> "Neutro"
        }
    }

    private fun calculateBestDay(notes: List<HumorNote>): String {
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
}