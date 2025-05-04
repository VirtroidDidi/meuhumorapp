package com.example.apphumor.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.apphumor.models.User
import com.example.apphumor.repository.DatabaseRepository

class CadastroViewModel : ViewModel() {
    private val repository = DatabaseRepository()
    private val TAG = "CadastroViewModel"

    fun saveUser(user: User) {
        Log.d(TAG, "Iniciando saveUser para: ${user.email}")
        repository.saveUser(user)
        Log.d(TAG, "Finalizando saveUser para: ${user.email}")
    }
}