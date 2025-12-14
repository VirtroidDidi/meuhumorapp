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

    val firebaseAuthInstance: FirebaseAuth get() = auth

    // --- ESTADOS PRINCIPAIS ---
    private val _userProfile = MutableLiveData<User?>()
    val userProfile: LiveData<User?> = _userProfile

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _updateStatus = MutableLiveData<String?>()
    val updateStatus: LiveData<String?> = _updateStatus

    private val _logoutEvent = MutableLiveData<Boolean>()
    val logoutEvent: LiveData<Boolean> = _logoutEvent

    // --- ESTADO TEMPORÁRIO (RASCUNHO / DRAFT) ---
    // Mover essas variáveis para cá evita perda de dados na rotação de tela
    private val _draftNotificationEnabled = MutableLiveData<Boolean>()
    val draftNotificationEnabled: LiveData<Boolean> = _draftNotificationEnabled

    private val _draftTime = MutableLiveData<Pair<Int, Int>>() // Par: Hora, Minuto
    val draftTime: LiveData<Pair<Int, Int>> = _draftTime

    // Evento de agendamento: dispara apenas quando o Firebase confirma o sucesso
    private val _scheduleNotificationEvent = MutableLiveData<Pair<Boolean, String>?>()
    val scheduleNotificationEvent: LiveData<Pair<Boolean, String>?> = _scheduleNotificationEvent

    init {
        loadUserProfile()
    }

    // --- LÓGICA DE CARREGAMENTO ---
    fun loadUserProfile() {
        val userId = auth.currentUser?.uid ?: return
        _loading.value = true

        viewModelScope.launch {
            val result = dbRepository.getUser(userId)
            _loading.value = false

            if (result is DatabaseRepository.Result.Success) {
                val user = result.data
                _userProfile.value = user

                // Inicializa o rascunho com os dados vindos do banco
                user?.let {
                    _draftNotificationEnabled.value = it.notificacaoAtiva
                    val parts = it.horarioNotificacao.split(":")
                    if (parts.size == 2) {
                        _draftTime.value = Pair(parts[0].toIntOrNull() ?: 20, parts[1].toIntOrNull() ?: 0)
                    } else {
                        _draftTime.value = Pair(20, 0)
                    }
                }
            }
        }
    }

    // --- MANIPULAÇÃO DE RASCUNHO (DRAFT) ---
    fun setDraftNotificationEnabled(enabled: Boolean) { _draftNotificationEnabled.value = enabled }
    fun setDraftTime(hour: Int, minute: Int) { _draftTime.value = Pair(hour, minute) }
    fun clearScheduleEvent() { _scheduleNotificationEvent.value = null }
    fun clearStatus() { _updateStatus.value = null }
    fun clearLogoutEvent() { _logoutEvent.value = false }

    // --- SALVAMENTO UNIFICADO ---
    fun saveAllChanges(newName: String, newAge: Int) {
        val currentUser = _userProfile.value ?: return
        val userId = auth.currentUser?.uid ?: return

        if (newName.isBlank()) {
            _updateStatus.value = "O nome não pode ser vazio."
            return
        }

        // Consolida rascunho com o objeto User
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

            if (result is DatabaseRepository.Result.Success) {
                _userProfile.value = updatedUser
                _updateStatus.value = "Perfil salvo com sucesso!"
                // Só dispara o agendamento local após o sucesso remoto
                _scheduleNotificationEvent.value = Pair(isNotifEnabled, timeString)
            } else {
                _updateStatus.value = "Falha ao salvar alterações."
            }
        }
    }

    fun logout() {
        auth.signOut()
        _logoutEvent.value = true
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