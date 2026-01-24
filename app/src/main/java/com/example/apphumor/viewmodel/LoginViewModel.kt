package com.example.apphumor.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// --- SEALED CLASS (A estrela da refatoração) ---
sealed class LoginUiState {
    object Idle : LoginUiState()    // Esperando o usuário digitar
    object Loading : LoginUiState() // Girando a rodinha
    object Success : LoginUiState() // Login ok!
    data class Error(val message: String) : LoginUiState() // Deu ruim
}

class LoginViewModel(private val auth: FirebaseAuth) : ViewModel() {

    // Única fonte de verdade para a UI
    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun loginUser(email: String, senha: String) {
        if (email.isBlank() || senha.isBlank()) {
            _uiState.value = LoginUiState.Error("Preencha todos os campos.")
            return
        }

        // Entra no estado de Loading
        _uiState.value = LoginUiState.Loading

        auth.signInWithEmailAndPassword(email, senha)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _uiState.value = LoginUiState.Success
                } else {
                    val exception = task.exception
                    val errorMsg = when (exception) {
                        is FirebaseAuthInvalidUserException -> "Usuário não encontrado."
                        is FirebaseAuthInvalidCredentialsException -> "Credenciais inválidas."
                        else -> "Falha no login: ${exception?.message}"
                    }
                    _uiState.value = LoginUiState.Error(errorMsg)
                }
            }
    }

    // Reseta para Idle se necessário (ex: usuário apagou o erro e vai tentar de novo)
    fun resetState() {
        _uiState.value = LoginUiState.Idle
    }
}

// Factory (Mantida igual para compatibilidade, mas simplificada se quiser)
class LoginViewModelFactory(private val auth: FirebaseAuth) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LoginViewModel(auth) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}