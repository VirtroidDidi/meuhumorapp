package com.example.apphumor.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.apphumor.models.User
import com.example.apphumor.repository.DatabaseRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// Estado da UI para Cadastro
sealed class CadastroUiState {
    object Idle : CadastroUiState()
    object Loading : CadastroUiState()
    object Success : CadastroUiState()
    data class Error(val message: String) : CadastroUiState()
}

class CadastroViewModel(
    private val auth: FirebaseAuth,
    private val dbRepository: DatabaseRepository
) : ViewModel() {

    private val TAG = "CadastroViewModel"

    private val _uiState = MutableStateFlow<CadastroUiState>(CadastroUiState.Idle)
    val uiState: StateFlow<CadastroUiState> = _uiState.asStateFlow()

    // Função única que recebe tudo e decide o que fazer
    fun registerUser(nome: String, email: String, senha: String, confirmarSenha: String, idade: Int) {

        // 1. CAMADA DE VALIDAÇÃO (Regras de Negócio)
        if (nome.isBlank() || email.isBlank() || senha.isBlank() || confirmarSenha.isBlank()) {
            _uiState.value = CadastroUiState.Error("Preencha todos os campos.")
            return
        }

        if (senha != confirmarSenha) {
            _uiState.value = CadastroUiState.Error("As senhas não conferem.")
            return
        }

        if (senha.length < 6) {
            _uiState.value = CadastroUiState.Error("A senha deve ter pelo menos 6 caracteres.")
            return
        }

        // 2. CAMADA DE EXECUÇÃO
        _uiState.value = CadastroUiState.Loading

        viewModelScope.launch {
            try {
                // Tenta criar usuário no Auth
                val authResult = auth.createUserWithEmailAndPassword(email, senha).await()
                val firebaseUser = authResult.user

                if (firebaseUser?.uid != null) {
                    val user = User(firebaseUser.uid, nome, email, idade)

                    // Salva dados adicionais no Database
                    val saveResult = dbRepository.saveUser(user)

                    if (saveResult is DatabaseRepository.Result.Success) {
                        _uiState.value = CadastroUiState.Success
                    } else {
                        // Rollback manual (opcional): Se falhar no banco, desloga o auth
                        auth.signOut()
                        _uiState.value = CadastroUiState.Error("Conta criada, mas falha ao salvar perfil.")
                    }
                } else {
                    _uiState.value = CadastroUiState.Error("Erro interno: UID não gerado.")
                }

            } catch (e: Exception) {
                val errorMsg = when (e) {
                    is FirebaseAuthWeakPasswordException -> "A senha é muito fraca."
                    is FirebaseAuthInvalidCredentialsException -> "E-mail inválido."
                    is FirebaseAuthUserCollisionException -> "Este e-mail já está cadastrado."
                    else -> {
                        Log.e(TAG, "Erro no cadastro", e)
                        "Falha no cadastro: ${e.localizedMessage}"
                    }
                }
                _uiState.value = CadastroUiState.Error(errorMsg)
            }
        }
    }

    fun resetState() {
        _uiState.value = CadastroUiState.Idle
    }
}

class CadastroViewModelFactory(
    private val auth: FirebaseAuth,
    private val dbRepository: DatabaseRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CadastroViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CadastroViewModel(auth, dbRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}