package com.example.apphumor.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.apphumor.models.HumorNote
import com.example.apphumor.models.User
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.tasks.await

// AGORA RECEBE O BANCO NO CONSTRUTOR (INJEÇÃO DE DEPENDÊNCIA)
class DatabaseRepository(private val database: FirebaseDatabase) {

    // Usa a instância recebida, não cria uma nova estática
    private val db = database.getReference("users")
    private val TAG = "DatabaseRepository"

    sealed class Result<out T> {
        data class Success<out T>(val data: T) : Result<T>()
        data class Error(val exception: Exception) : Result<Nothing>()
    }

    // --- LEITURA INTELIGENTE ---
    fun getHumorNotesAsLiveData(userId: String): LiveData<List<HumorNote>> {
        val liveData = MutableLiveData<List<HumorNote>>()

        db.child(userId).child("notes").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val notes = snapshot.children.mapNotNull { child ->
                    val rawNote = child.getValue(HumorNote::class.java)

                    rawNote?.let { note ->
                        val realSyncStatus = if (child.hasChild("isSynced")) {
                            note.isSynced
                        } else {
                            true
                        }

                        var correctTimestamp = note.timestamp
                        if (correctTimestamp == 0L) {
                            val legacyTime = note.data?.get("time") as? Long
                            if (legacyTime != null) correctTimestamp = legacyTime
                        }

                        note.copy(
                            id = child.key,
                            timestamp = correctTimestamp,
                            isSynced = realSyncStatus
                        )
                    }
                }
                liveData.postValue(notes.sortedByDescending { it.timestamp })
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Erro de leitura: ${error.message}")
                liveData.postValue(emptyList())
            }
        })
        return liveData
    }

    // --- ESCRITA OFFLINE-FIRST ---
    suspend fun saveHumorNote(userId: String, note: HumorNote): Result<String> {
        return try {
            val noteRef = db.child(userId).child("notes").push()
            val newId = noteRef.key ?: throw Exception("Falha ao gerar chave.")

            // Salva como FALSE (Pendente)
            val noteToSave = note.copy(id = newId, userId = userId, isSynced = false)

            noteRef.setValue(noteToSave).addOnSuccessListener {
                // Callback do Servidor: Atualiza para TRUE
                noteRef.child("isSynced").setValue(true)
            }

            Result.Success(newId)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao salvar: ${e.message}")
            Result.Error(e)
        }
    }

    suspend fun updateHumorNote(userId: String, note: HumorNote): Result<Unit> {
        return try {
            val noteId = note.id ?: throw Exception("ID nulo.")

            val notePending = note.copy(isSynced = false)
            val ref = db.child(userId).child("notes").child(noteId)

            ref.setValue(notePending).addOnSuccessListener {
                ref.child("isSynced").setValue(true)
            }

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    // --- Outros Métodos (User, OneShot) mantidos ---
    suspend fun saveUser(user: User): Result<Unit> {
        val uid = user.uid ?: return Result.Error(Exception("UID nulo"))
        return try {
            db.child(uid).setValue(user)
            Result.Success(Unit)
        } catch (e: Exception) { Result.Error(e) }
    }

    suspend fun updateUser(user: User): Result<Unit> {
        val uid = user.uid ?: return Result.Error(IllegalArgumentException("UID nulo."))
        return try {
            val updates = mapOf<String, Any?>(
                "nome" to user.nome,
                "idade" to user.idade,
                "email" to user.email,
                "notificacaoAtiva" to user.notificacaoAtiva,
                "horarioNotificacao" to user.horarioNotificacao,
                "fotoBase64" to user.fotoBase64
            )
            db.child(uid).updateChildren(updates)
            Result.Success(Unit)
        } catch (e: Exception) { Result.Error(e) }
    }

    suspend fun getUser(userId: String): Result<User?> {
        return try {
            val snapshot = db.child(userId).get().await()
            val user = snapshot.getValue(User::class.java)
            Result.Success(user?.copy(uid = snapshot.key))
        } catch (e: Exception) { Result.Error(e) }
    }

    suspend fun getHumorNotesOnce(userId: String): List<HumorNote> {
        return try {
            val snapshot = db.child(userId).child("notes").get().await()
            snapshot.children.mapNotNull { child ->
                val rawNote = child.getValue(HumorNote::class.java)
                rawNote?.copy(id = child.key)
            }
        } catch (e: Exception) { emptyList() }
    }
}