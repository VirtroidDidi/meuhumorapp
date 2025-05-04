package com.example.apphumor

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.example.apphumor.databinding.ActivityCadastroBinding
import com.example.apphumor.models.User
import com.example.apphumor.viewmodel.CadastroViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException

class CadastroActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCadastroBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var viewModel: CadastroViewModel
    private val TAG = "CadastroActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCadastroBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d(TAG, "onCreate chamado")

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        Log.i(TAG, "Firebase inicializado")

        // Initialize ViewModel
        viewModel = ViewModelProvider(this).get(CadastroViewModel::class.java)
        Log.i(TAG, "ViewModel inicializado")

        // Configurando o NumberPicker
        binding.numberPickerIdade.minValue = 13 // Idade mínima
        binding.numberPickerIdade.maxValue = 100 // Idade máxima
        binding.numberPickerIdade.value = 18 // Idade inicial
        Log.d(TAG, "NumberPicker configurado")

        // Ação para clicar no texto login
        binding.textLogin.setOnClickListener {
            Log.d(TAG, "Clicou no texto 'login'")
            // Abrir a tela de login
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        // Ação do botão de cadastro
        binding.btnCadastro.setOnClickListener {
            Log.d(TAG, "Clicou no botão 'cadastro'")
            val nome = binding.editCadastroNome.text.toString()
            val email = binding.editCadastroEmail.text.toString()
            val senha = binding.editCadastroSenha.text.toString()
            val confirmarSenha = binding.editCadastroConfirmarSenha.text.toString()
            val idade = binding.numberPickerIdade.value

            // Verifica se algum campo está vazio
            if (nome.isEmpty() || email.isEmpty() || senha.isEmpty() || confirmarSenha.isEmpty()) {
                Log.w(TAG, "Algum campo está vazio")
                Toast.makeText(baseContext, "Preencha todos os campos.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener // Sai da função se algum campo estiver vazio
            }

            if (senha.length < 6) {
                Log.w(TAG, "Senha com menos de 6 caracteres")
                Toast.makeText(baseContext, "A senha deve ter pelo menos 6 caracteres.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (senha == confirmarSenha) {
                Log.d(TAG, "Senhas conferem, criando usuário")
                auth.createUserWithEmailAndPassword(email, senha)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            // Cadastro bem-sucedido
                            val userFirebase = auth.currentUser
                            val user = User(userFirebase?.uid, nome, email, idade)
                            Log.i(TAG, "Usuário criado com sucesso: ${userFirebase?.email}")

                            // Salvando o usuário no Realtime Database
                            viewModel.saveUser(user)

                            Toast.makeText(baseContext, "Cadastro realizado com sucesso.", Toast.LENGTH_SHORT).show()
                            //navegar para a main, nesse momento
                            // Cria um Intent para iniciar a MainActivity
                            val intent = Intent(this, MainActivity::class.java)
                            startActivity(intent)
                            Log.d(TAG, "Indo para MainActivity")
                        } else {
                            // Se o cadastro falhar, exibe uma mensagem para o usuário.
                            val exception = task.exception
                            // Imprima a causa da falha no Logcat
                            if (exception != null) {
                                Log.e(TAG, "Falha ao cadastrar:", exception)
                                if (exception is FirebaseAuthWeakPasswordException) {
                                    // Senha muito fraca
                                    Toast.makeText(baseContext, "Senha muito fraca. Use pelo menos 6 caracteres.", Toast.LENGTH_SHORT).show()
                                } else if (exception is FirebaseAuthInvalidCredentialsException) {
                                    // Credenciais inválidas
                                    Toast.makeText(baseContext, "Credenciais inválidas.", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(baseContext, "Falha ao cadastrar.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
            } else {
                Log.w(TAG, "Senhas não conferem")
                Toast.makeText(baseContext, "Senhas não conferem.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}