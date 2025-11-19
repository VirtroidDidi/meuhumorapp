package com.example.apphumor

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope // NOVO: Para coletar o StateFlow
import com.example.apphumor.databinding.ActivityAddHumorBinding
import com.example.apphumor.models.HumorNote
import com.example.apphumor.viewmodel.AddHumorViewModel
import com.example.apphumor.viewmodel.SaveState // NOVO: Import do sealed class de estado
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch // NOVO: Para Coroutines

class AddHumorActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddHumorBinding
    private lateinit var viewModel: AddHumorViewModel
    private var selectedHumor: String? = null
    private var existingNote: HumorNote? = null // Nota existente se estivermos editando

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddHumorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ViewModelProvider(this).get(AddHumorViewModel::class.java)

        // Verificar se estamos editando uma nota existente
        existingNote = intent.getParcelableExtra("EDIT_NOTE")

        setupHumorButtons()
        setupSaveButton()
        loadExistingNote()
        setupSaveStatusObserver() // NOVO: Observa o StateFlow
    }

    private fun setupHumorButtons() {
        // ... (lógica existente, sem alterações necessárias)
        listOf(
            binding.btnCalm,
            binding.btnEnergetic,
            binding.btnSad,
            binding.btnAngry,
            binding.btnNeutral
        ).forEach { button ->
            button.setOnClickListener {
                selectedHumor = when (button.id) {
                    R.id.btn_calm -> "Calm"
                    R.id.btn_energetic -> "Energetic"
                    R.id.btn_sad -> "Sad"
                    R.id.btn_angry -> "Angry"
                    else -> "Neutral"
                }
                updateButtonSelection(button)
            }
        }
    }

    private fun loadExistingNote() {
        // ... (lógica existente, sem alterações necessárias)
        existingNote?.let { note ->
            val buttonId = when (note.humor) {
                "Calm" -> R.id.btn_calm
                "Energetic" -> R.id.btn_energetic
                "Sad" -> R.id.btn_sad
                "Angry" -> R.id.btn_angry
                else -> R.id.btn_neutral
            }
            // Verifica se o ID existe antes de chamar findViewById
            if (buttonId != R.id.btn_neutral || note.humor == "Neutral") {
                findViewById<MaterialButton>(buttonId)?.performClick()
            }

            binding.etNotes.setText(note.descricao)
            binding.tvTitle.text = "Editar Registro" // Atualiza o título
        }
    }

    private fun updateButtonSelection(selectedButton: MaterialButton) {
        listOf(
            binding.btnCalm,
            binding.btnEnergetic,
            binding.btnSad,
            binding.btnAngry,
            binding.btnNeutral
        ).forEach { it.isChecked = it == selectedButton }
    }

    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            val user = FirebaseAuth.getInstance().currentUser
            val humor = selectedHumor ?: run {
                showToast("Selecione um humor!")
                return@setOnClickListener
            }

            if (user == null) {
                showToast("Usuário não autenticado. Tente logar novamente.")
                return@setOnClickListener
            }

            val note = createHumorNote(humor)

            // CHAMA O NOVO MÉTODO DO VIEWMODEL
            viewModel.saveOrUpdateHumorNote(note, existingNote != null)
        }
    }

    private fun createHumorNote(humor: String): HumorNote {
        return existingNote?.copy(
            // Mantém o ID e o timestamp original
            humor = humor,
            descricao = binding.etNotes.text.toString().takeIf { it.isNotEmpty() }
        ) ?: HumorNote( // Criar nova nota se não existir
            data = mapOf("time" to System.currentTimeMillis()),
            humor = humor,
            descricao = binding.etNotes.text.toString().takeIf { it.isNotEmpty() }
        )
    }

    /**
     * NOVO: Configura a observação do StateFlow para reagir ao estado de salvamento.
     */
    private fun setupSaveStatusObserver() {
        // Coleta o StateFlow no escopo da Activity (lifecycleScope)
        lifecycleScope.launch {
            viewModel.saveStatus.collect { state ->
                when (state) {
                    is SaveState.Idle -> {
                        // Estado inicial
                        binding.btnSave.isEnabled = true
                        binding.btnSave.text = if (existingNote != null) "Salvar Alterações" else "Salvar Registro"
                    }
                    is SaveState.Loading -> {
                        // Desabilita o botão e mostra carregamento
                        binding.btnSave.isEnabled = false
                        binding.btnSave.text = "Salvando..."
                        // Aqui você adicionaria uma ProgressBar se o layout tivesse uma.
                    }
                    is SaveState.Success -> {
                        // Operação concluída com sucesso
                        val message = if (existingNote != null) "Registro atualizado!" else "Registro salvo!"
                        showToast(message)
                        setResult(Activity.RESULT_OK)
                        finish()
                        viewModel.resetSaveStatus() // Reseta o estado
                    }
                    is SaveState.Error -> {
                        // Trata o erro
                        showToast(state.message)
                        binding.btnSave.isEnabled = true
                        viewModel.resetSaveStatus() // Reseta o estado
                    }
                }
            }
        }
    }

    // REMOVIDOS: handleSaveSuccess e handleSaveError (substituídos pelo StateFlow)

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}