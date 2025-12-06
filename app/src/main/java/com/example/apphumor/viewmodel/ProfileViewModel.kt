// ARQUIVO: app/src/main/java/com/example/apphumor/viewmodel/ProfileViewModel.kt

package com.example.apphumor.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.apphumor.models.User
import com.example.apphumor.repository.DatabaseRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

/**
 * [ProfileViewModel]
 * Gerencia o estado e as operações do perfil do usuário.
 * Atualizado para usar Kotlin Coroutines com o DatabaseRepository.
 */
class ProfileViewModel(
    // Dependências injetadas
    private val auth: FirebaseAuth,
    private val dbRepository: DatabaseRepository
) : ViewModel() {

    // Expondo a autenticação APENAS para uso interno no Fragment
    val firebaseAuthInstance: FirebaseAuth
        get() = auth

    // Estados observáveis (Mantendo LiveData para compatibilidade com a View atual)
    private val _userProfile = MutableLiveData<User?>()
    val userProfile: LiveData<User?> = _userProfile

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _updateStatus = MutableLiveData<String?>()
    val updateStatus: MutableLiveData<String?> = _updateStatus

    private val _logoutEvent = MutableLiveData<Boolean>()
    val logoutEvent: LiveData<Boolean> = _logoutEvent

    init {
        loadUserProfile()
    }

    // --- Lógica de Carregamento (Refatorada para Coroutines) ---

    fun loadUserProfile() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _userProfile.value = null
            return
        }

        _loading.value = true

        // Lança uma Coroutine no escopo do ViewModel
        viewModelScope.launch {
            // Chama a função suspend do repositório
            val result = dbRepository.getUser(userId)

            _loading.value = false

            when (result) {
                is DatabaseRepository.Result.Success -> {
                    _userProfile.value = result.data
                }
                is DatabaseRepository.Result.Error -> {
                    // Em caso de erro no carregamento, podemos definir como null ou tratar o erro
                    _userProfile.value = null
                    // Opcional: _updateStatus.value = "Erro ao carregar: ${result.exception.message}"
                }
            }
        }
    }

    // --- Lógica de Atualização (Refatorada para Coroutines) ---

    fun updateProfile(newName: String, newAge: Int) {
        val currentUser = _userProfile.value
        val userId = auth.currentUser?.uid

        if (userId == null || currentUser == null) {
            _updateStatus.value = "Erro: Utilizador não autenticado ou dados ausentes."
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

        // Usa copy para criar uma nova instância (necessário com Data Classes imutáveis)
        val updatedUser = currentUser.copy(
            nome = newName,
            idade = newAge
        )

        _loading.value = true

        // Lança uma Coroutine para salvar
        viewModelScope.launch {
            val result = dbRepository.updateUser(updatedUser)

            _loading.value = false

            when (result) {
                is DatabaseRepository.Result.Success -> {
                    _userProfile.value = updatedUser // Atualiza a UI com os novos dados
                    _updateStatus.value = "Perfil atualizado com sucesso!"
                }
                is DatabaseRepository.Result.Error -> {
                    val errorMsg = result.exception.message ?: "Erro desconhecido"
                    _updateStatus.value = "Falha ao salvar: $errorMsg"
                }
            }
        }
    }

    // ====================================================================
    // 🔔 NOVO: Função para atualizar especificamente as preferências de notificação
    // ====================================================================
    fun updateNotificationPreferences(isEnabled: Boolean, newTime: String) {
        val currentUser = _userProfile.value
        val userId = auth.currentUser?.uid

        if (userId == null || currentUser == null) {
            _updateStatus.value = "Erro: Utilizador não autenticado ou dados ausentes."
            return
        }

        // Cria uma cópia do usuário com as novas configurações
        // A data class User precisa ter os campos 'notificacaoAtiva' e 'horarioNotificacao'
        val updatedUser = currentUser.copy(
            notificacaoAtiva = isEnabled,
            horarioNotificacao = newTime
        )

        viewModelScope.launch {
            // Salva no Firebase
            val result = dbRepository.updateUser(updatedUser)

            if (result is DatabaseRepository.Result.Success) {
                // Atualiza o LiveData local para refletir na UI imediatamente
                _userProfile.value = updatedUser
                _updateStatus.postValue("Preferências de lembrete salvas.") // Usando postValue por segurança em Coroutine
            } else {
                _updateStatus.postValue("Erro ao salvar preferências.")
            }
        }
    }
    // ====================================================================


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

/**
 * Factory personalizada para instanciar ProfileViewModel com as dependências necessárias.
 */
class ProfileViewModelFactory(
    private val auth: FirebaseAuth,
    private val dbRepository: DatabaseRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(auth, dbRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}