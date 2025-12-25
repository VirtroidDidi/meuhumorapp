package com.example.apphumor.viewmodel

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.credentials.CredentialManager
import com.example.apphumor.AppHumorApplication // Importante: Importe sua Application
import com.example.apphumor.CadastroActivity
import com.example.apphumor.MainActivity
import com.example.apphumor.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var viewModel: LoginViewModel
    private lateinit var oneTapClient: CredentialManager

    companion object {
        private const val TAG = "LoginActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d(TAG, "onCreate chamado")

        // 1. Acessa o Container através da Application
        val appContainer = (application as AppHumorApplication).container

        // 2. Pega a instância única do Auth do container
        auth = appContainer.auth

        // 3. Inicializa o ViewModel usando a Factory Global
        val factory = HumorViewModelFactory(appContainer.databaseRepository, appContainer.auth)
        viewModel = ViewModelProvider(this, factory)[LoginViewModel::class.java]

        // Inicializar oneTapClient
        oneTapClient = CredentialManager.create(this)

        // Verificar se o usuário já está logado
        if (auth.currentUser != null) {
            Log.i(TAG, "Usuário já logado, indo para MainActivity")
            goToMainActivity()
        }

        setupListeners()
        setupObservers()
    }

    private fun setupListeners() {
        binding.textCadastro.setOnClickListener {
            startActivity(Intent(this, CadastroActivity::class.java))
        }

        binding.btnLogin.setOnClickListener {
            val email = binding.editLoginEmail.text.toString().trim()
            val senha = binding.editLoginSenha.text.toString()
            viewModel.loginUser(email, senha)
        }
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(this) { isLoading ->
            binding.btnLogin.isEnabled = !isLoading
        }

        viewModel.errorMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.clearErrorMessage()
            }
        }

        viewModel.loginSuccess.observe(this) { isSuccess ->
            if (isSuccess) {
                Toast.makeText(baseContext, "Login realizado com sucesso.", Toast.LENGTH_SHORT).show()
                goToMainActivity()
            }
        }
    }

    private fun goToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}