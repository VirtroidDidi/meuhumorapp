package com.example.apphumor.adapter

import android.util.Log
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

class AllNotesAdapter(
    private val showEditButton: Boolean = false // Novo parâmetro
) : ListAdapter<HumorNote, AllNotesAdapter.AllNotesViewHolder>(DiffCallback()) {

    private val TAG = "AllNotesAdapter"

    inner class AllNotesViewHolder(private val binding: ItemNoteBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(note: HumorNote) {
            with(binding) {
                // Controle de visibilidade do botão
                btnEdit.visibility = if (showEditButton) View.VISIBLE else View.GONE

                // Restante do código permanece igual
                tvHumor.text = note.humor ?: "Sem humor"
                tvDescricao.text = note.descricao ?: "Sem descrição"

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
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AllNotesViewHolder {
        val binding = ItemNoteBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AllNotesViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AllNotesViewHolder, position: Int) {
        holder.bind(getItem(position))
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