package com.example.apphumor

import android.content.Context
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
import com.example.apphumor.models.User
import com.example.apphumor.viewmodel.ProfileViewModel // Import do ViewModel
// É necessário importar R se você usar recursos (drawables/colors) sem o R.
// import com.example.apphumor.R


/**
 * Fragmento de Perfil (antigo FragmentTelaB).
 * Agora usa ProfileViewModel para gerenciar o estado e as operações do perfil (carregar, editar, logout).
 */
class ProfileFragment : Fragment() {
    private var _binding: FragmentTelaBBinding? = null
    private val binding get() = _binding!!

    // ViewModel para gerenciar a lógica de dados do perfil
    private lateinit var viewModel: ProfileViewModel

    private val TAG = "ProfileFragment"

    // Controle de UI local
    private var isEditing = false

    // Interface para comunicar o evento de Logout para a Activity
    interface LogoutListener {
        fun onLogoutSuccess()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTelaBBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializa o ViewModel
        viewModel = ViewModelProvider(this).get(ProfileViewModel::class.java)

        setupListeners()
        setupObservers()

        // Inicializa a UI no modo de exibição
        setEditingMode(false)
    }

    private fun setupListeners() {
        // Ação para o botão de Editar/Salvar
        binding.btnEditProfile.setOnClickListener {
            if (isEditing) {
                saveProfileChanges() // Coleta dados e delega
            } else {
                setEditingMode(true)
                binding.etUserName.requestFocus()
            }
        }

        // Ação para o botão de Logout
        binding.btnLogoutFragment.setOnClickListener {
            viewModel.logout() // Delega o logout
        }
    }

    /**
     * Configura a observação dos LiveData do ViewModel para atualizar a UI.
     */
    private fun setupObservers() {
        // 1. Observa o perfil do usuário
        viewModel.userProfile.observe(viewLifecycleOwner) { user ->
            displayUserData(user)
            setEditingMode(false) // Sai do modo de edição após um carregamento ou sucesso na atualização
        }

        // 2. Observa o status de atualização (sucesso ou erro)
        viewModel.updateStatus.observe(viewLifecycleOwner) { status ->
            status?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearStatus() // Limpa o status
            }
        }

        // 3. Observa o evento de Logout
        viewModel.logoutEvent.observe(viewLifecycleOwner) { isLoggedOut ->
            if (isLoggedOut == true) {
                (activity as? LogoutListener)?.onLogoutSuccess()
                viewModel.clearLogoutEvent() // Limpa o evento
            }
        }

        // 4. Observa o estado de carregamento para controle de UI
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.btnEditProfile.isEnabled = !isLoading
            binding.btnLogoutFragment.isEnabled = !isLoading
        }
    }

    /**
     * Preenche os campos de texto na UI com os dados do User.
     */
    private fun displayUserData(user: User?) {
        // Obtenção do e-mail de autenticação (usado para o nome de exibição se o nome não existir)
        // Isso é possível porque o ViewModel expõe o `firebaseAuthInstance`
        val currentAuthEmail = viewModel.firebaseAuthInstance.currentUser?.email
        val defaultName = "Usuário(a)"

        // CORREÇÃO: Usando o e-mail do User ou do Auth
        val displayName = user?.nome ?: currentAuthEmail?.substringBefore('@') ?: defaultName

        // --- Atualiza o Card de Boas-vindas ---
        binding.tvWelcomeMessage.text = "Bem-vindo(a), $displayName!" // Usando string literal temporariamente aqui

        // --- Atualiza os Dados do Usuário nos campos de texto ---
        // CORREÇÃO: Corrigido para não usar o operador Elvis no setText
        binding.etUserName.setText(user?.nome ?: "Não informado")
        binding.etUserEmail.setText(user?.email ?: currentAuthEmail ?: "N/A")
        binding.etUserIdade.setText(user?.idade?.toString() ?: "Não informado")

        // Garante que o email não seja editável
        binding.etUserEmail.isEnabled = false
        context?.let {
            // Requer importação de R.drawable
            binding.etUserEmail.background = ContextCompat.getDrawable(it, android.R.color.transparent)
        }
    }

    /**
     * Alterna entre o modo de exibição (somente leitura) e o modo de edição (input ativo).
     */
    private fun setEditingMode(editing: Boolean) {
        this.isEditing = editing
        val context = context ?: return

        // Campos que podem ser editados: Nome e Idade
        val editableFields = listOf(binding.etUserName, binding.etUserIdade)

        for (field in editableFields) {
            field.isEnabled = editing
            field.isFocusable = editing
            field.isFocusableInTouchMode = editing

            if (editing) {
                // Modo de edição
                field.background = ContextCompat.getDrawable(context, R.drawable.edit_text_border)
            } else {
                // Modo de exibição
                field.background = ContextCompat.getDrawable(context, android.R.color.transparent)
            }
        }

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

        // DELEGAÇÃO CRÍTICA: Chama a função do ViewModel.
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
        if (context !is LogoutListener) {
            Log.e(TAG, "$context deve implementar LogoutListener para o logout funcionar.")
        }
    }
}