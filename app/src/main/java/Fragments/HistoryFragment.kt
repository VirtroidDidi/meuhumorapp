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
import com.example.apphumor.models.FilterTimeRange
import com.example.apphumor.viewmodel.HomeViewModel
import com.example.apphumor.viewmodel.HumorViewModelFactory

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: HomeViewModel
    private lateinit var adapter: HumorNoteAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val appContainer = (requireActivity().application as AppHumorApplication).container
        val factory = HumorViewModelFactory(appContainer.databaseRepository, appContainer.auth)
        viewModel = ViewModelProvider(this, factory)[HomeViewModel::class.java]

        setupRecyclerView()
        setupSearch()
        setupListeners()
        setupObservers()
    }

    private fun setupRecyclerView() {
        // MODO APENAS LEITURA (Histórico)
        // showEditButton = false (Sem lápis)
        // showSyncStatus = false (Sem ícone de nuvem)
        // onEditClick = null (Sem ação de clique)
        adapter = HumorNoteAdapter(
            showEditButton = false,
            showSyncStatus = false,
            onEditClick = null
        )

        // CORREÇÃO: ID do XML é rv_history, então aqui usamos rvHistory
        binding.rvHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHistory.adapter = adapter
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.updateSearchQuery(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupListeners() {
        binding.btnFilter.setOnClickListener {
            val currentFilters = viewModel.filterState.value ?: com.example.apphumor.models.FilterState()

            val filterSheet = FilterBottomSheet(
                currentState = currentFilters,
                onApply = { newState ->
                    viewModel.updateFilterState(newState)
                }
            )
            filterSheet.show(childFragmentManager, "FilterBottomSheet")
        }
    }

    private fun setupObservers() {
        viewModel.filteredHistoryNotes.observe(viewLifecycleOwner) { notes ->
            val hasNotes = notes.isNotEmpty()

            // CORREÇÃO: Usando rvHistory
            binding.rvHistory.isVisible = hasNotes
            binding.emptyState.isVisible = !hasNotes

            if (hasNotes) {
                adapter.submitList(notes)
            } else {
                val isFiltering = viewModel.filterState.value?.let {
                    it.query.isNotEmpty() || it.selectedHumors.isNotEmpty() || it.timeRange != FilterTimeRange.ALL_TIME
                } ?: false

                binding.tvEmptyMessage.text = if (isFiltering) {
                    getString(R.string.history_empty_search)
                } else {
                    getString(R.string.history_empty_list)
                }
            }
        }

        viewModel.filterState.observe(viewLifecycleOwner) { state ->
            val hasFilters = state.selectedHumors.isNotEmpty() || state.timeRange != FilterTimeRange.ALL_TIME
            binding.tvFilterStatus.isVisible = hasFilters

            if (hasFilters) {
                binding.btnFilter.setIconTintResource(R.color.teal_700)
            } else {
                binding.btnFilter.setIconTintResource(R.color.black)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}