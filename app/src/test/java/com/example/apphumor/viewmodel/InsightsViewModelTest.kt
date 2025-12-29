package com.example.apphumor.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.example.apphumor.R
import com.example.apphumor.models.HumorNote
import com.example.apphumor.repository.DatabaseRepository
import com.example.apphumor.utils.MainDispatcherRule
import com.example.apphumor.utils.getOrAwaitValue
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.TimeUnit

class InsightsViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var auth: FirebaseAuth
    private lateinit var repository: DatabaseRepository
    private lateinit var currentUser: FirebaseUser
    private lateinit var viewModel: InsightsViewModel

    // LiveData controlado para simular o banco de dados
    private val notesLiveData = MutableLiveData<List<HumorNote>>()

    @Before
    fun setup() {
        auth = mockk()
        repository = mockk()
        currentUser = mockk()

        every { auth.currentUser } returns currentUser
        every { currentUser.uid } returns "user_test_id"
        every { repository.getHumorNotesAsLiveData("user_test_id") } returns notesLiveData

        viewModel = InsightsViewModel(auth, repository)
    }

    @Test
    fun `Calculo de Insights - Deve identificar o humor predominante corretamente`() {
        // Arrange: 3 notas "Feliz" e 1 nota "Triste"
        val notes = listOf(
            createNote(humor = "Feliz"),
            createNote(humor = "Feliz"),
            createNote(humor = "Feliz"),
            createNote(humor = "Triste")
        )

        // Act
        notesLiveData.value = notes

        // Assert
        val insights = viewModel.insights.getOrAwaitValue()

        // O primeiro card deve ser o de Humor Predominante
        val humorCard = insights.find { it.rotulo == "Humor Predominante" }
        assertEquals("Feliz", humorCard?.valor)
    }

    @Test
    fun `Calculo de Insights - Deve contar total de registros corretamente`() {
        // Arrange
        val notes = listOf(
            createNote(humor = "Bem"),
            createNote(humor = "Mal")
        )

        // Act
        notesLiveData.value = notes

        // Assert
        val insights = viewModel.insights.getOrAwaitValue()
        val totalCard = insights.find { it.rotulo == "Total de Registros" }

        // Verifica se a string contém "2" (ex: "2 notas")
        assert(totalCard?.valor?.contains("2") == true)
    }

    @Test
    fun `Filtro de Tempo - Nao deve considerar notas antigas (fora do mes ou 30 dias)`() {
        // Arrange
        val notes = listOf(
            createNote(daysAgo = 0, humor = "Hoje"),  // Deve entrar
            createNote(daysAgo = 60, humor = "Antigo") // Deve ser ignorado (60 dias atrás)
        )

        // Act
        viewModel.setTimeRange(TimeRange.LAST_30_DAYS)
        notesLiveData.value = notes

        // Assert
        val insights = viewModel.insights.getOrAwaitValue()
        val totalCard = insights.find { it.rotulo == "Total de Registros" }

        // Esperamos apenas 1 nota contada
        assert(totalCard?.valor?.contains("1") == true)
    }

    @Test
    fun `Estado Vazio - Deve retornar mensagem de aviso se nao houver notas no periodo`() {
        // Arrange
        notesLiveData.value = emptyList()

        // Act
        val insights = viewModel.insights.getOrAwaitValue()

        // Assert
        assertEquals(1, insights.size) // Apenas 1 card de aviso
        assertEquals("Nenhum registro no período.", insights[0].valor)
    }

    // --- Helper para criar notas falsas ---
    private fun createNote(daysAgo: Int = 0, humor: String = "Neutro"): HumorNote {
        val millisPerDay = TimeUnit.DAYS.toMillis(1)
        val time = System.currentTimeMillis() - (daysAgo * millisPerDay)

        return HumorNote(
            id = "id_${System.nanoTime()}",
            humor = humor,
            timestamp = time,
            descricao = "Teste autom"
        )
    }
}