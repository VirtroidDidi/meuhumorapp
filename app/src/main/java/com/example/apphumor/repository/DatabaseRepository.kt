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
                val notes = snapshot.children.mapNotNull { dataSnapshot ->
                    // CORREÇÃO: Mapeamos o objeto e garantimos que o ID da chave
                    // do Firebase Realtime Database (RTDB) seja usado como o ID do objeto.
                    val note = dataSnapshot.getValue(HumorNote::class.java)
                    val noteId = dataSnapshot.key

                    if (note != null && noteId != null) {
                        note.id = noteId // Atribui a chave do RTDB ao campo 'id' da nota
                        note // Retorna a nota com o ID corrigido
                    } else {
                        Log.w(TAG, "Falha ao desserializar nota em: ${dataSnapshot.key}")
                        null
                    }
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

    /**
     * Atualiza o perfil do usuário no Firebase Realtime Database.
     * @param user O objeto User com os dados atualizados.
     * @param onSuccess Callback para sucesso.
     * @param onError Callback para erro.
     */
    fun updateUser(user: User, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (user.uid == null) {
            onError("ID do usuário é nulo. Não foi possível atualizar.")
            return
        }

        // Usa o setValue para substituir/atualizar todos os campos
        db.child(user.uid!!).setValue(user)
            .addOnSuccessListener {
                Log.d(TAG, "Usuário ${user.uid} atualizado com sucesso.")
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
