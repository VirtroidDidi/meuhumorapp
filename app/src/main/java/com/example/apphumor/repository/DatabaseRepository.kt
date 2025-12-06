// ARQUIVO: app/src/main/java/com/example/apphumor/repository/DatabaseRepository.kt

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
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException

class DatabaseRepository {
    // Referência raiz do banco de dados
    private val db = FirebaseDatabase.getInstance().getReference("users")
    private val TAG = "DatabaseRepository"

    // --- Modelos de Resposta ---
    sealed class Result<out T> {
        data class Success<out T>(val data: T) : Result<T>()
        data class Error(val exception: Exception) : Result<Nothing>()
    }

    // --- Notas de Humor (COM PROTEÇÃO OFFLINE) ---

    suspend fun updateHumorNote(userId: String, note: HumorNote): Result<Unit> {
        return try {
            // CORREÇÃO DO ERRO SMART CAST:
            // Jogamos o valor para uma variável local 'noteId'.
            // O operador ?: lança a exceção se for nulo.
            val noteId = note.id ?: throw Exception("ID da nota é nulo na atualização.")

            try {
                withTimeout(2000L) {
                    // Agora usamos 'noteId' (variável local) em vez de 'note.id'
                    db.child(userId).child("notes").child(noteId)
                        .setValue(note)
                        .await()
                }
            } catch (e: TimeoutCancellationException) {
                Log.w(TAG, "Timeout no update (Offline): Seguindo com dados locais.")
            }

            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Erro crítico ao atualizar nota: ${e.message}")
            Result.Error(e)
        }
    }

    suspend fun saveHumorNote(userId: String, note: HumorNote): Result<String> {
        return try {
            val noteRef = db.child(userId).child("notes").push()
            val newId = noteRef.key ?: throw Exception("Falha ao gerar chave no Firebase.")

            val noteWithId = note.copy(id = newId, userId = userId)

            try {
                withTimeout(2000L) {
                    noteRef.setValue(noteWithId).await()
                }
            } catch (e: TimeoutCancellationException) {
                Log.w(TAG, "Timeout no save (Offline): Seguindo com dados locais.")
            }

            Result.Success(newId)
        } catch (e: Exception) {
            Log.e(TAG, "Erro crítico ao salvar nota: ${e.message}")
            Result.Error(e)
        }
    }

    // --- Leitura em Tempo Real ---

    fun getHumorNotesAsLiveData(userId: String): LiveData<List<HumorNote>> {
        val liveData = MutableLiveData<List<HumorNote>>()

        db.child(userId).child("notes").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val notes = snapshot.children.mapNotNull { child ->
                    val rawNote = child.getValue(HumorNote::class.java)
                    rawNote?.let { note ->
                        var correctTimestamp = note.timestamp
                        if (correctTimestamp == 0L) {
                            val legacyTime = note.data?.get("time") as? Long
                            if (legacyTime != null) correctTimestamp = legacyTime
                        }
                        note.copy(id = child.key, timestamp = correctTimestamp)
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

    // --- Gestão de Usuário (CORRIGIDO SMART CAST TAMBÉM) ---

    suspend fun saveUser(user: User): Result<Unit> {
        // CORREÇÃO: Variável local segura 'uid'
        val uid = user.uid ?: return Result.Error(Exception("UID do usuário é nulo"))

        return try {
            // Usando a variável local 'uid'
            db.child(uid).setValue(user).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun updateUser(user: User): Result<Unit> {
        val uid = user.uid ?: return Result.Error(IllegalArgumentException("UID nulo."))

        return try {
            val updates = mapOf<String, Any?>(
                "nome" to user.nome,
                "idade" to user.idade,
                "email" to user.email,
                // --- NOVOS CAMPOS ADICIONADOS ---
                "notificacaoAtiva" to user.notificacaoAtiva,
                "horarioNotificacao" to user.horarioNotificacao
            )

            db.child(uid).updateChildren(updates).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun getUser(userId: String): Result<User?> {
        return try {
            val snapshot = db.child(userId).get().await()
            val user = snapshot.getValue(User::class.java)
            Result.Success(user?.copy(uid = snapshot.key))
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    // Adicionar dentro da classe DatabaseRepository

    suspend fun getHumorNotesOnce(userId: String): List<HumorNote> {
        return try {
            // .get() faz uma leitura única (One-Shot)
            val snapshot = db.child(userId).child("notes").get().await()

            snapshot.children.mapNotNull { child ->
                val rawNote = child.getValue(HumorNote::class.java)
                rawNote?.copy(id = child.key) // Garante que o ID venha junto
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro na leitura one-shot: ${e.message}")
            emptyList()
        }
    }
}