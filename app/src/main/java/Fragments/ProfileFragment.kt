// ARQUIVO: app/src/main/java/com/example/apphumor/ProfileFragment.kt

package com.example.apphumor

import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.apphumor.databinding.FragmentTelaBBinding
import com.example.apphumor.di.DependencyProvider
import com.example.apphumor.models.User
import com.example.apphumor.utils.NotificationScheduler
import com.example.apphumor.viewmodel.LoginActivity
import com.example.apphumor.viewmodel.ProfileViewModel
import com.example.apphumor.viewmodel.ProfileViewModelFactory
import java.util.Locale

/**
 * Fragmento de Perfil.
 * Gerencia a exibição e edição dos dados do usuário e preferências de notificação.
 */
class ProfileFragment : Fragment() {
    private var _binding: FragmentTelaBBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ProfileViewModel
    private val tag = "ProfileFragment"
    private var isEditing = false

    interface LogoutListener {
        fun onLogoutSuccess()
    }

    private var logoutListener: LogoutListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTelaBBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(
            this,
            ProfileViewModelFactory(
                DependencyProvider.auth,
                DependencyProvider.databaseRepository
            )
        ).get(ProfileViewModel::class.java)

        setupListeners()
        setupObservers()

        setEditingMode(false)
    }

    private fun setupListeners() {
        binding.btnEditProfile.setOnClickListener {
            if (isEditing) {
                saveProfileChanges()
            } else {
                setEditingMode(true)
                binding.etUserName.requestFocus()
            }
        }

        binding.btnLogoutFragment.setOnClickListener {
            viewModel.logout()
        }

        binding.tvSelectedTimePerfil.setOnClickListener {
            if (isEditing && binding.switchNotificacaoPerfil.isChecked) {
                showTimePickerDialog()
            } else if (isEditing) {
                Toast.makeText(context, "Ative o lembrete para selecionar o horário.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.switchNotificacaoPerfil.setOnCheckedChangeListener { _, isChecked ->
            if (isEditing) {
                viewModel.setDraftNotificationEnabled(isChecked)
            }
        }
    }

    private fun setupObservers() {
        // 1. Dados do Usuário
        viewModel.userProfile.observe(viewLifecycleOwner) { user ->
            user?.let {
                displayProfileData(it)
            } ?: run {
                if (viewModel.firebaseAuthInstance.currentUser == null) {
                    logoutListener?.onLogoutSuccess()
                }
            }
        }

        // 2. Loading com suporte a ProgressBar
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.btnEditProfile.isEnabled = !isLoading
            binding.btnLogoutFragment.isEnabled = !isLoading

            // Exibe ou esconde a ProgressBar adicionada no XML
            binding.progressBarProfile?.isVisible = isLoading
        }

        // 3. Status de Atualização
        viewModel.updateStatus.observe(viewLifecycleOwner) { status ->
            status?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.clearStatus()

                if (it.contains("sucesso", ignoreCase = true)) {
                    setEditingMode(false)
                }
            }
        }

        // 4. Logout
        viewModel.logoutEvent.observe(viewLifecycleOwner) { loggedOut ->
            if (loggedOut) {
                logoutListener?.onLogoutSuccess()
                viewModel.clearLogoutEvent()
            }
        }

        // --- OBSERVADORES DO RASCUNHO (DRAFT) ---
        viewModel.draftNotificationEnabled.observe(viewLifecycleOwner) { isEnabled ->
            if (binding.switchNotificacaoPerfil.isChecked != isEnabled) {
                binding.switchNotificacaoPerfil.isChecked = isEnabled
            }
            updateTimeUIState(isEnabled)
        }

        viewModel.draftTime.observe(viewLifecycleOwner) { (hour, minute) ->
            val formattedTime = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
            binding.tvSelectedTimePerfil.text = formattedTime
        }

        viewModel.scheduleNotificationEvent.observe(viewLifecycleOwner) { event ->
            event?.let { (isEnabled, timeString) ->
                val parts = timeString.split(":")
                val h = parts[0].toInt()
                val m = parts[1].toInt()

                NotificationScheduler.scheduleDailyReminder(requireContext(), h, m, isEnabled)
                viewModel.clearScheduleEvent()
            }
        }
    }

    private fun displayProfileData(user: User) {
        binding.etUserEmail.setText(user.email ?: viewModel.firebaseAuthInstance.currentUser?.email)
        binding.etUserName.setText(user.nome)
        binding.etUserIdade.setText(user.idade?.toString())

        val nomeExibicao = if (!user.nome.isNullOrEmpty()) user.nome else "Usuário"
        binding.tvWelcomeMessage.text = "Bem-vindo(a), $nomeExibicao!"
    }

    private fun updateTimeUIState(isEnabled: Boolean) {
        binding.llHorarioContainerPerfil.alpha = if (isEnabled) 1.0f else 0.4f
        binding.tvSelectedTimePerfil.isEnabled = isEditing && isEnabled
    }

    private fun showTimePickerDialog() {
        val current = viewModel.draftTime.value ?: Pair(20, 0)
        val timePicker = TimePickerDialog(requireContext(), { _, hour, minute ->
            viewModel.setDraftTime(hour, minute)
        }, current.first, current.second, true)
        timePicker.show()
    }

    private fun saveProfileChanges() {
        val newName = binding.etUserName.text.toString().trim()
        val newAge = binding.etUserIdade.text.toString().trim().toIntOrNull() ?: 0
        viewModel.saveAllChanges(newName, newAge)
    }

    private fun setEditingMode(editing: Boolean) {
        this.isEditing = editing

        // Simplificação: isEnabled já controla a aparência e interação através do TextInputLayout
        binding.etUserName.isEnabled = editing
        binding.etUserIdade.isEnabled = editing
        binding.etUserEmail.isEnabled = false

        binding.switchNotificacaoPerfil.isEnabled = editing

        val isTimeClickable = editing && binding.switchNotificacaoPerfil.isChecked
        binding.tvSelectedTimePerfil.isEnabled = isTimeClickable
        binding.llHorarioContainerPerfil.alpha = if (isTimeClickable) 1.0f else 0.4f

        if (editing) {
            binding.btnEditProfile.text = "Salvar Alterações"
            binding.btnEditProfile.setIconResource(R.drawable.ic_save_24)
        } else {
            binding.btnEditProfile.text = "Editar Perfil"
            binding.btnEditProfile.setIconResource(R.drawable.ic_edit_24)
        }

        binding.btnLogoutFragment.visibility = if (editing) View.GONE else View.VISIBLE
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
        }
    }

    override fun onDetach() {
        super.onDetach()
        logoutListener = null
    }
}