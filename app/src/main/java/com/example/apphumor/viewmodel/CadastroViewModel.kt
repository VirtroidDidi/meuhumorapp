package com.example.apphumor.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.apphumor.models.User
import com.example.apphumor.repository.DatabaseRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class CadastroViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val dbRepository = DatabaseRepository()
    private val TAG = "CadastroViewModel"

    private val _cadastroSuccess = MutableLiveData<Boolean>()
    val cadastroSuccess: LiveData<Boolean> = _cadastroSuccess

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun registerUser(nome: String, email: String, senha: String, idade: Int) {
        _isLoading.value = true

        if (nome.isBlank() || email.isBlank() || senha.isBlank() || senha.length < 6) {
            _errorMessage.value = "Dados inválidos. Verifique todos os campos."
            _isLoading.value = false
            return
        }

        viewModelScope.launch {
            try {
                val authResult = auth.createUserWithEmailAndPassword(email, senha).await()
                val firebaseUser = authResult.user

                if (firebaseUser?.uid != null) {
                    val user = User(firebaseUser.uid, nome, email, idade)

                    // Chama a nova função suspend do repositório
                    val saveResult = dbRepository.saveUser(user)

                    _isLoading.value = false

                    if (saveResult is DatabaseRepository.Result.Success) {
                        _cadastroSuccess.value = true
                    } else {
                        _errorMessage.value = "Conta criada, mas falha ao salvar perfil."
                    }
                } else {
                    _isLoading.value = false
                    _errorMessage.value = "Erro interno: UID não gerado."
                }

            } catch (e: Exception) {
                _isLoading.value = false
                when (e) {
                    is FirebaseAuthWeakPasswordException ->
                        _errorMessage.value = "A senha é muito fraca."
                    is FirebaseAuthInvalidCredentialsException ->
                        _errorMessage.value = "E-mail inválido ou já em uso."
                    else -> {
                        Log.e(TAG, "Erro no cadastro", e)
                        _errorMessage.value = "Falha no cadastro: ${e.message}"
                    }
                }
            }
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}