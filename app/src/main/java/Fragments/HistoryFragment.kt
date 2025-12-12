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
import com.example.apphumor.databinding.FragmentTelaCBinding
import com.example.apphumor.di.DependencyProvider
import com.example.apphumor.models.FilterState
import com.example.apphumor.models.FilterTimeRange
import com.example.apphumor.viewmodel.HomeViewModel
import com.example.apphumor.viewmodel.HomeViewModelFactory

class HistoryFragment : Fragment() {
    private var _binding: FragmentTelaCBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: HomeViewModel
    private lateinit var adapter: HumorNoteAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTelaCBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Inicializa ViewModel
        viewModel = ViewModelProvider(
            this, // Compartilhando o ViewModel com a Activity se quisesse, mas aqui usamos 'this' para escopo local ou 'requireActivity()' se quiser compartilhar dados com a Home
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
        adapter = HumorNoteAdapter(showEditButton = false)
        binding.recyclerViewAllNotes.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@HistoryFragment.adapter
        }
    }

    private fun setupSearchAndFilters() {
        // A. Listener da Barra de Busca (Digitação em tempo real)
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                // Atualiza o ViewModel a cada letra digitada
                viewModel.updateSearchQuery(s.toString())
            }
        })

        // B. Listener do Botão de Filtro
        binding.btnFilter.setOnClickListener {
            // Pega o estado atual ou cria um novo se for nulo
            val currentState = viewModel.filterState.value ?: FilterState()

            // Abre o BottomSheet passando o estado atual
            val bottomSheet = FilterBottomSheet(currentState) { newState ->
                // Callback: Quando o usuário clica em "Aplicar", recebemos o newState aqui
                viewModel.updateFilterState(newState)
            }
            bottomSheet.show(parentFragmentManager, "FilterBottomSheet")
        }
    }

    private fun setupObservers() {
        // 1. Observa a LISTA FILTRADA (Não mais a lista bruta)
        viewModel.filteredHistoryNotes.observe(viewLifecycleOwner) { notes ->
            adapter.submitList(notes)

            // Controle de Visibilidade (Lista vs Estado Vazio)
            binding.recyclerViewAllNotes.isVisible = notes.isNotEmpty()
            binding.emptyState.isVisible = notes.isEmpty()

            // Atualiza texto de "vazio" dependendo se tem filtro ou não
            if (notes.isEmpty()) {
                val isFiltering = viewModel.filterState.value?.let {
                    it.query.isNotEmpty() || it.timeRange != FilterTimeRange.ALL_TIME
                } ?: false

                binding.tvEmptyMessage.text = if (isFiltering) {
                    "Nenhum resultado para sua busca."
                } else {
                    "Seu histórico está vazio."
                }
            }
        }

        // 2. Observa o ESTADO DO FILTRO para dar feedback visual
        viewModel.filterState.observe(viewLifecycleOwner) { state ->
            // Atualiza o texto de feedback (aquela linha pequena abaixo da busca)
            val activeFilters = mutableListOf<String>()

            if (state.timeRange == FilterTimeRange.LAST_7_DAYS) activeFilters.add("7 dias")
            if (state.timeRange == FilterTimeRange.LAST_30_DAYS) activeFilters.add("30 dias")
            if (state.selectedHumors.isNotEmpty()) activeFilters.add("${state.selectedHumors.size} humores")
            if (state.onlyWithNotes) activeFilters.add("Com anotação")

            if (activeFilters.isNotEmpty()) {
                binding.tvFilterStatus.isVisible = true
                binding.tvFilterStatus.text = "Filtros ativos: ${activeFilters.joinToString(", ")}"
                // Opcional: Mudar cor do ícone de filtro para indicar atividade
                binding.btnFilter.setIconTintResource(R.color.teal_700)
            } else {
                binding.tvFilterStatus.isVisible = false
                binding.btnFilter.setIconTintResource(R.color.black) // Ou cor padrão
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}