package com.example.apphumor.utils

import com.example.apphumor.R

/**
 * Data class para agrupar todos os recursos visuais de um humor.
 * Isso evita ter que decidir qual cor usar em cada lugar: aqui entregamos tudo.
 */
data class MoodStyle(
    val iconRes: Int,
    val contentColorRes: Int,    // Cor para tintas/textos (Ex: R.color.mood_rad)
    val backgroundColorRes: Int, // Cor para fundos (Ex: R.color.insight_rad_bg)
    val labelRes: Int            // ID da String traduzida
)

object HumorUtils {

    // Função única que resolve o estilo baseado na String do banco de dados
    fun getMoodStyle(moodName: String?): MoodStyle {
        // Normaliza para garantir que "Rad" e "rad" funcionem igual
        val moodKey = moodName ?: "Neutral"

        return when (moodKey) {
            // Alta Energia +
            "Rad", "Incrível", "Excelente", "Excellent", "Energetic", "Energético" ->
                MoodStyle(R.drawable.ic_mood_rad, R.color.mood_rad, R.color.insight_rad_bg, R.string.humor_rad)

            "Happy", "Feliz", "Bem", "Good" ->
                MoodStyle(R.drawable.ic_mood_happy, R.color.mood_happy, R.color.insight_happy_bg, R.string.humor_happy)

            // Baixa Energia +
            "Grateful", "Grato" ->
                MoodStyle(R.drawable.ic_mood_grateful, R.color.mood_grateful, R.color.insight_grateful_bg, R.string.humor_grateful)

            "Calm", "Calmo" ->
                MoodStyle(R.drawable.ic_mood_calm, R.color.mood_calm, R.color.insight_calm_bg, R.string.humor_calm)

            // Neutros
            "Neutral", "Neutro" ->
                MoodStyle(R.drawable.ic_mood_neutral, R.color.mood_neutral, R.color.insight_neutral_bg, R.string.humor_neutral)

            "Pensive", "Pensativo" ->
                MoodStyle(R.drawable.ic_mood_pensive, R.color.mood_pensive, R.color.insight_pensive_bg, R.string.humor_pensive)

            // Baixa Energia -
            "Tired", "Cansado" ->
                MoodStyle(R.drawable.ic_mood_tired, R.color.mood_tired, R.color.insight_tired_bg, R.string.humor_tired)

            "Sad", "Triste" ->
                MoodStyle(R.drawable.ic_mood_sad, R.color.mood_sad, R.color.insight_sad_bg, R.string.humor_sad)

            // Alta Energia -
            "Anxious", "Ansioso" ->
                MoodStyle(R.drawable.ic_mood_anxious, R.color.mood_anxious, R.color.insight_anxious_bg, R.string.humor_anxious)

            "Angry", "Irritado" ->
                MoodStyle(R.drawable.ic_mood_angry, R.color.mood_angry, R.color.insight_angry_bg, R.string.humor_angry)

            // Fallback
            else ->
                MoodStyle(R.drawable.ic_mood_neutral, R.color.mood_neutral, R.color.insight_neutral_bg, R.string.humor_neutral)
        }
    }
}