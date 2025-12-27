package com.example.apphumor.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.apphumor.repository.DatabaseRepository
import com.google.firebase.auth.FirebaseAuth

class AppViewModelFactory(
    private val auth: FirebaseAuth,
    private val repository: DatabaseRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // Configuração para o HomeViewModel
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(auth, repository) as T
        }

        // Configuração para o AddHumorViewModel
        if (modelClass.isAssignableFrom(AddHumorViewModel::class.java)) {
            return AddHumorViewModel(repository, auth) as T
        }

        // Se criar novos ViewModels, adicione os 'if' aqui.

        throw IllegalArgumentException("ViewModel desconhecido: ${modelClass.name}")
    }
}