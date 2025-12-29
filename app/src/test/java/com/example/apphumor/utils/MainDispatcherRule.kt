package com.example.apphumor.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {

    override fun starting(description: Description) {
        // Define o Dispatchers.Main para usar nosso testDispatcher
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        // Reseta o Dispatchers.Main para o original após o teste
        Dispatchers.resetMain()
    }
}