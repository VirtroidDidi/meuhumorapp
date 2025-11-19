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

/**
 * [InsightsFragment]
 * Responsável apenas pela exibição dos insights.
 * A lógica de obtenção e cálculo dos dados é delegada ao InsightsViewModel.
 */
class InsightsFragment : Fragment() {
    private var _binding: FragmentInsightsBinding? = null
    private val binding get() = _binding!!

    // Inicializa o ViewModel
    private val viewModel: InsightsViewModel by lazy { ViewModelProvider(this).get(InsightsViewModel::class.java) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInsightsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()
    }

    /**
     * Configura a observação dos LiveData do ViewModel.
     */
    private fun setupObservers() {
        // Observa os Insights calculados. O ViewModel cuida da atualização em tempo real.
        viewModel.insights.observe(viewLifecycleOwner) { insights ->
            displayInsights(insights)
        }

        // Opcional: Observa o estado de carregamento
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            // Implemente aqui a lógica de ProgressBar se necessário.
        }
    }

    /**
     * Infla a UI dinamicamente com os Insights prontos, recebidos do ViewModel.
     */
    private fun displayInsights(insights: List<Insight>) {
        val container = binding.llInsightsContainer
        container.removeAllViews()

        if (insights.isEmpty()) {
            val noDataTv = TextView(context).apply {
                text = "Sem dados de humor suficientes para gerar insights."
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                setPadding(0, 32, 0, 32)
            }
            container.addView(noDataTv)
            return
        }

        insights.forEach { insight ->
            // Usamos o layout existente: card_insight_item (R.layout.card_insight_item)
            val itemView = LayoutInflater.from(context).inflate(R.layout.card_insight_item, container, false)

            // CORREÇÃO: Acessando rotulo e valor do Model Insight
            itemView.findViewById<TextView>(R.id.tv_insight_label).text = insight.rotulo
            itemView.findViewById<TextView>(R.id.tv_insight_value).text = insight.valor

            val iconContainer = itemView.findViewById<FrameLayout>(R.id.icon_container)
            val iconView = itemView.findViewById<ImageView>(R.id.iv_insight_icon)

            context?.let { ctx ->
                // Aplica a cor de fundo e o ícone dinamicamente
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