package com.example.apphumor.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.apphumor.models.User
import com.example.apphumor.repository.DatabaseRepository
import com.example.apphumor.utils.ImageUtils
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.Locale

class ProfileViewModel(
    private val auth: FirebaseAuth,
    private val dbRepository: DatabaseRepository
) : ViewModel() {

    private val _userProfile = MutableLiveData<User?>()
    val userProfile: LiveData<User?> = _userProfile

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _updateStatus = MutableLiveData<String?>()
    val updateStatus: LiveData<String?> = _updateStatus

    private val _logoutEvent = MutableLiveData<Boolean>()
    val logoutEvent: LiveData<Boolean> = _logoutEvent

    // Rascunhos para edição
    private val _draftNotificationEnabled = MutableLiveData<Boolean>()
    private val _draftTime = MutableLiveData<Pair<Int, Int>>()
    val draftTime: LiveData<Pair<Int, Int>> = _draftTime

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        val userId = auth.currentUser?.uid ?: return
        _loading.value = true
        viewModelScope.launch {
            val result = dbRepository.getUser(userId)
            _loading.value = false
            if (result is DatabaseRepository.Result.Success) {
                _userProfile.value = result.data
                // Inicializa os rascunhos com os valores atuais
                result.data?.let { user ->
                    _draftNotificationEnabled.value = user.notificacaoAtiva
                    val timeParts = user.horarioNotificacao.split(":")
                    if (timeParts.size == 2) {
                        _draftTime.value = Pair(timeParts[0].toInt(), timeParts[1].toInt())
                    }
                }
            }
        }
    }

    // --- LÓGICA DE IMAGEM ---
    // Atualiza a foto assim que o usuário seleciona na galeria (sem esperar o botão Salvar)
    fun updatePhotoImmediately(context: Context, uri: Uri) {
        val currentUser = _userProfile.value ?: return
        _loading.value = true

        viewModelScope.launch {
            // 1. Converte URI para Base64 usando ImageUtils
            val base64Image = ImageUtils.uriToBase64(context, uri)

            if (base64Image != null) {
                // 2. Cria usuário com a nova foto
                val updatedUser = currentUser.copy(fotoBase64 = base64Image)

                // 3. Salva no Firebase
                val result = dbRepository.updateUser(updatedUser)

                if (result is DatabaseRepository.Result.Success) {
                    // 4. Atualiza a UI imediatamente
                    _userProfile.value = updatedUser
                    _updateStatus.value = "Foto atualizada!"
                } else {
                    _updateStatus.value = "Erro ao salvar foto."
                }
            } else {
                _updateStatus.value = "Erro ao processar imagem."
            }
            _loading.value = false
        }
    }

    fun setDraftNotificationEnabled(enabled: Boolean) {
        _draftNotificationEnabled.value = enabled
    }

    fun setDraftTime(hour: Int, minute: Int) {
        _draftTime.value = Pair(hour, minute)
    }

    fun saveAllChanges(newName: String, newAge: Int) {
        val currentUser = _userProfile.value ?: return
        // Mantém a foto que já está lá (seja antiga ou atualizada via updatePhotoImmediately)
        val finalPhoto = currentUser.fotoBase64

        val isNotifEnabled = _draftNotificationEnabled.value ?: currentUser.notificacaoAtiva
        val (hour, minute) = _draftTime.value ?: Pair(20, 0)
        val timeString = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)

        val updatedUser = currentUser.copy(
            nome = newName,
            idade = newAge,
            notificacaoAtiva = isNotifEnabled,
            horarioNotificacao = timeString,
            fotoBase64 = finalPhoto
        )

        _loading.value = true
        viewModelScope.launch {
            val result = dbRepository.updateUser(updatedUser)
            _loading.value = false
            if (result is DatabaseRepository.Result.Success) {
                _userProfile.value = updatedUser
                _updateStatus.value = "Dados salvos com sucesso!"
            } else {
                _updateStatus.value = "Falha ao salvar dados."
            }
        }
    }

    fun clearStatus() {
        _updateStatus.value = null
    }

    fun clearLogoutEvent() {
        _logoutEvent.value = false
    }

    fun logout() {
        auth.signOut()
        _logoutEvent.value = true
    }
}