// ARQUIVO: app/src/main/java/com/example/apphumor/ProfileFragment.kt

package com.example.apphumor

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.apphumor.databinding.FragmentTelaBBinding

// 1. SERVICE LOCATOR
import com.example.apphumor.di.DependencyProvider

// 2. VIEWMODEL FACTORY
import com.example.apphumor.viewmodel.ProfileViewModelFactory
import com.example.apphumor.models.User
import com.example.apphumor.viewmodel.ProfileViewModel
import com.example.apphumor.viewmodel.LoginActivity
import com.example.apphumor.R

// ===========================================
// NOVOS IMPORTS
// ===========================================
import android.app.TimePickerDialog
import com.example.apphumor.utils.NotificationScheduler // Import do Scheduler
import java.util.Locale
// ===========================================

/**
 * Fragmento de Perfil (antigo FragmentTelaB).
 */
class ProfileFragment : Fragment() {
    private var _binding: FragmentTelaBBinding? = null
    private val binding get() = _binding!!

    // ViewModel para gerenciar a lógica de dados do perfil
    private lateinit var viewModel: ProfileViewModel

    private val tag = "ProfileFragment"

    // Controle de UI local
    private var isEditing = false

    interface LogoutListener {
        fun onLogoutSuccess()
    }

    private var logoutListener: LogoutListener? = null

    // ===========================================
    // NOVAS VARIÁVEIS DE ESTADO TEMPORÁRIO
    // ===========================================
    // Variáveis temporárias para armazenar o horário escolhido
    private var notificationHour: Int = 20
    private var notificationMinute: Int = 0
    // Variável para armazenar o estado do switch temporariamente, sem salvar
    private var tempNotificationEnabled: Boolean = true
    // ===========================================


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTelaBBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // INICIALIZAÇÃO CORRETA DO VIEWMODEL COM FACTORY
        viewModel = ViewModelProvider(
            this,
            ProfileViewModelFactory(
                DependencyProvider.auth,
                DependencyProvider.databaseRepository
            )
        ).get(ProfileViewModel::class.java)

        setupListeners()
        setupObservers()

