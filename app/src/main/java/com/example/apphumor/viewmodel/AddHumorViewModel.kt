package com.example.apphumor.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.apphumor.models.HumorNote
import com.example.apphumor.models.User
import com.example.apphumor.repository.DatabaseRepository


class AddHumorViewModel : ViewModel() {
    fun updateHumorNote(
        userId: String,
        note: HumorNote,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        repository.updateHumorNote(userId, note, onSuccess, onError)
    }

    private val repository = DatabaseRepository()
    private val TAG = "AddHumorViewModel"


    fun saveHumorNote(
        userId: String,
        note: HumorNote,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        repository.saveHumorNote(userId, note, onSuccess, onError)
    }

    fun getHumorNotes(userId: String, onNotesRetrieved: (List<HumorNote>) -> Unit) {
        repository.getHumorNotes(userId) { notes ->
            Log.d(TAG, "Notas recuperadas: ${notes.size}")
            notes.forEach { note ->
                val timestamp = (note.data as? Map<String, Any>)?.get("time")
                Log.d(TAG, "Nota: $timestamp")
            }
            onNotesRetrieved(notes)
        }
    }


    fun getUser(userId: String, onUserRetrieved: (User?) -> Unit) {
        repository.getUser(userId, onUserRetrieved)
    }
}