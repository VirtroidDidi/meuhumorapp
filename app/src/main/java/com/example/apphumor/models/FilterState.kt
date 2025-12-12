package com.example.apphumor.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// Define a ordem
enum class SortOrder {
    NEWEST, OLDEST
}

// Define o período (diferente do TimeRange do Insights para não misturar lógicas)
enum class FilterTimeRange {
    ALL_TIME,
    LAST_7_DAYS,
    LAST_30_DAYS
}

// O objeto que guarda toda a configuração do filtro
@Parcelize
data class FilterState(
    val query: String = "", // Texto da busca
    val sortOrder: SortOrder = SortOrder.NEWEST,
    val timeRange: FilterTimeRange = FilterTimeRange.ALL_TIME,
    val selectedHumors: Set<String> = emptySet(), // Ex: {"Happy", "Calm"}
    val onlyWithNotes: Boolean = false
) : Parcelable