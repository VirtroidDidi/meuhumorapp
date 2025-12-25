package com.example.apphumor.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException

/**
 * [LoginViewModel]
 * Refatorado para receber FirebaseAuth via construtor (Injeção de Dependência).
 */
class LoginViewModel(private val auth: FirebaseAuth) : ViewModel() {

    // REMOVIDO: private val auth = FirebaseAuth.getInstance() (Agora vem do construtor)

    private val _loginSuccess = MutableLiveData<Boolean>()
    val loginSuccess: LiveData<Boolean> = _loginSuccess

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: MutableLiveData<String?> = _errorMessage

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun loginUser(email: String, senha: String) {
        _isLoading.value = true

        if (email.isBlank() || senha.isBlank()) {
            _errorMessage.value = "Preencha todos os campos."
            _isLoading.value = false
            return
        }

        auth.signInWithEmailAndPassword(email, senha)
            .addOnCompleteListener { task ->
                _isLoading.value = false
                if (task.isSuccessful) {
                    _loginSuccess.value = true
                } else {
                    val exception = task.exception
                    when (exception) {
                        is FirebaseAuthInvalidUserException ->
                            _errorMessage.value = "Usuário não encontrado."
                        is FirebaseAuthInvalidCredentialsException ->
                            _errorMessage.value = "Senha incorreta ou credenciais inválidas."
                        else ->
                            _errorMessage.value = "Falha ao realizar o login. Tente novamente."
                    }
                }
            }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}