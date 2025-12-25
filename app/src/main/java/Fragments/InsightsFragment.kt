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
import com.example.apphumor.models.Insight
import com.example.apphumor.viewmodel.InsightsViewModel
import com.example.apphumor.viewmodel.HumorViewModelFactory
import com.example.apphumor.viewmodel.TimeRange

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

        // 1. Acessa o Container via Application
        val appContainer = (requireActivity().application as AppHumorApplication).container
        val factory = HumorViewModelFactory(appContainer.databaseRepository, appContainer.auth)

        // 2. Inicializa o ViewModel com a Factory Unificada
        viewModel = ViewModelProvider(this, factory)[InsightsViewModel::class.java]

        setupFilters()
        setupObservers()
    }

    private fun setupFilters() {
        binding.chipGroupFilters.setOnCheckedStateChangeListener { group, checkedIds ->
            val range = when (checkedIds.firstOrNull()) {
                R.id.chip7Days -> TimeRange.LAST_7_DAYS
                R.id.chip30Days -> TimeRange.LAST_30_DAYS
                R.id.chipMonth -> TimeRange.CURRENT_MONTH
                else -> TimeRange.CURRENT_MONTH
            }
            viewModel.setTimeRange(range)
        }
    }

    private fun setupObservers() {
        viewModel.insights.observe(viewLifecycleOwner) { insights ->
            displayInsights(insights)
        }

        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            // Você pode adicionar um ProgressBar aqui se desejar
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
                // Nota: Verifique se circle_background_insight existe em drawables
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