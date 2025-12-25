package com.example.apphumor

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.apphumor.adapter.HumorNoteAdapter
import com.example.apphumor.databinding.FragmentHomeBinding
import com.example.apphumor.viewmodel.HomeViewModel
import com.example.apphumor.viewmodel.HumorViewModelFactory

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: HomeViewModel
    private lateinit var adapter: HumorNoteAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val appContainer = (requireActivity().application as AppHumorApplication).container
        val factory = HumorViewModelFactory(appContainer.databaseRepository, appContainer.auth)
        viewModel = ViewModelProvider(this, factory)[HomeViewModel::class.java]

        setupRecyclerView()
        setupListeners()
        setupObservers()
    }

    private fun setupRecyclerView() {
        // ESTRATÉGIA CIRÚRGICA APLICADA:
        // showEditButton = true -> Exibe o lápis para editar
        // showSyncStatus = true -> Exibe o status de sincronização (double check)
        adapter = HumorNoteAdapter(
            showEditButton = true,
            showSyncStatus = true,
            onEditClick = { note ->
                // Abre a tela de edição enviando a nota clicada
                val intent = Intent(requireContext(), AddHumorActivity::class.java).apply {
                    putExtra("humor_note", note)
                }
                startActivity(intent)
            }
        )

        binding.recyclerViewNotes.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewNotes.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnAddRecord.setOnClickListener {
            startActivity(Intent(requireContext(), AddHumorActivity::class.java))
        }
    }

    private fun setupObservers() {
        viewModel.todayNotes.observe(viewLifecycleOwner) { notes ->
            if (notes.isNotEmpty()) {
                binding.recyclerViewNotes.visibility = View.VISIBLE
                binding.emptyState.visibility = View.GONE
                adapter.submitList(notes)
            } else {
                binding.recyclerViewNotes.visibility = View.GONE
                binding.emptyState.visibility = View.VISIBLE
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}