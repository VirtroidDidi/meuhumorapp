package com.example.apphumor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.apphumor.databinding.FragmentInsightsBinding
import com.example.apphumor.di.DependencyProvider
import com.example.apphumor.models.Insight
import com.example.apphumor.viewmodel.InsightsViewModel
import com.example.apphumor.viewmodel.InsightsViewModelFactory
import com.example.apphumor.viewmodel.TimeRange // Importando do pacote correto
import com.google.android.material.chip.ChipGroup

class InsightsFragment : Fragment() {
    private var _binding: FragmentInsightsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: InsightsViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInsightsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Inicializa o ViewModel
        viewModel = ViewModelProvider(
            this,
            InsightsViewModelFactory(
                DependencyProvider.auth,
                DependencyProvider.databaseRepository
            )
        ).get(InsightsViewModel::class.java)

        // 2. Configura os filtros e observadores
        setupChips()
        setupObservers()
    }

    private fun setupChips() {
        // Configura o listener do ChipGroup para filtrar por período
        binding.chipGroupFilters.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener

            val selectedId = checkedIds[0]
            val range = when (selectedId) {
                R.id.chip7Days -> TimeRange.LAST_7_DAYS
                R.id.chip30Days -> TimeRange.LAST_30_DAYS
                else -> TimeRange.CURRENT_MONTH
            }
            viewModel.setTimeRange(range)
        }
    }

    private fun setupObservers() {
        // Observa a lista de insights calculados
        viewModel.insights.observe(viewLifecycleOwner) { insights ->
            displayInsights(insights)
        }

        // (Opcional) Observa loading se quiser colocar uma ProgressBar depois
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            // binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    private fun displayInsights(insights: List<Insight>) {
        val container = binding.llInsightsContainer
        container.removeAllViews()

        if (insights.isEmpty()) {
            val noDataTv = TextView(context).apply {
                text = "Sem dados para o período selecionado."
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                setPadding(0, 32, 0, 32)
            }
            container.addView(noDataTv)
            return
        }

        insights.forEach { insight ->
            val itemView = LayoutInflater.from(context).inflate(R.layout.card_insight_item, container, false)

            itemView.findViewById<TextView>(R.id.tv_insight_label).text = insight.rotulo
            itemView.findViewById<TextView>(R.id.tv_insight_value).text = insight.valor

            val iconContainer = itemView.findViewById<FrameLayout>(R.id.icon_container)
            val iconView = itemView.findViewById<ImageView>(R.id.iv_insight_icon)

            context?.let { ctx ->
                val backgroundDrawable = ContextCompat.getDrawable(ctx, R.drawable.circle_background_insight)
                backgroundDrawable?.setTint(ContextCompat.getColor(ctx, insight.backgroundColorResId))
                iconContainer.background = backgroundDrawable
                iconView.setImageResource(insight.iconResId)
            }

            container.addView(itemView)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}