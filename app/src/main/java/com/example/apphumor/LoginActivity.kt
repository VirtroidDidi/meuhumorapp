package com.example.apphumor

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.credentials.CredentialManager
import com.example.apphumor.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var oneTapClient: CredentialManager
    private val TAG = "LoginActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d(TAG, "onCreate chamado")

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // inicializar oneTapClient
        oneTapClient = CredentialManager.create(this)
        Log.i(TAG, "firebase e OneTapClient inicializados")
        // Verificar se o usuário já está logado
        if (auth.currentUser != null) {
            // O usuário está logado, então vá para a MainActivity
            Log.i(TAG, "Usuário já logado, indo para MainActivity")
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // Finaliza a LoginActivity para que o usuário não volte para ela com o botão "voltar"
        }

        // Ação para clicar no texto cadastro
        binding.textCadastro.setOnClickListener {
            Log.d(TAG, "Clicou no texto 'cadastro'")
            // Abrir a tela de cadastro
            val intent = Intent(this, CadastroActivity::class.java)
            startActivity(intent)
        }

        // Ação do botão de login
        binding.btnLogin.setOnClickListener {
            Log.d(TAG, "Clicou no botão 'login'")
            val email = binding.editLoginEmail.text.toString()
            val senha = binding.editLoginSenha.text.toString()

            if (email.isNotEmpty() && senha.isNotEmpty()) {
                Log.d(TAG, "Email e senha preenchidos: $email")
                auth.signInWithEmailAndPassword(email, senha)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            // Login bem-sucedido
                            val user = auth.currentUser
                            Log.i(TAG, "Login realizado com sucesso: ${user?.email}")
                            Toast.makeText(baseContext, "Login realizado com sucesso.", Toast.LENGTH_SHORT).show()
                            //navegar para a main, nesse momento
                            // Cria um Intent para iniciar a MainActivity
                            val intent = Intent(this, MainActivity::class.java)
                            startActivity(intent)
                            finish() // Finaliza a LoginActivity depois do login
                        } else {
                            // Se o login falhar, exibe uma mensagem para o usuário.
                            val exception = task.exception

                            if (exception != null) {
                                Log.e(TAG, "Falha ao logar:", exception)
                                if (exception is FirebaseAuthInvalidUserException) {
                                    Toast.makeText(baseContext, "Usuário não encontrado.", Toast.LENGTH_SHORT).show()
                                } else if (exception is FirebaseAuthInvalidCredentialsException) {
                                    Toast.makeText(baseContext, "Senha incorreta.", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(baseContext, "Falha ao realizar o login.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
            } else {
                Log.w(TAG, "Email ou senha vazios")
                Toast.makeText(baseContext, "Preencha todos os campos.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}