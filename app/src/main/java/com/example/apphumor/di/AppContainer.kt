package com.example.apphumor.di

import com.example.apphumor.repository.DatabaseRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

/**
 * Container atualizado: Agora injeta o banco de dados no repositório.
 */
class AppContainer {

    // 1. Instância do FirebaseAuth
    val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    // 2. Instância do Banco de Dados (Dependência do Repositório)
    private val database: FirebaseDatabase by lazy {
        FirebaseDatabase.getInstance()
    }

    // 3. Instância do Repositório (Recebe o banco via construtor)
    val databaseRepository: DatabaseRepository by lazy {
        DatabaseRepository(database)
    }
}