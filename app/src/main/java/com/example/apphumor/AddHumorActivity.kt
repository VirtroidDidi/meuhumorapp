package com.example.apphumor

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.apphumor.databinding.ActivityAddHumorBinding
import com.example.apphumor.di.DependencyProvider
import com.example.apphumor.models.HumorNote
import com.example.apphumor.utils.hideKeyboard
import com.example.apphumor.viewmodel.AddHumorViewModel
import com.example.apphumor.viewmodel.AppViewModelFactory
import com.example.apphumor.viewmodel.SaveState
import com.google.android.material.snackbar.Snackbar // Import do Snackbar
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
                // Legado
                "Excellent", "Incrível" -> R.id.chip_rad
                "Good", "Bem" -> R.id.chip_happy
                "Energetic", "Energético" -> R.id.chip_rad
                "Irritado" -> R.id.chip_angry
                "Triste" -> R.id.chip_sad
                else -> R.id.chip_neutral
            }
            binding.cgHumor.check(chipId)
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
                        // O Toast é melhor aqui porque a Activity vai fechar logo em seguida
                        if (isOffline) {
                            Toast.makeText(this@AddHumorActivity, getString(R.string.msg_saved_offline, baseMsg), Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this@AddHumorActivity, baseMsg, Toast.LENGTH_SHORT).show()
                        }

                        setResult(Activity.RESULT_OK)
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

    // Função auxiliar para mostrar Snackbar com estilo
    private fun showSnackbar(message: String, isError: Boolean = false) {
        val snackbar = Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
        if (isError) {
            snackbar.setBackgroundTint(getColor(R.color.error_color)) // Usando a cor de erro definida
            snackbar.setTextColor(getColor(R.color.white))
        } else {
            snackbar.setBackgroundTint(getColor(R.color.secondary_color))
            snackbar.setTextColor(getColor(R.color.white))
        }
        snackbar.show()
    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
            return when {
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                else -> false
            }
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            return networkInfo != null && networkInfo.isConnected
        }
    }
}