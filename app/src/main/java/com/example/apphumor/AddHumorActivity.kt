// ARQUIVO: app/src/main/java/com/example/apphumor/AddHumorActivity.kt

package com.example.apphumor

import android.app.Activity
// ===============================================
// NOVO IMPORT NECESSÁRIO PARA CONECTIVITY_SERVICE
// ===============================================
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.apphumor.databinding.ActivityAddHumorBinding
import com.example.apphumor.di.DependencyProvider
import com.example.apphumor.models.HumorNote
import com.example.apphumor.viewmodel.AddHumorViewModel
import com.example.apphumor.viewmodel.AddHumorViewModelFactory
import com.example.apphumor.viewmodel.SaveState
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class AddHumorActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddHumorBinding
    private lateinit var viewModel: AddHumorViewModel
    private var selectedHumor: String? = null
    private var existingNote: HumorNote? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddHumorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(
            this,
            AddHumorViewModelFactory(
                DependencyProvider.databaseRepository,
                DependencyProvider.auth
            )
        ).get(AddHumorViewModel::class.java)

        existingNote = intent.getParcelableExtra("EDIT_NOTE")

        setupHumorButtons()
        setupSaveButton()
        loadExistingNote()
        setupSaveStatusObserver()
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

    private fun loadExistingNote() {
        existingNote?.let { note ->
            val buttonId = when (note.humor) {
                "Calm" -> R.id.btn_calm
                "Energetic" -> R.id.btn_energetic
                "Sad" -> R.id.btn_sad
                "Angry" -> R.id.btn_angry
                else -> R.id.btn_neutral
            }
            if (buttonId != R.id.btn_neutral || note.humor == "Neutral") {
                findViewById<MaterialButton>(buttonId)?.performClick()
            }

            binding.etNotes.setText(note.descricao)
            binding.tvTitle.text = "Editar Registro"
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
            val user = DependencyProvider.auth.currentUser
            val humor = selectedHumor ?: run {
                showToast("Selecione um humor!")
                return@setOnClickListener
            }

            if (user == null) {
                showToast("Usuário não autenticado. Tente logar novamente.")
                return@setOnClickListener
            }

            val note = createHumorNote(humor)

            viewModel.saveOrUpdateHumorNote(note, existingNote != null)
        }
    }

    private fun createHumorNote(humor: String): HumorNote {
        return existingNote?.copy(
            humor = humor,
            descricao = binding.etNotes.text.toString().takeIf { it.isNotEmpty() }
        ) ?: HumorNote(
            // CORREÇÃO: Usando timestamp direto em vez de mapa de dados
            timestamp = System.currentTimeMillis(),
            humor = humor,
            descricao = binding.etNotes.text.toString().takeIf { it.isNotEmpty() }
        )
    }

    private fun setupSaveStatusObserver() {
        lifecycleScope.launch {
            viewModel.saveStatus.collect { state ->
                when (state) {
                    is SaveState.Idle -> {
                        binding.btnSave.isEnabled = true
                        binding.btnSave.text = if (existingNote != null) "Salvar Alterações" else "Salvar Registro"
                    }
                    is SaveState.Loading -> {
                        binding.btnSave.isEnabled = false
                        binding.btnSave.text = "Salvando..."
                    }
                    is SaveState.Success -> {
                        // ===============================================
                        // 🟡 TAREFA 2.4: Feedback de Gravação Offline
                        // ===============================================
                        // Verifica conexão simples para feedback visual
                        val isOffline = !isInternetAvailable()

                        val message = if (existingNote != null) "Registro atualizado!" else "Registro salvo!"

                        if (isOffline) {
                            // Feedback específico se o dispositivo está offline
                            showToast("$message (Salvo no dispositivo)")
                        } else {
                            showToast(message)
                        }

                        setResult(Activity.RESULT_OK)
                        finish()
                        viewModel.resetSaveStatus()
                        // ===============================================
                    }
                    is SaveState.Error -> {
                        showToast(state.message)
                        binding.btnSave.isEnabled = true
                        viewModel.resetSaveStatus()
                    }
                }
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // ===============================================
    // 🟡 TAREFA 2.4: Função utilitária para checagem de rede
    // ===============================================
    private fun isInternetAvailable(): Boolean {
        // Checagem básica via ConnectivityManager
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        // A partir do API 29 (Android 10), métodos mais novos são preferidos,
        // mas activeNetworkInfo ainda é amplamente usado para compatibilidade.
        @Suppress("DEPRECATION")
        val activeNetwork = cm.activeNetworkInfo
        return activeNetwork != null && activeNetwork.isConnected
    }
    // ===============================================
}