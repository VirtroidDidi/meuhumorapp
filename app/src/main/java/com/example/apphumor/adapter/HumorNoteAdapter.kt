package com.example.apphumor.adapter

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
import com.example.apphumor.utils.HumorUtils
import java.text.SimpleDateFormat
import java.util.*

class HumorNoteAdapter(
    private val showEditButton: Boolean = true,
    private val showSyncStatus: Boolean = true, // NOVO PARÂMETRO COM PADRÃO TRUE (Home não quebra)
    private val onEditClick: ((HumorNote) -> Unit)? = null
) : ListAdapter<HumorNote, HumorNoteAdapter.HumorNoteViewHolder>(HumorNoteDiffCallback()) {

    inner class HumorNoteViewHolder(private val binding: ItemNoteBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(note: HumorNote) {
            val context = binding.root.context

            with(binding) {
                // 1. Botão Editar
                btnEdit.visibility = if (showEditButton) View.VISIBLE else View.GONE

                // 2. Configuração Visual (Ícone e Cor do Humor)
                val moodStyle = HumorUtils.getMoodStyle(note.humor)
                ivHumorIcon.setImageResource(moodStyle.iconRes)
                val color = ContextCompat.getColor(context, moodStyle.contentColorRes)
                ImageViewCompat.setImageTintList(ivHumorIcon, ColorStateList.valueOf(color))
                tvHumor.text = context.getString(moodStyle.labelRes)

                // 3. Descrição
                tvDescricao.text = note.descricao.takeIf { !it.isNullOrEmpty() } ?: "Sem descrição"
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

                // 6. Status de Sincronização (WhatsApp Style)
                // LÓGICA ALTERADA: Verifica primeiro se deve mostrar o status
                if (showSyncStatus) {
                    ivSyncStatus.visibility = View.VISIBLE

                    if (note.isSynced) {
                        // TRUE = Já foi para a nuvem -> Check Duplo Azul
                        ivSyncStatus.setImageResource(R.drawable.ic_status_synced)
                        ivSyncStatus.contentDescription = "Sincronizado na nuvem"
                    } else {
                        // FALSE = Ainda está só no cache -> Check Simples Cinza
                        ivSyncStatus.setImageResource(R.drawable.ic_status_pending)
                        ivSyncStatus.contentDescription = "Salvo no dispositivo"
                    }
                } else {
                    // Se estiver no histórico, escondemos o ícone completamente
                    ivSyncStatus.visibility = View.GONE
                }
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