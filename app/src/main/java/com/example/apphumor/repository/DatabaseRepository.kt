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

class DatabaseRepository {
    // Referência raiz do banco de dados
    private val db = FirebaseDatabase.getInstance().getReference("users")
    private val TAG = "DatabaseRepository"

    // --- Modelos de Resposta ---
    sealed class Result<out T> {
        data class Success<out T>(val data: T) : Result<T>()
        data class Error(val exception: Exception) : Result<Nothing>()
    }

    // --- Notas de Humor (Escrita com Coroutines) ---

    /**
     * Atualiza uma nota existente.
     * Como o objeto é imutável, sobrescrevemos todo o nó com os novos valores.
     */
    suspend fun updateHumorNote(userId: String, note: HumorNote): Result<Unit> {
        return try {
            if (note.id == null) throw Exception("ID da nota é nulo na atualização.")

            db.child(userId).child("notes").child(note.id)
                .setValue(note)
                .await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao atualizar nota: ${e.message}")
            Result.Error(e)
        }
    }

    /**
     * Salva uma nova nota.
     * Gera um ID automático e cria uma cópia imutável da nota com esse ID.
     */
    suspend fun saveHumorNote(userId: String, note: HumorNote): Result<String> {
        return try {
            val noteRef = db.child(userId).child("notes").push()
            val newId = noteRef.key ?: throw Exception("Falha ao gerar chave no Firebase.")

            // Cria uma nova instância imutável vinculando o ID e UserID
            val noteWithId = note.copy(id = newId, userId = userId)

            noteRef.setValue(noteWithId).await()
            Result.Success(newId)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao salvar nota: ${e.message}")
            Result.Error(e)
        }
    }

    // --- Leitura em Tempo Real (LiveData) ---

    fun getHumorNotesAsLiveData(userId: String): LiveData<List<HumorNote>> {
        val liveData = MutableLiveData<List<HumorNote>>()

        db.child(userId).child("notes").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val notes = snapshot.children.mapNotNull { child ->
                    // O Firebase usa o construtor padrão (valores default) para instanciar
                    val rawNote = child.getValue(HumorNote::class.java)

                    rawNote?.let { note ->
                        // --- LÓGICA DE MIGRAÇÃO DE DADOS ---
                        var correctTimestamp = note.timestamp

                        // Se timestamp for 0, significa que é um registro legado (antigo)
                        if (correctTimestamp == 0L) {
                            val legacyTime = note.data?.get("time") as? Long
                            if (legacyTime != null) {
                                correctTimestamp = legacyTime
                                // Log opcional para monitorar a migração
                                Log.i(TAG, "Migrando nota legada ${child.key} para timestamp: $correctTimestamp")
                            }
                        }

                        // Retorna uma cópia segura com ID e Timestamp garantidos
                        note.copy(id = child.key, timestamp = correctTimestamp)
                    }
                }
                // Ordena: Mais recentes no topo
                liveData.postValue(notes.sortedByDescending { it.timestamp })
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Erro de leitura no Firebase: ${error.message}")
                liveData.postValue(emptyList())
            }
        })

        return liveData
    }

    // --- Gestão de Usuário (Coroutines) ---

    suspend fun saveUser(user: User): Result<Unit> {
        if (user.uid == null) return Result.Error(Exception("UID do usuário é nulo"))

        return try {
            db.child(user.uid).setValue(user).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao salvar usuário: ${e.message}")
            Result.Error(e)
        }
    }

    suspend fun updateUser(user: User): Result<Unit> {
        if (user.uid == null) return Result.Error(IllegalArgumentException("UID nulo."))

        return try {
            // Mapeamento manual para updateChildren (necessário pois user é imutável e queremos atualizar parcial)
            val updates = mapOf<String, Any?>(
                "nome" to user.nome,
                "idade" to user.idade,
                "email" to user.email
            )
            db.child(user.uid).updateChildren(updates).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao atualizar usuário: ${e.message}")
            Result.Error(e)
        }
    }

    suspend fun getUser(userId: String): Result<User?> {
        return try {
            val snapshot = db.child(userId).get().await()
            val user = snapshot.getValue(User::class.java)
            // O ID vem da chave do snapshot, garantimos que ele esteja no objeto
            Result.Success(user?.copy(uid = snapshot.key))
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao buscar usuário: ${e.message}")
            Result.Error(e)
        }
    }
}