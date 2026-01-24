package com.example.apphumor

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.apphumor.databinding.ActivityLoginBinding
import com.example.apphumor.di.DependencyProvider
import com.example.apphumor.viewmodel.AppViewModelFactory
import com.example.apphumor.viewmodel.LoginUiState
import com.example.apphumor.viewmodel.LoginViewModel
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var viewModel: LoginViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val factory = AppViewModelFactory(
            DependencyProvider.auth,
            DependencyProvider.databaseRepository
        )
        viewModel = ViewModelProvider(this, factory)[LoginViewModel::class.java]

        if (DependencyProvider.auth.currentUser != null) {
            goToHome()
        }

        setupListeners()
        setupStateObserver() // Nova função de observação
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

    private fun setupStateObserver() {
        // Usa lifecycleScope + repeatOnLifecycle para coletar Flows de forma segura
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is LoginUiState.Idle -> {
                            binding.btnLogin.isEnabled = true
                            binding.btnLogin.text = getString(R.string.action_login)
                        }
                        is LoginUiState.Loading -> {
                            binding.btnLogin.isEnabled = false
                            binding.btnLogin.text = "Entrando..."
                        }
                        is LoginUiState.Success -> {
                            goToHome()
                        }
                        is LoginUiState.Error -> {
                            binding.btnLogin.isEnabled = true
                            binding.btnLogin.text = getString(R.string.action_login)
                            Toast.makeText(this@LoginActivity, state.message, Toast.LENGTH_LONG).show()
                            // Opcional: Resetar estado após erro
                            viewModel.resetState()
                        }
                    }
                }
            }
        }
    }

    private fun goToHome() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}