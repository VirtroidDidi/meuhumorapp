    package com.example.apphumor.bottomsheet

    import android.os.Bundle
    import android.view.LayoutInflater
    import android.view.View
    import android.view.ViewGroup
    import com.example.apphumor.R
    import com.example.apphumor.databinding.LayoutFilterBottomSheetBinding
    import com.example.apphumor.models.FilterState
    import com.example.apphumor.models.FilterTimeRange
    import com.example.apphumor.models.SortOrder
    import com.google.android.material.bottomsheet.BottomSheetDialogFragment
    import com.google.android.material.chip.Chip
    import com.google.android.material.chip.ChipGroup

    class FilterBottomSheet(
        private val currentState: FilterState, // Recebe o estado atual para preencher a tela
        private val onApply: (FilterState) -> Unit // Callback para devolver o resultado
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

            // 1. Preencher a UI com o estado atual
            setupInitialState()

            // 2. Configurar Botão "Aplicar"
            binding.btnApplyFilters.setOnClickListener {
                val newState = collectState()
                onApply(newState)
                dismiss()
            }

            // 3. Configurar Botão "Limpar"
            binding.btnClearFilters.setOnClickListener {
                // Reseta para o padrão
                resetUI()
            }
        }

        private fun setupInitialState() {
            // Ordenação
            binding.rgSort.check(
                if (currentState.sortOrder == SortOrder.NEWEST) R.id.rb_newest else R.id.rb_oldest
            )

            // Período
            val dateChipId = when (currentState.timeRange) {
                FilterTimeRange.LAST_7_DAYS -> R.id.chip_7_days
                FilterTimeRange.LAST_30_DAYS -> R.id.chip_30_days
                else -> R.id.chip_all_time
            }
            binding.chipGroupDate.check(dateChipId)

            // Humores (Multisseleção)
            // Mapeamento dos textos/tags para os IDs dos Chips
            val humorMap = mapOf(
                "Excelente" to R.id.chip_humor_super_happy, // Ajuste conforme o texto que você usa no DB
                "Bem" to R.id.chip_humor_happy,
                "Neutro" to R.id.chip_humor_neutral,
                "Triste" to R.id.chip_humor_sad,
                "Irritado" to R.id.chip_humor_angry,
                "Calmo" to R.id.chip_humor_calm,
                "Energético" to R.id.chip_humor_energetic,
                // Adicione variantes em inglês se seu DB salva em inglês
                "Energetic" to R.id.chip_humor_energetic,
                "Sad" to R.id.chip_humor_sad,
                "Angry" to R.id.chip_humor_angry,
                "Calm" to R.id.chip_humor_calm,
                "Neutral" to R.id.chip_humor_neutral
            )

            currentState.selectedHumors.forEach { humor ->
                val chipId = humorMap[humor] ?: humorMap[humor.replaceFirstChar { it.titlecase() }]
                chipId?.let { id ->
                    binding.chipGroupHumor.findViewById<Chip>(id)?.isChecked = true
                }
            }

            // Switch
            binding.switchOnlyNotes.isChecked = currentState.onlyWithNotes
        }

        private fun collectState(): FilterState {
            // Ler Ordenação
            val sortOrder = if (binding.rbNewest.isChecked) SortOrder.NEWEST else SortOrder.OLDEST

            // Ler Período
            val timeRange = when (binding.chipGroupDate.checkedChipId) {
                R.id.chip_7_days -> FilterTimeRange.LAST_7_DAYS
                R.id.chip_30_days -> FilterTimeRange.LAST_30_DAYS
                else -> FilterTimeRange.ALL_TIME
            }

            // Ler Humores Selecionados
            val selectedHumors = mutableSetOf<String>()
            val checkedHumorIds = binding.chipGroupHumor.checkedChipIds

            // Mapeia ID de volta para String (Importante: Deve bater com o que é salvo no Banco!)
            checkedHumorIds.forEach { id ->
                when(id) {
                    R.id.chip_humor_super_happy -> selectedHumors.add("Excelente")
                    R.id.chip_humor_happy -> selectedHumors.add("Bem") // Ou "Happy"
                    R.id.chip_humor_neutral -> selectedHumors.add("Neutro") // Ou "Neutral"
                    R.id.chip_humor_sad -> selectedHumors.add("Sad") // Ou "Triste" (Verificar como seu DB salva)
                    R.id.chip_humor_angry -> selectedHumors.add("Angry") // Ou "Irritado"
                    R.id.chip_humor_calm -> selectedHumors.add("Calm") // Ou "Calmo"
                    R.id.chip_humor_energetic -> selectedHumors.add("Energetic") // Ou "Energético"
                }
            }

            // Ler Switch
            val onlyNotes = binding.switchOnlyNotes.isChecked

            // Mantém a query de texto original (não editada aqui)
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

        override fun onDestroyView() {
            super.onDestroyView()
            _binding = null
        }
    }