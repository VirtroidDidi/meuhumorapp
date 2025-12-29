package com.example.apphumor.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.example.apphumor.models.HumorNote
import com.example.apphumor.repository.DatabaseRepository
import com.example.apphumor.utils.MainDispatcherRule
import com.example.apphumor.utils.getOrAwaitValue
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.TimeUnit

class HomeViewModelTest {

    // 1. Regras para rodar LiveData e Coroutines de forma síncrona
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // 2. Mocks
    private lateinit var auth: FirebaseAuth
    private lateinit var repository: DatabaseRepository
    private lateinit var currentUser: FirebaseUser

    // O "Subject Under Test" (O cara que vamos testar)
    private lateinit var viewModel: HomeViewModel

    // LiveData falso que o repositório vai retornar
    private val notesLiveDataFromRepo = MutableLiveData<List<HumorNote>>()

    @Before
    fun setup() {
        auth = mockk()
        repository = mockk()
        currentUser = mockk()

        // Configuração padrão: Usuário logado
        every { auth.currentUser } returns currentUser
        every { currentUser.uid } returns "user123"

        // Configuração padrão: Repositório retorna nosso LiveData controlado
        every { repository.getHumorNotesAsLiveData("user123") } returns notesLiveDataFromRepo
    }

    @Test
    fun `Deve carregar lista de notas quando usuario estiver logado`() {
        // Arrange (Preparação)
        val dummyNotes = listOf(
            HumorNote(id = "1", humor = "Feliz", timestamp = System.currentTimeMillis())
        )

        // Act (Ação: Instanciar VM e simular dados chegando do banco)
        viewModel = HomeViewModel(auth, repository)
        notesLiveDataFromRepo.value = dummyNotes

        // Assert (Verificação)
        val result = viewModel.filteredHistoryNotes.getOrAwaitValue()
        assertEquals(1, result.size)
        assertEquals("Feliz", result[0].humor)
    }

    @Test
    fun `Calculo de Sequencia - Deve retornar 1 dia quando houver nota apenas hoje`() {
        // Arrange
        val notes = listOf(
            createNote(daysAgo = 0) // Hoje
        )

        // Act
        viewModel = HomeViewModel(auth, repository)
        notesLiveDataFromRepo.value = notes

        // Assert
        val (sequence, _) = viewModel.dailyProgress.getOrAwaitValue()
        assertEquals(1, sequence)
    }

    @Test
    fun `Calculo de Sequencia - Deve retornar 2 dias quando houver nota hoje e ontem`() {
        // Arrange
        val notes = listOf(
            createNote(daysAgo = 0), // Hoje
            createNote(daysAgo = 1)  // Ontem
        )

        // Act
        viewModel = HomeViewModel(auth, repository)
        notesLiveDataFromRepo.value = notes

        // Assert
        val (sequence, _) = viewModel.dailyProgress.getOrAwaitValue()
        assertEquals(2, sequence)
    }

    @Test
    fun `Calculo de Sequencia - Deve quebrar sequencia se pular um dia`() {
        // Arrange: Nota hoje e nota anteontem (pulou ontem)
        val notes = listOf(
            createNote(daysAgo = 0), // Hoje
            createNote(daysAgo = 2)  // Anteontem (Buraco no meio)
        )

        // Act
        viewModel = HomeViewModel(auth, repository)
        notesLiveDataFromRepo.value = notes

        // Assert: A sequência deve contar apenas o dia atual (1), pois a cadeia quebrou
        val (sequence, _) = viewModel.dailyProgress.getOrAwaitValue()
        assertEquals(1, sequence)
    }

    @Test
    fun `Filtro - Deve filtrar lista por texto de busca`() {
        // Arrange
        val notes = listOf(
            createNote(daysAgo = 0).copy(humor = "Feliz", descricao = "Dia ótimo"),
            createNote(daysAgo = 1).copy(humor = "Triste", descricao = "Dia ruim")
        )

        viewModel = HomeViewModel(auth, repository)
        notesLiveDataFromRepo.value = notes

        // Act: Usuário digita "Triste"
        viewModel.updateSearchQuery("Triste")

        // Assert
        val filteredList = viewModel.filteredHistoryNotes.getOrAwaitValue()
        assertEquals(1, filteredList.size)
        assertEquals("Triste", filteredList[0].humor)
    }

    // --- Helpers ---

    /**
     * Cria uma nota com timestamp relativo a "X dias atrás" a partir de agora.
     * Isso evita problemas com datas fixas (hardcoded) nos testes.
     */
    private fun createNote(daysAgo: Int): HumorNote {
        val millisPerDay = TimeUnit.DAYS.toMillis(1)
        val time = System.currentTimeMillis() - (daysAgo * millisPerDay)
        return HumorNote(
            id = "id_$daysAgo",
            humor = "Neutro",
            timestamp = time,
            isSynced = true
        )
    }
}