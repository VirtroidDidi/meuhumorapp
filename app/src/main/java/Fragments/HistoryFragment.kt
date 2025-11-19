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
import com.example.apphumor.viewmodel.HomeViewModel // Usaremos HomeViewModel

/**
 * [HistoryFragment] (Antigo FragmentTelaC)
 * Exibe o histórico completo de notas.
 * Observa o LiveData de todas as notas mantido pelo HomeViewModel.
 */
class HistoryFragment : Fragment() {
    private lateinit var binding: FragmentTelaCBinding

    // Agora usa HomeViewModel (que contém o LiveData de todas as notas)
    private val viewModel: HomeViewModel by lazy { ViewModelProvider(this).get(HomeViewModel::class.java) }

    private lateinit var adapter: HumorNoteAdapter
    private val TAG = "HistoryFragment"

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
                // Ordena as notas pela data mais recente (opcional, mas recomendado para histórico)
                val sortedNotes = notes.sortedByDescending { it.data?.get("time") as? Long ?: 0L }

                binding.recyclerViewAllNotes.visibility = View.VISIBLE
                binding.emptyState.visibility = View.GONE
                adapter.submitList(sortedNotes)
                Log.d(TAG, "Histórico atualizado com ${sortedNotes.size} notas.")
            } else {
                binding.recyclerViewAllNotes.visibility = View.GONE
                binding.emptyState.visibility = View.VISIBLE
                adapter.submitList(emptyList())
                Log.d(TAG, "Histórico vazio.")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Não é mais necessário chamar loadAllNotes() ou qualquer outra função de busca.
        // O LiveData no ViewModel cuida da atualização.
    }
}