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
import java.util.Locale

/**
 * [ProfileViewModel]
 * Gerencia o estado e as operações do perfil do usuário.
 * Atualizado para suportar rascunhos de notificação e salvamento atômico.
 */
class ProfileViewModel(
    private val auth: FirebaseAuth,
    private val dbRepository: DatabaseRepository
) : ViewModel() {

    // Expondo a autenticação para o Fragment
    val firebaseAuthInstance: FirebaseAuth
        get() = auth

    // --- ESTADOS PRINCIPAIS ---
    private val _userProfile = MutableLiveData<User?>()
    val userProfile: LiveData<User?> = _userProfile

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _updateStatus = MutableLiveData<String?>()
    val updateStatus: LiveData<String?> = _updateStatus

    private val _logoutEvent = MutableLiveData<Boolean>()
    val logoutEvent: LiveData<Boolean> = _logoutEvent

    // --- ESTADO TEMPORÁRIO (RASCUNHO) ---
    // Guarda o horário e estado enquanto o usuário edita, para não perder na rotação de tela
    private val _draftNotificationEnabled = MutableLiveData<Boolean>()
    val draftNotificationEnabled: LiveData<Boolean> = _draftNotificationEnabled

    private val _draftTime = MutableLiveData<Pair<Int, Int>>() // Par: Hora, Minuto
    val draftTime: LiveData<Pair<Int, Int>> = _draftTime

    // --- EVENTOS DE EFEITO COLATERAL (Side Effects) ---
    // Dispara apenas quando o salvamento no banco foi SUCESSO.
    private val _scheduleNotificationEvent = MutableLiveData<Pair<Boolean, String>?>()
    val scheduleNotificationEvent: LiveData<Pair<Boolean, String>?> = _scheduleNotificationEvent

    init {
        loadUserProfile()
    }

    // --- LÓGICA DE CARREGAMENTO ---

    fun loadUserProfile() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _userProfile.value = null
            return
        }

        _loading.value = true

        viewModelScope.launch {
            val result = dbRepository.getUser(userId)
            _loading.value = false

            when (result) {
                is DatabaseRepository.Result.Success -> {
                    val user = result.data
                    _userProfile.value = user

                    // Inicializa o rascunho com os dados vindos do Firebase
                    user?.let {
                        _draftNotificationEnabled.value = it.notificacaoAtiva

                        val timeParts = it.horarioNotificacao.split(":")
                        if (timeParts.size == 2) {
                            val h = timeParts[0].toIntOrNull() ?: 20
                            val m = timeParts[1].toIntOrNull() ?: 0
                            _draftTime.value = Pair(h, m)
                        } else {
                            _draftTime.value = Pair(20, 0)
                        }
                    }
                }
                is DatabaseRepository.Result.Error -> {
                    _userProfile.value = null
                }
            }
        }
    }

    // --- MANIPULAÇÃO DE RASCUNHO (UI) ---

    fun setDraftNotificationEnabled(enabled: Boolean) {
        _draftNotificationEnabled.value = enabled
    }

    fun setDraftTime(hour: Int, minute: Int) {
        _draftTime.value = Pair(hour, minute)
    }

    fun clearScheduleEvent() {
        _scheduleNotificationEvent.value = null
    }

    // --- SALVAMENTO UNIFICADO ---

    /**
     * Salva todas as alterações do perfil (Nome, Idade e Notificações) de uma vez.
     */
    fun saveAllChanges(newName: String, newAge: Int) {
        val currentUser = _userProfile.value
        val userId = auth.currentUser?.uid

        if (userId == null || currentUser == null) {
            _updateStatus.value = "Erro: Usuário não autenticado."
            return
        }

        // Validações básicas
        if (newName.isBlank()) {
            _updateStatus.value = "O nome não pode ser vazio."
            return
        }
        if (newAge <= 0 || newAge > 150) {
            _updateStatus.value = "Idade inválida."
            return
        }

        // Prepara os valores finais baseados no rascunho
        val isNotifEnabled = _draftNotificationEnabled.value ?: currentUser.notificacaoAtiva
        val (hour, minute) = _draftTime.value ?: Pair(20, 0)
        val timeString = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)

        val updatedUser = currentUser.copy(
            nome = newName,
            idade = newAge,
            notificacaoAtiva = isNotifEnabled,
            horarioNotificacao = timeString
        )

        _loading.value = true

        viewModelScope.launch {
            val result = dbRepository.updateUser(updatedUser)
            _loading.value = false

            when (result) {
                is DatabaseRepository.Result.Success -> {
                    _userProfile.value = updatedUser
                    _updateStatus.value = "Perfil salvo com sucesso!"

                    // Notifica o Fragment para agendar o alarme no Android
                    _scheduleNotificationEvent.value = Pair(isNotifEnabled, timeString)
                }
                is DatabaseRepository.Result.Error -> {
                    val errorMsg = result.exception.message ?: "Erro desconhecido"
                    _updateStatus.value = "Falha ao salvar: $errorMsg"
                }
            }
        }
    }

    // --- OPERAÇÕES DE CONTA ---

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