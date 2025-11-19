// ARQUIVO: app/src/main/java/com/example/apphumor/adapter/HumorNoteAdapter.kt

package com.example.apphumor.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.apphumor.databinding.ItemNoteBinding
import com.example.apphumor.models.HumorNote
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log

/**
 * Adaptador unificado e flexível para exibir Notas de Humor.
 * Controla a visibilidade do botão de edição e o callback de clique.
 *
 * @param showEditButton Define se o botão de edição deve ser visível.
 * @param onEditClick Função de callback opcional, executada ao clicar no botão de edição.
 */
class HumorNoteAdapter(
    private val showEditButton: Boolean = true,
    private val onEditClick: ((HumorNote) -> Unit)? = null
) : ListAdapter<HumorNote, HumorNoteAdapter.HumorNoteViewHolder>(HumorNoteDiffCallback()) {

    private val TAG = "HumorNoteAdapter"

    inner class HumorNoteViewHolder(private val binding: ItemNoteBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(note: HumorNote) {
            with(binding) {

                // 1. Controle de Visibilidade do Botão
                btnEdit.visibility = if (showEditButton) View.VISIBLE else View.GONE

                // 2. Configuração dos Dados
                tvHumor.text = note.humor.takeIf { !it.isNullOrEmpty() } ?: "Sem humor"
                tvDescricao.text = note.descricao.takeIf { !it.isNullOrEmpty() } ?: "Sem descrição"

                // 3. Formatação da Data (CORREÇÃO CRÍTICA: Usa 'note.timestamp')
                val timestamp = note.timestamp
                if (timestamp > 0) {
                    try {
                        tvData.text = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                            .format(Date(timestamp))
                    } catch (e: Exception) {
                        tvData.text = "Data inválida"
                        Log.e(TAG, "Erro de formatação: ${e.message}")
                    }
                } else {
                    tvData.text = "Sem data"
                    Log.w(TAG, "Campo 'timestamp' ausente na nota ${note.id}")
                }
                // Fim da correção da Formatação da Data

                // 4. Configuração do Listener
                if (showEditButton && onEditClick != null) {
                    btnEdit.setOnClickListener { onEditClick.invoke(note) }
                } else {
                    btnEdit.setOnClickListener(null)
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
            // CORREÇÃO CRÍTICA: Compara o novo campo 'timestamp'
            return oldItem.id == newItem.id &&
                    oldItem.humor == newItem.humor &&
                    oldItem.descricao == newItem.descricao &&
                    oldItem.timestamp == newItem.timestamp
        }
    }
}