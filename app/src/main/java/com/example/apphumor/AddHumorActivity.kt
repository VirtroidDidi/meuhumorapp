package com.example.apphumor


import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.apphumor.databinding.ActivityAddHumorBinding
import com.example.apphumor.di.DependencyProvider
import com.example.apphumor.models.HumorNote
import com.example.apphumor.models.HumorType
import com.example.apphumor.utils.hideKeyboard
import com.example.apphumor.viewmodel.AddHumorViewModel
import com.example.apphumor.viewmodel.AppViewModelFactory
import com.example.apphumor.viewmodel.SaveState
import com.google.android.material.color.MaterialColors
import com.google.android.material.snackbar.Snackbar
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

        val factory = AppViewModelFactory(
            DependencyProvider.auth,
            DependencyProvider.databaseRepository
        )
        viewModel = ViewModelProvider(this, factory)[AddHumorViewModel::class.java]
        existingNote = intent.getParcelableExtra("EDIT_NOTE")

        setupChipGroup()
        setupSaveButton()
        loadExistingNote()
        setupSaveStatusObserver()

        // UX Extra: Clicar fora do campo de texto fecha o teclado
        binding.root.setOnClickListener {
            binding.root.hideKeyboard()
        }
    }

    private fun setupChipGroup() {
        binding.cgHumor.setOnCheckedStateChangeListener { _, checkedIds ->
            // UX: Esconde o teclado se o usuário for selecionar o humor
            binding.root.hideKeyboard()

            if (checkedIds.isNotEmpty()) {
                val chipId = checkedIds[0]

                // Substituição do 'when' gigante pelo Enum HumorType
                selectedHumor = HumorType.fromChipId(chipId)?.key

            } else {
                selectedHumor = null
            }
        }
    }

    private fun loadExistingNote() {
        existingNote?.let { note ->

            // O Enum já trata "Incrível", "Energetic", etc, automaticamente via fromKey()
            val type = HumorType.fromKey(note.humor)
            binding.cgHumor.check(type.chipId)

            binding.etNotes.setText(note.descricao)
            binding.tvTitle.text = getString(R.string.add_humor_edit_title)
        }
    }

    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            // UX CRÍTICA: Fechar teclado imediatamente ao clicar em salvar
            binding.root.hideKeyboard()

            val user = DependencyProvider.auth.currentUser
            val humor = selectedHumor

            if (humor == null) {
                showSnackbar(getString(R.string.error_select_humor), isError = true)
                return@setOnClickListener
            }

            if (user == null) {
                showSnackbar(getString(R.string.error_user_not_auth), isError = true)
                return@setOnClickListener
            }

            val note = existingNote?.copy(
                humor = humor,
                descricao = binding.etNotes.text.toString().takeIf { it.isNotEmpty() },
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

                        // Se estiver offline, usamos uma mensagem especial
                        if (isOffline) {
                            Toast.makeText(this@AddHumorActivity, getString(R.string.msg_saved_offline, baseMsg), Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this@AddHumorActivity, baseMsg, Toast.LENGTH_SHORT).show()
                        }

                        setResult(RESULT_OK)
                        finish()
                        viewModel.resetSaveStatus()
                    }
                    is SaveState.Error -> {
                        showSnackbar(state.message, isError = true)
                        binding.btnSave.isEnabled = true
                        viewModel.resetSaveStatus()
                    }
                }
            }
        }
    }

    // Função auxiliar para mostrar Snackbar com estilo e cores do tema
    private fun showSnackbar(message: String, isError: Boolean = false) {
        val snackbar = Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)

        if (isError) {
            // Busca as cores semânticas de ERRO do tema atual
            // Usamos o pacote completo para garantir que não haja confusão com R local
                val colorError = MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorError)
            val colorOnError = MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorOnError)

            snackbar.setBackgroundTint(colorError)
            snackbar.setTextColor(colorOnError)
        } else {
            // Busca as cores semânticas de SUCESSO/SECUNDÁRIA do tema atual
            val colorBackground = MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorSecondary)
            val colorText = MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorOnSecondary)

            snackbar.setBackgroundTint(colorBackground)
            snackbar.setTextColor(colorText)
        }
        snackbar.show()
    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            else -> false
        }
    }
}