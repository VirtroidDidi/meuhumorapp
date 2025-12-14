package com.example.apphumor

import android.app.Activity
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

        setupChipGroup()
        setupSaveButton()
        loadExistingNote()
        setupSaveStatusObserver()
    }

    // Configura a seleção dos 10 novos humores
    private fun setupChipGroup() {
        binding.cgHumor.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val chipId = checkedIds[0]
                selectedHumor = when (chipId) {
                    R.id.chip_rad -> "Rad"
                    R.id.chip_happy -> "Happy"
                    R.id.chip_grateful -> "Grateful"
                    R.id.chip_calm -> "Calm"
                    R.id.chip_neutral -> "Neutral"
                    R.id.chip_pensive -> "Pensive"
                    R.id.chip_tired -> "Tired"
                    R.id.chip_sad -> "Sad"
                    R.id.chip_anxious -> "Anxious"
                    R.id.chip_angry -> "Angry"
                    else -> null
                }
            } else {
                selectedHumor = null
            }
        }
    }

    private fun loadExistingNote() {
        existingNote?.let { note ->
            val chipId = when (note.humor) {
                // Novos Mapeamentos
                "Rad" -> R.id.chip_rad
                "Happy" -> R.id.chip_happy
                "Grateful" -> R.id.chip_grateful
                "Calm" -> R.id.chip_calm
                "Neutral" -> R.id.chip_neutral
                "Pensive" -> R.id.chip_pensive
                "Tired" -> R.id.chip_tired
                "Sad" -> R.id.chip_sad
                "Anxious" -> R.id.chip_anxious
                "Angry" -> R.id.chip_angry

                // Compatibilidade com registros antigos (Legacy)
                "Excellent", "Incrível" -> R.id.chip_rad
                "Good", "Bem" -> R.id.chip_happy
                "Energetic", "Energético" -> R.id.chip_rad // Promovido para Rad
                "Irritado" -> R.id.chip_angry
                "Triste" -> R.id.chip_sad
                else -> R.id.chip_neutral // Fallback seguro
            }

            // Marca o chip correspondente
            binding.cgHumor.check(chipId)

            binding.etNotes.setText(note.descricao)
            binding.tvTitle.text = getString(R.string.add_humor_edit_title)
        }
    }

    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            val user = DependencyProvider.auth.currentUser
            val humor = selectedHumor

            if (humor == null) {
                showToast(getString(R.string.error_select_humor))
                return@setOnClickListener
            }

            if (user == null) {
                showToast(getString(R.string.error_user_not_auth))
                return@setOnClickListener
            }

            // Cria ou atualiza o objeto HumorNote
            val note = existingNote?.copy(
                humor = humor,
                descricao = binding.etNotes.text.toString().takeIf { it.isNotEmpty() },
                // Mantém timestamp original se for edição
                timestamp = existingNote?.timestamp ?: System.currentTimeMillis()
            ) ?: HumorNote(
                timestamp = System.currentTimeMillis(),
                humor = humor,
                descricao = binding.etNotes.text.toString().takeIf { it.isNotEmpty() }
            )

            viewModel.saveOrUpdateHumorNote(note, existingNote != null)
        }
    }

    private fun setupSaveStatusObserver() {
        lifecycleScope.launch {
            viewModel.saveStatus.collect { state ->
                when (state) {
                    is SaveState.Idle -> {
                        binding.btnSave.isEnabled = true
                        binding.btnSave.text = if (existingNote != null)
                            getString(R.string.action_save_changes)
                        else
                            getString(R.string.action_save_record)
                    }
                    is SaveState.Loading -> {
                        binding.btnSave.isEnabled = false
                        binding.btnSave.text = getString(R.string.status_saving)
                    }
                    is SaveState.Success -> {
                        val isOffline = !isInternetAvailable()
                        val baseMsg = if (existingNote != null)
                            getString(R.string.msg_record_updated)
                        else
                            getString(R.string.msg_record_saved)

                        if (isOffline) {
                            showToast(getString(R.string.msg_saved_offline, baseMsg))
                        } else {
                            showToast(baseMsg)
                        }

                        setResult(Activity.RESULT_OK)
                        finish()
                        viewModel.resetSaveStatus()
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

    private fun isInternetAvailable(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val activeNetwork = cm.activeNetworkInfo
        return activeNetwork != null && activeNetwork.isConnected
    }
}