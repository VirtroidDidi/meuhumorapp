// ARQUIVO: app/src/main/java/com/example/apphumor/viewmodel/AddHumorViewModel.kt

package com.example.apphumor.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.apphumor.models.HumorNote
import com.example.apphumor.repository.DatabaseRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Representa os possíveis estados da operação de salvamento/atualização na UI.
 */
sealed class SaveState {
    object Idle : SaveState()
    object Loading : SaveState()
    data class Success(val noteId: String) : SaveState()
    data class Error(val message: String) : SaveState()
}

/**
 * [AddHumorViewModel]
 * Gerencia a lógica de salvar e atualizar notas usando Kotlin Coroutines e StateFlow.
 * * AGORA RECEBE DEPENDÊNCIAS VIA CONSTRUTOR.
 */
class AddHumorViewModel(
    private val repository: DatabaseRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val TAG = "AddHumorViewModel"

    // StateFlow para expor o estado de salvamento à Activity
    private val _saveStatus = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveStatus: StateFlow<SaveState> = _saveStatus

    /**
     * Inicia a operação de salvamento ou atualização de uma nota de humor,
     * usando funções 'suspend' do Repositório.
     * @param note A nota a ser salva ou atualizada.
     * @param isExisting Se a nota é uma atualização (true) ou um novo registro (false).
     */
    fun saveOrUpdateHumorNote(note: HumorNote, isExisting: Boolean) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _saveStatus.value = SaveState.Error("Usuário não autenticado.")
            return
        }

        // 1. Inicia o estado de carregamento
        _saveStatus.value = SaveState.Loading

        // 2. Lança a coroutine para executar a operação I/O em background
        viewModelScope.launch {
            val result = if (isExisting && note.id != null) {
                // Atualizar nota existente
                repository.updateHumorNote(userId, note)
            } else {
                // Salvar nova nota
                repository.saveHumorNote(userId, note)
            }

            // 3. Processa o resultado (thread-safe)
            when (result) {
                is DatabaseRepository.Result.Success -> {
                    // Retorna o ID da nota (String se for Save, Unit se for Update)
                    val noteId = if (isExisting) note.id!! else result.data.toString()
                    _saveStatus.value = SaveState.Success(noteId)
                }
                is DatabaseRepository.Result.Error -> {
                    val message = "Falha ao salvar/atualizar: ${result.exception.message ?: "Erro desconhecido"}"
                    Log.e(TAG, message)
                    _saveStatus.value = SaveState.Error(message)
                }
            }
        }
    }

    /**
     * Reseta o estado de salvamento para Idle (inativo).
     * Chamado pela Activity após consumir os estados Success ou Error.
     */
    fun resetSaveStatus() {
        _saveStatus.value = SaveState.Idle
    }
}
