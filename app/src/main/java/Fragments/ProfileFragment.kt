package com.example.apphumor

import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.apphumor.databinding.FragmentProfileBinding
import com.example.apphumor.utils.ImageUtils
import com.example.apphumor.viewmodel.ProfileViewModel
import com.example.apphumor.viewmodel.HumorViewModelFactory

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ProfileViewModel
    private var isEditing = false

    interface LogoutListener { fun onLogoutSuccess() }
    private var logoutListener: LogoutListener? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.updatePhotoImmediately(requireContext(), it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val appContainer = (requireActivity().application as AppHumorApplication).container
        val factory = HumorViewModelFactory(appContainer.databaseRepository, appContainer.auth)

        viewModel = ViewModelProvider(this, factory)[ProfileViewModel::class.java]

        setupListeners()
        setupObservers()
        // CORREÇÃO: Carrega o fragmento de insights no container
        setupInsights(savedInstanceState)
    }

    private fun setupInsights(savedInstanceState: Bundle?) {
        // Só adiciona se for a primeira vez (evita duplicar ao girar a tela)
        if (savedInstanceState == null) {
            childFragmentManager.beginTransaction()
                .replace(R.id.fragment_insights_container, InsightsFragment())
                .commit()
        }
    }

    private fun setupListeners() {
        binding.ivProfileAvatar.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.btnEditProfile.setOnClickListener {
            if (isEditing) {
                val name = binding.etUserName.text.toString()
                val age = binding.etUserIdade.text.toString().toIntOrNull() ?: 0
                viewModel.saveAllChanges(name, age)
            } else {
                setEditingMode(true)
            }
        }

        binding.btnLogoutFragment.setOnClickListener {
            viewModel.logout()
        }

        binding.switchNotificacaoPerfil.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setDraftNotificationEnabled(isChecked)
        }

        binding.tvSelectedTimePerfil.setOnClickListener {
            if (isEditing) showTimePickerDialog()
        }
    }

    private fun setupObservers() {
        viewModel.userProfile.observe(viewLifecycleOwner) { user ->
            user?.let {
                binding.etUserName.setText(it.nome)
                binding.etUserIdade.setText(it.idade?.toString())
                binding.etUserEmail.setText(it.email)

                if (!it.fotoBase64.isNullOrEmpty()) {
                    val bitmap = ImageUtils.base64ToBitmap(it.fotoBase64)
                    binding.ivProfileAvatar.setImageBitmap(bitmap)
                } else {
                    binding.ivProfileAvatar.setImageResource(R.drawable.ic_usuario_24)
                }
            }
        }

        viewModel.updateStatus.observe(viewLifecycleOwner) { status ->
            status?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearStatus()
                if (it.contains("sucesso", true)) setEditingMode(false)
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
        binding.etUserEmail.isEnabled = false
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