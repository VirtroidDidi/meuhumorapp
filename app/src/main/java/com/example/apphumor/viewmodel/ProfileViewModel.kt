package com.example.apphumor.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.apphumor.models.HumorNote
import com.example.apphumor.models.HumorType
import com.example.apphumor.models.User
import com.example.apphumor.repository.DatabaseRepository
import com.example.apphumor.utils.ImageUtils
import com.example.apphumor.utils.InsightAnalysis
import com.example.apphumor.utils.InsightResult
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

// --- [CORREÇÃO] A CLASSE QUE FALTAVA ---
data class MoodStat(
    val labelRes: Int,
    val count: Int,
    val colorRes: Int
)
// ---------------------------------------

// 1. ESTADO DE EXIBIÇÃO DA TELA (Dados carregados)
sealed interface ProfileUiState {
    object Loading : ProfileUiState
    data class Error(val message: String) : ProfileUiState
    data class Content(
        val user: User,
        val moodStats: List<MoodStat>,
        val insight: InsightResult
    ) : ProfileUiState
}

// 2. ESTADO DE AÇÃO (Salvar/Editar)
sealed interface ProfileSaveState {
    object Idle : ProfileSaveState
    object Saving : ProfileSaveState
    data class Success(val message: String) : ProfileSaveState
    data class Error(val message: String) : ProfileSaveState
}

