package com.example.apphumor.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.apphumor.repository.DatabaseRepository
import com.google.firebase.auth.FirebaseAuth

@Suppress("UNCHECKED_CAST")
class HumorViewModelFactory(
    private val repository: DatabaseRepository,
    private val auth: FirebaseAuth
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            // Home
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> {
                HomeViewModel(auth, repository) as T
            }
            // Adicionar/Editar Humor
            modelClass.isAssignableFrom(AddHumorViewModel::class.java) -> {
                AddHumorViewModel(repository, auth) as T
            }
            // Cadastro
            modelClass.isAssignableFrom(CadastroViewModel::class.java) -> {
                CadastroViewModel(auth, repository) as T
            }
            // Perfil
            modelClass.isAssignableFrom(ProfileViewModel::class.java) -> {
                ProfileViewModel(auth, repository) as T
            }
            // Insights
            modelClass.isAssignableFrom(InsightsViewModel::class.java) -> {
                InsightsViewModel(auth, repository) as T
            }
            // Login (Ainda vamos refatorar este VM para aceitar argumentos, mas já deixamos pronto aqui)
            modelClass.isAssignableFrom(LoginViewModel::class.java) -> {
                LoginViewModel(auth) as T
            }
            else -> throw IllegalArgumentException("ViewModel desconhecido: ${modelClass.name}")
        }
    }
}