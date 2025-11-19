
package com.example.apphumor.di

import com.example.apphumor.repository.DatabaseRepository
import com.google.firebase.auth.FirebaseAuth
// Importações necessárias para o Firebase (já devem estar na aplicação)
import com.google.firebase.database.FirebaseDatabase

/**
 * Service Locator simples para centralizar a criação e fornecimento
 * de instâncias singleton de dependências (FirebaseAuth, DatabaseRepository).
 * Isso elimina a necessidade de inicializar as dependências em cada ViewModel.
 */
object DependencyProvider {

    // Instância única de FirebaseAuth
    val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    // Instância única de DatabaseRepository
    // NOTA: O DatabaseRepository já inicializa o FirebaseDatabase internamente.
    val databaseRepository: DatabaseRepository by lazy {
        DatabaseRepository()
    }

    // Adicione outros singletons aqui conforme a necessidade
}