package com.example.apphumor.repository

import android.util.Log
import com.example.apphumor.models.HumorNote
import com.example.apphumor.models.User
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class DatabaseRepository {
    fun updateHumorNote(userId: String, note: HumorNote, onSuccess: () -> Unit, onError: (String) -> Unit) {
        db.child(userId).child("notes").child(note.id!!)
            .setValue(note)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener {
                onError(it.message ?: "Erro ao atualizar nota")
                Log.e(TAG, "Erro ao atualizar: ${it.stackTraceToString()}")
            }
    }

    private val db = FirebaseDatabase.getInstance().getReference("users")
    private val TAG = "DatabaseRepository"

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
                onError(e.message ?: "Erro ao salvar")
                Log.e(TAG, "Erro: ${e.stackTraceToString()}")
            }
    }

    fun getHumorNotes(userId: String, callback: (List<HumorNote>) -> Unit) {
        db.child(userId).child("notes").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val notes = snapshot.children.mapNotNull {
                    it.getValue(HumorNote::class.java)?.copy(id = it.key)
                }
                callback(notes)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Erro: ${error.message}")
                callback(emptyList())
            }
        })
    }

    fun saveUser(user: User) {
        db.child(user.uid!!).setValue(user)
            .addOnFailureListener { e ->
                Log.e(TAG, "Erro ao salvar usuário: ${e.message}")
            }
    }

    fun getUser(userId: String, callback: (User?) -> Unit) {
        // Corrigido: usar a referência 'db' em vez de 'usersRef'
        db.child(userId).get().addOnSuccessListener { snapshot ->
            val user = snapshot.getValue(User::class.java)
            callback(user)
        }.addOnFailureListener { exception ->
            Log.e(TAG, "Erro ao buscar usuário:", exception)
            callback(null)
        }
    }
}