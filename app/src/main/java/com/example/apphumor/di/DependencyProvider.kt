package com.example.apphumor.di

import com.example.apphumor.repository.DatabaseRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

/**
 * Service Locator (Container Manual).
 * Centraliza a criação de objetos complexos.
 */
object DependencyProvider {

    // 1. Fonte de Dados (Database)
    // Instanciamos o FirebaseDatabase aqui.
    private val firebaseDatabase: FirebaseDatabase by lazy {
        FirebaseDatabase.getInstance()
    }

    // 2. Autenticação
    val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    // 3. Repositório
    // Injetamos o firebaseDatabase dentro dele!
    val databaseRepository: DatabaseRepository by lazy {
        DatabaseRepository(firebaseDatabase)
    }
}