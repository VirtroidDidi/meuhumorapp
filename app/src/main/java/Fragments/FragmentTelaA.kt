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
// Certifique-se de que o binding para o fragment_tela_a inclui a referência ao seu card.
// Se você estiver usando ViewBinding (FragmentTelaABinding), o novo card é acessado
// através do binding.progressCard (o ID que demos ao <include>).
import com.example.apphumor.databinding.FragmentTelaABinding
import com.example.apphumor.models.HumorNote
import com.example.apphumor.viewmodel.AddHumorViewModel
import com.google.firebase.auth.FirebaseAuth
import java.util.*
import java.util.concurrent.TimeUnit // Importação para facilitar a conversão de tempo

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
            // Se estiver em modo de teste, simula uma sequência para o card
            updateProgressCard(5)
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
        // Agora o botão de adicionar está dentro do emptyState (e no XML principal),
        // mas é melhor ter a lógica de clique aqui, associada à função do Fragment.
        // Se o botão for aquele FAB na MainActivity, você precisa gerenciar o clique lá.
        // Assumindo que o FAB está na MainActivity, esta parte não precisa de binding.
        // No entanto, para fins de demonstração, o botão de 'Adicionar Registro'
        // no emptyState está sendo configurado.
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
        // O método updateUI já chama adapter.submitList
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
                    // 1. Lógica para o RecyclerView (Notas de Hoje)
                    val todayNotes = filterTodayNotes(notes)
                    Log.d(TAG, "Notas de hoje: ${todayNotes.size}")
                    updateUI(todayNotes)

                    // 2. Lógica para o Card de Progresso (Sequência)
                    val sequence = calculateDailySequence(notes)
                    updateProgressCard(sequence)
                }
            }
        } ?: run {
            Log.d(TAG, "Usuário não logado")
            showEmptyState()
            updateProgressCard(0) // Mostra 0 na sequência se não estiver logado
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

    // Função para calcular a sequência diária (dias consecutivos de registro)
    private fun calculateDailySequence(notes: List<HumorNote>): Int {
        if (notes.isEmpty()) return 0

        // Extrai timestamps e garante que só há um por dia (usando a data em milissegundos)
        val sortedDailyTimestamps = notes
            .mapNotNull { it.data?.get("time") as? Long }
            .map { it / TimeUnit.DAYS.toMillis(1) } // Converte para o dia em que aconteceu
            .distinct()
            .sortedDescending() // Começa do dia mais recente

        if (sortedDailyTimestamps.isEmpty()) return 0

        var sequence = 0
        var expectedDay = sortedDailyTimestamps.first() // O dia mais recente registrado

        for (day in sortedDailyTimestamps) {
            if (day == expectedDay) {
                sequence++
                expectedDay-- // Esperamos o dia anterior
            } else if (day < expectedDay) {
                // Se o dia for muito mais antigo, a sequência quebrou
                break
            }
        }
        return sequence
    }

    // Função para atualizar o Card de Progresso com a Sequência
    private fun updateProgressCard(sequence: Int) {
        // Acessa os elementos do layout incluído (progress_card) via ViewBinding
        binding.progressCard.tvSequenceDays.text = sequence.toString()
        binding.progressCard.progressBar.progress = sequence

        val maxDays = binding.progressCard.progressBar.max // 7 dias

        if (sequence >= maxDays) {
            binding.progressCard.tvSequenceDescription.text = "Parabéns! Sequência semanal completa!"
        } else {
            binding.progressCard.tvSequenceDescription.text = "Sua sequência diária de notas."
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
        // O emptyState já é um LinearLayout com todos os elementos
        binding.emptyState.visibility = View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        if (!isTesting) {
            loadNotes()
        }
    }
}
