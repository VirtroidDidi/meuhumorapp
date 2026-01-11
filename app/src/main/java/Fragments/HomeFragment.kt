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

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: HomeViewModel
    private lateinit var adapter: HumorNoteAdapter

    private val TAG = "HomeFragment"

    companion object {
        const val ADD_NOTE_REQUEST_CODE = 1001
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
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
    }

    private fun setupRecyclerView() {
        adapter = HumorNoteAdapter(showEditButton = true, onEditClick = { note ->
            val intent = Intent(requireActivity(), AddHumorActivity::class.java).apply {
                putExtra("EDIT_NOTE", note)
            }
            startActivityForResult(intent, ADD_NOTE_REQUEST_CODE)
        })

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

        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(binding.recyclerViewNotes)
    }

    private fun showUndoSnackbar() {
        Snackbar.make(binding.root, "Registro excluído", Snackbar.LENGTH_LONG)
            .setAction("DESFAZER") {
                viewModel.undoDelete()
            }
            .show()
    }

    private fun setupButton() {
        binding.emptyState.findViewById<View>(R.id.btn_add_record).setOnClickListener {
            val intent = Intent(requireActivity(), AddHumorActivity::class.java)
            startActivityForResult(intent, ADD_NOTE_REQUEST_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ADD_NOTE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Log.d(TAG, "Nota salva com sucesso.")
        }
    }

    private fun setupObservers() {
        // 1. Observa lista de notas de HOJE
        viewModel.todayNotes.observe(viewLifecycleOwner) { notes ->
            if (notes.isNotEmpty()) {
                binding.recyclerViewNotes.visibility = View.VISIBLE
                binding.emptyState.visibility = View.GONE
                adapter.submitList(notes)
            } else {
                showEmptyState()
                adapter.submitList(emptyList())
            }
        }

        // --- NOVA LÓGICA DO CARD DE SEQUÊNCIA (STREAK) ---

        // 2. Observa o Texto do Badge ("5 Dias")
        viewModel.streakText.observe(viewLifecycleOwner) { text ->
            // Atenção: Certifique-se que o ID 'tv_streak_count' existe no seu layout 'item_sequence_card.xml'
            val badgeText = binding.progressCard.root.findViewById<TextView>(R.id.tv_streak_count)
            badgeText?.text = text
        }

        // 3. Observa a animação de Confetes
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

        // 4. Renderiza as Bolinhas (Dias da Semana)
        viewModel.weekDays.observe(viewLifecycleOwner) { days ->
            val container = binding.progressCard.root.findViewById<LinearLayout>(R.id.ll_days_container)
            if (container == null) return@observe // Segurança caso o ID não exista ainda

            container.removeAllViews()
            val context = requireContext()

            // Dimensões
            val size = 32.dpToPx(context)
            val margin = 4.dpToPx(context)

            days.forEach { day ->
                // Layout Vertical para cada dia (Bolinha + Letra)
                val itemLayout = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }

                // A Bolinha (Visual)
                val circleView = ImageView(context).apply {
                    layoutParams = LinearLayout.LayoutParams(size, size).apply {
                        bottomMargin = margin
                    }

                    if (day.hasEntry) {
                        // Dia Completo (Verde com Check)
                        // Certifique-se de ter criado o drawable 'bg_day_filled.xml'
                        setBackgroundResource(R.drawable.bg_day_filled)
                        // Use um ícone padrão do Android ou seu próprio
                        setImageResource(R.drawable.ic_check_24)
                        setColorFilter(ContextCompat.getColor(context, android.R.color.white))
                        scaleType = ImageView.ScaleType.CENTER_INSIDE
                        setPadding(10, 10, 10, 10) // Padding interno para o ícone não ficar gigante
                    } else {
                        // Dia Vazio
                        if (day.isToday) {
                            // Hoje mas vazio (Destaque/Borda)
                            setBackgroundResource(R.drawable.bg_day_today_highlight)
                        } else {
                            // Passado/Futuro vazio
                            setBackgroundResource(R.drawable.bg_day_empty)
                        }
                    }

                    if (day.isToday) {
                        elevation = 8f
                    }
                }

                // O Texto (S, T, Q... H)
                val labelView = TextView(context).apply {
                    text = day.label
                    textSize = 12f
                    gravity = Gravity.CENTER

                    // Se for hoje, cor primária e negrito
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

    // Função utilitária para conversão de DP
    private fun Int.dpToPx(context: Context): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}