package com.example.apphumor.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException

/**
 * [LoginViewModel]
 * Gerencia a lógica de autenticação de login, controlando o estado de sucesso,
 * carregamento e possíveis erros do Firebase Auth.
 */
class LoginViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    // Estados observáveis pela View. Usamos MutableLiveData internamente para garantir que apenas o VM os modifique.
    private val _loginSuccess = MutableLiveData<Boolean>()
    private val _errorMessage = MutableLiveData<String?>()
    private val _isLoading = MutableLiveData<Boolean>()

    // LiveData exposto (Somente leitura para a Activity)
    val loginSuccess: LiveData<Boolean> = _loginSuccess
    val errorMessage: MutableLiveData<String?> = _errorMessage
    val isLoading: LiveData<Boolean> = _isLoading

    /**
     * Tenta autenticar o usuário com e-mail e senha.
     * Expõe o resultado via LiveData (sucesso ou mensagem de erro).
     * @param email O e-mail do usuário.
     * @param senha A senha do usuário.
     */
    fun loginUser(email: String, senha: String) {
        _isLoading.value = true

        // Validação de entrada para evitar chamadas desnecessárias ao Firebase
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
                    // Mapeamento e tratamento de exceções do Firebase para feedback claro
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

    /**
     * Limpa o estado de erro após o erro ter sido consumido pela View (exibido como Toast).
     * Isso impede que o Toast reapareça em mudanças de configuração (rotação de tela, etc.).
     */
    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}