// ARQUIVO: app/src/main/java/com/example/apphumor/HomeFragment.kt

package com.example.apphumor

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.apphumor.adapter.HumorNoteAdapter
import com.example.apphumor.databinding.FragmentTelaABinding
import com.example.apphumor.di.DependencyProvider
import com.example.apphumor.models.HumorNote
import com.example.apphumor.viewmodel.HomeViewModel
import com.example.apphumor.viewmodel.HomeViewModelFactory
import java.util.*

/**
 * [HomeFragment]
 * Exibe APENAS as notas registradas no dia de hoje e o progresso.
 */
class HomeFragment : Fragment() {
    private lateinit var binding: FragmentTelaABinding
    private lateinit var viewModel: HomeViewModel
    private lateinit var adapter: HumorNoteAdapter
    private val TAG = "HomeFragment"

    companion object {
        const val ADD_NOTE_REQUEST_CODE = 1001
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTelaABinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicialização com Factory
        viewModel = ViewModelProvider(
            this,
            HomeViewModelFactory(
                DependencyProvider.auth,
                DependencyProvider.databaseRepository
            )
        ).get(HomeViewModel::class.java)

        setupRecyclerView()
        setupButton()
        setupObservers()
    }

    private fun setupRecyclerView() {
        // showEditButton = true garante que o usuário possa editar a nota
        adapter = HumorNoteAdapter(showEditButton = true, onEditClick = { note ->
            val intent = Intent(requireActivity(), AddHumorActivity::class.java).apply {
                putExtra("EDIT_NOTE", note)
            }
            startActivityForResult(intent, ADD_NOTE_REQUEST_CODE)
        })

        binding.recyclerViewNotes.apply {
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
            adapter = this@HomeFragment.adapter
        }
    }

    private fun setupButton() {
        binding.emptyState.findViewById<View>(R.id.btn_add_record).setOnClickListener {
            val intent = Intent(requireActivity(), AddHumorActivity::class.java)
            startActivityForResult(intent, ADD_NOTE_REQUEST_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ADD_NOTE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Log.d(TAG, "Nota salva. UI será atualizada via LiveData.")
        }
    }

    private fun setupObservers() {
        // Observa APENAS as notas de HOJE
        viewModel.todayNotes.observe(viewLifecycleOwner) { notes ->
            if (notes.isNotEmpty()) {
                binding.recyclerViewNotes.visibility = View.VISIBLE
                binding.emptyState.visibility = View.GONE
                // O Adapter recebe apenas as notas filtradas de hoje
                adapter.submitList(notes)
            } else {
                showEmptyState()
                adapter.submitList(emptyList())
            }
        }

        viewModel.dailyProgress.observe(viewLifecycleOwner) { (sequence, lastRecordedTimestamp) ->
            updateProgressCard(sequence, lastRecordedTimestamp)
        }
    }

    private fun updateProgressCard(sequence: Int, lastRecordedTimestamp: Long?) {
        binding.progressCard.tvSequenceDays.text = sequence.toString()
        binding.progressCard.progressBar.progress = sequence

        val maxDays = binding.progressCard.progressBar.max
        var descriptionText = when {
            sequence >= maxDays -> "Parabéns! Sequência semanal completa!"
            sequence > 0 -> "Sequência de $sequence dias consecutivos!"
            else -> "Sua sequência diária de notas."
        }

        if (sequence == 0 && lastRecordedTimestamp != null) {
            descriptionText = "Sequência Reiniciada. Comece hoje!"
        }
        binding.progressCard.tvSequenceDescription.text = descriptionText
    }

    private fun showEmptyState() {
        binding.recyclerViewNotes.visibility = View.GONE
        binding.emptyState.visibility = View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
    }
}