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
import com.example.apphumor.utils.ThemePreferences
import com.example.apphumor.viewmodel.ProfileViewModel
import com.example.apphumor.viewmodel.ProfileViewModelFactory
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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

    // Começa falso (Modo Visualização - Tudo travado)
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
        setupObservers()
        setupThemeUI()

        // Garante que a tela comece travada (Modo Leitura)
        setEditingMode(false)
    }

    private fun setupThemeUI() {
        updateThemeLabel()

        binding.llThemeSelector.setOnClickListener {
            // TRAVA DE SEGURANÇA: Só abre o dialog se estiver editando
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
        // LÓGICA DO BOTÃO PRINCIPAL (CORRIGIDA)
        binding.btnEditProfile.setOnClickListener {
            if (isEditing) {
                // Se ESTÁ editando (botão diz "Salvar"), então SALVA
                val newName = binding.etUserName.text.toString().trim()
                val newAge = binding.etUserIdade.text.toString().toIntOrNull() ?: 0
                viewModel.saveAllChanges(newName, newAge)
            } else {
                // Se NÃO está editando (botão diz "Editar"), então HABILITA EDIÇÃO
                setEditingMode(true)
            }
        }

        // FOTO: Só permite clicar se estiver editando
        val imageClickListener = View.OnClickListener {
            if (isEditing) {
                pickImageLauncher.launch("image/*")
            }
        }
        binding.ivProfileAvatar.setOnClickListener(imageClickListener)
        binding.ivEditIconIndicator.setOnClickListener(imageClickListener)

        binding.btnLogoutFragment.setOnClickListener { viewModel.logout() }

        // HORÁRIO: Só abre se estiver editando E switch ligado
        binding.tvSelectedTimePerfil.setOnClickListener {
            if (isEditing && binding.switchNotificacaoPerfil.isChecked) {
                showTimePickerDialog()
            }
        }

        // SWITCH: Atualiza visual imediatamente ao mudar
        binding.switchNotificacaoPerfil.setOnCheckedChangeListener { buttonView, isChecked ->
            // Se estiver editando, atualiza a visibilidade do horário em tempo real
            if (isEditing) {
                binding.tvSelectedTimePerfil.isEnabled = isChecked
                binding.llHorarioContainerPerfil.alpha = if (isChecked) 1.0f else 0.5f
            }

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

    // --- CORAÇÃO DA LÓGICA DE ESTADO ---
    private fun setEditingMode(editing: Boolean) {
        isEditing = editing

        // 1. Campos de Texto (Habilita/Desabilita)
        binding.etUserName.isEnabled = editing
        binding.etUserIdade.isEnabled = editing

        // 2. Switch
        binding.switchNotificacaoPerfil.isEnabled = editing

        // 3. Horário (Só habilita se Editando + Switch Ligado)
        val isNotifOn = binding.switchNotificacaoPerfil.isChecked
        binding.tvSelectedTimePerfil.isEnabled = editing && isNotifOn
        binding.llHorarioContainerPerfil.alpha = if (editing && isNotifOn) 1.0f else 0.5f

        // 4. Seletor de Tema (CORRIGIDO: Agora segue a regra do resto)
        // Antes estava !editing (invertido). Agora é editing.
        binding.llThemeSelector.isEnabled = editing
        binding.llThemeSelector.alpha = if (editing) 1.0f else 0.5f

        // 5. Configura o Botão (Texto e Ícone)
        if (editing) {
            // MODO EDIÇÃO -> Botão vira "SALVAR"
            binding.btnEditProfile.text = getString(R.string.action_save_changes)
            binding.btnEditProfile.setIconResource(R.drawable.ic_save_24)
        } else {
            // MODO VISUALIZAÇÃO -> Botão vira "EDITAR"
            binding.btnEditProfile.text = getString(R.string.action_edit_profile)
            binding.btnEditProfile.setIconResource(R.drawable.ic_edit_24)
        }

        // 6. Botão de Logout e ícone de edição de foto
        binding.btnLogoutFragment.isVisible = !editing
        binding.ivEditIconIndicator.isVisible = editing // Ícone de lápis na foto só aparece ao editar
    }

    private fun setupObservers() {
        // 1. Dados do Usuário (Nome, Email, Idade)
        viewModel.userProfile.observe(viewLifecycleOwner) { user ->
            user?.let {
                binding.etUserEmail.setText(it.email)
                binding.etUserName.setText(it.nome)
                binding.etUserIdade.setText(it.idade.toString())
                binding.tvWelcomeMessage.text = it.nome ?: "Usuário"
            }
        }

        // 2. Foto de Perfil (COM A CORREÇÃO DE COR)
        viewModel.draftPhotoBase64.observe(viewLifecycleOwner) { base64 ->
            if (base64 != null) {
                // CASO 1: Tem foto real
                val bitmap = ImageUtils.base64ToBitmap(base64)
                binding.ivProfileAvatar.setImageBitmap(bitmap)
                binding.ivProfileAvatar.scaleType = ImageView.ScaleType.CENTER_CROP

                // --- CORREÇÃO CRUCIAL ---
                // Remove qualquer filtro de cor ou tint que existia antes
                binding.ivProfileAvatar.clearColorFilter()
                binding.ivProfileAvatar.imageTintList = null
                // -----------------------
            } else {
                // CASO 2: Sem foto (Bonequinho padrão)
                binding.ivProfileAvatar.setImageResource(R.drawable.ic_usuario_24)
                // Aplica a cor do tema (Roxo/Lilás) no bonequinho
                val primaryColor = MaterialColors.getColor(binding.ivProfileAvatar, com.google.android.material.R.attr.colorPrimary)
                binding.ivProfileAvatar.setColorFilter(primaryColor)
                binding.ivProfileAvatar.scaleType = ImageView.ScaleType.CENTER_INSIDE
            }
        }

        // 3. Switch de Notificação
        viewModel.draftNotificationEnabled.observe(viewLifecycleOwner) { isEnabled ->
            if (binding.switchNotificacaoPerfil.isChecked != isEnabled) {
                binding.switchNotificacaoPerfil.isChecked = isEnabled
            }
            // Só muda a opacidade visual se estiver editando
            if (isEditing) {
                binding.llHorarioContainerPerfil.alpha = if (isEnabled) 1.0f else 0.5f
            }
        }

        // 4. Horário do Lembrete
        viewModel.draftTime.observe(viewLifecycleOwner) { (h, m) ->
            binding.tvSelectedTimePerfil.text = String.format(Locale.getDefault(), "%02d:%02d", h, m)
        }

        // 5. Estado de Carregamento (Loading)
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBarProfile.isVisible = isLoading
            // Bloqueia botões enquanto salva
            binding.btnEditProfile.isEnabled = !isLoading
            binding.ivProfileAvatar.isEnabled = !isLoading && isEditing
        }

        // 6. Mensagens de Sucesso/Erro (Toast)
        viewModel.updateStatus.observe(viewLifecycleOwner) { status ->
            status?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                // Se salvou dados (e não apenas foto), sai do modo edição
                if (it.contains("sucesso", ignoreCase = true) && !it.contains("Foto")) {
                    setEditingMode(false)
                }
                viewModel.clearStatus()
            }
        }

        // 7. Agendamento do Alarme (WorkManager)
        viewModel.scheduleNotificationEvent.observe(viewLifecycleOwner) { event ->
            event?.let { (enabled, time) ->
                val parts = time.split(":")
                NotificationScheduler.scheduleDailyReminder(requireContext(), parts[0].toInt(), parts[1].toInt(), enabled)
                viewModel.clearScheduleEvent()
            }
        }

        // 8. Logout
        viewModel.logoutEvent.observe(viewLifecycleOwner) { loggedOut ->
            if (loggedOut) {
                logoutListener?.onLogoutSuccess()
                viewModel.clearLogoutEvent()
            }
        }

        // 9. Gráfico de Pizza (Stats)
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

        // 10. Card Inteligente (Insights)
        viewModel.insight.observe(viewLifecycleOwner) { insight ->
            insight?.let {
                binding.tvInsightTitle.text = it.title
                binding.tvInsightMessage.text = it.message
                binding.ivInsightIcon.setImageResource(it.iconRes)
                try {
                    val colorContent = ContextCompat.getColor(requireContext(), it.colorRes)
                    val colorBackground = ContextCompat.getColor(requireContext(), it.backgroundTint)

                    binding.ivInsightIcon.setColorFilter(colorContent)
                    binding.tvInsightTitle.setTextColor(colorContent)
                    binding.cardSmartInsight.setCardBackgroundColor(colorBackground)
                    binding.cardSmartInsight.isVisible = true
                } catch (e: Exception) {
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
        val dynamicColor = MaterialColors.getColor(binding.pieChartProfile, com.google.android.material.R.attr.colorOnSurface)
        binding.pieChartProfile.setCenterTextColor(dynamicColor)
        binding.pieChartProfile.legend.isEnabled = false
        binding.pieChartProfile.description.isEnabled = false
        binding.pieChartProfile.animateY(1400, Easing.EaseInOutQuad)
        binding.pieChartProfile.invalidate()
    }

    private fun showTimePickerDialog() {
        val current = viewModel.draftTime.value ?: Pair(20, 0)
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