package com.example.apphumor.models

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import com.example.apphumor.R

enum class HumorType(
    val key: String, // O valor salvo no banco (Ex: "Rad")
    val legacyKeys: List<String> = emptyList(), // Para suportar dados antigos
    @IdRes val chipId: Int, // O ID do Chip na tela de cadastro
    @StringRes val labelRes: Int, // O texto traduzido
    @DrawableRes val iconRes: Int, // O ícone estático
    @DrawableRes val animRes: Int, // A animação (Lottie/Drawable)
    @ColorRes val colorRes: Int, // Cor do ícone/texto
    @ColorRes val backgroundTint: Int // Cor de fundo do card
) {
    // Alta Energia +
    RAD(
        key = "Rad",
        legacyKeys = listOf("Incrível", "Excelente", "Excellent", "Energetic", "Energético"),
        chipId = R.id.chip_rad,
        labelRes = R.string.humor_rad,
        iconRes = R.drawable.ic_mood_rad,
        animRes = R.drawable.ic_mood_rad_anim,
        colorRes = R.color.mood_rad,
        backgroundTint = R.color.insight_rad_bg
    ),
    HAPPY(
        key = "Happy",
        legacyKeys = listOf("Feliz", "Bem", "Good"),
        chipId = R.id.chip_happy,
        labelRes = R.string.humor_happy,
        iconRes = R.drawable.ic_mood_happy,
        animRes = R.drawable.ic_mood_happy_anim,
        colorRes = R.color.mood_happy,
        backgroundTint = R.color.insight_happy_bg
    ),

    // Baixa Energia +
    GRATEFUL(
        key = "Grateful",
        legacyKeys = listOf("Grato"),
        chipId = R.id.chip_grateful,
        labelRes = R.string.humor_grateful,
        iconRes = R.drawable.ic_mood_grateful,
        animRes = R.drawable.ic_mood_grateful_anim,
        colorRes = R.color.mood_grateful,
        backgroundTint = R.color.insight_grateful_bg
    ),
    CALM(
        key = "Calm",
        legacyKeys = listOf("Calmo"),
        chipId = R.id.chip_calm,
        labelRes = R.string.humor_calm,
        iconRes = R.drawable.ic_mood_calm,
        animRes = R.drawable.ic_mood_calm_anim,
        colorRes = R.color.mood_calm,
        backgroundTint = R.color.insight_calm_bg
    ),

    // Neutros
    NEUTRAL(
        key = "Neutral",
        legacyKeys = listOf("Neutro"),
        chipId = R.id.chip_neutral,
        labelRes = R.string.humor_neutral,
        iconRes = R.drawable.ic_mood_neutral,
        animRes = R.drawable.ic_mood_neutral_anim,
        colorRes = R.color.mood_neutral,
        backgroundTint = R.color.insight_neutral_bg
    ),
    PENSIVE(
        key = "Pensive",
        legacyKeys = listOf("Pensativo"),
        chipId = R.id.chip_pensive,
        labelRes = R.string.humor_pensive,
        iconRes = R.drawable.ic_mood_pensive,
        animRes = R.drawable.ic_mood_pensive_anim,
        colorRes = R.color.mood_pensive,
        backgroundTint = R.color.insight_pensive_bg
    ),

    // Baixa Energia -
    TIRED(
        key = "Tired",
        legacyKeys = listOf("Cansado"),
        chipId = R.id.chip_tired,
        labelRes = R.string.humor_tired,
        iconRes = R.drawable.ic_mood_tired,
        animRes = R.drawable.ic_mood_tired_anim,
        colorRes = R.color.mood_tired,
        backgroundTint = R.color.insight_tired_bg
    ),
    SAD(
        key = "Sad",
        legacyKeys = listOf("Triste"),
        chipId = R.id.chip_sad,
        labelRes = R.string.humor_sad,
        iconRes = R.drawable.ic_mood_sad,
        animRes = R.drawable.ic_mood_sad_anim,
        colorRes = R.color.mood_sad,
        backgroundTint = R.color.insight_sad_bg
    ),

    // Alta Energia -
    ANXIOUS(
        key = "Anxious",
        legacyKeys = listOf("Ansioso"),
        chipId = R.id.chip_anxious,
        labelRes = R.string.humor_anxious,
        iconRes = R.drawable.ic_mood_anxious,
        animRes = R.drawable.ic_mood_anxious_anim,
        colorRes = R.color.mood_anxious,
        backgroundTint = R.color.insight_anxious_bg
    ),
    ANGRY(
        key = "Angry",
        legacyKeys = listOf("Irritado"),
        chipId = R.id.chip_angry,
        labelRes = R.string.humor_angry,
        iconRes = R.drawable.ic_mood_angry,
        animRes = R.drawable.ic_mood_angry_anim,
        colorRes = R.color.mood_angry,
        backgroundTint = R.color.insight_angry_bg
    );

    companion object {
        // Encontra o HumorType pela String salva no banco (incluindo legados)
        fun fromKey(key: String?): HumorType {
            if (key.isNullOrBlank()) return NEUTRAL

            return entries.find { type ->
                type.key.equals(key, ignoreCase = true) ||
                        type.legacyKeys.any { it.equals(key, ignoreCase = true) }
            } ?: NEUTRAL
        }

        // Encontra o HumorType pelo ID do Chip selecionado na UI
        fun fromChipId(@IdRes chipId: Int): HumorType? {
            return entries.find { it.chipId == chipId }
        }
    }
}