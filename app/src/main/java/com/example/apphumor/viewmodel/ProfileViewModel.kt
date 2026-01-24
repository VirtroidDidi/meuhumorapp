package com.example.apphumor.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.apphumor.models.HumorNote
import com.example.apphumor.models.HumorType // [NOVO] Import do Enum
import com.example.apphumor.models.User
import com.example.apphumor.repository.DatabaseRepository
import com.example.apphumor.utils.ImageUtils
import com.example.apphumor.utils.InsightAnalysis
import com.example.apphumor.utils.InsightResult
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale


data class MoodStat(
    val labelRes: Int,
    val count: Int,
    val colorRes: Int
)

class ProfileViewModel(
    private val auth: FirebaseAuth,
    private val dbRepository: DatabaseRepository
) : ViewModel() {

    private val _insight = MutableLiveData<InsightResult>()
    val insight: LiveData<InsightResult> = _insight
    // --- ESTADOS ---
    private val _userProfile = MutableLiveData<User?>()
    val userProfile: LiveData<User?> = _userProfile

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _updateStatus = MutableLiveData<String?>()
    val updateStatus: LiveData<String?> = _updateStatus

    private val _logoutEvent = MutableLiveData<Boolean>()
    val logoutEvent: LiveData<Boolean> = _logoutEvent

    // --- RASCUNHOS (DRAFT) ---
    private val _draftNotificationEnabled = MutableLiveData<Boolean>()
    val draftNotificationEnabled: LiveData<Boolean> = _draftNotificationEnabled

    private val _draftTime = MutableLiveData<Pair<Int, Int>>()
    val draftTime: LiveData<Pair<Int, Int>> = _draftTime

    // Rascunho da Foto (apenas para exibição imediata)
    private val _draftPhotoBase64 = MutableLiveData<String?>()
    val draftPhotoBase64: LiveData<String?> = _draftPhotoBase64

    private val _scheduleNotificationEvent = MutableLiveData<Pair<Boolean, String>?>()
    val scheduleNotificationEvent: LiveData<Pair<Boolean, String>?> = _scheduleNotificationEvent

    private val _moodStats = MutableLiveData<List<MoodStat>>()
    val moodStats: LiveData<List<MoodStat>> = _moodStats

    init {
        loadUserProfile()
        loadMoodStats()
    }


    fun loadMoodStats() {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            // Buscamos todas as notas
            val notes = dbRepository.getHumorNotesOnce(userId)

            if (notes.isNotEmpty()) {
                // 1. O GRÁFICO (Atualizado para usar HumorType)
                val stats = notes
                    .groupingBy {
                        // Agrupa usando o Enum (resolve legados automaticamente)
                        HumorType.fromKey(it.humor)
                    }
                    .eachCount()
                    .map { (type, count) ->
                        // Agora pegamos as cores e textos direto do Enum, sem HumorUtils
                        MoodStat(
                            labelRes = type.labelRes,
                            count = count,
                            colorRes = type.colorRes
                        )
                    }
                    .sortedByDescending { it.count }
                _moodStats.value = stats

                // 2. O INSIGHT INTELIGENTE
                val smartInsight = InsightAnalysis.generateInsight(notes)
                _insight.value = smartInsight

            } else {
                _moodStats.value = emptyList()
                _insight.value = InsightAnalysis.generateInsight(emptyList())
            }
        }
    }

    fun loadUserProfile() {
        val userId = auth.currentUser?.uid ?: return
        _loading.value = true

        viewModelScope.launch {
            val result = dbRepository.getUser(userId)
            _loading.value = false
            if (result is DatabaseRepository.Result.Success) {
                val user = result.data
                _userProfile.value = user
                user?.let {
                    _draftNotificationEnabled.value = it.notificacaoAtiva
                    _draftPhotoBase64.value = it.fotoBase64 // Carrega a foto atual
                    val parts = it.horarioNotificacao.split(":")
                    if (parts.size == 2) {
                        _draftTime.value =
                            Pair(parts[0].toIntOrNull() ?: 20, parts[1].toIntOrNull() ?: 0)
                    }
                }
            }
        }
    }

    // --- NOVA FUNÇÃO: SALVA A FOTO IMEDIATAMENTE ---
    fun updatePhotoImmediately(context: Context, uri: Uri) {
        _loading.value = true // Mostra loading

        viewModelScope.launch(Dispatchers.IO) {
            // 1. Processa a imagem em Background
            val base64 = ImageUtils.uriToBase64(context, uri)

            if (base64 != null) {
                // 2. Atualiza a visualização local
                _draftPhotoBase64.postValue(base64)

                // 3. Prepara o objeto atualizado
                val currentUser = _userProfile.value
                if (currentUser != null) {
                    val updatedUser = currentUser.copy(fotoBase64 = base64)

                    // 4. Salva no Firebase AGORA
                    val result = dbRepository.updateUser(updatedUser)

                    if (result is DatabaseRepository.Result.Success) {
                        // Sucesso: Atualiza o perfil oficial na memória
                        _userProfile.postValue(updatedUser)
                        _updateStatus.postValue("Foto de perfil atualizada!")
                    } else {
                        _updateStatus.postValue("Erro ao salvar foto.")
                    }
                }
            } else {
                _updateStatus.postValue("Erro ao processar imagem.")
            }
            _loading.postValue(false) // Esconde loading
        }
    }

    // Apenas atualiza o draft (se precisar exibir sem salvar, mas agora usamos o de cima)
    fun processSelectedImage(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val base64 = ImageUtils.uriToBase64(context, uri)
            _draftPhotoBase64.postValue(base64)
        }
    }

    // Setters de Draft
    fun setDraftNotificationEnabled(enabled: Boolean) {
        _draftNotificationEnabled.value = enabled
    }

    fun setDraftTime(hour: Int, minute: Int) {
        _draftTime.value = Pair(hour, minute)
    }

    fun clearScheduleEvent() {
        _scheduleNotificationEvent.value = null
    }

    fun clearStatus() {
        _updateStatus.value = null
    }

    fun clearLogoutEvent() {
        _logoutEvent.value = false
    }

    // Salva APENAS Nome, Idade e Configurações (A foto já foi salva antes)
    fun saveAllChanges(newName: String, newAge: Int) {
        val currentUser = _userProfile.value ?: return

        // Mantém a foto que já está salva no usuário (ou a nova se tiver atualizado o objeto)
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
                _scheduleNotificationEvent.value = Pair(isNotifEnabled, timeString)
                setEditingMode(false)
            } else {
                _updateStatus.value = "Falha ao salvar dados."
            }
        }
    }

    private fun setEditingMode(editing: Boolean) { /* Helper se necessário */
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