        // Inicializa a UI no modo de exibição
        setEditingMode(false)
    }

    private fun setupListeners() {
        // --- Listeners de Perfil e Logout (Existente) ---
        binding.btnEditProfile.setOnClickListener {
            if (isEditing) {
                // Ao clicar para SALVAR
                saveProfileChanges()
            } else {
                // Ao clicar para EDITAR
                setEditingMode(true)
                binding.etUserName.requestFocus()
            }
        }

        binding.btnLogoutFragment.setOnClickListener {
            viewModel.logout()
        }

        // --- Listeners de Notificação (AGORA APENAS ATUALIZAM O ESTADO LOCAL) ---

        // 1. Clique no texto da hora -> Abre o Relógio
        binding.tvSelectedTimePerfil.setOnClickListener {
            if (isEditing && binding.switchNotificacaoPerfil.isChecked) {
                showTimePickerDialog()
            } else if (isEditing) {
                Toast.makeText(context, "Ative o lembrete para selecionar o horário.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Pressione 'Editar Perfil' para alterar o horário.", Toast.LENGTH_SHORT).show()
            }
        }

        // 2. Clique no Switch -> Ativa/Desativa visualmente e ATUALIZA A VARIÁVEL TEMPORÁRIA
        binding.switchNotificacaoPerfil.setOnCheckedChangeListener { _, isChecked ->
            // Se estiver no modo de edição, apenas atualiza a variável temporária.
            if (isEditing) {
                tempNotificationEnabled = isChecked // ATUALIZA O ESTADO LOCALMENTE
                updateTimeUIState(isChecked)
            } else {
                // Quando não está editando, apenas chama a função para atualizar o estado visual
                updateTimeUIState(isChecked)
            }
        }
    }

    private fun setupObservers() {
        viewModel.userProfile.observe(viewLifecycleOwner) { user ->
            user?.let {
                displayProfileData(it)
                // Se saiu do modo de edição, exibe os dados atualizados
                setEditingMode(false)
            } ?: run {
                if (viewModel.firebaseAuthInstance.currentUser == null) {
                    logoutListener?.onLogoutSuccess()
                }
            }
        }

        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.btnEditProfile.isEnabled = !isLoading
            binding.btnLogoutFragment.isEnabled = !isLoading
        }

        viewModel.updateStatus.observe(viewLifecycleOwner) { status ->
            status?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.clearStatus()
            }
        }

        viewModel.logoutEvent.observe(viewLifecycleOwner) { loggedOut ->
            if (loggedOut) {
                logoutListener?.onLogoutSuccess()
                viewModel.clearLogoutEvent()
            }
        }
    }

    /**
     * Exibe os dados do usuário na UI.
     */
    private fun displayProfileData(user: User) {
        binding.etUserEmail.setText(user.email ?: viewModel.firebaseAuthInstance.currentUser?.email)
        binding.etUserName.setText(user.nome)
        binding.etUserIdade.setText(user.idade?.toString())

        val nomeExibicao = if (!user.nome.isNullOrEmpty()) user.nome else "Usuário"
        binding.tvWelcomeMessage.text = "Bem-vindo(a), $nomeExibicao!"

        // ===========================================
        // CARREGAR DADOS DE NOTIFICAÇÃO E INICIALIZAR VARIAVEIS TEMPORÁRIAS
        // ===========================================
        tempNotificationEnabled = user.notificacaoAtiva // Inicializa o estado TEMPORÁRIO com o valor do DB
        binding.switchNotificacaoPerfil.isChecked = tempNotificationEnabled
        binding.tvSelectedTimePerfil.text = user.horarioNotificacao

        // Parse do horário string "HH:mm" para inteiros
        val timeParts = user.horarioNotificacao.split(":")
        if (timeParts.size == 2) {
            notificationHour = timeParts[0].toIntOrNull() ?: 20
            notificationMinute = timeParts[1].toIntOrNull() ?: 0
        }

        updateTimeUIState(user.notificacaoAtiva)
        // ===========================================
    }

    // ===========================================
    // FUNÇÕES DE NOTIFICAÇÃO
    // ===========================================
    private fun updateTimeUIState(isEnabled: Boolean) {
        // Habilita/desabilita visualmente e logicamente os campos de notificação.
        binding.llHorarioContainerPerfil.alpha = if (isEnabled) 1.0f else 0.4f
        // A capacidade de clique final é decidida por setEditingMode
    }

    private fun showTimePickerDialog() {
        val timePicker = TimePickerDialog(requireContext(), { _, hour, minute ->
            notificationHour = hour
            notificationMinute = minute

            val formattedTime = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
            binding.tvSelectedTimePerfil.text = formattedTime

            // NÃO CHAMA O SALVAMENTO AQUI

        }, notificationHour, notificationMinute, true) // true = formato 24h

        timePicker.show()
    }

    private fun saveAndScheduleNotification(isEnabled: Boolean, time: String) {
        // 1. Salva no Firebase (via ViewModel)
        viewModel.updateNotificationPreferences(isEnabled, time)

        // 2. Agenda no Android (via Scheduler)
        NotificationScheduler.scheduleDailyReminder(
            requireContext(),
            notificationHour,
            notificationMinute,
            isEnabled
        )
    }
    // ===========================================

    /**
     * Alterna o modo de edição dos campos de texto e o texto do botão principal.
     */
    private fun setEditingMode(editing: Boolean) {
        this.isEditing = editing
        val context = context ?: return

        val editableFields = listOf(binding.etUserName, binding.etUserIdade)

        for (field in editableFields) {
            field.isEnabled = editing
            field.isFocusable = editing
            field.isFocusableInTouchMode = editing

            if (editing) {
                field.background = ContextCompat.getDrawable(context, R.drawable.edit_text_border)
            } else {
                field.background = ContextCompat.getDrawable(context, android.R.color.transparent)
            }
        }

        binding.etUserEmail.isEnabled = false
        binding.etUserEmail.background = ContextCompat.getDrawable(context, android.R.color.transparent)

        // =======================================================
        // LÓGICA DE EDIÇÃO DE NOTIFICAÇÃO: SÓ EDITA SE isEditing for TRUE
        // =======================================================
        binding.switchNotificacaoPerfil.isEnabled = editing

        // O TextView da hora é clicável apenas se estiver editando E o Switch estiver ligado
        val isTimeClickable = editing && binding.switchNotificacaoPerfil.isChecked
        binding.tvSelectedTimePerfil.isEnabled = isTimeClickable

        // Ajuste visual (Alpha)
        binding.switchNotificacaoPerfil.alpha = if (editing) 1.0f else 0.8f

        // Ajusta a visibilidade do container da hora
        binding.llHorarioContainerPerfil.alpha = if (isTimeClickable) 1.0f else 0.4f
        // =======================================================


        // Atualiza o botão
        if (editing) {
            binding.btnEditProfile.text = "Salvar Alterações"
            binding.btnEditProfile.setIconResource(R.drawable.ic_save_24)
        } else {
            binding.btnEditProfile.text = "Editar Perfil"
            binding.btnEditProfile.setIconResource(R.drawable.ic_edit_24)
        }

        // Esconde o botão de Logout durante a edição
        binding.btnLogoutFragment.visibility = if (editing) View.GONE else View.VISIBLE
    }

    /**
     * Coleta os dados dos campos, valida e salva TODAS as alterações (perfil e notificação).
     */
    private fun saveProfileChanges() {
        val newName = binding.etUserName.text.toString().trim()
        val newAgeString = binding.etUserIdade.text.toString().trim()

        val newAge = newAgeString.toIntOrNull()

        if (newName.isBlank() || newAge == null) {
            Toast.makeText(context, "Preencha o nome e a idade corretamente.", Toast.LENGTH_SHORT).show()
            return
        }

        // 1. Salva Nome e Idade (mantendo a lógica existente)
        viewModel.updateProfile(newName, newAge)

        // 2. Salva e Agenda as Preferências de Notificação (AGORA É SALVO AQUI)
        val newTime = String.format(Locale.getDefault(), "%02d:%02d", notificationHour, notificationMinute)

        // Usa a variável temporária 'tempNotificationEnabled' para o estado do switch
        saveAndScheduleNotification(tempNotificationEnabled, newTime)

        // Nota: setEditingMode(false) será chamado automaticamente quando o LiveData 'userProfile'
        // for atualizado após o salvamento.
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadUserProfile()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is LogoutListener) {
            logoutListener = context
        } else {
            Log.e(tag, "$context deve implementar LogoutListener para o logout.")
            logoutListener = object : LogoutListener {
                override fun onLogoutSuccess() {
                    val intent = Intent(requireActivity(), LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    requireActivity().finish()
                }
            }
        }
    }

    override fun onDetach() {
        super.onDetach()
        logoutListener = null
    }
}