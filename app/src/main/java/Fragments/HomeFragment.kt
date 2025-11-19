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
import com.example.apphumor.databinding.FragmentTelaABinding // Layout da Home
import com.example.apphumor.models.HumorNote
import com.example.apphumor.viewmodel.HomeViewModel // NOVO ViewModel
import com.google.firebase.auth.FirebaseAuth
import java.util.*
// REMOVIDOS: TimeUnit, kotlin.math.abs

/**
 * [HomeFragment] (Antigo FragmentTelaA)
 * Exibe as notas registradas no dia e o progresso da sequência diária.
 * Toda a lógica de filtro de notas e cálculo de sequência foi delegada ao HomeViewModel.
 */
class HomeFragment : Fragment() {
    private lateinit var binding: FragmentTelaABinding

    // Mude de AddHumorViewModel para HomeViewModel
    private val viewModel: HomeViewModel by lazy { ViewModelProvider(this).get(HomeViewModel::class.java) }

    private lateinit var adapter: HumorNoteAdapter
    private val TAG = "HomeFragment" // Nomenclatura atualizada

    private var isTesting = false // MANTIDO

    companion object {
        const val ADD_NOTE_REQUEST_CODE = 1001
    }

    // REMOVIDAS: Funções getDayUnit, calculateDailySequence, filterTodayNotes

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

        setupRecyclerView()
        setupButton()
        setupObservers() // NOVO: Configura a observação dos dados

        // loadNotes() é substituído pela observação no setupObservers
        if (isTesting) {
            testAdapter()
        }
    }

    private fun setupRecyclerView() {
        // Adaptador configurado para permitir edição na tela Home
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
        // OnClickListener para o botão de adicionar nota no emptyState
        binding.emptyState.findViewById<View>(R.id.btn_add_record).setOnClickListener {
            val intent = Intent(requireActivity(), AddHumorActivity::class.java)
            startActivityForResult(intent, ADD_NOTE_REQUEST_CODE)
        }
    }

    private fun testAdapter() {
        // MANTIDO: Lógica de teste
        // ... (código do testAdapter) ...
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ADD_NOTE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // O ViewModel já está observando o Repositório LiveData, então não precisamos
            // chamar loadNotes() explicitamente; a UI se atualizará sozinha.
            Log.d(TAG, "Nota salva/atualizada. LiveData irá disparar a atualização da UI.")
        }
    }

    // REMOVIDA: função loadNotes()

    private fun setupObservers() {
        // 1. Observa as Notas de Hoje (Lista filtrada pelo ViewModel)
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

        // 2. Observa o Progresso Diário (Sequência e Timestamp)
        viewModel.dailyProgress.observe(viewLifecycleOwner) { (sequence, lastRecordedTimestamp) ->
            updateProgressCard(sequence, lastRecordedTimestamp)
        }
    }

    // Lógica movida para o ViewModel (mantive apenas o corpo do Fragment para referência)

    /**
     * Atualiza os elementos visuais do Card de Progresso (Sequência, ProgressBar e Texto de Feedback).
     * Esta função agora recebe os dados prontos do ViewModel.
     * @param sequence O valor da sequência atual (0-7).
     * @param lastRecordedTimestamp O timestamp do último registro.
     */
    private fun updateProgressCard(sequence: Int, lastRecordedTimestamp: Long?) {
        // Note que o HomeViewModel tem acesso à função getDayUnit, então
        // a lógica de cálculo de D_Hoje e D_Última para o feedback ainda precisa
        // ser resolvida se quisermos manter o feedback de "Sequência Reiniciada".

        // Vamos simplificar o feedback aqui (assumindo que 0 significa reset ou início)

        binding.progressCard.tvSequenceDays.text = sequence.toString()
        binding.progressCard.progressBar.progress = sequence

        val maxDays = binding.progressCard.progressBar.max

        var descriptionText = when {
            sequence >= maxDays -> "Parabéns! Sequência semanal completa!"
            sequence > 0 -> "Sequência de $sequence dias consecutivos!"
            else -> "Sua sequência diária de notas."
        }

        // Se a sequência é 0 E o último registro não foi hoje, podemos dar o feedback de reset
        if (sequence == 0 && lastRecordedTimestamp != null) {
            // NOTA: Para saber se houve reset, precisaríamos saber a data de hoje.
            // O ViewModel forneceu apenas o número da sequência.
            // Para simplificar: se for 0 e há registros anteriores, assumimos que quebrou.
            descriptionText = "Sequência Reiniciada. Comece hoje!"
        }

        binding.progressCard.tvSequenceDescription.text = descriptionText
    }

    // REMOVIDA: função updateUI

    private fun showEmptyState() {
        binding.recyclerViewNotes.visibility = View.GONE
        binding.emptyState.visibility = View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        // O LiveData no init do ViewModel fará o trabalho de busca
    }
}