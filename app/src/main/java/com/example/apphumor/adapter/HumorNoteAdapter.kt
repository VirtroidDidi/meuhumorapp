package com.example.apphumor.adapter

import android.os.Build.VERSION.SDK_INT
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.load
import coil.request.repeatCount
import com.example.apphumor.R
import com.example.apphumor.databinding.ItemNoteBinding
import com.example.apphumor.models.HumorNote
import com.example.apphumor.models.HumorType // [NOVO] Usamos o Enum agora
import java.text.SimpleDateFormat
import java.util.*

class HumorNoteAdapter(
    private val showEditButton: Boolean = true,
    private val showSyncStatus: Boolean = true,
    private val onEditClick: ((HumorNote) -> Unit)? = null
) : ListAdapter<HumorNote, HumorNoteAdapter.HumorNoteViewHolder>(HumorNoteDiffCallback()) {

    // ID da nota que deve ser animada. Se não for null, essa nota vai girar.
    private var animateTargetId: String? = null

    // Função chamada pelo Fragment para definir quem vai brilhar
    fun triggerAnimation(noteId: String) {
        animateTargetId = noteId
        notifyDataSetChanged()
    }

    inner class HumorNoteViewHolder(private val binding: ItemNoteBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(note: HumorNote) {
            val context = binding.root.context

            // [REFATORAÇÃO] Recupera tudo do Enum (Ícone, Texto, Animação)
            // Resolve legados automaticamente (ex: "Incrível" vira RAD)
            val type = HumorType.fromKey(note.humor)

            with(binding) {
                // --- LÓGICA VISUAL PRINCIPAL ---

                // Verifica: Esta é a nota que acabou de ser criada/editada?
                if (note.id == animateTargetId) {
                    // [MODO ANIMAÇÃO]
                    ivHumorIcon.clearColorFilter()
                    ImageViewCompat.setImageTintList(ivHumorIcon, null)

                    val imageLoader = ImageLoader.Builder(context)
                        .components {
                            if (SDK_INT >= 28) {
                                add(ImageDecoderDecoder.Factory())
                            } else {
                                add(GifDecoder.Factory())
                            }
                        }
                        .build()

                    // Usa o recurso de animação direto do Enum
                    ivHumorIcon.load(type.animRes, imageLoader) {
                        crossfade(true)
                        repeatCount(0) // Toca 1 vez + 0 repetições
                        // Se o GIF falhar, coloca o estático como segurança
                        error(type.iconRes)
                    }

                } else {
                    // [MODO ESTÁTICO PADRÃO]
                    ivHumorIcon.load(type.iconRes) {
                        // Garante que não use o decoder de GIF aqui
                    }
                    ivHumorIcon.clearColorFilter()
                    ImageViewCompat.setImageTintList(ivHumorIcon, null)
                }

                // --- Restante dos dados ---
                btnEdit.visibility = if (showEditButton) View.VISIBLE else View.GONE

                // Texto traduzido vindo do Enum
                tvHumor.text = context.getString(type.labelRes)

                tvDescricao.text = note.descricao.takeIf { !it.isNullOrEmpty() } ?: "Sem descrição"
                tvDescricao.visibility = if (note.descricao.isNullOrEmpty()) View.GONE else View.VISIBLE

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

                // Configuração do clique manual
                ivHumorIcon.setOnClickListener {
                    triggerAnimation(note.id ?: "")
                }

                if (showEditButton && onEditClick != null) {
                    btnEdit.setOnClickListener { onEditClick.invoke(note) }
                } else {
                    btnEdit.setOnClickListener(null)
                }

                if (showSyncStatus) {
                    ivSyncStatus.visibility = View.VISIBLE
                    if (note.isSynced) {
                        ivSyncStatus.setImageResource(R.drawable.ic_status_synced)
                    } else {
                        ivSyncStatus.setImageResource(R.drawable.ic_status_pending)
                    }
                } else {
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