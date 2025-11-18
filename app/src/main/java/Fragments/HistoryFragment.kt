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
import com.example.apphumor.viewmodel.AddHumorViewModel
import com.google.firebase.auth.FirebaseAuth

class HistoryFragment : Fragment() {
    private lateinit var binding: FragmentTelaCBinding
    private val viewModel: AddHumorViewModel by lazy { ViewModelProvider(this).get(AddHumorViewModel::class.java) }
    private lateinit var adapter: HumorNoteAdapter
    private val currentUser = FirebaseAuth.getInstance().currentUser
    private val TAG = "FragmentTelaC"

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
        loadAllNotes()
    }

    private fun setupRecyclerView() {
        // Remove o parâmetro de click e passa false para showEditButton
        adapter = HumorNoteAdapter(showEditButton = false)

        binding.recyclerViewAllNotes.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@HistoryFragment.adapter
        }
    }

    private fun loadAllNotes() {
        currentUser?.uid?.let { userId ->
            viewModel.getHumorNotes(userId) { notes ->
                activity?.runOnUiThread {
                    if (notes.isNotEmpty()) {
                        binding.recyclerViewAllNotes.visibility = View.VISIBLE
                        binding.emptyState.visibility = View.GONE
                        adapter.submitList(notes)
                    } else {
                        binding.recyclerViewAllNotes.visibility = View.GONE
                        binding.emptyState.visibility = View.VISIBLE
                    }
                }
            }
        } ?: run {
            Log.d(TAG, "Usuário não logado")
            binding.emptyState.visibility = View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        loadAllNotes()
    }
}