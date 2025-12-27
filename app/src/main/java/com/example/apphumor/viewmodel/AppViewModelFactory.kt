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

        // 1. Home
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(auth, repository) as T
        }

        // 2. Adicionar Humor
        if (modelClass.isAssignableFrom(AddHumorViewModel::class.java)) {
            return AddHumorViewModel(repository, auth) as T
        }

        // 3. Login (A Ovelha Negra agora entra na família!)
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            // Login só precisa de Auth, mas a factory tem os dois. Tudo bem!
            return LoginViewModel(auth) as T
        }

        // 4. Cadastro
        if (modelClass.isAssignableFrom(CadastroViewModel::class.java)) {
            return CadastroViewModel(auth, repository) as T
        }

        // 5. Insights
        if (modelClass.isAssignableFrom(InsightsViewModel::class.java)) {
            return InsightsViewModel(auth, repository) as T
        }

        // 6. Perfil
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            return ProfileViewModel(auth, repository) as T
        }

        throw IllegalArgumentException("ViewModel desconhecido: ${modelClass.name}")
    }
}