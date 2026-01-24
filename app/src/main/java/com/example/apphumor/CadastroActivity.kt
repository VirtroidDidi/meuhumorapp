package com.example.apphumor

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.apphumor.databinding.ActivityCadastroBinding
import com.example.apphumor.di.DependencyProvider
import com.example.apphumor.viewmodel.CadastroUiState
import com.example.apphumor.viewmodel.CadastroViewModel
import com.example.apphumor.viewmodel.CadastroViewModelFactory
import kotlinx.coroutines.launch

class CadastroActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCadastroBinding
    private lateinit var viewModel: CadastroViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCadastroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicialização do ViewModel via Factory
        viewModel = ViewModelProvider(
            this,
            CadastroViewModelFactory(
                DependencyProvider.auth,
                DependencyProvider.databaseRepository
            )
        )[CadastroViewModel::class.java]

        configurarNumberPicker()
        setupListeners()
        setupObservers()
    }

    private fun configurarNumberPicker() {
        binding.numberPickerIdade.minValue = 13
        binding.numberPickerIdade.maxValue = 100
        binding.numberPickerIdade.value = 18
    }

    private fun setupListeners() {
        binding.textLogin.setOnClickListener {
            finish() // Apenas fecha para voltar ao Login (se ele estava na pilha) ou abre Login
        }

        binding.btnCadastro.setOnClickListener {
            // A Activity APENAS coleta os dados. Zero lógica aqui.
            val nome = binding.editCadastroNome.text.toString().trim()
            val email = binding.editCadastroEmail.text.toString().trim()
            val senha = binding.editCadastroSenha.text.toString()
            val confirmarSenha = binding.editCadastroConfirmarSenha.text.toString()
            val idade = binding.numberPickerIdade.value

            // Envia tudo para o ViewModel validar e processar
            viewModel.registerUser(nome, email, senha, confirmarSenha, idade)
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is CadastroUiState.Idle -> {
                            binding.btnCadastro.isEnabled = true
                            binding.btnCadastro.text = getString(R.string.action_register)
                        }
                        is CadastroUiState.Loading -> {
                            binding.btnCadastro.isEnabled = false
                            binding.btnCadastro.text = "Criando conta..."
                        }
                        is CadastroUiState.Success -> {
                            binding.btnCadastro.isEnabled = true
                            Toast.makeText(this@CadastroActivity, "Bem-vindo!", Toast.LENGTH_SHORT).show()

                            // Navega para Home e limpa a pilha para o usuário não voltar para o cadastro
                            val intent = Intent(this@CadastroActivity, MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        }
                        is CadastroUiState.Error -> {
                            binding.btnCadastro.isEnabled = true
                            binding.btnCadastro.text = getString(R.string.action_register)
                            Toast.makeText(this@CadastroActivity, state.message, Toast.LENGTH_LONG).show()
                            // Reseta para não ficar travado no erro se o usuário rotacionar a tela
                            viewModel.resetState()
                        }
                    }
                }
            }
        }
    }
}