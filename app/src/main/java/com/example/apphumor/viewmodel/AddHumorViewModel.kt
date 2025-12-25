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

sealed class SaveState {
    object Idle : SaveState()
    object Loading : SaveState()
    data class Success(val noteId: String) : SaveState()
    data class Error(val message: String) : SaveState()
}

class AddHumorViewModel(
    private val repository: DatabaseRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val TAG = "AddHumorViewModel"

    private val _saveStatus = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveStatus: StateFlow<SaveState> = _saveStatus

    fun saveOrUpdateHumorNote(note: HumorNote, isExisting: Boolean) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _saveStatus.value = SaveState.Error("Usuário não autenticado.")
            return
        }

        _saveStatus.value = SaveState.Loading

        viewModelScope.launch {
            // Repositório agora é injetado, não instanciado
            val result = if (isExisting && note.id != null) {
                repository.updateHumorNote(userId, note)
            } else {
                repository.saveHumorNote(userId, note)
            }

            when (result) {
                is DatabaseRepository.Result.Success -> {
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

    fun resetSaveStatus() {
        _saveStatus.value = SaveState.Idle
    }
}
// REMOVIDO: class AddHumorViewModelFactory (Não precisamos mais dela!)