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
import com.example.apphumor.adapter.NoteAdapter
import com.example.apphumor.databinding.FragmentTelaABinding
import com.example.apphumor.models.HumorNote
import com.example.apphumor.viewmodel.AddHumorViewModel
import com.google.firebase.auth.FirebaseAuth
import java.util.*

class FragmentTelaA : Fragment() {
    private lateinit var binding: FragmentTelaABinding
    private val viewModel: AddHumorViewModel by lazy { ViewModelProvider(this).get(AddHumorViewModel::class.java) }
    private lateinit var adapter: NoteAdapter
    private val currentUser = FirebaseAuth.getInstance().currentUser
    private val TAG = "FragmentTelaA"

    // Altere para false quando quiser testar com dados reais
    private var isTesting = false

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

        // Configurações iniciais obrigatórias
        setupRecyclerView()
        setupButton()

        if (isTesting) {
            testAdapter()
        } else {
            loadNotes()
        }
    }

    private fun setupRecyclerView() {
        adapter = NoteAdapter { note ->
            val intent = Intent(requireActivity(), AddHumorActivity::class.java).apply {
                putExtra("EDIT_NOTE", note) // Agora funcionará
            }
            startActivityForResult(intent, ADD_NOTE_REQUEST_CODE)
        }

        binding.recyclerViewNotes.apply {
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
            adapter = this@FragmentTelaA.adapter
        }
    }

    private fun setupButton() {
        binding.btnAddRecord.setOnClickListener {
            startActivityForResult(
                Intent(activity, AddHumorActivity::class.java),
                ADD_NOTE_REQUEST_CODE
            )
        }
    }

    private fun testAdapter() {
        val testNotes = listOf(
            HumorNote(
                id = "TESTE1",
                humor = "Feliz",
                descricao = "Nota mockada para teste",
                data = mapOf("time" to System.currentTimeMillis())
            )
        )
        updateUI(testNotes)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ADD_NOTE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            loadNotes()
        }
    }

    private fun loadNotes() {
        Log.d(TAG, "Carregando notas do Firebase...")
        currentUser?.uid?.let { userId ->
            viewModel.getHumorNotes(userId) { notes ->
                Log.d(TAG, "Notas recebidas: ${notes.size}")
                activity?.runOnUiThread {
                    val todayNotes = filterTodayNotes(notes)
                    Log.d(TAG, "Notas de hoje: ${todayNotes.size}")
                    updateUI(todayNotes)
                }
            }
        } ?: run {
            Log.d(TAG, "Usuário não logado")
            showEmptyState()
        }
    }

    private fun updateUI(notes: List<HumorNote>) {
        if (notes.isNotEmpty()) {
            binding.recyclerViewNotes.visibility = View.VISIBLE
            binding.emptyState.visibility = View.GONE
            adapter.submitList(notes)
        } else {
            binding.recyclerViewNotes.visibility = View.GONE
            binding.emptyState.visibility = View.VISIBLE
        }
    }

    private fun filterTodayNotes(notes: List<HumorNote>): List<HumorNote> {
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val todayEnd = todayStart + 86400000 // 24 horas

        return notes.filter { note ->
            val timestamp = note.data?.get("time") as? Long ?: 0L
            timestamp in todayStart until todayEnd
        }
    }


    private fun showEmptyState() {
        binding.recyclerViewNotes.visibility = View.GONE
        binding.tvNoRecord.visibility = View.VISIBLE
        binding.btnAddRecord.visibility = View.GONE
    }

    override fun onResume() {
        super.onResume()
        if (!isTesting) {
            loadNotes()
        }
    }
}