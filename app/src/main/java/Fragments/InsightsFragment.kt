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
import com.example.apphumor.databinding.FragmentInsightsBinding // Necessário ter viewBinding ativado
import com.example.apphumor.models.HumorNote
import com.example.apphumor.models.Insight
import com.example.apphumor.repository.DatabaseRepository
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

class InsightsFragment : Fragment() {
    private var _binding: FragmentInsightsBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private val dbRepository = DatabaseRepository()



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Assume-se que você tem viewBinding configurado
        _binding = FragmentInsightsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()
        loadAndCalculateInsights()
    }

    override fun onResume() {
        super.onResume()
        // Recarrega os insights ao voltar para o fragmento
        loadAndCalculateInsights()
    }

    /**
     * Carrega as notas de humor e inicia o cálculo dos insights.
     */
    private fun loadAndCalculateInsights() {
        val userId = auth.currentUser?.uid ?: return

        dbRepository.getHumorNotes(userId) { notes ->
            activity?.runOnUiThread {
                val insights = calculateInsights(notes)
                displayInsights(insights)
            }
        }
    }

    /**
     * Lógica principal de cálculo dos insights a partir das notas de humor.
     */
    private fun calculateInsights(notes: List<HumorNote>): List<Insight> {
        if (notes.isEmpty()) return emptyList()

        // ----------------------------------------------------
        // PASSO 1: Filtra as notas apenas para o MÊS ATUAL
        // ----------------------------------------------------
        val currentCalendar = Calendar.getInstance()
        val currentMonth = currentCalendar.get(Calendar.MONTH)
        val currentYear = currentCalendar.get(Calendar.YEAR)

        // Filtra notas que estão no mês e ano atual
        val notesCurrentMonth = notes.filter { note ->
            val noteCalendar = Calendar.getInstance().apply {
                timeInMillis = note.timestamp
            }
            noteCalendar.get(Calendar.MONTH) == currentMonth &&
                    noteCalendar.get(Calendar.YEAR) == currentYear
        }

        if (notesCurrentMonth.isEmpty()) {
            // Se não houver notas este mês, ainda podemos mostrar o insight de total lifetime
            return listOf(
                Insight(
                    rotulo = "Dias Ativos (Mês)",
                    valor = "0 dias",
                    iconResId = R.drawable.ic_active_days_24,
                    backgroundColorResId = R.color.insight_calm_bg
                ),
                Insight(
                    rotulo = "Humor Mais Comum",
                    valor = "Sem Registros",
                    iconResId = R.drawable.ic_neutral_24,
                    backgroundColorResId = R.color.insight_neutral_bg
                ),
                Insight(
                    rotulo = "Humor Menos Comum",
                    valor = "Sem Registros",
                    iconResId = R.drawable.ic_neutral_24,
                    backgroundColorResId = R.color.insight_neutral_bg
                )
            )
        }

        // Recalcula o total de notas (agora é o total do MÊS)
        val totalNotesInMonth = notesCurrentMonth.size

        // Agrupa e conta a frequência de humores APENAS para o mês atual
        val humorCounts = notesCurrentMonth
            .filter { it.humor != null }
            .groupingBy { it.humor!!.lowercase(Locale.ROOT) }
            .eachCount()


        // ----------------------------------------------------
        // 1. Humor Mais Comum (Baseado no MÊS)
        // ----------------------------------------------------
        val mostCommonHumorEntry = humorCounts.maxByOrNull { it.value }
        val mostCommonHumor = mostCommonHumorEntry?.key?.replaceFirstChar { it.titlecase(Locale.ROOT) } ?: "Neutro"
        val countCommon = mostCommonHumorEntry?.value ?: 0
        val humorIconCommon = getIconAndColorForHumor(mostCommonHumor)

        val humorInsight = Insight(
            rotulo = "Humor Mais Comum (Mês)",
            valor = "$mostCommonHumor ($countCommon notas)", // Mostra a contagem de notas
            iconResId = humorIconCommon.first,
            backgroundColorResId = humorIconCommon.second
        )

        // ----------------------------------------------------
        // 2. Dias de Registros Ativos (que é igual a Total de Notas do Mês)
        // ----------------------------------------------------
        // Este insight se torna um contador direto de notas/dias do mês.
        val activeDaysInsight = Insight(
            rotulo = "Dias Ativos (Mês)",
            valor = "$totalNotesInMonth dias",
            iconResId = R.drawable.ic_active_days_24,
            backgroundColorResId = R.color.insight_calm_bg
        )

        // ----------------------------------------------------
        // 3. Humor Menos Comum (Baseado no MÊS)
        // ----------------------------------------------------
        val leastCommonHumorEntry = humorCounts
            // Filtramos para garantir que o humor Menos Comum seja diferente do Mais Comum
            // (A menos que só haja um humor registrado)
            .minByOrNull { it.value }

        val leastCommonHumor = leastCommonHumorEntry?.key?.replaceFirstChar { it.titlecase(Locale.ROOT) } ?: "N/A"
        val countLeastCommon = leastCommonHumorEntry?.value ?: 0

        val humorIconLeastCommon = getIconAndColorForHumor(leastCommonHumor)

        val humorLeastCommonInsight = Insight(
            rotulo = "Humor Menos Comum (Mês)",
            valor = "$leastCommonHumor ($countLeastCommon notas)", // Mostra a contagem de notas
            iconResId = humorIconLeastCommon.first,
            backgroundColorResId = R.color.insight_angry_bg
        )

        // Retorna a nova lista de insights baseada no Mês Atual
        return listOf(humorInsight, activeDaysInsight, humorLeastCommonInsight)
    }

    /**
     * Infla a UI dinamicamente com os Insights calculados.
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
            val itemView = LayoutInflater.from(context).inflate(R.layout.card_insight_item, container, false)

            itemView.findViewById<TextView>(R.id.tv_insight_label).text = insight.rotulo
            itemView.findViewById<TextView>(R.id.tv_insight_value).text = insight.valor

            val iconContainer = itemView.findViewById<FrameLayout>(R.id.icon_container)
            val iconView = itemView.findViewById<ImageView>(R.id.iv_insight_icon)

            context?.let { ctx ->
                // Aplica a cor de fundo dinamicamente ao drawable do FrameLayout
                val backgroundDrawable = ContextCompat.getDrawable(ctx, R.drawable.circle_background_insight)
                backgroundDrawable?.setTint(ContextCompat.getColor(ctx, insight.backgroundColorResId))
                iconContainer.background = backgroundDrawable

                iconView.setImageResource(insight.iconResId)
            }

            container.addView(itemView)
        }
    }

    // Mapeamento de humor para ícone e cor
    private fun getIconAndColorForHumor(humorType: String): Pair<Int, Int> {
        return when (humorType.lowercase(Locale.ROOT)) {
            "calmo" -> Pair(R.drawable.ic_calm_24, R.color.insight_calm_bg)
            "energetico" -> Pair(R.drawable.ic_energetic_24, R.color.insight_energetic_bg)
            "triste" -> Pair(R.drawable.ic_sad_24, R.color.insight_sad_bg)
            "irritado" -> Pair(R.drawable.ic_angry_24, R.color.insight_angry_bg)
            else -> Pair(R.drawable.ic_neutral_24, R.color.insight_neutral_bg)
        }
    }

    // Mapeamento do número do dia da semana para nome (garantir localização se necessário)
    private fun getDayName(dayNum: Int): String {
        return when (dayNum) {
            1 -> "Domingo"
            2 -> "Segunda"
            3 -> "Terça"
            4 -> "Quarta"
            5 -> "Quinta"
            6 -> "Sexta"
            7 -> "Sábado"
            else -> "Dia Desconhecido"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}