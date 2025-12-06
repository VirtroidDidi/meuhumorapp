package com.example.apphumor

import android.app.Application
import com.google.firebase.database.FirebaseDatabase

class AppHumorApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // ATIVA A PERSISTÊNCIA OFFLINE
        // Isso permite que o app funcione sem internet e sincronize quando voltar.
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
    }
}