package com.example.apphumor.bottomsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.apphumor.R
import com.example.apphumor.databinding.LayoutFilterBottomSheetBinding
import com.example.apphumor.models.FilterState
import com.example.apphumor.models.FilterTimeRange
import com.example.apphumor.models.HumorType // [NOVO] Import do Enum
import com.example.apphumor.models.SortOrder
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip

class FilterBottomSheet(
    private val currentState: FilterState,
    private val onApply: (FilterState) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: LayoutFilterBottomSheetBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = LayoutFilterBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupInitialState()

        binding.btnApplyFilters.setOnClickListener {
            val newState = collectState()
            onApply(newState)
            dismiss()
        }

        binding.btnClearFilters.setOnClickListener {
            resetUI()
        }
    }

    private fun setupInitialState() {
        // 1. Ordenação
        binding.rgSort.check(
            if (currentState.sortOrder == SortOrder.NEWEST) R.id.rb_newest else R.id.rb_oldest
        )

        // 2. Período
        val dateChipId = when (currentState.timeRange) {
            FilterTimeRange.LAST_7_DAYS -> R.id.chip_7_days
            FilterTimeRange.LAST_30_DAYS -> R.id.chip_30_days
            else -> R.id.chip_all_time
        }
        binding.chipGroupDate.check(dateChipId)

        // 3. Humores (Usando HumorType para marcar os chips)
        // Percorremos os humores salvos no estado (Strings)
        currentState.selectedHumors.forEach { humorKey ->
            // Convertemos String -> Enum -> ChipId do Filtro
            val type = HumorType.fromKey(humorKey)
            val chipId = getFilterChipIdFromType(type)

            if (chipId != View.NO_ID) {
                binding.chipGroupHumor.findViewById<Chip>(chipId)?.isChecked = true
            }
        }

        // 4. Switch
        binding.switchOnlyNotes.isChecked = currentState.onlyWithNotes
    }

    private fun collectState(): FilterState {
        // Ordenação
        val sortOrder = if (binding.rbNewest.isChecked) SortOrder.NEWEST else SortOrder.OLDEST

        // Período
        val timeRange = when (binding.chipGroupDate.checkedChipId) {
            R.id.chip_7_days -> FilterTimeRange.LAST_7_DAYS
            R.id.chip_30_days -> FilterTimeRange.LAST_30_DAYS
            else -> FilterTimeRange.ALL_TIME
        }

        // Humores
        val selectedHumors = mutableSetOf<String>()
        val checkedHumorIds = binding.chipGroupHumor.checkedChipIds

        checkedHumorIds.forEach { id ->
            // Convertemos ChipId do Filtro -> Enum -> String (Key)
            val type = getHumorTypeFromFilterChip(id)
            if (type != null) {
                selectedHumors.add(type.key)
            }
        }

        // Switch
        val onlyNotes = binding.switchOnlyNotes.isChecked

        return currentState.copy(
            sortOrder = sortOrder,
            timeRange = timeRange,
            selectedHumors = selectedHumors,
            onlyWithNotes = onlyNotes
        )
    }

    private fun resetUI() {
        binding.rbNewest.isChecked = true
        binding.chipAllTime.isChecked = true
        binding.chipGroupHumor.clearCheck()
        binding.switchOnlyNotes.isChecked = false
    }

    // --- MAPAS DE CONVERSÃO (Filtro <-> Enum) ---

    private fun getHumorTypeFromFilterChip(chipId: Int): HumorType? {
        return when (chipId) {
            R.id.chip_humor_super_happy -> HumorType.RAD
            R.id.chip_humor_happy -> HumorType.HAPPY
            R.id.chip_humor_neutral -> HumorType.NEUTRAL
            R.id.chip_humor_sad -> HumorType.SAD
            R.id.chip_humor_angry -> HumorType.ANGRY
            R.id.chip_humor_calm -> HumorType.CALM
            R.id.chip_humor_energetic -> HumorType.RAD // Consolida "Energetic" em RAD
            else -> null
        }
    }

    private fun getFilterChipIdFromType(type: HumorType): Int {
        return when (type) {
            HumorType.RAD -> R.id.chip_humor_super_happy // Ou Energetic, escolhemos o principal
            HumorType.HAPPY -> R.id.chip_humor_happy
            HumorType.NEUTRAL -> R.id.chip_humor_neutral
            HumorType.SAD -> R.id.chip_humor_sad
            HumorType.ANGRY -> R.id.chip_humor_angry
            HumorType.CALM -> R.id.chip_humor_calm
            // Tipos que não têm chip específico no filtro visual atual caem aqui.
            // Se quiser adicionar chips para GRATEFUL, PENSIVE, etc, precisa editar o XML do filtro.
            else -> View.NO_ID
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}