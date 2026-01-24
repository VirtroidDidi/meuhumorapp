package com.example.apphumor.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.example.apphumor.models.FilterState
import com.example.apphumor.models.HumorNote
import com.example.apphumor.models.User
import com.example.apphumor.repository.DatabaseRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: HomeViewModel
    private lateinit var repository: DatabaseRepository
    private lateinit var auth: FirebaseAuth

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        repository = mockk()
        auth = mockk()
        val firebaseUser = mockk<FirebaseUser>()

        every { auth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns "user123"

        // Mock do Usuário
        val fakeUser = User("user123", "Teste", "teste@email.com", 25)
        coEvery { repository.getUser("user123") } returns DatabaseRepository.Result.Success(fakeUser)

        // Mock das Notas
        val fakeNotes = listOf(
            HumorNote(id = "1", humor = "Rad", descricao = "Dia top", timestamp = 100000L),
            HumorNote(id = "2", humor = "Sad", descricao = "Dia ruim", timestamp = 200000L),
            HumorNote(id = "3", humor = "Incrível", descricao = "Legacy note", timestamp = 300000L)
        )
        coEvery { repository.getHumorNotesFlow("user123") } returns flowOf(fakeNotes)

        viewModel = HomeViewModel(auth, repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `quando filtrar por RAD deve retornar notas Rad e notas legado Incrivel`() = runTest {
        // TÉCNICA DO ESPIÃO (Observer)
        // Criamos uma lista para guardar todas as emissões do LiveData
        val values = mutableListOf<List<HumorNote>>()
        val observer = Observer<List<HumorNote>> { values.add(it) }

        // Começamos a observar ANTES de filtrar
        viewModel.filteredHistoryNotes.observeForever(observer)

        // 1. Deixa o load inicial acontecer
        testDispatcher.scheduler.advanceUntilIdle()

        // 2. Aplica o filtro
        val filterState = FilterState(selectedHumors = setOf("Rad"))
        viewModel.updateFilterState(filterState)

        // 3. Deixa o filtro processar
        testDispatcher.scheduler.advanceUntilIdle()

        // VERIFICAÇÃO: Pegamos o ÚLTIMO valor emitido
        // (O primeiro deve ser vazio, o último deve ser o filtrado)
        val currentList = values.last()

        assertEquals(2, currentList.size)
        assertEquals("Rad", currentList.find { it.id == "1" }?.humor)
        assertEquals("Incrível", currentList.find { it.id == "3" }?.humor)

        viewModel.filteredHistoryNotes.removeObserver(observer)
    }

    @Test
    fun `quando limpar filtros deve retornar todas as notas`() = runTest {
        val values = mutableListOf<List<HumorNote>>()
        val observer = Observer<List<HumorNote>> { values.add(it) }
        viewModel.filteredHistoryNotes.observeForever(observer)

        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateFilterState(FilterState()) // Limpa filtro

        testDispatcher.scheduler.advanceUntilIdle()

        val currentList = values.last()
        assertEquals(3, currentList.size)

        viewModel.filteredHistoryNotes.removeObserver(observer)
    }
}