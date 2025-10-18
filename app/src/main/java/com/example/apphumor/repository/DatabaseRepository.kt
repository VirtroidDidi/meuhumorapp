package com.example.apphumor.repository

import android.util.Log
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

    fun getHumorNotes(userId: String, callback: (List<HumorNote>) -> Unit) {
        db.child(userId).child("notes").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val notes = snapshot.children.mapNotNull {
                    // Garante que o ID da chave do nó é atribuído ao objeto
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

    fun saveUser(user: User) {
        db.child(user.uid!!).setValue(user)
            .addOnFailureListener { e ->
                Log.e(TAG, "Erro ao salvar usuário: ${e.message}")
            }
    }

    /**
     * CORREÇÃO CRÍTICA: Atualiza o perfil do usuário no Firebase Realtime Database
     * usando updateChildren para PRESERVAR a coleção 'notes'.
     * @param user O objeto User com os dados atualizados.
     * @param onSuccess Callback para sucesso.
     * @param onError Callback para erro.
     */
    fun updateUser(user: User, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (user.uid == null) {
            onError("ID do usuário é nulo. Não foi possível atualizar.")
            return
        }

        // Criamos um mapa apenas com os campos que queremos ATUALIZAR.
        // Isso evita que o setValue apague sub-nós como "notes".
        val updates = mutableMapOf<String, Any?>()
        updates["nome"] = user.nome
        updates["idade"] = user.idade
        // O email não é atualizado, pois é uma chave de autenticação, mas o colocamos
        // aqui para garantir que ele esteja no nó principal.
        updates["email"] = user.email

        // Usa updateChildren para mesclar os novos dados com os existentes.
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
            // A desserialização funciona automaticamente com o get()
            val user = snapshot.getValue(User::class.java)
            callback(user)
        }.addOnFailureListener { exception ->
            Log.e(TAG, "Erro ao buscar usuário:", exception)
            callback(null)
        }
    }
}
