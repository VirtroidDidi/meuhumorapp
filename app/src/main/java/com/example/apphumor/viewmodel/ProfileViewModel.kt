package com.example.apphumor.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.apphumor.models.User
import com.example.apphumor.repository.DatabaseRepository
import com.google.firebase.auth.FirebaseAuth

/**
 * [ProfileViewModel]
 * Gerencia o estado e as operações do perfil do usuário:
 * Carregamento, atualização de dados (nome, idade) e logout.
 */
class ProfileViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    // CORREÇÃO CRÍTICA: Inicializa o repositório ANTES de ser usado no init
    private val dbRepository = DatabaseRepository()

    // Expondo a autenticação APENAS para uso interno no Fragment (ex: displayUserData)
    // NOTA: Em um projeto mais limpo, isso seria evitado, mas mantido para facilidade de refatoração.
    val firebaseAuthInstance: FirebaseAuth
        get() = auth

    // Estados observáveis
    private val _userProfile = MutableLiveData<User?>()
    val userProfile: LiveData<User?> = _userProfile

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _updateStatus = MutableLiveData<String>()
    val updateStatus: LiveData<String> = _updateStatus

    private val _logoutEvent = MutableLiveData<Boolean>()
    val logoutEvent: LiveData<Boolean> = _logoutEvent

    init {
        // Agora, loadUserProfile é executado APÓS a inicialização de dbRepository
        loadUserProfile()
    }

    // --- Lógica de Carregamento ---

    fun loadUserProfile() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _userProfile.value = null
            return
        }

        _loading.value = true
        dbRepository.getUser(userId) { user ->
            _loading.value = false
            _userProfile.value = user
        }
    }

    // --- Lógica de Atualização ---

    fun updateProfile(newName: String, newAge: Int) {
        val currentUser = _userProfile.value
        val userId = auth.currentUser?.uid

        if (userId == null || currentUser == null) {
            _updateStatus.value = "Erro: Usuário não autenticado ou dados ausentes."
            return
        }

        if (newName.isEmpty()) {
            _updateStatus.value = "O nome não pode ser vazio."
            return
        }
        if (newAge <= 0 || newAge > 150) {
            _updateStatus.value = "Idade inválida. Use um número entre 1 e 150."
            return
        }

        val updatedUser = currentUser.copy(
            nome = newName,
            idade = newAge
        )

        _loading.value = true
        dbRepository.updateUser(updatedUser,
            onSuccess = {
                _loading.value = false
                _userProfile.value = updatedUser // Atualiza o LiveData com o novo perfil
                _updateStatus.value = "Perfil atualizado com sucesso!"
            },
            onError = { errorMsg ->
                _loading.value = false
                _updateStatus.value = "Falha ao salvar: $errorMsg"
            }
        )
    }

    // --- Lógica de Logout ---

    fun logout() {
        auth.signOut()
        _logoutEvent.value = true
    }

    fun clearStatus() {
        _updateStatus.value = null
    }

    fun clearLogoutEvent() {
        _logoutEvent.value = false
    }
}