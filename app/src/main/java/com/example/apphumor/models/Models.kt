// ARQUIVO: app/src/main/java/com/example/apphumor/models/Models.kt

package com.example.apphumor.models

import android.os.Parcelable
import com.google.firebase.firestore.DocumentId
import com.google.firebase.database.Exclude // Importação necessária para evitar duplicação no DB
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

/**
 * Classe de modelo para armazenar informações do usuário.
 * Imutável (val).
 */
data class User(
    @DocumentId
    @get:Exclude // Não salva o UID dentro do objeto JSON, pois já é a chave do nó
    val uid: String? = null,

    val nome: String? = null,
    val email: String? = null,
    val idade: Int? = null,

    val notificacaoAtiva: Boolean = true, // Padrão ativado
    val horarioNotificacao: String = "20:00" // Padrão 20h
)

/**
 * Classe de modelo para uma Nota de Humor.
 * Totalmente imutável (val).
 */
@Parcelize
data class HumorNote(
    @DocumentId
    @get:Exclude // O ID é a chave do nó, não precisa ser gravado dentro do JSON
    val id: String? = null,

    val userId: String? = null,
    val humor: String? = null,
    val descricao: String? = null,

    // Timestamp é a fonte da verdade agora. Padrão 0L ajuda na migração.
    val timestamp: Long = 0L,

    // Campo legado: Usado APENAS para ler dados antigos.
    // @RawValue permite que o Parcelize ignore o tipo genérico Any.
    val data: Map<String, @RawValue Any>? = null
) : Parcelable