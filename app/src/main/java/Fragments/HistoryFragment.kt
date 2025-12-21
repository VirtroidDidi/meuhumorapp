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
import com.example.apphumor.viewmodel.HomeViewModel
import com.example.apphumor.viewmodel.HomeViewModelFactory

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

        viewModel = ViewModelProvider(
            this,
            HomeViewModelFactory(
                DependencyProvider.auth,
                DependencyProvider.databaseRepository
            )
        ).get(HomeViewModel::class.java)

        setupRecyclerView()
        setupSearchAndFilters()
        setupObservers()
    }

    private fun setupRecyclerView() {
        // ALTERAÇÃO AQUI: Passamos showSyncStatus = false para esconder o check na tela de histórico
        adapter = HumorNoteAdapter(
            showEditButton = false,
            showSyncStatus = false
        )

        binding.recyclerViewAllNotes.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@HistoryFragment.adapter
        }
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
                binding.btnFilter.setIconTintResource(R.color.teal_700)
            } else {
                binding.tvFilterStatus.isVisible = false
                binding.btnFilter.setIconTintResource(R.color.black)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}