package com.example.apphumor

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.apphumor.databinding.ActivityAddHumorBinding
import com.example.apphumor.models.HumorNote
import com.example.apphumor.viewmodel.AddHumorViewModel
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth

class AddHumorActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddHumorBinding
    private lateinit var viewModel: AddHumorViewModel
    private var selectedHumor: String? = null
    private var existingNote: HumorNote? = null // Nova propriedade para armazenar a nota existente

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddHumorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ViewModelProvider(this).get(AddHumorViewModel::class.java)

        // Verificar se estamos editando uma nota existente
        existingNote = intent.getParcelableExtra("EDIT_NOTE")

        setupHumorButtons()
        setupSaveButton()
        loadExistingNote() // Carregar dados da nota existente se houver
    }

    private fun setupHumorButtons() {
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

    // Novo método para carregar dados da nota existente
    private fun loadExistingNote() {
        existingNote?.let { note ->
            // Selecionar o humor correspondente
            val buttonId = when (note.humor) {
                "Calm" -> R.id.btn_calm
                "Energetic" -> R.id.btn_energetic
                "Sad" -> R.id.btn_sad
                "Angry" -> R.id.btn_angry
                else -> R.id.btn_neutral
            }
            findViewById<MaterialButton>(buttonId)?.performClick()

            // Preencher descrição
            binding.etNotes.setText(note.descricao)
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

            val note = createHumorNote(humor)

            user?.uid?.let { userId ->
                if (existingNote != null) {
                    // Atualizar nota existente
                    viewModel.updateHumorNote(userId, note, ::handleSaveSuccess, ::handleSaveError)
                } else {
                    // Criar nova nota
                    viewModel.saveHumorNote(userId, note, ::handleSaveSuccess, ::handleSaveError)
                }
            }
        }
    }

    private fun createHumorNote(humor: String): HumorNote {
        return existingNote?.copy(
            // Manter o timestamp original
            humor = humor,
            descricao = binding.etNotes.text.toString().takeIf { it.isNotEmpty() }
        ) ?: HumorNote( // Criar nova nota se não existir
            data = mapOf("time" to System.currentTimeMillis()),
            humor = humor,
            descricao = binding.etNotes.text.toString().takeIf { it.isNotEmpty() }
        )
    }

    private fun handleSaveSuccess() {
        setResult(Activity.RESULT_OK)
        finish()
        showToast(if (existingNote != null) "Registro atualizado!" else "Registro salvo!")
    }

    private fun handleSaveError(error: String) {
        showToast(error)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}