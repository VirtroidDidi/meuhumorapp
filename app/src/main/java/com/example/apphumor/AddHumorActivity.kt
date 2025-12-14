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
        val buttons = listOf(
            binding.btnCalm, binding.btnEnergetic,
            binding.btnSad, binding.btnAngry, binding.btnNeutral
        )

        buttons.forEach { button ->
            button.setOnClickListener {
                selectedHumor = when (button.id) {
                    R.id.btn_calm -> "Calm"
                    R.id.btn_energetic -> "Energetic"
                    R.id.btn_sad -> "Sad"
                    R.id.btn_angry -> "Angry"
                    else -> "Neutral"
                }
                buttons.forEach { it.isChecked = (it == button) }
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
            // Simula clique para selecionar visualmente e setar a variável
            findViewById<MaterialButton>(buttonId)?.performClick()

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

            val note = existingNote?.copy(
                humor = humor,
                descricao = binding.etNotes.text.toString().takeIf { it.isNotEmpty() },
                // Mantém o timestamp original se for edição
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
                        showToast(state.message) // Mensagem de erro do VM (pode ser técnica)
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