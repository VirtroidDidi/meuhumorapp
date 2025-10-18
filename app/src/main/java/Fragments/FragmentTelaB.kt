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
import com.example.apphumor.databinding.FragmentTelaBBinding // Importação correta
import com.example.apphumor.models.User
import com.example.apphumor.repository.DatabaseRepository
import com.google.firebase.auth.FirebaseAuth

class FragmentTelaB : Fragment() {
    // Usamos _binding para a referência mutável e binding para o getter não-nulo
    private var _binding: FragmentTelaBBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private val dbRepository = DatabaseRepository()
    private val TAG = "FragmentTelaB"

    private var currentUserData: User? = null
    private var isEditing = false

    // Interface para comunicar o evento de Logout para a Activity
    interface LogoutListener {
        fun onLogoutSuccess()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Infla o layout e configura o binding
        _binding = FragmentTelaBBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()

        loadUserData()
        // CORREÇÃO: Chama o setup do botão de logout
        setupLogoutButton()
        setupEditProfileButton()

        // Inicializa a UI no modo de exibição (sem edição)
        setEditingMode(false)
    }

    /**
     * Carrega os dados do usuário do Firebase e armazena localmente.
     */
    private fun loadUserData() {
        val firebaseUser = auth.currentUser
        if (firebaseUser == null) {
            updateUIWithUserData(null)
            return
        }

        // Busca os dados do usuário no Realtime Database
        dbRepository.getUser(firebaseUser.uid) { user ->
            activity?.runOnUiThread {
                // Armazena os dados recuperados
                currentUserData = user
                updateUIWithUserData(user)
            }
        }
    }

    /**
     * Atualiza os campos de texto na UI com os dados do usuário.
     */
    private fun updateUIWithUserData(user: User?) {
        val defaultName = "Usuário(a)"
        // Tenta usar o nome do banco, ou a primeira parte do e-mail, ou o nome padrão
        val displayName = user?.nome ?: auth.currentUser?.email?.substringBefore('@') ?: defaultName

        // --- Atualiza o Card de Boas-vindas ---
        binding.tvWelcomeMessage.text = "Bem-vindo(a), $displayName!"

        // --- Atualiza os Dados do Usuário nos campos de texto (TextInputEditTexts) ---
        binding.etUserName.setText(user?.nome ?: "Não informado")
        // O email é preenchido, mas permanece não editável
        binding.etUserEmail.setText(user?.email ?: auth.currentUser?.email ?: "N/A")
        // A Idade será exibida apenas o número
        binding.etUserIdade.setText(user?.idade?.toString() ?: "Não informado")

        // Garante que o email não seja editável, mesmo que o isEditing mude
        binding.etUserEmail.isEnabled = false
        context?.let {
            // Garante que o fundo seja transparente para o email, pois não deve ser editado
            binding.etUserEmail.background = ContextCompat.getDrawable(it, android.R.color.transparent)
        }
    }

    /**
     * Alterna entre o modo de exibição (somente leitura) e o modo de edição (input ativo).
     * @param editing Se true, entra no modo de edição. Se false, volta para o modo de exibição.
     */
    private fun setEditingMode(editing: Boolean) {
        this.isEditing = editing
        val context = context ?: return

        // Campos que podem ser editados: Nome e Idade
        val editableFields = listOf(binding.etUserName, binding.etUserIdade)

        for (field in editableFields) {
            // Habilita/Desabilita a edição e o foco
            field.isEnabled = editing
            field.isFocusable = editing
            field.isFocusableInTouchMode = editing

            // Altera a cor de fundo para dar feedback visual
            if (editing) {
                // No modo de edição, usamos o fundo branco (ou outra cor)
                // O Ideal é usar um background que indique edição, como um underline
                field.background = ContextCompat.getDrawable(context, R.drawable.edit_text_border) // Certifique-se de ter um drawable edit_text_background ou remova esta linha se estiver usando tema Material padrão
            } else {
                // No modo de exibição, usamos o fundo transparente
                field.background = ContextCompat.getDrawable(context, android.R.color.transparent)
            }
        }

        // Atualiza o botão
        if (editing) {
            binding.btnEditProfile.text = "Salvar Alterações"
            // Referencia o drawable do SEU projeto
            binding.btnEditProfile.setIconResource(R.drawable.ic_save_24)
        } else {
            binding.btnEditProfile.text = "Editar Perfil"
            // Referencia o drawable do SEU projeto
            binding.btnEditProfile.setIconResource(R.drawable.ic_edit_24)
        }
    }


