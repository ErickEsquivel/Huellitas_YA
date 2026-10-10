package com.example.huellitasya

import app.cash.turbine.test
import com.example.huellitasya.core.domain.usecase.ObserveSessionUseCase
import com.example.huellitasya.core.model.SessionState
import com.example.huellitasya.core.testing.FakeAuthRepository
import com.example.huellitasya.core.testing.MainDispatcherRule
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

    @Test
    fun `empieza en Loading y sigue los cambios de sesion`() = runTest {
        val viewModel = MainViewModel(ObserveSessionUseCase(authRepository))

        viewModel.sessionState.test {
            assertEquals(SessionState.Loading, awaitItem())
            assertEquals(SessionState.SignedOut, awaitItem())

            authRepository.currentUser.value = TestUser
            assertEquals(SessionState.SignedIn(TestUser), awaitItem())

            authRepository.currentUser.value = null
            assertEquals(SessionState.SignedOut, awaitItem())
        }
    }
}
