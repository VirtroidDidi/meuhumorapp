package com.example.apphumor

import android.Manifest
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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
import com.google.android.material.snackbar.Snackbar
import java.util.Locale

import android.graphics.Color
import com.example.apphumor.viewmodel.MoodStat
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.animation.Easing

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ProfileViewModel
    private var isEditing = false

    // Launcher para selecionar imagem da galeria
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                viewModel.updatePhotoImmediately(requireContext(), it)
            }
        }

    // Launcher para Permissão de Notificação (Android 13+)
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.setDraftNotificationEnabled(true)
            Toast.makeText(context, "Lembretes ativados!", Toast.LENGTH_SHORT).show()
        } else {
            viewModel.setDraftNotificationEnabled(false)
            showPermissionDeniedSnackbar()
        }
    }

    interface LogoutListener {
        fun onLogoutSuccess()
    }

    private var logoutListener: LogoutListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(
            this,
            ProfileViewModelFactory(DependencyProvider.auth, DependencyProvider.databaseRepository)
        )[ProfileViewModel::class.java]

        setupChartUI()
        setupListeners()
        setupObservers()
        setEditingMode(false)
    }

    private fun setupChartUI() {
        binding.pieChartProfile.apply {
            description.isEnabled = false
            legend.isEnabled = false
            isDrawHoleEnabled = true
            setHoleColor(Color.TRANSPARENT)
            holeRadius = 50f
            transparentCircleRadius = 55f
            setEntryLabelColor(Color.BLACK)
            setEntryLabelTextSize(10f)
            setNoDataText("")
        }
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

        binding.ivProfileAvatar.setOnClickListener { pickImageLauncher.launch("image/*") }
        binding.ivEditIconIndicator.setOnClickListener { pickImageLauncher.launch("image/*") }
        binding.btnLogoutFragment.setOnClickListener { viewModel.logout() }

        binding.tvSelectedTimePerfil.setOnClickListener {
            if (isEditing && binding.switchNotificacaoPerfil.isChecked) showTimePickerDialog()
        }

        binding.switchNotificacaoPerfil.setOnCheckedChangeListener { buttonView, isChecked ->
            if (buttonView.isPressed) {
                if (isChecked) {
                    if (hasNotificationPermission()) {
                        viewModel.setDraftNotificationEnabled(true)
                    } else {
                        buttonView.isChecked = false
                        requestNotificationPermission()
                    }
                } else {
                    viewModel.setDraftNotificationEnabled(false)
                }
            }
        }
    }

    private fun setupObservers() {
        viewModel.userProfile.observe(viewLifecycleOwner) { user ->
            user?.let {
                binding.etUserEmail.setText(it.email)
                binding.etUserName.setText(it.nome)
                binding.etUserIdade.setText(it.idade.toString())
                binding.tvWelcomeMessage.text = it.nome ?: "Usuário"
            }
        }

        viewModel.draftPhotoBase64.observe(viewLifecycleOwner) { base64 ->
            if (base64 != null) {
                val bitmap = ImageUtils.base64ToBitmap(base64)
                binding.ivProfileAvatar.setImageBitmap(bitmap)
                binding.ivProfileAvatar.imageTintList = null
                binding.ivProfileAvatar.scaleType = ImageView.ScaleType.CENTER_CROP
            } else {
                binding.ivProfileAvatar.setImageResource(R.drawable.ic_usuario_24)
                val primaryColor = MaterialColors.getColor(binding.ivProfileAvatar, com.google.android.material.R.attr.colorPrimary)
                binding.ivProfileAvatar.setColorFilter(primaryColor)
                binding.ivProfileAvatar.scaleType = ImageView.ScaleType.CENTER_INSIDE
            }
        }

        viewModel.draftNotificationEnabled.observe(viewLifecycleOwner) { isEnabled ->
            if (binding.switchNotificacaoPerfil.isChecked != isEnabled) {
                binding.switchNotificacaoPerfil.isChecked = isEnabled
            }
            binding.llHorarioContainerPerfil.alpha = if (isEnabled) 1.0f else 0.4f
        }

        viewModel.draftTime.observe(viewLifecycleOwner) { (h, m) ->
            binding.tvSelectedTimePerfil.text = String.format(Locale.getDefault(), "%02d:%02d", h, m)
        }

        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBarProfile.isVisible = isLoading
            binding.btnEditProfile.isEnabled = !isLoading
            binding.ivProfileAvatar.isEnabled = !isLoading
        }

        viewModel.updateStatus.observe(viewLifecycleOwner) { status ->
            status?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                if (it.contains("sucesso", ignoreCase = true) && !it.contains("Foto", ignoreCase = true)) {
                    setEditingMode(false)
                }
                viewModel.clearStatus()
            }
        }

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

        viewModel.moodStats.observe(viewLifecycleOwner) { stats ->
            if (stats.isNullOrEmpty()) {
                binding.pieChartProfile.isVisible = false
                binding.tvChartEmptyState.isVisible = true
            } else {
                binding.pieChartProfile.isVisible = true
                binding.tvChartEmptyState.isVisible = false
                updateChartData(stats)
            }
        }

        // --- NOVO: OBSERVAR O INSIGHT (Faltava isso!) ---
        viewModel.insight.observe(viewLifecycleOwner) { insight ->
            insight?.let {
                binding.tvInsightTitle.text = it.title
                binding.tvInsightMessage.text = it.message
                binding.ivInsightIcon.setImageResource(it.iconRes)

                // Segura para evitar crash se a cor não existir
                try {
                    val colorContent = ContextCompat.getColor(requireContext(), it.colorRes)
                    val colorBackground = ContextCompat.getColor(requireContext(), it.backgroundTint)

                    binding.ivInsightIcon.setColorFilter(colorContent)
                    binding.tvInsightTitle.setTextColor(colorContent)
                    binding.cardSmartInsight.setCardBackgroundColor(colorBackground)

                    // Garante que o card fique visível
                    binding.cardSmartInsight.isVisible = true
                } catch (e: Exception) {
                    // Fallback para cinza se a cor falhar
                    binding.cardSmartInsight.setCardBackgroundColor(Color.LTGRAY)
                }
            }
        }
    }

    private fun updateChartData(stats: List<MoodStat>) {
        val entries = ArrayList<PieEntry>()
        val colors = ArrayList<Int>()

        var total = 0
        stats.forEach { stat ->
            total += stat.count
            // Lógica de limpeza: só mostra texto se tiver 3 ou mais
            val labelText = if (stat.count < 3) "" else requireContext().getString(stat.labelRes)

            entries.add(PieEntry(stat.count.toFloat(), labelText))
            val colorInt = ContextCompat.getColor(requireContext(), stat.colorRes)
            colors.add(colorInt)
        }

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = colors
        dataSet.sliceSpace = 3f
        dataSet.selectionShift = 5f
        dataSet.valueTextSize = 12f
        dataSet.valueTextColor = Color.WHITE

        val data = PieData(dataSet)
        data.setValueFormatter(object : com.github.mikephil.charting.formatter.ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return value.toInt().toString()
            }
        })

        binding.pieChartProfile.data = data
        binding.pieChartProfile.centerText = "Total\n$total"
        binding.pieChartProfile.setCenterTextSize(14f)
        binding.pieChartProfile.setCenterTextColor(ContextCompat.getColor(requireContext(), R.color.black))

        binding.pieChartProfile.legend.isEnabled = false
        binding.pieChartProfile.description.isEnabled = false

        binding.pieChartProfile.animateY(1400, Easing.EaseInOutQuad)
        binding.pieChartProfile.invalidate()
    }

    private fun showTimePickerDialog() {
        val current = viewModel.draftTime.value ?: Pair(20, 0)
        TimePickerDialog(
            requireContext(),
            { _, h, m -> viewModel.setDraftTime(h, m) },
            current.first,
            current.second,
            true
        ).show()
    }

    private fun setEditingMode(editing: Boolean) {
        isEditing = editing
        binding.etUserName.isEnabled = editing
        binding.etUserIdade.isEnabled = editing
        binding.switchNotificacaoPerfil.isEnabled = editing
        binding.tvSelectedTimePerfil.isEnabled = editing && binding.switchNotificacaoPerfil.isChecked

        binding.btnEditProfile.text = if (editing) getString(R.string.action_save_changes) else getString(R.string.action_edit_profile)
        binding.btnEditProfile.setIconResource(if (editing) R.drawable.ic_save_24 else R.drawable.ic_edit_24)

        // Esconde o botão de logout durante a edição
        binding.btnLogoutFragment.isVisible = !editing
    }

    private fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun showPermissionDeniedSnackbar() {
        Snackbar.make(
            binding.root,
            "É necessário permitir notificações para receber os lembretes.",
            Snackbar.LENGTH_LONG
        ).setAction("Configurações") { }.show()
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