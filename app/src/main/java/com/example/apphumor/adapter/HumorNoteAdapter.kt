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
import android.util.Log // Adicionado Log para debug, seguindo padrão dos originais

/**
 * Adaptador unificado e flexível para exibir Notas de Humor.
 * Substitui os adaptadores NoteAdapter e AllNotesAdapter, controlando
 * a visibilidade do botão de edição e o callback de clique.
 *
 * @param showEditButton Define se o botão de edição deve ser visível (true para HomeFragment, false para HistoryFragment).
 * @param onEditClick Função de callback opcional, executada ao clicar no botão de edição.
 */
class HumorNoteAdapter(
    private val showEditButton: Boolean = true, // Padrão: Visível (como era no NoteAdapter)
    private val onEditClick: ((HumorNote) -> Unit)? = null
) : ListAdapter<HumorNote, HumorNoteAdapter.HumorNoteViewHolder>(HumorNoteDiffCallback()) {

    // Nomenclatura Padrão
    private val TAG = "HumorNoteAdapter"

    // CORREÇÃO: Usamos o nome do ViewHolder consistente com o nome do Adapter
    inner class HumorNoteViewHolder(private val binding: ItemNoteBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(note: HumorNote) {
            with(binding) {

                // 1. Controle de Visibilidade do Botão (Baseado no parâmetro showEditButton)
                btnEdit.visibility = if (showEditButton) View.VISIBLE else View.GONE

                // 2. Configuração dos Dados (Combinando a lógica dos dois originais)
                tvHumor.text = note.humor.takeIf { !it.isNullOrEmpty() } ?: "Sem humor"
                tvDescricao.text = note.descricao.takeIf { !it.isNullOrEmpty() } ?: "Sem descrição"

                // 3. Formatação da Data
                note.data["time"]?.let {
                    try {
                        val timestamp = it as Long
                        tvData.text = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                            .format(Date(timestamp))
                    } catch (e: Exception) {
                        tvData.text = "Data inválida"
                        Log.e(TAG, "Erro de formatação: ${e.message}")
                    }
                } ?: run {
                    tvData.text = "Sem data"
                    Log.w(TAG, "Campo 'time' ausente na nota ${note.id}")
                }

                // 4. Configuração do Listener (Apenas se o botão for visível E o callback existir)
                if (showEditButton && onEditClick != null) {
                    btnEdit.setOnClickListener { onEditClick.invoke(note) }
                } else {
                    btnEdit.setOnClickListener(null) // Limpa listener se não for usado
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

    // CORREÇÃO: Nomenclatura do DiffCallback
    class HumorNoteDiffCallback : DiffUtil.ItemCallback<HumorNote>() {
        override fun areItemsTheSame(oldItem: HumorNote, newItem: HumorNote): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: HumorNote, newItem: HumorNote): Boolean {
            // Compara os campos relevantes para saber se precisa redesenhar
            return oldItem.id == newItem.id &&
                    oldItem.humor == newItem.humor &&
                    oldItem.descricao == newItem.descricao &&
                    oldItem.data == newItem.data
        }
    }
}