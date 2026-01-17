package com.example.apphumor

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.apphumor.adapter.HumorNoteAdapter
import com.example.apphumor.bottomsheet.FilterBottomSheet
import com.example.apphumor.databinding.FragmentHistoryBinding
import com.example.apphumor.di.DependencyProvider
import com.example.apphumor.models.FilterState
import com.example.apphumor.models.FilterTimeRange
import com.example.apphumor.viewmodel.AppViewModelFactory // <--- IMPORTANTE: Nova Factory
import com.example.apphumor.viewmodel.HomeViewModel
import androidx.recyclerview.widget.ItemTouchHelper
import com.google.android.material.snackbar.Snackbar
import com.example.apphumor.utils.SwipeToDeleteCallback
import android.content.res.ColorStateList
import com.google.android.material.color.MaterialColors
// Removido: import com.example.apphumor.viewmodel.HomeViewModelFactory (Não existe mais)

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: HomeViewModel
    private lateinit var adapter: HumorNoteAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- CORREÇÃO: Usando a Factory Global ---
        val factory = AppViewModelFactory(
            DependencyProvider.auth,
            DependencyProvider.databaseRepository
        )

        viewModel = ViewModelProvider(this, factory)[HomeViewModel::class.java]
        // -----------------------------------------

        setupRecyclerView()
        setupSearchAndFilters()
        setupObservers()
    }

    private fun setupRecyclerView() {
        adapter = HumorNoteAdapter(
            showEditButton = false,
            showSyncStatus = false
        )

        binding.recyclerViewAllNotes.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@HistoryFragment.adapter
        }
        setupSwipeToDelete()
    }

    private fun setupSearchAndFilters() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                viewModel.updateSearchQuery(s.toString())
            }
        })

        binding.btnFilter.setOnClickListener {
            val currentState = viewModel.filterState.value ?: FilterState()
            val bottomSheet = FilterBottomSheet(currentState) { newState ->
                viewModel.updateFilterState(newState)
            }
            bottomSheet.show(parentFragmentManager, "FilterBottomSheet")
        }
    }

    private fun setupObservers() {
        viewModel.filteredHistoryNotes.observe(viewLifecycleOwner) { notes ->
            adapter.submitList(notes)

            binding.recyclerViewAllNotes.isVisible = notes.isNotEmpty()
            binding.emptyState.isVisible = notes.isEmpty()

            if (notes.isEmpty()) {
                val isFiltering = viewModel.filterState.value?.let {
                    it.query.isNotEmpty() || it.timeRange != FilterTimeRange.ALL_TIME
                } ?: false

                binding.tvEmptyMessage.text = if (isFiltering) {
                    getString(R.string.history_empty_search)
                } else {
                    getString(R.string.history_empty_list)
                }
            }
        }

        viewModel.filterState.observe(viewLifecycleOwner) { state ->
            val activeFilters = mutableListOf<String>()

            if (state.timeRange == FilterTimeRange.LAST_7_DAYS) activeFilters.add(getString(R.string.period_7_days))
            if (state.timeRange == FilterTimeRange.LAST_30_DAYS) activeFilters.add(getString(R.string.period_30_days))
            if (state.selectedHumors.isNotEmpty()) activeFilters.add("${state.selectedHumors.size} humores")
            if (state.onlyWithNotes) activeFilters.add(getString(R.string.filter_only_notes))

            if (activeFilters.isNotEmpty()) {
                binding.tvFilterStatus.isVisible = true
                binding.tvFilterStatus.text = getString(R.string.filter_active_format, activeFilters.joinToString(", "))

                // CORREÇÃO: Usa a cor PRIMÁRIA do tema (Roxo no Light / Lilás no Dark)
                val primaryColor = MaterialColors.getColor(binding.btnFilter, com.google.android.material.R.attr.colorPrimary)
                binding.btnFilter.iconTint = ColorStateList.valueOf(primaryColor)
            } else {
                binding.tvFilterStatus.isVisible = false

                // CORREÇÃO: Usa a cor do TEXTO/ÍCONE padrão (Preto no Light / Branco no Dark)
                val onSurfaceColor = MaterialColors.getColor(binding.btnFilter, com.google.android.material.R.attr.colorOnSurfaceVariant)
                binding.btnFilter.iconTint = ColorStateList.valueOf(onSurfaceColor)
            }
        }
        viewModel.totalNotesCount.observe(viewLifecycleOwner) { count ->
            val text = "Você já registrou <b>$count momentos</b>."
            // Html.fromHtml faz o <b></b> virar negrito real
            binding.tvHistoryStats.text = android.text.Html.fromHtml(text, android.text.Html.FROM_HTML_MODE_COMPACT)
        }
    }
    private fun setupSwipeToDelete() {
        val swipeHandler = SwipeToDeleteCallback(requireContext()) { position ->
            val currentList = adapter.currentList

            if (position >= 0 && position < currentList.size) {
                val noteToDelete = currentList[position]

                // 1. Deleta usando o mesmo ViewModel (que já tem a lógica pronta)
                viewModel.deleteNote(noteToDelete)

                // 2. Mostra o Snackbar de desfazer
                showUndoSnackbar()
            }
        }

        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        // Atenção ao ID do RecyclerView aqui, que é diferente da Home
        itemTouchHelper.attachToRecyclerView(binding.recyclerViewAllNotes)
    }

    private fun showUndoSnackbar() {
        Snackbar.make(binding.root, "Registro excluído", Snackbar.LENGTH_LONG)
            .setAction("DESFAZER") {
                viewModel.undoDelete()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}