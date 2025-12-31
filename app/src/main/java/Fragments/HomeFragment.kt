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
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.apphumor.adapter.HumorNoteAdapter
import com.example.apphumor.databinding.FragmentHomeBinding
import com.example.apphumor.di.DependencyProvider
import com.example.apphumor.utils.SwipeToDeleteCallback // Import da nossa nova classe
import com.example.apphumor.viewmodel.AppViewModelFactory
import com.example.apphumor.viewmodel.HomeViewModel
import com.google.android.material.snackbar.Snackbar


class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

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
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val factory = AppViewModelFactory(
            DependencyProvider.auth,
            DependencyProvider.databaseRepository
        )
        viewModel = ViewModelProvider(this, factory)[HomeViewModel::class.java]

        setupRecyclerView()
        setupButton()
        setupObservers()
    }

    private fun setupRecyclerView() {
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

        // Chamada limpa e corrigida!
        setupSwipeToDelete()
    }

    private fun setupSwipeToDelete() {
        // Usamos a classe utilitária para limpar o código e corrigir os imports de gráficos
        val swipeHandler = SwipeToDeleteCallback(requireContext()) { position ->
            val currentList = adapter.currentList

            // Verificação de segurança
            if (position >= 0 && position < currentList.size) {
                val noteToDelete = currentList[position]

                // 1. Lógica de negócio (ViewModel)
                viewModel.deleteNote(noteToDelete)

                // 2. Feedback Visual
                showUndoSnackbar()
            }
        }

        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(binding.recyclerViewNotes)
    }

    private fun showUndoSnackbar() {
        Snackbar.make(binding.root, "Registro excluído", Snackbar.LENGTH_LONG)
            .setAction("DESFAZER") {
                viewModel.undoDelete()
            }
            .show()
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
            Log.d(TAG, "Nota salva com sucesso.")
        }
    }

    private fun setupObservers() {
        viewModel.todayNotes.observe(viewLifecycleOwner) { notes ->
            if (notes.isNotEmpty()) {
                binding.recyclerViewNotes.visibility = View.VISIBLE
                binding.emptyState.visibility = View.GONE
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
            sequence >= maxDays -> getString(R.string.sequence_congrats)
            sequence > 0 -> getString(R.string.sequence_days, sequence)
            else -> getString(R.string.sequence_default)
        }

        if (sequence == 0 && lastRecordedTimestamp != null) {
            descriptionText = getString(R.string.sequence_reset)
        }
        binding.progressCard.tvSequenceDescription.text = descriptionText
    }

    private fun showEmptyState() {
        binding.recyclerViewNotes.visibility = View.GONE
        binding.emptyState.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}