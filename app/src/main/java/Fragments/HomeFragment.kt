package com.example.apphumor

import android.animation.Animator
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.airbnb.lottie.LottieAnimationView
import com.example.apphumor.adapter.HumorNoteAdapter
import com.example.apphumor.databinding.FragmentHomeBinding
import com.example.apphumor.di.DependencyProvider
import com.example.apphumor.utils.SwipeToDeleteCallback
import com.example.apphumor.viewmodel.AppViewModelFactory
import com.example.apphumor.viewmodel.HomeViewModel
import com.google.android.material.snackbar.Snackbar
// Imports Novos
import android.util.Base64
import android.graphics.BitmapFactory
import java.util.Calendar

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: HomeViewModel
    private lateinit var adapter: HumorNoteAdapter

    private var pendingAnimation = false
    private var editedNoteId: String? = null

    private val TAG = "HomeFragment"

    companion object {
        const val ADD_NOTE_REQUEST_CODE = 1001
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val factory = AppViewModelFactory(
            DependencyProvider.auth,
            DependencyProvider.databaseRepository
        )
        viewModel = ViewModelProvider(this, factory)[HomeViewModel::class.java]

        setupRecyclerView()
        setupButton()
        setupObservers()
        setupHeader() // <--- NOVA CHAMADA PARA CONFIGURAR O TOPO
    }

    // --- NOVA FUNÇÃO: Configura Saudação e Avatar ---
    private fun setupHeader() {
        // 1. Saudação (Bom dia/tarde/noite)
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val greeting = when (hour) {
            in 5..11 -> "Bom dia,"
            in 12..17 -> "Boa tarde,"
            else -> "Boa noite,"
        }
        binding.tvGreeting.text = greeting

        // 2. Dados do Usuário (Nome e Foto)
        viewModel.currentUser.observe(viewLifecycleOwner) { user ->
            user?.let {
                val firstName = it.nome?.split(" ")?.firstOrNull() ?: "Visitante"
                binding.tvUserName.text = "$firstName!"

                if (!it.fotoBase64.isNullOrEmpty()) {
                    try {
                        val imageBytes = Base64.decode(it.fotoBase64, Base64.DEFAULT)
                        val decodedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                        binding.ivUserAvatar.setImageBitmap(decodedImage)
                    } catch (e: Exception) {
                        Log.e(TAG, "Erro ao decodificar avatar: ${e.message}")
                        binding.ivUserAvatar.setImageResource(R.drawable.ic_mood_neutral_anim)
                    }
                } else {
                    binding.ivUserAvatar.setImageResource(R.drawable.ic_mood_neutral_anim)
                }
            }
        }

        // 3. AÇÃO DE CLIQUE (CORRIGIDA COM SEU ID REAL)
        binding.ivUserAvatar.setOnClickListener {
            // Busca a barra pelo ID correto que vi no seu XML: 'bottomNav'
            val bottomNav = requireActivity().findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNav)

            if (bottomNav != null) {
                // Simula o clique no botão de Perfil
                // IMPORTANTE: Se 'navigation_profile' ficar vermelho, abra o arquivo 'res/menu/bottom_nav_menu.xml'
                // e veja qual é o ID do item de perfil (pode ser id_profile, navigation_user, etc)
                bottomNav.selectedItemId = R.id.nav_profile
            } else {
                Log.e(TAG, "Barra de navegação não encontrada!")
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = HumorNoteAdapter(
            showEditButton = true,
            onEditClick = { note ->
                editedNoteId = note.id
                val intent = Intent(requireActivity(), AddHumorActivity::class.java).apply {
                    putExtra("EDIT_NOTE", note)
                }
                startActivityForResult(intent, ADD_NOTE_REQUEST_CODE)
            }
        )

        binding.recyclerViewNotes.apply {
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
            adapter = this@HomeFragment.adapter
        }

        setupSwipeToDelete()
    }

    private fun setupSwipeToDelete() {
        val swipeHandler = SwipeToDeleteCallback(requireContext()) { position ->
            val currentList = adapter.currentList
            if (position >= 0 && position < currentList.size) {
                val noteToDelete = currentList[position]
                viewModel.deleteNote(noteToDelete)
                showUndoSnackbar()
            }
        }
        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.recyclerViewNotes)
    }

    private fun showUndoSnackbar() {
        Snackbar.make(binding.root, "Registro excluído", Snackbar.LENGTH_LONG)
            .setAction("DESFAZER") { viewModel.undoDelete() }
            .show()
    }

    private fun setupButton() {
        binding.emptyState.findViewById<View>(R.id.btn_add_record).setOnClickListener {
            editedNoteId = null
            val intent = Intent(requireActivity(), AddHumorActivity::class.java)
            startActivityForResult(intent, ADD_NOTE_REQUEST_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ADD_NOTE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Log.d(TAG, "Nota salva com sucesso. Preparando animação.")
            pendingAnimation = true
            // Recarrega dados do usuário caso ele tenha mudado a foto/nome no processo
            viewModel.loadUserData()
        }
    }

    private fun setupObservers() {
        viewModel.todayNotes.observe(viewLifecycleOwner) { notes ->
            if (notes.isNotEmpty()) {
                binding.recyclerViewNotes.visibility = View.VISIBLE
                binding.emptyState.visibility = View.GONE

                adapter.submitList(notes) {
                    if (pendingAnimation) {
                        val targetId = editedNoteId ?: notes.firstOrNull()?.id
                        if (targetId != null) {
                            adapter.triggerAnimation(targetId)
                            binding.recyclerViewNotes.smoothScrollToPosition(0)
                        }
                        pendingAnimation = false
                        editedNoteId = null
                    }
                }
            } else {
                showEmptyState()
                adapter.submitList(emptyList())
            }
        }

        viewModel.streakText.observe(viewLifecycleOwner) { text ->
            val badgeText = binding.progressCard.root.findViewById<TextView>(R.id.tv_streak_count)
            badgeText?.text = text
        }

        viewModel.showConfetti.observe(viewLifecycleOwner) { show ->
            if (show) {
                val lottie = binding.progressCard.root.findViewById<LottieAnimationView>(R.id.lottieConfetti)
                lottie?.let { animation ->
                    animation.visibility = View.VISIBLE
                    animation.playAnimation()
                    animation.addAnimatorListener(object : Animator.AnimatorListener {
                        override fun onAnimationEnd(animation: Animator) {
                            lottie.visibility = View.GONE
                            viewModel.resetConfetti()
                        }
                        override fun onAnimationStart(animation: Animator) {}
                        override fun onAnimationCancel(animation: Animator) {}
                        override fun onAnimationRepeat(animation: Animator) {}
                    })
                }
            }
        }

        viewModel.weekDays.observe(viewLifecycleOwner) { days ->
            val container = binding.progressCard.root.findViewById<LinearLayout>(R.id.ll_days_container)
            if (container == null) return@observe
            container.removeAllViews()
            val context = requireContext()
            val size = 32.dpToPx(context)
            val margin = 4.dpToPx(context)

            days.forEach { day ->
                val itemLayout = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                val circleView = ImageView(context).apply {
                    layoutParams = LinearLayout.LayoutParams(size, size).apply { bottomMargin = margin }
                    if (day.hasEntry) {
                        setBackgroundResource(R.drawable.bg_day_filled)
                        setImageResource(R.drawable.ic_check_24)
                        setColorFilter(ContextCompat.getColor(context, android.R.color.white))
                        scaleType = ImageView.ScaleType.CENTER_INSIDE
                        setPadding(10, 10, 10, 10)
                    } else {
                        if (day.isToday) setBackgroundResource(R.drawable.bg_day_today_highlight)
                        else setBackgroundResource(R.drawable.bg_day_empty)
                    }
                    if (day.isToday) elevation = 8f
                }
                val labelView = TextView(context).apply {
                    text = day.label
                    textSize = 12f
                    gravity = Gravity.CENTER
                    if (day.isToday) {
                        setTextColor(ContextCompat.getColor(context, R.color.md_theme_light_primary))
                        setTypeface(null, Typeface.BOLD)
                    } else {
                        setTextColor(ContextCompat.getColor(context, R.color.md_theme_dark_onSurface))
                    }
                }
                itemLayout.addView(circleView)
                itemLayout.addView(labelView)
                container.addView(itemLayout)
            }
        }
    }

    private fun showEmptyState() {
        binding.recyclerViewNotes.visibility = View.GONE
        binding.emptyState.visibility = View.VISIBLE
    }

    private fun Int.dpToPx(context: Context): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}