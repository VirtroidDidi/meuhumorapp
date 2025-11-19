package com.example.apphumor.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData // NOVO
import com.example.apphumor.models.HumorNote
import com.example.apphumor.models.User
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class DatabaseRepository {
    private val db = FirebaseDatabase.getInstance().getReference("users")
    private val TAG = "DatabaseRepository"

    fun updateHumorNote(userId: String, note: HumorNote, onSuccess: () -> Unit, onError: (String) -> Unit) {
        db.child(userId).child("notes").child(note.id!!)
            .setValue(note)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener {
                onError(it.message ?: "Erro ao atualizar nota")
                Log.e(TAG, "Erro ao atualizar nota: ${it.stackTraceToString()}")
            }
    }

    fun saveHumorNote(
        userId: String,
        note: HumorNote,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val noteRef = db.child(userId).child("notes").push()
        note.id = noteRef.key
        noteRef.setValue(note)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e ->
                onError(e.message ?: "Erro ao salvar nota")
                Log.e(TAG, "Erro ao salvar nota: ${e.stackTraceToString()}")
            }
    }

    /**
     * MANTIDO: Método antigo com callback (usado por AddHumorViewModel e outros).
     */
    fun getHumorNotes(userId: String, callback: (List<HumorNote>) -> Unit) {
        // ... (código existente)
        db.child(userId).child("notes").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val notes = snapshot.children.mapNotNull {
                    it.getValue(HumorNote::class.java)?.copy(id = it.key)
                }
                callback(notes)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Erro ao buscar notas: ${error.message}")
                callback(emptyList())
            }
        })
    }

    /**
     * NOVO: Método para retornar notas como LiveData.
     * Isso permite que os ViewModels observem o banco de dados em tempo real.
     */
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


    fun saveUser(user: User) {
        db.child(user.uid!!).setValue(user)
            .addOnFailureListener { e ->
                Log.e(TAG, "Erro ao salvar usuário: ${e.message}")
            }
    }

    /**
     * Atualiza o perfil do usuário no Firebase Realtime Database
     * usando updateChildren para PRESERVAR a coleção 'notes'.
     */
    fun updateUser(user: User, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (user.uid == null) {
            onError("ID do usuário é nulo. Não foi possível atualizar.")
            return
        }

        val updates = mutableMapOf<String, Any?>()
        updates["nome"] = user.nome
        updates["idade"] = user.idade
        updates["email"] = user.email

        db.child(user.uid!!).updateChildren(updates)
            .addOnSuccessListener {
                Log.d(TAG, "Usuário ${user.uid} atualizado com sucesso (updateChildren).")
                onSuccess()
            }
            .addOnFailureListener { e ->
                val errorMessage = e.message ?: "Erro desconhecido ao atualizar usuário"
                onError(errorMessage)
                Log.e(TAG, "Erro ao atualizar usuário: $errorMessage", e)
            }
    }

    fun getUser(userId: String, callback: (User?) -> Unit) {
        db.child(userId).get().addOnSuccessListener { snapshot ->
            val user = snapshot.getValue(User::class.java)
            callback(user)
        }.addOnFailureListener { exception ->
            Log.e(TAG, "Erro ao buscar usuário:", exception)
            callback(null)
        }
    }
}