class ProfileViewModel(
    private val auth: FirebaseAuth,
    private val dbRepository: DatabaseRepository
) : ViewModel() {

    // Estados Reativos (Flows)
    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _saveState = MutableStateFlow<ProfileSaveState>(ProfileSaveState.Idle)
    val saveState: StateFlow<ProfileSaveState> = _saveState.asStateFlow()

    // Estados internos para Rascunho (Drafts)
    private val _draftPhotoBase64 = MutableStateFlow<String?>(null)
    val draftPhotoBase64: StateFlow<String?> = _draftPhotoBase64.asStateFlow()

    private val _draftNotificationEnabled = MutableStateFlow(true)
    val draftNotificationEnabled: StateFlow<Boolean> = _draftNotificationEnabled.asStateFlow()

    private val _draftTime = MutableStateFlow(Pair(20, 0))
    val draftTime: StateFlow<Pair<Int, Int>> = _draftTime.asStateFlow()

    private val _scheduleNotificationEvent = MutableStateFlow<Pair<Boolean, String>?>(null)
    val scheduleNotificationEvent: StateFlow<Pair<Boolean, String>?> = _scheduleNotificationEvent.asStateFlow()

    private val _logoutEvent = MutableStateFlow(false)
    val logoutEvent: StateFlow<Boolean> = _logoutEvent.asStateFlow()

    init {
        loadData()
    }

    // Carrega TUDO de uma vez (Parallel Fetch)
    private fun loadData() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _uiState.value = ProfileUiState.Error("Usuário não logado.")
            return
        }

        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            try {
                // Roda as duas buscas em paralelo para ser mais rápido
                val userDeferred = async { dbRepository.getUser(userId) }
                val notesDeferred = async { dbRepository.getHumorNotesOnce(userId) }

                val userResult = userDeferred.await()
                val notes = notesDeferred.await()

                if (userResult is DatabaseRepository.Result.Success && userResult.data != null) {
                    val user = userResult.data

                    // Processa estatísticas
                    val stats = processStats(notes)
                    val insight = InsightAnalysis.generateInsight(notes)

                    // Prepara drafts iniciais
                    _draftNotificationEnabled.value = user.notificacaoAtiva
                    _draftPhotoBase64.value = user.fotoBase64
                    val parts = user.horarioNotificacao.split(":")
                    if (parts.size == 2) {
                        _draftTime.value = Pair(parts[0].toIntOrNull() ?: 20, parts[1].toIntOrNull() ?: 0)
                    }

                    // Emite SUCESSO com todos os dados prontos
                    _uiState.value = ProfileUiState.Content(user, stats, insight)
                } else {
                    _uiState.value = ProfileUiState.Error("Falha ao carregar perfil.")
                }
            } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error(e.message ?: "Erro desconhecido")
            }
        }
    }

    private fun processStats(notes: List<HumorNote>): List<MoodStat> {
        if (notes.isEmpty()) return emptyList()
        return notes
            .groupingBy { HumorType.fromKey(it.humor) }
            .eachCount()
            .map { (type, count) ->
                MoodStat(type.labelRes, count, type.colorRes)
            }
            .sortedByDescending { it.count }
    }

    // --- AÇÕES DE SALVAMENTO ---

    fun saveAllChanges(newName: String, newAge: Int) {
        val currentState = _uiState.value
        if (currentState !is ProfileUiState.Content) return // Só salva se tiver dados carregados

        val currentUser = currentState.user
        val finalPhoto = currentState.user.fotoBase64 // A foto já foi salva antes

        val isNotifEnabled = _draftNotificationEnabled.value
        val (hour, minute) = _draftTime.value
        val timeString = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)

        val updatedUser = currentUser.copy(
            nome = newName,
            idade = newAge,
            notificacaoAtiva = isNotifEnabled,
            horarioNotificacao = timeString,
            fotoBase64 = finalPhoto
        )

        viewModelScope.launch {
            _saveState.value = ProfileSaveState.Saving
            val result = dbRepository.updateUser(updatedUser)

            if (result is DatabaseRepository.Result.Success) {
                // Atualiza o estado da tela com os dados novos
                _uiState.value = currentState.copy(user = updatedUser)
                _scheduleNotificationEvent.value = Pair(isNotifEnabled, timeString)
                _saveState.value = ProfileSaveState.Success("Dados salvos com sucesso!")
            } else {
                _saveState.value = ProfileSaveState.Error("Falha ao salvar dados.")
            }
        }
    }

    fun updatePhotoImmediately(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _saveState.value = ProfileSaveState.Saving

            // TODO: [DÉBITO TÉCNICO - MVP]
            // Decisão Arquitetural: Estamos convertendo a imagem para Base64 e salvando
            // diretamente no Realtime Database para reduzir custos e complexidade inicial (Serverless).
            //
            // RISCO: Strings Base64 aumentam drasticamente o payload do JSON.
            // SOLUÇÃO FUTURA:
            // 1. Upload da imagem para o Firebase Storage (Bucket).
            // 2. Obter a URL pública (Download URL).
            // 3. Salvar apenas a URL (String curta) no banco de dados.
            val base64 = ImageUtils.uriToBase64(context, uri)

            if (base64 != null) {
                _draftPhotoBase64.value = base64 // Atualiza visual imediato

                val currentState = _uiState.value
                if (currentState is ProfileUiState.Content) {
                    val updatedUser = currentState.user.copy(fotoBase64 = base64)
                    val result = dbRepository.updateUser(updatedUser)

                    if (result is DatabaseRepository.Result.Success) {
                        _uiState.value = currentState.copy(user = updatedUser)
                        _saveState.value = ProfileSaveState.Success("Foto atualizada!")
                    } else {
                        _saveState.value = ProfileSaveState.Error("Erro ao salvar foto.")
                    }
                }
            } else {
                _saveState.value = ProfileSaveState.Error("Erro ao processar imagem.")
            }
        }
    }

    // Setters e Clears
    fun setDraftNotificationEnabled(enabled: Boolean) { _draftNotificationEnabled.value = enabled }
    fun setDraftTime(hour: Int, minute: Int) { _draftTime.value = Pair(hour, minute) }
    fun clearScheduleEvent() { _scheduleNotificationEvent.value = null }
    fun clearSaveStatus() { _saveState.value = ProfileSaveState.Idle }
    fun clearLogoutEvent() { _logoutEvent.value = false }

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