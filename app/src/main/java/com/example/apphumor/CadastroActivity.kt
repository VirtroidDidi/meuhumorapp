package com.example.apphumor

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.apphumor.databinding.ActivityCadastroBinding
import com.example.apphumor.viewmodel.CadastroViewModel
import com.example.apphumor.viewmodel.HumorViewModelFactory
import com.google.firebase.auth.FirebaseAuth

class CadastroActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCadastroBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var viewModel: CadastroViewModel

    companion object {
        private const val TAG = "CadastroActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCadastroBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d(TAG, "Activity iniciada.")

        // 1. Acessa o Container da Aplicação
        val appContainer = (application as AppHumorApplication).container

        // 2. Obtém dependências
        auth = appContainer.auth

        // 3. Inicializa o ViewModel usando a Factory Universal
        val factory = HumorViewModelFactory(appContainer.databaseRepository, appContainer.auth)
        viewModel = ViewModelProvider(this, factory)[CadastroViewModel::class.java]

        setupListeners()
        setupObservers()
    }

    private fun setupListeners() {
        binding.btnCadastro.setOnClickListener {
            // Acessa os campos usando os IDs definidos no XML
            val nome = binding.etNome.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val senha = binding.etSenha.text.toString()
            val idadeStr = binding.etIdade.text.toString()

            if (nome.isBlank() || email.isBlank() || senha.isBlank() || idadeStr.isBlank()) {
                Toast.makeText(baseContext, "Preencha todos os campos.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val idade = idadeStr.toIntOrNull()
            if (idade == null || idade <= 0) {
                Toast.makeText(baseContext, "Idade inválida.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (senha.length < 6) {
                Toast.makeText(baseContext, "A senha deve ter pelo menos 6 caracteres.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.registerUser(nome, email, senha, idade)
        }

        binding.textLogin.setOnClickListener {
            // Volta para a tela de Login
            finish()
        }
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(this) { isLoading ->
            binding.btnCadastro.isEnabled = !isLoading
            binding.btnCadastro.text = if (isLoading) "Cadastrando..." else getString(R.string.action_register)
        }

        viewModel.errorMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.clearErrorMessage()
            }
        }

        viewModel.cadastroSuccess.observe(this) { isSuccess ->
            if (isSuccess) {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }
}