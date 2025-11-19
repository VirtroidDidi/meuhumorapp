// ARQUIVO: app/src/main/java/com/example/apphumor/HistoryFragment.kt

package com.example.apphumor

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.apphumor.adapter.HumorNoteAdapter
import com.example.apphumor.databinding.FragmentTelaCBinding
import com.example.apphumor.di.DependencyProvider
import com.example.apphumor.viewmodel.HomeViewModel // Usaremos HomeViewModel
import com.example.apphumor.viewmodel.HomeViewModelFactory // NOVO: Importa a Factory

/**
 * [HistoryFragment] (Antigo FragmentTelaC)
 * Exibe o histórico completo de notas.
 * Observa o LiveData de todas as notas mantido pelo HomeViewModel.
 */
class HistoryFragment : Fragment() {
    private lateinit var binding: FragmentTelaCBinding

    // Usa lateinit var para ser inicializada no onViewCreated
    private lateinit var viewModel: HomeViewModel

    private lateinit var adapter: HumorNoteAdapter
    // CORREÇÃO: Variável privada deve ser 'tag' minúscula (convenção de Kotlin)
    private val tag = "HistoryFragment"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTelaCBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. INICIALIZAÇÃO CORRETA DO VIEWMODEL COM FACTORY
        viewModel = ViewModelProvider(
            this,
            HomeViewModelFactory(
                DependencyProvider.auth,                // Obtém a dependência centralizada
                DependencyProvider.databaseRepository   // Obtém a dependência centralizada
            )
        ).get(HomeViewModel::class.java)
        // -------------------------------------------------------------

        setupRecyclerView()
        setupObservers() // Configura a observação dos dados
    }

    private fun setupRecyclerView() {
        // O adaptador é inicializado para NÃO mostrar o botão de edição
        adapter = HumorNoteAdapter(showEditButton = false)

        binding.recyclerViewAllNotes.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@HistoryFragment.adapter
        }
    }

    /**
     * Configura a observação do LiveData de TODAS as notas no HomeViewModel.
     */
    private fun setupObservers() {
        // Observa o LiveData de TODAS as notas (allNotes)
        viewModel.allNotes.observe(viewLifecycleOwner) { notes ->
            if (notes.isNotEmpty()) {
                // CORREÇÃO CRÍTICA: Acessa a nova propriedade 'timestamp' do modelo HumorNote.
                val sortedNotes = notes.sortedByDescending { it.timestamp }

                binding.recyclerViewAllNotes.visibility = View.VISIBLE
                binding.emptyState.visibility = View.GONE
                adapter.submitList(sortedNotes)
                // CORREÇÃO: Usando a variável 'tag' (minúscula)
                Log.d(tag, "Histórico atualizado com ${sortedNotes.size} notas.")
            } else {
                binding.recyclerViewAllNotes.visibility = View.GONE
                binding.emptyState.visibility = View.VISIBLE
                adapter.submitList(emptyList())
                // CORREÇÃO: Usando a variável 'tag' (minúscula)
                Log.d(tag, "Histórico vazio.")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // O LiveData no ViewModel cuida da atualização.
    }

    // CORREÇÃO: Adicionando onDestroyView para evitar Memory Leaks com ViewBinding
    override fun onDestroyView() {
        super.onDestroyView()
        // O _binding não existe aqui, mas é uma boa prática adicionar
        // _binding = null se estiver usando FragmentTelaCBinding?
    }
}