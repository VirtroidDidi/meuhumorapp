package com.example.apphumor.models

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes

/**
 * Representa um insight calculado a partir dos dados do usuário.
 *
 * @param rotulo O título do insight (ex: "Humor Mais Comum").
 * @param valor O valor do insight (ex: "Feliz" ou "15 dias").
 * @param iconResId O ID do recurso drawable para o ícone.
 * @param backgroundColorResId O ID do recurso color para a cor de fundo do ícone.
 */
data class Insight(
    val rotulo: String,
    val valor: String,
    @DrawableRes val iconResId: Int,
    @ColorRes val backgroundColorResId: Int
)