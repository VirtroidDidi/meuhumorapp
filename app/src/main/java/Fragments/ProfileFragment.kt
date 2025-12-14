package com.example.apphumor

import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.apphumor.databinding.FragmentProfileBinding
import com.example.apphumor.di.DependencyProvider
import com.example.apphumor.utils.NotificationScheduler
import com.example.apphumor.viewmodel.LoginActivity
import com.example.apphumor.viewmodel.ProfileViewModel
import com.example.apphumor.viewmodel.ProfileViewModelFactory
import java.util.Locale

/**
 * [ProfileFragment]
 * Fragmento de Perfil atualizado para suportar rascunhos via ViewModel
 * e agendamento seguro de notificações.
 */
class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ProfileViewModel
    private var isEditing = false

    interface LogoutListener {
        fun onLogoutSuccess()
    }

    private var logoutListener: LogoutListener? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // Vincula ao novo nome do layout: fragment_profile
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(
            this,
            ProfileViewModelFactory(DependencyProvider.auth, DependencyProvider.databaseRepository)
        )[ProfileViewModel::class.java]

        setupListeners()
        setupObservers()
        setEditingMode(false)
    }

    private fun setupListeners() {
        binding.btnEditProfile.setOnClickListener {
            if (isEditing) {
                val newName = binding.etUserName.text.toString().trim()
                val newAge = binding.etUserIdade.text.toString().toIntOrNull() ?: 0
                viewModel.saveAllChanges(newName, newAge)
            } else {
                setEditingMode(true)
            }
        }

        binding.btnLogoutFragment.setOnClickListener { viewModel.logout() }

        binding.tvSelectedTimePerfil.setOnClickListener {
            if (isEditing && binding.switchNotificacaoPerfil.isChecked) showTimePickerDialog()
        }

        binding.switchNotificacaoPerfil.setOnCheckedChangeListener { _, isChecked ->
            if (isEditing) viewModel.setDraftNotificationEnabled(isChecked)
        }
    }

    private fun setupObservers() {
        // Observa dados reais do banco
        viewModel.userProfile.observe(viewLifecycleOwner) { user ->
            user?.let {
                binding.etUserEmail.setText(it.email)
                binding.etUserName.setText(it.nome)
                binding.etUserIdade.setText(it.idade.toString())
                binding.tvWelcomeMessage.text = getString(R.string.welcome_format, it.nome ?: "Usuário")
            }
        }

        // Observa estado de rascunho do Switch (Notificação)
        viewModel.draftNotificationEnabled.observe(viewLifecycleOwner) { isEnabled ->
            binding.switchNotificacaoPerfil.isChecked = isEnabled
            binding.llHorarioContainerPerfil.alpha = if (isEnabled) 1.0f else 0.4f
        }

        // Observa estado de rascunho da Hora
        viewModel.draftTime.observe(viewLifecycleOwner) { (h, m) ->
            binding.tvSelectedTimePerfil.text = String.format(Locale.getDefault(), "%02d:%02d", h, m)
        }

        // Feedback visual de carregamento
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBarProfile.isVisible = isLoading
            binding.btnEditProfile.isEnabled = !isLoading
        }

        // Status de salvamento e reset de modo
        viewModel.updateStatus.observe(viewLifecycleOwner) { status ->
            status?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                if (it.contains("sucesso", ignoreCase = true)) setEditingMode(false)
                viewModel.clearStatus()
            }
        }

        // AGENDAMENTO SEGURO: Só ocorre após confirmação de sucesso do banco
        viewModel.scheduleNotificationEvent.observe(viewLifecycleOwner) { event ->
            event?.let { (enabled, time) ->
                val parts = time.split(":")
                NotificationScheduler.scheduleDailyReminder(requireContext(), parts[0].toInt(), parts[1].toInt(), enabled)
                viewModel.clearScheduleEvent()
            }
        }

        viewModel.logoutEvent.observe(viewLifecycleOwner) { loggedOut ->
            if (loggedOut) {
                logoutListener?.onLogoutSuccess()
                viewModel.clearLogoutEvent()
            }
        }
    }

    private fun showTimePickerDialog() {
        val current = viewModel.draftTime.value ?: Pair(20, 0)
        TimePickerDialog(requireContext(), { _, h, m ->
            viewModel.setDraftTime(h, m)
        }, current.first, current.second, true).show()
    }

    private fun setEditingMode(editing: Boolean) {
        isEditing = editing
        binding.etUserName.isEnabled = editing
        binding.etUserIdade.isEnabled = editing
        binding.switchNotificacaoPerfil.isEnabled = editing
        binding.tvSelectedTimePerfil.isEnabled = editing && binding.switchNotificacaoPerfil.isChecked

        binding.btnEditProfile.text = if (editing) getString(R.string.action_save_changes) else getString(R.string.action_edit_profile)
        binding.btnEditProfile.setIconResource(if (editing) R.drawable.ic_save_24 else R.drawable.ic_edit_24)
        binding.btnLogoutFragment.isVisible = !editing
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is LogoutListener) logoutListener = context
    }

    override fun onDetach() {
        super.onDetach()
        logoutListener = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}