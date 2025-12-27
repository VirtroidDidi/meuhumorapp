// ARQUIVO: app/src/main/java/com/example/apphumor/CadastroActivity.kt

package com.example.apphumor

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.example.apphumor.databinding.ActivityCadastroBinding
import com.example.apphumor.di.DependencyProvider
import com.example.apphumor.viewmodel.CadastroViewModel
import com.example.apphumor.viewmodel.CadastroViewModelFactory // NOVO: Import da Factory
import com.google.firebase.auth.FirebaseAuth

class CadastroActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCadastroBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var viewModel: CadastroViewModel

    // Nomenclatura Padrão: Constante de Log no Companion Object
    companion object {
        private const val TAG = "CadastroActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCadastroBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d(TAG, "Activity iniciada.")

        // Inicialização de dependências
        // CORREÇÃO: Usando o DependencyProvider para obter as instâncias
        auth = DependencyProvider.auth

        // NOVO: Inicialização do ViewModel usando a Factory
        viewModel = ViewModelProvider(
            this,
            CadastroViewModelFactory(
                DependencyProvider.auth,
                DependencyProvider.databaseRepository
            )
        ).get(CadastroViewModel::class.java)

        // Configurando o NumberPicker
        binding.numberPickerIdade.minValue = 13
        binding.numberPickerIdade.maxValue = 100
        binding.numberPickerIdade.value = 18

        setupListeners()
        setupObservers()
    }

    /**
     * Configura os listeners de cliques na UI.
     */
    private fun setupListeners() {
        // Navegação para a tela de Login
        binding.textLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        // Ação do botão de cadastro: Coleta dados e inicia a validação/registro via ViewModel
        binding.btnCadastro.setOnClickListener {
            val nome = binding.editCadastroNome.text.toString().trim()
            val email = binding.editCadastroEmail.text.toString().trim()
            val senha = binding.editCadastroSenha.text.toString()
            val confirmarSenha = binding.editCadastroConfirmarSenha.text.toString()
            val idade = binding.numberPickerIdade.value

            // 1. Validação local: Comparação de senhas
            if (senha != confirmarSenha) {
                Toast.makeText(baseContext, "Senhas não conferem.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 2. Validação local: Campos vazios e tamanho de senha
            if (nome.isEmpty() || email.isEmpty() || senha.isEmpty() || confirmarSenha.isEmpty()) {
                Toast.makeText(baseContext, "Preencha todos os campos.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (senha.length < 6) {
                Toast.makeText(baseContext, "A senha deve ter pelo menos 6 caracteres.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Delega a lógica de registro para o ViewModel
            viewModel.registerUser(nome, email, senha, idade)
        }
    }

    /**
     * Configura os observadores para reagir às mudanças de estado no ViewModel.
     */
    private fun setupObservers() {
        // Controla o estado de carregamento da UI
        viewModel.isLoading.observe(this) { isLoading ->
            binding.btnCadastro.isEnabled = !isLoading
        }

        // Trata mensagens de erro vindas do ViewModel (validação ou Firebase)
        viewModel.errorMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                // Limpa o estado de erro no ViewModel para evitar repetição do Toast
                viewModel.clearErrorMessage()
            }
        }

        // Reage ao sucesso do cadastro para navegar para a próxima tela
        viewModel.cadastroSuccess.observe(this) { isSuccess ->
            if (isSuccess) {
                // Navega para a MainActivity
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish() // Finaliza a Activity para impedir o retorno pelo botão 'Voltar'
            }
        }
    }
}