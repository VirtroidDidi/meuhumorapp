package com.example.apphumor.adapter

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.apphumor.R
import com.example.apphumor.databinding.ItemNoteBinding
import com.example.apphumor.models.HumorNote
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log

class HumorNoteAdapter(
    private val showEditButton: Boolean = true,
    private val onEditClick: ((HumorNote) -> Unit)? = null
) : ListAdapter<HumorNote, HumorNoteAdapter.HumorNoteViewHolder>(HumorNoteDiffCallback()) {

    private val TAG = "HumorNoteAdapter"

    inner class HumorNoteViewHolder(private val binding: ItemNoteBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(note: HumorNote) {
            val context = binding.root.context

            with(binding) {
                // 1. Visibilidade do Botão Editar
                btnEdit.visibility = if (showEditButton) View.VISIBLE else View.GONE

                // 2. Configuração Visual do Humor (Ícone e Cor)
                val moodKey = note.humor ?: "Neutral"
                val (iconRes, colorRes, labelRes) = getMoodConfig(moodKey)

                // Define o Ícone
                ivHumorIcon.setImageResource(iconRes)

                // Define a Cor do Ícone (Tint)
                val color = ContextCompat.getColor(context, colorRes)
                ImageViewCompat.setImageTintList(ivHumorIcon, ColorStateList.valueOf(color))

                // Define o Texto Traduzido (ex: mostra "Incrível" em vez de "Rad")
                tvHumor.text = try {
                    context.getString(labelRes)
                } catch (e: Exception) {
                    moodKey // Fallback se não achar string
                }

                // 3. Descrição
                tvDescricao.text = note.descricao.takeIf { !it.isNullOrEmpty() } ?: "Sem descrição"

                // Oculta TextView de descrição se estiver vazia para economizar espaço
                tvDescricao.visibility = if (note.descricao.isNullOrEmpty()) View.GONE else View.VISIBLE

                // 4. Data
                val timestamp = note.timestamp
                if (timestamp > 0) {
                    try {
                        val sdf = SimpleDateFormat("dd/MM/yyyy • HH:mm", Locale.getDefault())
                        tvData.text = sdf.format(Date(timestamp))
                    } catch (e: Exception) {
                        tvData.text = "--/--/----"
                    }
                } else {
                    tvData.text = ""
                }

                // 5. Listener de Edição
                if (showEditButton && onEditClick != null) {
                    btnEdit.setOnClickListener { onEditClick.invoke(note) }
                } else {
                    btnEdit.setOnClickListener(null)
                }
            }
        }

        // Função auxiliar para mapear String -> Recursos
        private fun getMoodConfig(mood: String): Triple<Int, Int, Int> {
            return when (mood) {
                "Rad", "Incrível", "Excelente" -> Triple(R.drawable.ic_mood_rad, R.color.mood_rad, R.string.humor_rad)
                "Happy", "Feliz", "Bem", "Good" -> Triple(R.drawable.ic_mood_happy, R.color.mood_happy, R.string.humor_happy)
                "Grateful", "Grato" -> Triple(R.drawable.ic_mood_grateful, R.color.mood_grateful, R.string.humor_grateful)
                "Calm", "Calmo" -> Triple(R.drawable.ic_mood_calm, R.color.mood_calm, R.string.humor_calm)
                "Neutral", "Neutro" -> Triple(R.drawable.ic_mood_neutral, R.color.mood_neutral, R.string.humor_neutral)
                "Pensive", "Pensativo" -> Triple(R.drawable.ic_mood_pensive, R.color.mood_pensive, R.string.humor_pensive)
                "Tired", "Cansado" -> Triple(R.drawable.ic_mood_tired, R.color.mood_tired, R.string.humor_tired)
                "Sad", "Triste" -> Triple(R.drawable.ic_mood_sad, R.color.mood_sad, R.string.humor_sad)
                "Anxious", "Ansioso" -> Triple(R.drawable.ic_mood_anxious, R.color.mood_anxious, R.string.humor_anxious)
                "Angry", "Irritado" -> Triple(R.drawable.ic_mood_angry, R.color.mood_angry, R.string.humor_angry)
                // Fallback para energéticos antigos ou desconhecidos
                "Energetic", "Energético" -> Triple(R.drawable.ic_mood_rad, R.color.mood_rad, R.string.humor_rad)
                else -> Triple(R.drawable.ic_mood_neutral, R.color.mood_neutral, R.string.humor_neutral)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HumorNoteViewHolder {
        val binding = ItemNoteBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HumorNoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HumorNoteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class HumorNoteDiffCallback : DiffUtil.ItemCallback<HumorNote>() {
        override fun areItemsTheSame(oldItem: HumorNote, newItem: HumorNote): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: HumorNote, newItem: HumorNote): Boolean {
            return oldItem == newItem
        }
    }
}