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
import com.example.apphumor.utils.HumorUtils
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
        // Atualiza a lista para aplicar a mudança visual
        notifyDataSetChanged()
    }

    inner class HumorNoteViewHolder(private val binding: ItemNoteBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(note: HumorNote) {
            val context = binding.root.context
            val moodStyle = HumorUtils.getMoodStyle(note.humor)

            with(binding) {
                // --- LÓGICA VISUAL PRINCIPAL ---

                // Verifica: Esta é a nota que acabou de ser criada/editada?
                if (note.id == animateTargetId) {
                    // [MODO ANIMAÇÃO]
                    // 1. Limpa qualquer filtro de cor para o GIF aparecer bonito
                    ivHumorIcon.clearColorFilter()
                    ImageViewCompat.setImageTintList(ivHumorIcon, null)

                    // 2. Prepara o Loader de GIF
                    val imageLoader = ImageLoader.Builder(context)
                        .components {
                            if (SDK_INT >= 28) {
                                add(ImageDecoderDecoder.Factory())
                            } else {
                                add(GifDecoder.Factory())
                            }
                        }
                        .build()

                    // 3. Carrega o GIF Animado
                    val animRes = getMoodAnimResource(note.humor ?: "NEUTRAL")
                    ivHumorIcon.load(animRes, imageLoader) {
                        crossfade(true)
                        repeatCount(0) // Toca 1 vez + 1 repetição (2 ciclos) e para

                        // Se o GIF falhar, coloca o estático como segurança
                        error(moodStyle.iconRes)
                    }

                    // IMPORTANTE: Não limpamos o animateTargetId aqui.
                    // Deixamos ele ativo para garantir que a animação não seja cortada
                    // se a lista atualizar sozinha.

                } else {
                    // [MODO ESTÁTICO PADRÃO]
                    // Carrega a imagem estática (SVG/PNG) imediatamente
                    ivHumorIcon.load(moodStyle.iconRes) {
                        // Garante que não use o decoder de GIF aqui para economizar memória
                    }
                    ivHumorIcon.clearColorFilter()
                    ImageViewCompat.setImageTintList(ivHumorIcon, null)
                }

                // --- Restante dos dados (Texto, Data, etc) ---
                btnEdit.visibility = if (showEditButton) View.VISIBLE else View.GONE
                tvHumor.text = context.getString(moodStyle.labelRes)

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

                // Configuração do clique manual (Sempre permite animar ao clicar)
                ivHumorIcon.setOnClickListener {
                    // Ao clicar, forçamos a animação pontual deste item
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

        private fun getMoodAnimResource(mood: String): Int {
            return when (mood.uppercase()) {
                "RAD" -> R.drawable.ic_mood_rad_anim
                "HAPPY" -> R.drawable.ic_mood_happy_anim
                "GRATEFUL" -> R.drawable.ic_mood_grateful_anim
                "CALM" -> R.drawable.ic_mood_calm_anim
                "NEUTRAL" -> R.drawable.ic_mood_neutral_anim
                "PENSIVE" -> R.drawable.ic_mood_pensive_anim
                "ANXIOUS" -> R.drawable.ic_mood_anxious_anim
                "SAD" -> R.drawable.ic_mood_sad_anim
                "ANGRY" -> R.drawable.ic_mood_angry_anim
                "TIRED" -> R.drawable.ic_mood_tired_anim
                else -> R.drawable.ic_mood_neutral_anim
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