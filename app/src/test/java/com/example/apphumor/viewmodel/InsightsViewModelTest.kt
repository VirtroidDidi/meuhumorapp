package com.example.apphumor.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.example.apphumor.models.HumorNote
import com.example.apphumor.models.Insight
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
class InsightsViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: InsightsViewModel
    private lateinit var repository: DatabaseRepository
    private lateinit var auth: FirebaseAuth
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        auth = mockk()

        val mockUser = mockk<FirebaseUser>()
        every { auth.currentUser } returns mockUser
        every { mockUser.uid } returns "user123"

        val fakeUser = User("user123", "Teste", "teste@email.com", 25)
        coEvery { repository.getUser("user123") } returns DatabaseRepository.Result.Success(fakeUser)

        val emptyNotes = emptyList<HumorNote>()
        coEvery { repository.getHumorNotesFlow("user123") } returns flowOf(emptyNotes)

        viewModel = InsightsViewModel(auth, repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `quando nao houver notas insights deve retornar lista vazia ou padrao`() = runTest {
        // TÉCNICA DO ESPIÃO
        val values = mutableListOf<List<Insight>>()
        val observer = Observer<List<Insight>> { values.add(it) }
        viewModel.insights.observeForever(observer)

        // Avança o tempo para o cálculo acontecer
        testDispatcher.scheduler.advanceUntilIdle()

        // Pega o último valor calculado
        val insights = values.last()

        // Verifica se gerou o card de "Sem registros"
        assertEquals(1, insights.size)

        viewModel.insights.removeObserver(observer)
    }
}