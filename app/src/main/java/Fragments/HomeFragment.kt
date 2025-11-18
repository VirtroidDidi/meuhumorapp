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

class HomeFragment : Fragment() {
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

    // Função utilitária para converter timestamp para a unidade de "dia"
    // Isso ignora a hora, simplificando a comparação de datas
    private fun getDayUnit(timestamp: Long): Long {
        return timestamp / TimeUnit.DAYS.toMillis(1)
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
            adapter = this@HomeFragment.adapter
        }
    }

    private fun setupButton() {
        // Implementando o OnClickListener para o botão de adicionar nota no emptyState
        binding.emptyState.findViewById<View>(R.id.btn_add_record).setOnClickListener {
            val intent = Intent(requireActivity(), AddHumorActivity::class.java)
            startActivityForResult(intent, ADD_NOTE_REQUEST_CODE)
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
        // O método updateUI já chama adapter.submitList
        updateUI(testNotes)
        // No modo de teste, a atualização do card é feita pela lógica real se loadNotes não for chamada.
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ADD_NOTE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // O registro foi concluído. Recarrega as notas, o que aciona a lógica de incremento.
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
                    // Encontra o timestamp do registro mais recente para usar no cálculo de reset e no feedback
                    val lastRecordedTimestamp = notes.mapNotNull { it.data?.get("time") as? Long }.maxOrNull()

                    // O cálculo da sequência agora inclui a lógica de reset.
                    val sequence = calculateDailySequence(notes)

                    updateProgressCard(sequence, lastRecordedTimestamp)
                }
            }
        } ?: run {
            Log.d(TAG, "Usuário não logado")
            showEmptyState()
            updateProgressCard(0, null) // Mostra 0 na sequência se não estiver logado
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

    /**
     * Calcula a sequência de dias consecutivos de registro de humor, aplicando a lógica de Reset.
     * Implementa a lógica do Mapa de Funcionalidade.
     * @param notes A lista completa de HumorNote do usuário.
     * @return O número de dias consecutivos (máximo 7).
     */
    private fun calculateDailySequence(notes: List<HumorNote>): Int {
        if (notes.isEmpty()) return 0

        // 1. Preparar os dias únicos e ordenados
        val distinctRecordedDays = notes
            .mapNotNull { it.data?.get("time") as? Long }
            .map { getDayUnit(it) } // Converte para o dia unitário (ignora a hora)
            .distinct()
            .sortedDescending() // Começa do dia mais recente

        if (distinctRecordedDays.isEmpty()) return 0

        // 2. Obter as datas de referência (D_Hoje e D_Última)
        val todayDayUnit = getDayUnit(System.currentTimeMillis())
        val lastRecordedDayUnit = distinctRecordedDays.first()

        // 3. Verificação de Reset da Sequência (Etapa 1 do Mapa)
        // Se a diferença entre D_Hoje e D_Última for maior que 1, houve quebra.
        val dayDifference = todayDayUnit - lastRecordedDayUnit

        // Se a última nota for de anteontem ou mais antiga (diff > 1), a sequência quebrou.
        if (dayDifference > 1) {
            Log.d(TAG, "RESET DE SEQUÊNCIA: Último registro ($lastRecordedDayUnit) muito antigo. Hoje: $todayDayUnit")
            return 0 // Executar Reset (Etapa 4 do Mapa)
        }

        // 4. Lógica de Contagem
        var sequence = 0
        // O ponto de partida para a contagem retroativa é o dia mais recente registrado.
        var expectedDay = lastRecordedDayUnit

        for (day in distinctRecordedDays) {
            if (day == expectedDay) {
                // A sequência continua
                sequence++
                expectedDay-- // Esperamos o dia anterior
            } else if (day < expectedDay) {
                // Se o dia for muito mais antigo, a sequência consecutiva quebrou.
                break
            }
        }

        // 5. Garantir o limite máximo de 7 (Etapa 6 do Mapa - Manter 7)
        return sequence.coerceAtMost(7)
    }

    /**
     * Atualiza os elementos visuais do Card de Progresso (Sequência, ProgressBar e Texto de Feedback).
     * Removemos o emoji '🔥' conforme sua solicitação.
     * @param sequence O valor da sequência atual (0-7).
     * @param lastRecordedTimestamp O timestamp do último registro, usado para verificar se houve reset.
     */
    private fun updateProgressCard(sequence: Int, lastRecordedTimestamp: Long?) {
        // Acessa os elementos do layout incluído (progress_card) via ViewBinding
        binding.progressCard.tvSequenceDays.text = sequence.toString()
        binding.progressCard.progressBar.progress = sequence

        val maxDays = binding.progressCard.progressBar.max // 7 dias

        val todayDayUnit = getDayUnit(System.currentTimeMillis())
        // Converte o timestamp para a unidade de dia
        val lastDayUnit = if (lastRecordedTimestamp != null) getDayUnit(lastRecordedTimestamp) else null

        // A sequência de 0 dias pode ser por 3 motivos:
        // 1. Nunca houve registro (lastDayUnit == null).
        // 2. Houve quebra de sequência (lastDayUnit != null e (todayDayUnit - lastDayUnit) > 1).

        val isReset = sequence == 0 && lastDayUnit != null && (todayDayUnit - lastDayUnit) > 1

        val descriptionText = when {
            isReset -> "Sequência Reiniciada. Comece hoje!" // Feedback de Quebra
            sequence >= maxDays -> "Parabéns! Sequência semanal completa!" // Feedback de Sucesso (sem emoji)
            sequence > 0 -> "Sequência de $sequence dias consecutivos!" // Feedback de Sequência (sem emoji)
            else -> "Sua sequência diária de notas." // Estado inicial (0 registros)
        }

        binding.progressCard.tvSequenceDescription.text = descriptionText
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
