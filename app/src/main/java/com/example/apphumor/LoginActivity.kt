package com.example.apphumor

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.apphumor.databinding.ActivityLoginBinding
import com.example.apphumor.di.DependencyProvider
import com.example.apphumor.viewmodel.AppViewModelFactory
import com.example.apphumor.viewmodel.LoginViewModel

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var viewModel: LoginViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Injeção de Dependência Manual
        val factory = AppViewModelFactory(
            DependencyProvider.auth,
            DependencyProvider.databaseRepository
        )
        viewModel = ViewModelProvider(this, factory)[LoginViewModel::class.java]

        // 2. Verificar se já está logado
        if (DependencyProvider.auth.currentUser != null) {
            goToHome()
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
            binding.btnLogin.text = if (isLoading) "Entrando..." else getString(R.string.action_login)
        }

        viewModel.errorMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.clearErrorMessage()
            }
        }

        viewModel.loginSuccess.observe(this) { isSuccess ->
            if (isSuccess) {
                goToHome()
            }
        }
    }

    private fun goToHome() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}