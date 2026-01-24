package com.example.apphumor

import android.Manifest
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.apphumor.databinding.FragmentProfileBinding
import com.example.apphumor.di.DependencyProvider
import com.example.apphumor.models.Insight
import com.example.apphumor.models.User
import com.example.apphumor.utils.ImageUtils
import com.example.apphumor.utils.NotificationScheduler
import com.example.apphumor.utils.ThemePreferences
import com.example.apphumor.viewmodel.MoodStat
import com.example.apphumor.viewmodel.ProfileSaveState // Importe o estado novo
import com.example.apphumor.viewmodel.ProfileUiState // Importe o estado novo
import com.example.apphumor.viewmodel.ProfileViewModel
import com.example.apphumor.viewmodel.ProfileViewModelFactory
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.util.Locale

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: ProfileViewModel

    private var isEditing = false

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { viewModel.updatePhotoImmediately(requireContext(), it) }
        }

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
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
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
        setupObservers() // Aqui está a mágica
        setupThemeUI()

        setEditingMode(false)
    }

    // (setupThemeUI, showThemeSelectionDialog, updateThemeLabel -> MANTÉM IGUAL)
    private fun setupThemeUI() {
        updateThemeLabel()
        binding.llThemeSelector.setOnClickListener {
            if (!isEditing) return@setOnClickListener
            showThemeSelectionDialog()
        }
    }

    private fun showThemeSelectionDialog() {
        val options = arrayOf("Claro", "Escuro", "Padrão do Sistema")
        val values = arrayOf(
            ThemePreferences.THEME_LIGHT,
            ThemePreferences.THEME_DARK,
            ThemePreferences.THEME_SYSTEM
        )
        val currentCode = ThemePreferences.getSavedTheme(requireContext())
        val checkedItem = values.indexOf(currentCode).coerceAtLeast(2)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Escolha o Tema")
            .setSingleChoiceItems(options, checkedItem) { dialog, which ->
                ThemePreferences.saveTheme(requireContext(), values[which])
                updateThemeLabel()
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun updateThemeLabel() {
        val currentCode = ThemePreferences.getSavedTheme(requireContext())
        binding.tvCurrentTheme.text = when (currentCode) {
            ThemePreferences.THEME_LIGHT -> "Claro"
            ThemePreferences.THEME_DARK -> "Escuro"
            else -> "Sistema"
        }
    }

    private fun setupChartUI() {
        // (MANTÉM IGUAL)
        binding.pieChartProfile.apply {
            description.isEnabled = false
            legend.isEnabled = false
            isDrawHoleEnabled = true
            setHoleColor(Color.TRANSPARENT)
            holeRadius = 50f
            transparentCircleRadius = 55f
            setEntryLabelColor(Color.WHITE)
            setEntryLabelTextSize(10f)
            setNoDataText("")
        }
    }

    private fun setupListeners() {
        // (MANTÉM IGUAL)
        binding.btnEditProfile.setOnClickListener {
            if (isEditing) {
                val newName = binding.etUserName.text.toString().trim()
                val newAge = binding.etUserIdade.text.toString().toIntOrNull() ?: 0
                viewModel.saveAllChanges(newName, newAge)
            } else {
                setEditingMode(true)
            }
        }

        val imageClickListener = View.OnClickListener {
            if (isEditing) pickImageLauncher.launch("image/*")
        }
        binding.ivProfileAvatar.setOnClickListener(imageClickListener)
        binding.ivEditIconIndicator.setOnClickListener(imageClickListener)

        binding.btnLogoutFragment.setOnClickListener { viewModel.logout() }

        binding.tvSelectedTimePerfil.setOnClickListener {
            if (isEditing && binding.switchNotificacaoPerfil.isChecked) {
                showTimePickerDialog()
            }
        }

        binding.switchNotificacaoPerfil.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isEditing) {
                binding.tvSelectedTimePerfil.isEnabled = isChecked
                binding.llHorarioContainerPerfil.alpha = if (isChecked) 1.0f else 0.5f
            }
            if (buttonView.isPressed) {
                if (isChecked) {
                    if (hasNotificationPermission()) viewModel.setDraftNotificationEnabled(true)
                    else {
                        buttonView.isChecked = false
                        requestNotificationPermission()
                    }
                } else {
                    viewModel.setDraftNotificationEnabled(false)
                }
            }
        }
    }

    private fun setEditingMode(editing: Boolean) {
        // (MANTÉM IGUAL - Lógica visual de habilitar/desabilitar)
        isEditing = editing
        binding.etUserName.isEnabled = editing
        binding.etUserIdade.isEnabled = editing
        binding.switchNotificacaoPerfil.isEnabled = editing
        val isNotifOn = binding.switchNotificacaoPerfil.isChecked
        binding.tvSelectedTimePerfil.isEnabled = editing && isNotifOn
        binding.llHorarioContainerPerfil.alpha = if (editing && isNotifOn) 1.0f else 0.5f
        binding.llThemeSelector.isEnabled = editing
        binding.llThemeSelector.alpha = if (editing) 1.0f else 0.5f

        if (editing) {
            binding.btnEditProfile.text = getString(R.string.action_save_changes)
            binding.btnEditProfile.setIconResource(R.drawable.ic_save_24)
        } else {
            binding.btnEditProfile.text = getString(R.string.action_edit_profile)
            binding.btnEditProfile.setIconResource(R.drawable.ic_edit_24)
        }
        binding.btnLogoutFragment.isVisible = !editing
        binding.ivEditIconIndicator.isVisible = editing
    }

    // --- AQUI ESTÁ A GRANDE MUDANÇA ---
    private fun setupObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 1. OBSERVAR ESTADO GERAL DA TELA (Loading / Content / Error)
                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            is ProfileUiState.Loading -> {
                                binding.progressBarProfile.isVisible = true
                                binding.cardData.isVisible = false
                                binding.cardAchievements.isVisible = false
                            }
                            is ProfileUiState.Error -> {
                                binding.progressBarProfile.isVisible = false
                                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                            }
                            is ProfileUiState.Content -> {
                                binding.progressBarProfile.isVisible = false
                                binding.cardData.isVisible = true
                                binding.cardAchievements.isVisible = true
                                updateUIWithContent(state)
                            }
                        }
                    }
                }

                // 2. OBSERVAR ESTADO DE SALVAMENTO (Feedback)
                launch {
                    viewModel.saveState.collect { state ->
                        when (state) {
                            is ProfileSaveState.Idle -> {
                                binding.progressBarProfile.isVisible = false
                                binding.btnEditProfile.isEnabled = true
                            }
                            is ProfileSaveState.Saving -> {
                                binding.progressBarProfile.isVisible = true
                                binding.btnEditProfile.isEnabled = false
                            }
                            is ProfileSaveState.Success -> {
                                binding.progressBarProfile.isVisible = false
                                binding.btnEditProfile.isEnabled = true
                                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()

                                // Se foi um salvamento total (não apenas foto), sai da edição
                                if (!state.message.contains("Foto")) {
                                    setEditingMode(false)
                                }
                                viewModel.clearSaveStatus()
                            }
                            is ProfileSaveState.Error -> {
                                binding.progressBarProfile.isVisible = false
                                binding.btnEditProfile.isEnabled = true
                                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                                viewModel.clearSaveStatus()
                            }
                        }
                    }
                }

                // 3. OBSERVAR DRAFTS (Mudanças em tempo real durante edição)
                launch {
                    viewModel.draftPhotoBase64.collect { base64 ->
                        updateAvatar(base64)
                    }
                }
                launch {
                    viewModel.draftNotificationEnabled.collect { isEnabled ->
                        if (binding.switchNotificacaoPerfil.isChecked != isEnabled) {
                            binding.switchNotificacaoPerfil.isChecked = isEnabled
                        }
                        if (isEditing) binding.llHorarioContainerPerfil.alpha = if (isEnabled) 1.0f else 0.5f
                    }
                }
                launch {
                    viewModel.draftTime.collect { (h, m) ->
                        binding.tvSelectedTimePerfil.text = String.format(Locale.getDefault(), "%02d:%02d", h, m)
                    }
                }
                launch {
                    viewModel.scheduleNotificationEvent.collect { event ->
                        event?.let { (enabled, time) ->
                            val parts = time.split(":")
                            NotificationScheduler.scheduleDailyReminder(requireContext(), parts[0].toInt(), parts[1].toInt(), enabled)
                            viewModel.clearScheduleEvent()
                        }
                    }
                }
                launch {
                    viewModel.logoutEvent.collect { loggedOut ->
                        if (loggedOut) {
                            logoutListener?.onLogoutSuccess()
                            viewModel.clearLogoutEvent()
                        }
                    }
                }
            }
        }
    }

    private fun updateUIWithContent(content: ProfileUiState.Content) {
        // Preenche campos
        binding.etUserEmail.setText(content.user.email)
        binding.etUserName.setText(content.user.nome)
        binding.etUserIdade.setText(content.user.idade.toString())
        binding.tvWelcomeMessage.text = content.user.nome ?: "Usuário"

        // Conquistas
        binding.tvLevelTitle.text = content.user.getTituloNivel()
        val weeks = content.user.semanasPerfeitas
        binding.chipWeeksCount.text = if (weeks == 1) "1 Semana Perfeita" else "$weeks Semanas Perfeitas"

        // Gráfico
        if (content.moodStats.isEmpty()) {
            binding.pieChartProfile.isVisible = false
            binding.tvChartEmptyState.isVisible = true
        } else {
            binding.pieChartProfile.isVisible = true
            binding.tvChartEmptyState.isVisible = false
            updateChartData(content.moodStats)
        }

        // Insight
        val insight = content.insight
        binding.tvInsightTitle.text = insight.title
        binding.tvInsightMessage.text = insight.message
        binding.ivInsightIcon.setImageResource(insight.iconRes)
        try {
            val colorContent = ContextCompat.getColor(requireContext(), insight.colorRes)
            val colorBackground = ContextCompat.getColor(requireContext(), insight.backgroundTint)
            binding.ivInsightIcon.setColorFilter(colorContent)
            binding.tvInsightTitle.setTextColor(colorContent)
            binding.cardSmartInsight.setCardBackgroundColor(colorBackground)
            binding.cardSmartInsight.isVisible = true
        } catch (e: Exception) {
            binding.cardSmartInsight.setCardBackgroundColor(Color.LTGRAY)
        }
    }

    private fun updateAvatar(base64: String?) {
        if (base64 != null) {
            val bitmap = ImageUtils.base64ToBitmap(base64)
            binding.ivProfileAvatar.setImageBitmap(bitmap)
            binding.ivProfileAvatar.scaleType = ImageView.ScaleType.CENTER_CROP
            binding.ivProfileAvatar.clearColorFilter()
            binding.ivProfileAvatar.imageTintList = null
        } else {
            binding.ivProfileAvatar.setImageResource(R.drawable.ic_usuario_24)
            val primaryColor = MaterialColors.getColor(binding.ivProfileAvatar, com.google.android.material.R.attr.colorPrimary)
            binding.ivProfileAvatar.setColorFilter(primaryColor)
            binding.ivProfileAvatar.scaleType = ImageView.ScaleType.CENTER_INSIDE
        }
    }

    // (updateChartData, showTimePickerDialog, hasNotificationPermission... MANTÉM IGUAL)
    private fun updateChartData(stats: List<MoodStat>) {
        val entries = ArrayList<PieEntry>()
        val colors = ArrayList<Int>()
        var total = 0

        stats.forEach { stat ->
            total += stat.count
            val labelText = if (stat.count < 3) "" else requireContext().getString(stat.labelRes)
            entries.add(PieEntry(stat.count.toFloat(), labelText))
            colors.add(ContextCompat.getColor(requireContext(), stat.colorRes))
        }

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = colors
        dataSet.sliceSpace = 3f
        dataSet.selectionShift = 5f
        dataSet.valueTextSize = 12f
        dataSet.valueTextColor = Color.WHITE

        val data = PieData(dataSet)
        data.setValueFormatter(object : com.github.mikephil.charting.formatter.ValueFormatter() {
            override fun getFormattedValue(value: Float) = value.toInt().toString()
        })

        binding.pieChartProfile.data = data
        binding.pieChartProfile.centerText = "Total\n$total"
        binding.pieChartProfile.setCenterTextSize(14f)

        val typedValue = android.util.TypedValue()
        requireContext().theme.resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true)
        val dynamicColor = typedValue.data
        binding.pieChartProfile.setCenterTextColor(dynamicColor)

        binding.pieChartProfile.legend.isEnabled = false
        binding.pieChartProfile.description.isEnabled = false
        binding.pieChartProfile.animateY(1400, Easing.EaseInOutQuad)
        binding.pieChartProfile.invalidate()
    }

    private fun showTimePickerDialog() {
        val current = viewModel.draftTime.value
        TimePickerDialog(requireContext(), { _, h, m -> viewModel.setDraftTime(h, m) }, current.first, current.second, true).show()
    }

    private fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun showPermissionDeniedSnackbar() {
        Snackbar.make(binding.root, "É necessário permitir notificações para receber os lembretes.", Snackbar.LENGTH_LONG)
            .setAction("Configurações") { }.show()
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