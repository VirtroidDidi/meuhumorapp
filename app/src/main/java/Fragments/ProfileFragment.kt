package com.example.apphumor

import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.apphumor.databinding.FragmentProfileBinding
import com.example.apphumor.di.DependencyProvider
import com.example.apphumor.utils.ImageUtils
import com.example.apphumor.utils.NotificationScheduler
import com.example.apphumor.viewmodel.ProfileViewModel
import com.example.apphumor.viewmodel.ProfileViewModelFactory
import com.google.android.material.color.MaterialColors
import java.util.Locale

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ProfileViewModel
    private var isEditing = false

    // Lançador da Galeria: SALVA IMEDIATAMENTE e corrige o bug visual ao retornar
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            // Chama o método que processa E salva no Firebase na mesma hora
            viewModel.updatePhotoImmediately(requireContext(), it)
        }
    }

    interface LogoutListener { fun onLogoutSuccess() }
    private var logoutListener: LogoutListener? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this, ProfileViewModelFactory(DependencyProvider.auth, DependencyProvider.databaseRepository))[ProfileViewModel::class.java]

        setupListeners()
        setupObservers()
        setEditingMode(false)
    }

    private fun setupListeners() {
        // Botão Salvar/Editar (Para Nome, Idade e Configurações)
        binding.btnEditProfile.setOnClickListener {
            if (isEditing) {
                val newName = binding.etUserName.text.toString().trim()
                val newAge = binding.etUserIdade.text.toString().toIntOrNull() ?: 0
                viewModel.saveAllChanges(newName, newAge)
            } else {
                setEditingMode(true)
            }
        }

        // --- CLIQUE NA FOTO (SEMPRE ATIVO) ---
        // Permite trocar a foto a qualquer momento, independente do modo de edição
        binding.ivProfileAvatar.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        // O ícone pequeno da câmera também abre a galeria
        binding.ivEditIconIndicator.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.btnLogoutFragment.setOnClickListener { viewModel.logout() }

        // Listener do horário (só funciona se estiver editando e notificação ativa)
        binding.tvSelectedTimePerfil.setOnClickListener {
            if (isEditing && binding.switchNotificacaoPerfil.isChecked) showTimePickerDialog()
        }

        // Listener do Switch
        binding.switchNotificacaoPerfil.setOnCheckedChangeListener { _, isChecked ->
            if (isEditing) viewModel.setDraftNotificationEnabled(isChecked)
        }
    }

    private fun setupObservers() {
        // 1. Dados do Usuário (Texto)
        viewModel.userProfile.observe(viewLifecycleOwner) { user ->
            user?.let {
                binding.etUserEmail.setText(it.email)
                binding.etUserName.setText(it.nome)
                binding.etUserIdade.setText(it.idade.toString())
                binding.tvWelcomeMessage.text = it.nome ?: "Usuário"
                // Nota: A imagem é gerenciada pelo observer 'draftPhotoBase64' abaixo
            }
        }

        // 2. Foto de Perfil (Com a correção do Bug Roxo)
        viewModel.draftPhotoBase64.observe(viewLifecycleOwner) { base64 ->
            if (base64 != null) {
                // CASO A: TEM FOTO REAL
                val bitmap = ImageUtils.base64ToBitmap(base64)
                binding.ivProfileAvatar.setImageBitmap(bitmap)

                // --- A CORREÇÃO DO BUG ROXO ESTÁ AQUI ---
                // Removemos o filtro de cor (tint) para mostrar a foto original
                binding.ivProfileAvatar.imageTintList = null

                // Ajustamos o zoom para preencher o círculo (Center Crop)
                binding.ivProfileAvatar.scaleType = ImageView.ScaleType.CENTER_CROP
            } else {
                // CASO B: NÃO TEM FOTO (Bonequinho Padrão)
                binding.ivProfileAvatar.setImageResource(R.drawable.ic_usuario_24)

                // Reaplica a cor roxa (Primary) no ícone
                val primaryColor = MaterialColors.getColor(binding.ivProfileAvatar, com.google.android.material.R.attr.colorPrimary)
                binding.ivProfileAvatar.setColorFilter(primaryColor)

                // Ajusta para o ícone ficar inteiro dentro do círculo
                binding.ivProfileAvatar.scaleType = ImageView.ScaleType.CENTER_INSIDE
            }
        }

        // 3. Estado do Switch (Notificação)
        viewModel.draftNotificationEnabled.observe(viewLifecycleOwner) { isEnabled ->
            binding.switchNotificacaoPerfil.isChecked = isEnabled
            binding.llHorarioContainerPerfil.alpha = if (isEnabled) 1.0f else 0.4f
        }

        // 4. Horário Selecionado
        viewModel.draftTime.observe(viewLifecycleOwner) { (h, m) ->
            binding.tvSelectedTimePerfil.text = String.format(Locale.getDefault(), "%02d:%02d", h, m)
        }

        // 5. Loading (Bloqueia tela enquanto salva)
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBarProfile.isVisible = isLoading
            binding.btnEditProfile.isEnabled = !isLoading
            binding.ivProfileAvatar.isEnabled = !isLoading
        }

        // 6. Mensagens de Sucesso/Erro
        viewModel.updateStatus.observe(viewLifecycleOwner) { status ->
            status?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                // Se salvou DADOS (texto), sai do modo edição. Se foi só FOTO, mantém como está.
                if (it.contains("sucesso", ignoreCase = true) && !it.contains("Foto", ignoreCase = true)) {
                    setEditingMode(false)
                }
                viewModel.clearStatus()
            }
        }

        // 7. Agendamento de Notificação
        viewModel.scheduleNotificationEvent.observe(viewLifecycleOwner) { event ->
            event?.let { (enabled, time) ->
                val parts = time.split(":")
                NotificationScheduler.scheduleDailyReminder(requireContext(), parts[0].toInt(), parts[1].toInt(), enabled)
                viewModel.clearScheduleEvent()
            }
        }

        // 8. Logout
        viewModel.logoutEvent.observe(viewLifecycleOwner) { loggedOut ->
            if (loggedOut) { logoutListener?.onLogoutSuccess(); viewModel.clearLogoutEvent() }
        }
    }

    private fun showTimePickerDialog() {
        val current = viewModel.draftTime.value ?: Pair(20, 0)
        TimePickerDialog(requireContext(), { _, h, m -> viewModel.setDraftTime(h, m) }, current.first, current.second, true).show()
    }

    private fun setEditingMode(editing: Boolean) {
        isEditing = editing
        binding.etUserName.isEnabled = editing
        binding.etUserIdade.isEnabled = editing
        binding.switchNotificacaoPerfil.isEnabled = editing
        binding.tvSelectedTimePerfil.isEnabled = editing && binding.switchNotificacaoPerfil.isChecked

        // Ícone de Câmera (ivEditIconIndicator) agora é controlado pelo XML (sempre visível)

        binding.btnEditProfile.text = if (editing) getString(R.string.action_save_changes) else getString(R.string.action_edit_profile)
        binding.btnEditProfile.setIconResource(if (editing) R.drawable.ic_save_24 else R.drawable.ic_edit_24)

        // Esconde o botão Sair enquanto edita os dados
        binding.btnLogoutFragment.isVisible = !editing
    }

    override fun onAttach(context: Context) { super.onAttach(context); if (context is LogoutListener) logoutListener = context }
    override fun onDetach() { super.onDetach(); logoutListener = null }
    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}