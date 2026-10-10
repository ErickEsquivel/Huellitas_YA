package com.example.huellitasya.feature.auth.chooser

import app.cash.turbine.test
import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.domain.error.AuthError
import com.example.huellitasya.core.domain.usecase.SignInWithGoogleUseCase
import com.example.huellitasya.core.testing.FakeAuthRepository
import com.example.huellitasya.core.testing.FakeUserRepository
import com.example.huellitasya.core.testing.MainDispatcherRule
import com.example.huellitasya.core.ui.toUiText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AuthChooserViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = FakeAuthRepository()
    private val userRepository = FakeUserRepository()

    private fun viewModel(appScope: CoroutineScope) =
        AuthChooserViewModel(SignInWithGoogleUseCase(authRepository, userRepository, appScope))

    @Test
    fun `pulsar Google pide abrir el selector y activa la carga`() = runTest {
        val viewModel = viewModel(appScope = this)

        viewModel.events.test {
            viewModel.onGoogleClick()
            assertEquals(AuthChooserEvent.LaunchGoogleSignIn, awaitItem())
        }
        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `cancelar el selector no muestra mensaje`() = runTest {
        val viewModel = viewModel(appScope = this)

        viewModel.events.test {
            viewModel.onGoogleClick()
            awaitItem() // LaunchGoogleSignIn
            viewModel.onGoogleIdTokenResult(AppResult.Failure(AuthError.Cancelled))
            expectNoEvents()
        }
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `token valido autentica y crea el perfil`() = runTest {
        val viewModel = viewModel(appScope = this)

        viewModel.onGoogleIdTokenResult(AppResult.Success("id-token"))
        advanceUntilIdle()

        assertEquals(listOf("id-token"), authRepository.googleTokens)
        assertEquals(1, userRepository.createdUsers.size)
    }

    @Test
    fun `fallo de Google muestra mensaje`() = runTest {
        authRepository.googleResult = AppResult.Failure(AuthError.GoogleSignInFailed)
        val viewModel = viewModel(appScope = this)

        viewModel.events.test {
            viewModel.onGoogleIdTokenResult(AppResult.Success("id-token"))
            assertEquals(AuthChooserEvent.ShowMessage(AuthError.GoogleSignInFailed.toUiText()!!), awaitItem())
        }
        assertFalse(viewModel.uiState.value.isLoading)
    }
}
