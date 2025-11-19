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
    private val db = FirebaseDatabase.getInstance().getReference("users")
    private val TAG = "DatabaseRepository"

    // --- Modelos de Resposta ---
    sealed class Result<out T> {
        data class Success<out T>(val data: T) : Result<T>()
        data class Error(val exception: Exception) : Result<Nothing>()
    }

    // --- Notas de Humor (Coroutines) ---

    suspend fun updateHumorNote(userId: String, note: HumorNote): Result<Unit> {
        return try {
            db.child(userId).child("notes").child(note.id!!)
                .setValue(note)
                .await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao atualizar nota: ${e.stackTraceToString()}")
            Result.Error(e)
        }
    }

    suspend fun saveHumorNote(userId: String, note: HumorNote): Result<String> {
        return try {
            val noteRef = db.child(userId).child("notes").push()
            note.id = noteRef.key
            noteRef.setValue(note).await()
            Result.Success(noteRef.key!!)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao salvar nota: ${e.stackTraceToString()}")
            Result.Error(e)
        }
    }

    // --- Notas de Humor (LiveData - Mantido para Realtime) ---

    fun getHumorNotesAsLiveData(userId: String): LiveData<List<HumorNote>> {
        val liveData = MutableLiveData<List<HumorNote>>()

        db.child(userId).child("notes").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val notes = snapshot.children.mapNotNull {
                    it.getValue(HumorNote::class.java)?.copy(id = it.key)
                }
                liveData.postValue(notes)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Erro ao buscar notas via LiveData: ${error.message}")
                liveData.postValue(emptyList())
            }
        })

        return liveData
    }

    // --- Usuário (Coroutines) ---

    suspend fun saveUser(user: User): Result<Unit> {
        if (user.uid == null) return Result.Error(Exception("UID nulo"))

        return try {
            db.child(user.uid!!).setValue(user).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao salvar usuário: ${e.message}")
            Result.Error(e)
        }
    }

    suspend fun updateUser(user: User): Result<Unit> {
        if (user.uid == null) {
            return Result.Error(IllegalArgumentException("ID do usuário é nulo."))
        }

        return try {
            val updates = mapOf<String, Any?>(
                "nome" to user.nome,
                "idade" to user.idade,
                "email" to user.email
            )
            db.child(user.uid!!).updateChildren(updates).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao atualizar usuário:", e)
            Result.Error(e)
        }
    }

    suspend fun getUser(userId: String): Result<User?> {
        return try {
            val snapshot = db.child(userId).get().await()
            val user = snapshot.getValue(User::class.java)
            Result.Success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao buscar usuário:", e)
            Result.Error(e)
        }
    }
}