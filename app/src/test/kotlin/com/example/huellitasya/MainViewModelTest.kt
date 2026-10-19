package com.example.huellitasya

import app.cash.turbine.test
import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.domain.error.DataError
import com.example.huellitasya.core.domain.usecase.ObservePetsUseCase
import com.example.huellitasya.core.domain.usecase.ObserveSessionUseCase
import com.example.huellitasya.core.domain.usecase.ShouldShowPetsIntroUseCase
import com.example.huellitasya.core.testing.FakeAuthRepository
import com.example.huellitasya.core.testing.FakePetRepository
import com.example.huellitasya.core.testing.MainDispatcherRule
import com.example.huellitasya.core.testing.TestPet
import com.example.huellitasya.core.testing.TestUser
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MainViewModelTest {

    // StandardTestDispatcher: permite observar el estado inicial `Loading` antes de que llegue la sesión.
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    private val authRepository = FakeAuthRepository()
    private val petRepository = FakePetRepository()

    private fun viewModel() = MainViewModel(
        observeSession = ObserveSessionUseCase(authRepository),
        shouldShowPetsIntro = ShouldShowPetsIntroUseCase(ObservePetsUseCase(authRepository, petRepository)),
    )

    @Test
    fun `empieza en Loading y sigue los cambios de sesion`() = runTest {
        petRepository.pets.value = AppResult.Success(listOf(TestPet))

        viewModel().uiState.test {
            assertEquals(MainUiState.Loading, awaitItem())
            assertEquals(MainUiState.SignedOut, awaitItem())

            authRepository.currentUser.value = TestUser
            assertEquals(MainUiState.Loading, awaitItem())
            assertEquals(MainUiState.SignedIn(startWithIntro = false), awaitItem())

            authRepository.currentUser.value = null
            assertEquals(MainUiState.SignedOut, awaitItem())
        }
    }

    @Test
    fun `sin mascotas el grafo principal empieza en Presentacion`() = runTest {
        authRepository.currentUser.value = TestUser

        viewModel().uiState.test {
            assertEquals(MainUiState.Loading, awaitItem())
            assertEquals(MainUiState.SignedIn(startWithIntro = true), awaitItem())

            // Registrar la primera mascota no recrea el grafo.
            petRepository.pets.value = AppResult.Success(listOf(TestPet))
            expectNoEvents()
        }
    }

    @Test
    fun `si cambian los datos del usuario no se recrea el grafo`() = runTest {
        authRepository.currentUser.value = TestUser

        viewModel().uiState.test {
            skipItems(2)
            authRepository.currentUser.value = TestUser.copy(name = "Otro nombre")
            expectNoEvents()
        }
    }

    @Test
    fun `si no se pueden leer las mascotas entra a Home`() = runTest {
        authRepository.currentUser.value = TestUser
        petRepository.pets.value = AppResult.Failure(DataError.PermissionDenied)

        viewModel().uiState.test {
            skipItems(1)
            assertEquals(MainUiState.SignedIn(startWithIntro = false), awaitItem())
        }
    }
}
