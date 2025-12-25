package com.example.apphumor

import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.apphumor.databinding.ActivityAddHumorBinding
import com.example.apphumor.models.HumorNote
import com.example.apphumor.utils.hideKeyboard
import com.example.apphumor.viewmodel.AddHumorViewModel
import com.example.apphumor.viewmodel.HumorViewModelFactory
import com.example.apphumor.viewmodel.SaveState
import com.google.android.material.snackbar.Snackbar

class AddHumorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddHumorBinding
    private lateinit var viewModel: AddHumorViewModel
    private var selectedHumor: String? = null
    private var existingNote: HumorNote? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddHumorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Acessa o Container
        val appContainer = (application as AppHumorApplication).container

        // 2. Inicializa o ViewModel
        val factory = HumorViewModelFactory(appContainer.databaseRepository, appContainer.auth)
        viewModel = ViewModelProvider(this, factory)[AddHumorViewModel::class.java]

        // Recupera nota existente (edição)
        existingNote = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("humor_note", HumorNote::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("humor_note")
        }

        setupUI()
        setupObservers()
    }

    private fun setupUI() {
        // Preenche dados se for edição
        existingNote?.let { note ->
            selectedHumor = note.humor
            // CORREÇÃO: ID do XML é et_notes
            binding.etNotes.setText(note.descricao)
        }

        // CORREÇÃO: ID do XML é btn_save
        binding.btnSave.setOnClickListener {
            saveNote()
        }

        // IMPORTANTE: Certifique-se que no seu XML existe um ChipGroup com id "chip_group_humor"
        // Se o seu XML não tiver isso, comente as linhas abaixo temporariamente.
        /*
        binding.chipGroupHumor.setOnCheckedChangeListener { group, checkedId ->
             // Lógica de seleção do chip
        }
        */

        binding.root.setOnClickListener {
            hideKeyboard(it)
        }
    }

    private fun saveNote() {
        // CORREÇÃO: ID do XML é et_notes
        val descricao = binding.etNotes.text.toString()

        val note = existingNote?.copy(
            humor = selectedHumor,
            descricao = descricao,
            timestamp = existingNote?.timestamp ?: System.currentTimeMillis()
        ) ?: HumorNote(
            humor = selectedHumor,
            descricao = descricao,
            timestamp = System.currentTimeMillis()
        )

        val isExisting = existingNote != null
        viewModel.saveOrUpdateHumorNote(note, isExisting)
    }

    private fun setupObservers() {
        lifecycleScope.launchWhenStarted {
            viewModel.saveStatus.collect { state ->
                when (state) {
                    is SaveState.Loading -> {
                        binding.btnSave.isEnabled = false
                        binding.btnSave.text = "Salvando..."
                    }
                    is SaveState.Success -> {
                        binding.btnSave.isEnabled = true
                        binding.btnSave.text = "Salvar"
                        showSnackbar("Registro salvo com sucesso!")
                        finish()
                    }
                    is SaveState.Error -> {
                        binding.btnSave.isEnabled = true
                        binding.btnSave.text = "Salvar"
                        showSnackbar(state.message, isError = true)
                    }
                    else -> {}
                }
            }
        }
    }

    private fun showSnackbar(message: String, isError: Boolean = false) {
        val snackbar = Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
        if (isError) {
            // Se não tiver essa cor definida, troque por android.R.color.holo_red_dark
            snackbar.setBackgroundTint(getColor(R.color.error_color))
            snackbar.setTextColor(getColor(android.R.color.white))
        } else {
            // Se não tiver essa cor, troque por android.R.color.holo_green_dark
            snackbar.setBackgroundTint(getColor(R.color.secondary_color))
            snackbar.setTextColor(getColor(android.R.color.white))
        }
        snackbar.show()
    }
}