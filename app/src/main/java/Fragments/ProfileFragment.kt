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

// 1. SERVICE LOCATOR (usando 'di' como padrão final)
import com.example.apphumor.di.DependencyProvider

// 2. IMPORT DA CLASSE DO VIEWMODEL FACTORY
import com.example.apphumor.viewmodel.ProfileViewModelFactory // <--- ESSA LINHA É CRÍTICA!
import com.example.apphumor.models.User
import com.example.apphumor.viewmodel.ProfileViewModel
import com.example.apphumor.viewmodel.LoginActivity
import com.example.apphumor.R

/**
 * Fragmento de Perfil (antigo FragmentTelaB).
 */
class ProfileFragment : Fragment() {
    private var _binding: FragmentTelaBBinding? = null
    private val binding get() = _binding!!

    // ViewModel para gerenciar a lógica de dados do perfil
    private lateinit var viewModel: ProfileViewModel

    // Convenção de nomes (lowerCamelCase para variáveis)
    private val tag = "ProfileFragment"

    // Controle de UI local
    private var isEditing = false

    // Interface para comunicar o evento de Logout para a Activity
    interface LogoutListener {
        fun onLogoutSuccess()
    }

    // Variável para a interface de callback
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
    }

    private fun setupObservers() {
        viewModel.userProfile.observe(viewLifecycleOwner) { user ->
            user?.let {
                displayProfileData(it)
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
    }

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
     * Coleta os dados dos campos e delega a validação e o salvamento ao ViewModel.
     */
    private fun saveProfileChanges() {
        val newName = binding.etUserName.text.toString().trim()
        val newAgeString = binding.etUserIdade.text.toString().trim()

        val newAge = newAgeString.toIntOrNull()

        if (newName.isBlank() || newAge == null) {
            Toast.makeText(context, "Preencha o nome e a idade corretamente.", Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.updateProfile(newName, newAge)
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