package com.example.apphumor.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.apphumor.databinding.ItemNoteBinding
import com.example.apphumor.models.HumorNote
import java.text.SimpleDateFormat
import java.util.*

class NoteAdapter(
    private val onEditClicked: (HumorNote) -> Unit
) : ListAdapter<HumorNote, NoteAdapter.NoteViewHolder>(DiffCallback()) {

    private val TAG = "NoteAdapter"

    inner class NoteViewHolder(private val binding: ItemNoteBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(note: HumorNote) {
            with(binding) {
                // Configuração dos dados
                tvHumor.text = note.humor.takeIf { !it.isNullOrEmpty() } ?: "Sem humor registrado"
                tvDescricao.text = note.descricao.takeIf { !it.isNullOrEmpty() } ?: "Sem descrição"

                // Formatação da data
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
                    tvData.text = "Sem timestamp"
                    Log.w(TAG, "Campo 'time' ausente na nota ${note.id}")
                }

                // Listener do botão de edição
                btnEdit.setOnClickListener {
                    onEditClicked(note)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val binding = ItemNoteBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = getItem(position)
        Log.d(TAG, "Vinculando nota na posição $position: ${note.id}")
        holder.bind(note)
    }

    class DiffCallback : DiffUtil.ItemCallback<HumorNote>() {
        override fun areItemsTheSame(oldItem: HumorNote, newItem: HumorNote): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: HumorNote, newItem: HumorNote): Boolean {
            return oldItem == newItem
        }
    }
}