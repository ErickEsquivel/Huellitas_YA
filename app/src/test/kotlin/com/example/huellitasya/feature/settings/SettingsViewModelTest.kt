package com.example.huellitasya.feature.settings

import com.example.huellitasya.core.domain.usecase.SignOutUseCase
import com.example.huellitasya.core.testing.FakeAuthRepository
import com.example.huellitasya.core.testing.MainDispatcherRule
import com.example.huellitasya.core.testing.TestUser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = FakeAuthRepository().apply { currentUser.value = TestUser }
    private val viewModel = SettingsViewModel(SignOutUseCase(authRepository))

    @Test
    fun `cerrar sesion invalida la sesion una sola vez`() {
        viewModel.logout()
        viewModel.logout()

        assertEquals(1, authRepository.signOutCount)
        assertNull(authRepository.currentUser.value)
        assertTrue(viewModel.uiState.value.isLoggingOut)
    }
}
