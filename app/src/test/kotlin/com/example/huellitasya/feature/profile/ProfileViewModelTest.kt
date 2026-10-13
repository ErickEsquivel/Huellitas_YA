package com.example.huellitasya.feature.profile

import app.cash.turbine.test
import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.domain.error.DataError
import com.example.huellitasya.core.domain.usecase.ObserveUserProfileUseCase
import com.example.huellitasya.core.testing.FakeAuthRepository
import com.example.huellitasya.core.testing.FakeUserRepository
import com.example.huellitasya.core.testing.MainDispatcherRule
import com.example.huellitasya.core.testing.TestProfile
import com.example.huellitasya.core.testing.TestUser
import com.example.huellitasya.core.ui.toUiText
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = FakeAuthRepository().apply { currentUser.value = TestUser }
    private val userRepository = FakeUserRepository()

    private fun viewModel() = ProfileViewModel(ObserveUserProfileUseCase(authRepository, userRepository))

    @Test
    fun `muestra el perfil y se actualiza en vivo`() {
        val viewModel = viewModel()
        assertEquals(TestProfile, viewModel.uiState.value.profile)

        userRepository.profile.value = AppResult.Success(TestProfile.copy(address = "Calle 1"))

        assertEquals("Calle 1", viewModel.uiState.value.profile?.address)
    }

    @Test
    fun `un error de lectura se muestra como mensaje`() = runTest {
        userRepository.profile.value = AppResult.Failure(DataError.PermissionDenied)

        viewModel().events.test {
            assertEquals(ProfileEvent.ShowMessage(DataError.PermissionDenied.toUiText()), awaitItem())
        }
    }
}
