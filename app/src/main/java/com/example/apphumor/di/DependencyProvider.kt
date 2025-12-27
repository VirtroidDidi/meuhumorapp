package com.example.apphumor.di

import com.example.apphumor.repository.DatabaseRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

object DependencyProvider {

    // 1. Fonte de Dados (Database)
    private val firebaseDatabase: FirebaseDatabase by lazy {
        // ATENÇÃO: NÃO COLOQUE .setPersistenceEnabled(true) AQUI!
        // Ela já está no AppHumorApplication.kt
        FirebaseDatabase.getInstance()
    }

    // 2. Autenticação
    val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    // 3. Repositório
    val databaseRepository: DatabaseRepository by lazy {
        DatabaseRepository(firebaseDatabase)
    }
}