    private fun setupEditProfileButton() {
        binding.btnEditProfile.setOnClickListener {
            if (isEditing) {
                // Se está no modo de edição, o clique deve SALVAR
                saveProfileChanges()
            } else {
                // Se está no modo de exibição, o clique deve EDITAR
                setEditingMode(true)
                // Coloca o foco no primeiro campo para o usuário começar a digitar
                binding.etUserName.requestFocus()
            }
        }
    }

    /**
     * Coleta os dados dos campos, valida e salva no Firebase.
     */
    private fun saveProfileChanges() {
        val uid = auth.currentUser?.uid
        if (uid == null || currentUserData == null) {
            Toast.makeText(context, "Erro: Usuário não autenticado ou dados ausentes.", Toast.LENGTH_SHORT).show()
            setEditingMode(false)
            return
        }

        // 1. Coleta os novos dados
        val newName = binding.etUserName.text.toString().trim()
        val newAgeString = binding.etUserIdade.text.toString().trim()
        val newAge = newAgeString.toIntOrNull()

        // 2. Validação simples
        if (newName.isEmpty()) {
            Toast.makeText(context, "O nome não pode ser vazio.", Toast.LENGTH_SHORT).show()
            return
        }
        if (newAge == null || newAge <= 0 || newAge > 150) {
            Toast.makeText(context, "Idade inválida. Use um número entre 1 e 150.", Toast.LENGTH_SHORT).show()
            return
        }

        // 3. Cria o objeto User com os novos dados
        // Usamos .copy() para manter o UID e outros campos (como email) inalterados
        val updatedUser = currentUserData!!.copy(
            nome = newName,
            idade = newAge
        )

        // 4. Salva no Firebase
        dbRepository.updateUser(updatedUser,
            onSuccess = {
                // Reverte para o modo de exibição e recarrega para confirmar
                Toast.makeText(context, "Perfil atualizado com sucesso!", Toast.LENGTH_SHORT).show()
                loadUserData() // Recarrega os dados e atualiza a UI
                setEditingMode(false) // Volta para o modo de exibição
            },
            onError = { errorMsg ->
                Toast.makeText(context, "Falha ao salvar: $errorMsg", Toast.LENGTH_LONG).show()
                // Permanece no modo de edição para tentar corrigir o erro
            }
        )
    }

    private fun setupLogoutButton() {
        // Configura o Listener para o botão de Logout
        binding.btnLogoutFragment.setOnClickListener {
            performLogout()
        }
    }

    private fun performLogout() {
        // 1. Desloga do Firebase
        auth.signOut()

        // 2. Notifica a Activity (Main Activity) que o logout foi concluído
        // O casting seguro 'as? LogoutListener' garante que a notificação só
        // ocorra se a Activity implementar a interface, evitando crashes.
        (activity as? LogoutListener)?.onLogoutSuccess()

        // Exibe uma mensagem de sucesso no Fragment, caso o callback falhe
        Toast.makeText(context, "Sessão encerrada com sucesso!", Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        // Garante que os dados sejam recarregados ao voltar para a tela
        loadUserData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Limpa a referência ao binding para evitar vazamentos de memória
        _binding = null
    }

    // Associa o Fragment à Activity e garante que a interface LogoutListener seja implementada
    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Este é um check de segurança. Se você removeu a MainActivity, este log é importante.
        if (context !is LogoutListener) {
            Log.e(TAG, "$context deve implementar LogoutListener para o logout funcionar.")
            // Você pode até lançar uma exceção aqui para um debug mais agressivo
            // throw RuntimeException("$context must implement LogoutListener")
        }
    }
}
