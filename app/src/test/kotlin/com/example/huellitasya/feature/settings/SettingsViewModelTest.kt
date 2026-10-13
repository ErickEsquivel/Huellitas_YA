package com.example.huellitasya.feature.settings

import app.cash.turbine.test
import com.example.huellitasya.R
import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.designsystem.components.FieldStatus
import com.example.huellitasya.core.domain.error.AuthError
import com.example.huellitasya.core.domain.usecase.DeleteAccountUseCase
import com.example.huellitasya.core.domain.usecase.ObserveSessionUseCase
import com.example.huellitasya.core.domain.usecase.ReauthenticateUseCase
import com.example.huellitasya.core.domain.usecase.SignOutUseCase
import com.example.huellitasya.core.domain.validation.ValidatePasswordUseCase
import com.example.huellitasya.core.model.AuthProvider
import com.example.huellitasya.core.testing.FakeAccountRepository
import com.example.huellitasya.core.testing.FakeAuthRepository
import com.example.huellitasya.core.testing.FakeNetworkMonitor
import com.example.huellitasya.core.testing.MainDispatcherRule
import com.example.huellitasya.core.testing.TestUser
import com.example.huellitasya.core.ui.UiText
import com.example.huellitasya.core.ui.toUiText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = FakeAuthRepository().apply { currentUser.value = TestUser }
    private val accountRepository = FakeAccountRepository()
    private val networkMonitor = FakeNetworkMonitor()

    private fun viewModel(appScope: CoroutineScope) = SettingsViewModel(
        observeSession = ObserveSessionUseCase(authRepository),
        signOut = SignOutUseCase(authRepository),
        reauthenticate = ReauthenticateUseCase(authRepository),
        deleteAccount = DeleteAccountUseCase(accountRepository, authRepository, appScope),
        networkMonitor = networkMonitor,
        validatePassword = ValidatePasswordUseCase(),
    )

    @Test
    fun `cerrar sesion invalida la sesion una sola vez`() = runTest {
        val viewModel = viewModel(appScope = this)

        viewModel.onAction(SettingsAction.Logout)
        viewModel.onAction(SettingsAction.Logout)

        assertEquals(1, authRepository.signOutCount)
        assertNull(authRepository.currentUser.value)
        assertTrue(viewModel.uiState.value.isLoggingOut)
    }

    @Test
    fun `eliminar pide confirmacion antes de hacer nada`() = runTest {
        val viewModel = viewModel(appScope = this)

        viewModel.onAction(SettingsAction.DeleteAccountClick)

        assertTrue(viewModel.uiState.value.showDeleteConfirmation)
        assertEquals(0, accountRepository.deleteCount)
    }

    @Test
    fun `cerrar la confirmacion no elimina`() = runTest {
        val viewModel = viewModel(appScope = this)
        viewModel.onAction(SettingsAction.DeleteAccountClick)

        viewModel.onAction(SettingsAction.DismissDelete)

        assertFalse(viewModel.uiState.value.showDeleteConfirmation)
        assertNull(viewModel.uiState.value.passwordPrompt)
        assertEquals(0, accountRepository.deleteCount)
    }

    @Test
    fun `cuenta de correo confirma con contrasena y elimina`() = runTest {
        val viewModel = viewModel(appScope = this)
        viewModel.onAction(SettingsAction.DeleteAccountClick)
        viewModel.onAction(SettingsAction.ConfirmDelete)
        assertNotNull(viewModel.uiState.value.passwordPrompt)

        viewModel.onAction(SettingsAction.PasswordChanged("Huellitas1"))
        viewModel.onAction(SettingsAction.ConfirmPassword)
        advanceUntilIdle()

        assertEquals(listOf("Huellitas1"), authRepository.reauthPasswords)
        assertEquals(1, accountRepository.deleteCount)
        assertNull(authRepository.currentUser.value)
        assertTrue(viewModel.uiState.value.isDeleting)
    }

    @Test
    fun `contrasena vacia o incorrecta se marca y no elimina`() = runTest {
        authRepository.reauthResult = AppResult.Failure(AuthError.InvalidCredentials)
        val viewModel = viewModel(appScope = this)
        viewModel.onAction(SettingsAction.ConfirmDelete)

        viewModel.onAction(SettingsAction.ConfirmPassword)
        assertEquals(FieldStatus.Warning, viewModel.uiState.value.passwordPrompt?.feedback?.status)
        assertTrue(authRepository.reauthPasswords.isEmpty())

        viewModel.onAction(SettingsAction.PasswordChanged("mala"))
        viewModel.onAction(SettingsAction.ConfirmPassword)
        advanceUntilIdle()

        assertEquals(FieldStatus.Error, viewModel.uiState.value.passwordPrompt?.feedback?.status)
        assertEquals(0, accountRepository.deleteCount)
        assertNotNull(authRepository.currentUser.value)
    }

    @Test
    fun `cuenta Google abre el selector y elimina con el token`() = runTest {
        authRepository.currentUser.value = TestUser.copy(provider = AuthProvider.GOOGLE)
        val viewModel = viewModel(appScope = this)

        viewModel.events.test {
            viewModel.onAction(SettingsAction.ConfirmDelete)
            assertEquals(SettingsEvent.LaunchGoogleReauth, awaitItem())
        }
        viewModel.onGoogleIdTokenResult(AppResult.Success("id-token"))
        advanceUntilIdle()

        assertEquals(listOf("id-token"), authRepository.reauthGoogleTokens)
        assertEquals(1, accountRepository.deleteCount)
        assertNull(viewModel.uiState.value.passwordPrompt)
    }

    @Test
    fun `cancelar el selector de Google no muestra error ni elimina`() = runTest {
        authRepository.currentUser.value = TestUser.copy(provider = AuthProvider.GOOGLE)
        val viewModel = viewModel(appScope = this)

        viewModel.events.test {
            viewModel.onGoogleIdTokenResult(AppResult.Failure(AuthError.Cancelled))
            expectNoEvents()
        }
        assertEquals(0, accountRepository.deleteCount)
        assertFalse(viewModel.uiState.value.isDeleting)
    }

    @Test
    fun `sin conexion avisa y no pide la contrasena`() = runTest {
        networkMonitor.isOnline.value = false
        val viewModel = viewModel(appScope = this)

        viewModel.events.test {
            viewModel.onAction(SettingsAction.ConfirmDelete)
            assertEquals(SettingsEvent.ShowMessage(AuthError.Network.toUiText()!!), awaitItem())
        }
        assertNull(viewModel.uiState.value.passwordPrompt)
    }

    @Test
    fun `si el backend falla se muestra el error y la sesion sigue`() = runTest {
        accountRepository.result = AppResult.Failure(AuthError.Unknown)
        val viewModel = viewModel(appScope = this)
        viewModel.onAction(SettingsAction.ConfirmDelete)
        viewModel.onAction(SettingsAction.PasswordChanged("Huellitas1"))

        viewModel.events.test {
            viewModel.onAction(SettingsAction.ConfirmPassword)
            advanceUntilIdle()
            assertEquals(SettingsEvent.ShowMessage(AuthError.Unknown.toUiText()!!), awaitItem())
        }
        assertFalse(viewModel.uiState.value.isDeleting)
        assertNotNull(authRepository.currentUser.value)
    }

    @Test
    fun `Seguridad abre la pantalla en cuentas de correo`() = runTest {
        val viewModel = viewModel(appScope = this)

        viewModel.events.test {
            viewModel.onAction(SettingsAction.SecurityClick)
            assertEquals(SettingsEvent.NavigateToSecurity, awaitItem())
        }
    }

    @Test
    fun `Seguridad en una cuenta Google solo avisa`() = runTest {
        authRepository.currentUser.value = TestUser.copy(provider = AuthProvider.GOOGLE)
        val viewModel = viewModel(appScope = this)

        viewModel.events.test {
            viewModel.onAction(SettingsAction.SecurityClick)
            assertEquals(
                SettingsEvent.ShowMessage(UiText.Resource(R.string.settings_security_google)),
                awaitItem(),
            )
        }
    }
}
