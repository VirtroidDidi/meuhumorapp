package com.example.apphumor.viewmodel

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.credentials.CredentialManager
import com.example.apphumor.CadastroActivity
import com.example.apphumor.MainActivity
import com.example.apphumor.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth

/**
 * [LoginViewModel]
 * Gerencia a lógica de autenticação do usuário, expondo o estado de carregamento,
 * sucesso de login e mensagens de erro via LiveData.
 */

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var viewModel: LoginViewModel
    private lateinit var oneTapClient: CredentialManager

    // Nomenclatura Padrão: Constante de Log no Companion Object
    companion object {
        private const val TAG = "LoginActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d(TAG, "onCreate chamado")

        // Initialize Firebase Auth (Apenas para o check de sessão)
        auth = FirebaseAuth.getInstance()

        // Initialize ViewModel
        viewModel = ViewModelProvider(this).get(LoginViewModel::class.java)

        // Inicializar oneTapClient
        oneTapClient = CredentialManager.create(this)

        // Verificar se o usuário já está logado
        if (auth.currentUser != null) {
            Log.i(TAG, "Usuário já logado, indo para MainActivity")
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        setupListeners()
        setupObservers()
    }

    private fun setupListeners() {
        // Navegação para o cadastro
        binding.textCadastro.setOnClickListener {
            val intent = Intent(this, CadastroActivity::class.java)
            startActivity(intent)
        }

        // Ação do botão de login
        binding.btnLogin.setOnClickListener {
            val email = binding.editLoginEmail.text.toString().trim()
            val senha = binding.editLoginSenha.text.toString()

            // Delega a lógica de login para o ViewModel
            viewModel.loginUser(email, senha)
        }
    }

    private fun setupObservers() {
        // Controla o estado de carregamento do botão
        viewModel.isLoading.observe(this) { isLoading ->
            binding.btnLogin.isEnabled = !isLoading
        }

        // Trata mensagens de erro
        viewModel.errorMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                // Chama o método do ViewModel para limpar o estado de erro
                viewModel.clearErrorMessage()
            }
        }

        // Reage ao sucesso do login para navegar
        viewModel.loginSuccess.observe(this) { isSuccess ->
            if (isSuccess) {
                Toast.makeText(baseContext, "Login realizado com sucesso.", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }
}