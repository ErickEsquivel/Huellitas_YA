package com.example.huellitasya.feature.auth.connectivity

import com.example.huellitasya.core.testing.FakeNetworkMonitor
import com.example.huellitasya.core.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConnectivityViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val networkMonitor = FakeNetworkMonitor()
    // Lazy: el init del ViewModel lanza corrutinas y necesita el Main de prueba que instala la regla.
    private val viewModel by lazy { ConnectivityViewModel(networkMonitor) }

    /** `uiState` usa `WhileSubscribed`: hace falta un colector activo, como la UI. */
    private fun TestScope.observe() {
        backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()
    }

    @Test
    fun `con internet no muestra el overlay`() = runTest {
        observe()
        assertEquals(ConnectionUiState.Online, viewModel.uiState.value)
    }

    @Test
    fun `al perder la red muestra sin conexion y al volver se cierra solo`() = runTest {
        observe()

        networkMonitor.isOnline.value = false
        runCurrent()
        assertEquals(ConnectionUiState.Offline, viewModel.uiState.value)

        networkMonitor.isOnline.value = true
        runCurrent()
        assertEquals(ConnectionUiState.Online, viewModel.uiState.value)
    }

    @Test
    fun `un error de red de Firebase muestra el overlay aunque el sistema tenga red`() = runTest {
        observe()

        viewModel.onNetworkError()
        runCurrent()

        assertEquals(ConnectionUiState.Offline, viewModel.uiState.value)
    }

    @Test
    fun `si la red vuelve despues de un error de Firebase el overlay se cierra`() = runTest {
        networkMonitor.isOnline.value = false
        observe()
        viewModel.onNetworkError()
        runCurrent()

        networkMonitor.isOnline.value = true
        runCurrent()

        assertEquals(ConnectionUiState.Online, viewModel.uiState.value)
    }

    @Test
    fun `reintentar con red muestra conectando y luego cierra el overlay`() = runTest {
        observe()
        viewModel.onNetworkError()

        viewModel.onRetry()
        runCurrent()
        assertEquals(ConnectionUiState.Reconnecting, viewModel.uiState.value)

        advanceTimeBy(ConnectivityViewModel.MIN_RECONNECTING_MILLIS + 1)
        runCurrent()
        assertEquals(ConnectionUiState.Online, viewModel.uiState.value)
    }

    @Test
    fun `reintentar sin red vuelve a sin conexion`() = runTest {
        networkMonitor.isOnline.value = false
        observe()

        viewModel.onRetry()
        runCurrent()
        assertEquals(ConnectionUiState.Reconnecting, viewModel.uiState.value)

        advanceTimeBy(ConnectivityViewModel.MIN_RECONNECTING_MILLIS + 1)
        runCurrent()
        assertEquals(ConnectionUiState.Offline, viewModel.uiState.value)
    }
}
