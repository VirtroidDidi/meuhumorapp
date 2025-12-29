package com.example.apphumor.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.apphumor.models.User
import com.example.apphumor.repository.DatabaseRepository
import com.example.apphumor.utils.MainDispatcherRule
import com.example.apphumor.utils.getOrAwaitValue
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import io.mockk.*
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CadastroViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var auth: FirebaseAuth
    private lateinit var repository: DatabaseRepository
    private lateinit var viewModel: CadastroViewModel

    // Mocks auxiliares para o Firebase
    private lateinit var authResultTask: Task<AuthResult>
    private lateinit var authResult: AuthResult
    private lateinit var firebaseUser: FirebaseUser

    @Before
    fun setup() {
        auth = mockk()
        repository = mockk()

        // Mocks profundos para simular a resposta do Firebase
        authResultTask = mockk()
        authResult = mockk()
        firebaseUser = mockk()

        // TRUQUE DE MESTRE:
        // Precisamos habilitar o mock de funções estáticas para lidar com o ".await()"
        mockkStatic("kotlinx.coroutines.tasks.TasksKt")

        viewModel = CadastroViewModel(auth, repository)
    }

    @After
    fun tearDown() {
        // Limpa os mocks estáticos depois de cada teste
        unmockkStatic("kotlinx.coroutines.tasks.TasksKt")
    }

    @Test
    fun `Validacao - Nao deve chamar Firebase se senha for curta`() {
        // Arrange (Dados ruins)
        val nome = "Teste"
        val email = "teste@email.com"
        val senhaCurta = "123" // Menor que 6
        val idade = 20

        // Act
        viewModel.registerUser(nome, email, senhaCurta, idade)

        // Assert
        val erro = viewModel.errorMessage.getOrAwaitValue()
        assertEquals("Dados inválidos. Verifique todos os campos.", erro)

        // Verifica se o Firebase NUNCA foi chamado (segurança)
        verify(exactly = 0) { auth.createUserWithEmailAndPassword(any(), any()) }
    }

    @Test
    fun `Sucesso - Deve criar conta e salvar usuario no banco`() = runTest {
        // Arrange
        val email = "certo@email.com"
        val senha = "senha123"
        val uid = "novo_uid_123"

        // Ensinamos o Mock a retornar sucesso em cadeia
        every { auth.createUserWithEmailAndPassword(email, senha) } returns authResultTask
        coEvery { authResultTask.await() } returns authResult // Simula o fim da Task
        every { authResult.user } returns firebaseUser
        every { firebaseUser.uid } returns uid

        // Ensinamos o repositório a dizer "Sim, salvei com sucesso"
        coEvery { repository.saveUser(any()) } returns DatabaseRepository.Result.Success(Unit)

        // Act
        viewModel.registerUser("Nome Certo", email, senha, 25)

        // Assert
        val sucesso = viewModel.cadastroSuccess.getOrAwaitValue()
        assertTrue(sucesso)

        // Verifica se tentou salvar o objeto User correto
        coVerify {
            repository.saveUser(match { it.email == email && it.uid == uid })
        }
    }

    @Test
    fun `Erro Firebase - Deve tratar excecao de senha fraca`() = runTest {
        // Arrange
        val email = "fraco@email.com"
        val senha = "senha_ruim"

        // Simulando erro na chamada do Firebase
        every { auth.createUserWithEmailAndPassword(email, senha) } returns authResultTask
        coEvery { authResultTask.await() } throws FirebaseAuthWeakPasswordException("errorCode", "Senha fraca", null)

        // Act
        viewModel.registerUser("Nome", email, senha, 20)

        // Assert
        val erro = viewModel.errorMessage.getOrAwaitValue()
        assertEquals("A senha é muito fraca.", erro)

        // Garante que se falhou no Auth, NÃO tentou salvar no banco
        coVerify(exactly = 0) { repository.saveUser(any()) }
    }
}