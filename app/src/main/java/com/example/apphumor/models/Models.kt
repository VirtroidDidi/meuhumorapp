package com.example.apphumor.models

import android.os.Parcelable
import com.google.firebase.database.Exclude
import com.google.firebase.database.PropertyName
import com.google.firebase.firestore.DocumentId
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

@Parcelize
data class HumorNote(
    @DocumentId
    @get:Exclude
    val id: String? = null,
    val userId: String? = null,
    val humor: String? = null,
    val descricao: String? = null,
    val timestamp: Long = 0L,
    @get:PropertyName("isSynced")
    val isSynced: Boolean = false,
    val data: Map<String, @RawValue Any>? = null
) : Parcelable

data class User(
    @DocumentId @get:Exclude val uid: String? = null,
    val nome: String? = null,
    val email: String? = null,
    val idade: Int? = null,
    val notificacaoAtiva: Boolean = true,
    val horarioNotificacao: String = "20:00",
    val fotoBase64: String? = null
)