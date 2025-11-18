package com.example.apphumor.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.apphumor.models.User
import com.example.apphumor.repository.DatabaseRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import android.util.Log

/**
 * [CadastroViewModel]
 * Gerencia a lógica de negócio para a criação de novos usuários,
 * incluindo autenticação no Firebase Auth e persistência de dados do perfil no Realtime Database.
 */
class CadastroViewModel : ViewModel() {

    // Dependências (seriam tipicamente injetadas via DI em um projeto de produção)
    private val auth = FirebaseAuth.getInstance()
    private val dbRepository = DatabaseRepository()
    private val TAG = "CadastroViewModel"

    // Estados observáveis pela View. MutableLiveData é usado internamente.
    private val _cadastroSuccess = MutableLiveData<Boolean>()
    private val _errorMessage = MutableLiveData<String>()
    private val _isLoading = MutableLiveData<Boolean>()

    // LiveData exposto (Somente leitura para a Activity)
    val cadastroSuccess: LiveData<Boolean> = _cadastroSuccess
    val errorMessage: LiveData<String> = _errorMessage
    val isLoading: LiveData<Boolean> = _isLoading

    // Método mantido para compatibilidade com a Activity, mas não recomendado para chamadas diretas.
    fun saveUser(user: User) {
        dbRepository.saveUser(user)
        Log.d(TAG, "Chamou saveUser no Repositório para o UID: ${user.uid}")
    }

    /**
     * Tenta criar um novo usuário no Firebase Auth e, se bem-sucedido,
     * salva os dados de perfil (nome, idade) no Realtime Database.
     * * @param nome Nome completo do usuário.
     * @param email Endereço de e-mail (usado para autenticação).
     * @param senha Senha (deve ter no mínimo 6 caracteres).
     * @param idade Idade do usuário.
     */
    fun registerUser(nome: String, email: String, senha: String, idade: Int) {
        _isLoading.value = true

        // 1. Validação de campos (proteção do serviço)
        if (nome.isBlank() || email.isBlank() || senha.isBlank() || senha.length < 6) {
            _errorMessage.value = "Dados de entrada inválidos. Verifique nome, email e senha (mínimo 6 caracteres)."
            _isLoading.value = false
            return
        }

        // 2. Criação do usuário no Firebase Authentication
        auth.createUserWithEmailAndPassword(email, senha)
            .addOnCompleteListener { task ->
                _isLoading.value = false
                if (task.isSuccessful) {
                    val userFirebase = auth.currentUser
                    userFirebase?.uid?.let { uid ->
                        val user = User(uid, nome, email, idade)

                        // 3. Persiste o perfil do usuário
                        dbRepository.saveUser(user)
                        _cadastroSuccess.value = true // Sinaliza sucesso
                    } ?: run {
                        _errorMessage.value = "Erro interno: UID não encontrado após criação de conta."
                    }

                } else {
                    // 4. Mapeamento e tratamento de exceções específicas do Firebase
                    val exception = task.exception
                    when (exception) {
                        is FirebaseAuthWeakPasswordException ->
                            _errorMessage.value = "A senha é muito fraca. Use pelo menos 6 caracteres."
                        is FirebaseAuthInvalidCredentialsException ->
                            _errorMessage.value = "Endereço de e-mail inválido ou já em uso."
                        else ->
                            _errorMessage.value = "Falha no cadastro. Tente novamente."
                    }
                }
            }
    }

    /**
     * Limpa o estado de erro após o erro ter sido consumido pela View (exibido como Toast).
     * Isso impede que o Toast reapareça em mudanças de configuração ou navegação.
     */
    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}