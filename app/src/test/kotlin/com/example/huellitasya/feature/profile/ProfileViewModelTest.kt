package com.example.huellitasya.feature.profile

import app.cash.turbine.test
import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.domain.error.DataError
import com.example.huellitasya.core.domain.usecase.ChangeProfilePhotoUseCase
import com.example.huellitasya.core.domain.usecase.ObserveUserProfileUseCase
import com.example.huellitasya.core.testing.FakeAuthRepository
import com.example.huellitasya.core.testing.FakeNetworkMonitor
import com.example.huellitasya.core.testing.FakeProfilePhotoRepository
import com.example.huellitasya.core.testing.FakeUserRepository
import com.example.huellitasya.core.testing.MainDispatcherRule
import com.example.huellitasya.core.testing.TestProfile
import com.example.huellitasya.core.testing.TestUser
import com.example.huellitasya.core.ui.toUiText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = FakeAuthRepository().apply { currentUser.value = TestUser }
    private val userRepository = FakeUserRepository()
    private val photoRepository = FakeProfilePhotoRepository()
    private val networkMonitor = FakeNetworkMonitor()

    private fun viewModel(appScope: CoroutineScope) = ProfileViewModel(
        observeUserProfile = ObserveUserProfileUseCase(authRepository, userRepository),
        changeProfilePhoto = ChangeProfilePhotoUseCase(photoRepository, userRepository, appScope),
        networkMonitor = networkMonitor,
    )

    @Test
    fun `muestra el perfil y se actualiza en vivo`() = runTest {
        val viewModel = viewModel(appScope = this)
        assertEquals(TestProfile, viewModel.uiState.value.profile)

        userRepository.profile.value = AppResult.Success(TestProfile.copy(address = "Calle 1"))

        assertEquals("Calle 1", viewModel.uiState.value.profile?.address)
    }

    @Test
    fun `un error de lectura se muestra como mensaje`() = runTest {
        userRepository.profile.value = AppResult.Failure(DataError.PermissionDenied)

        viewModel(appScope = this).events.test {
            assertEquals(ProfileEvent.ShowMessage(DataError.PermissionDenied.toUiText()), awaitItem())
        }
    }

    @Test
    fun `elegir una foto la sube y el avatar muestra la nueva`() = runTest {
        val viewModel = viewModel(appScope = this)

        viewModel.onAction(ProfileAction.PhotoPicked("content://media/1"))
        testScheduler.advanceUntilIdle()

        assertEquals(TestProfile.uid to "content://media/1", photoRepository.uploads.single())
        assertEquals("https://storage.test/users/uid-123/profile.jpg", viewModel.uiState.value.profile?.photoUrl)
        assertFalse(viewModel.uiState.value.isUploadingPhoto)
    }

    @Test
    fun `una foto demasiado grande muestra el aviso`() = runTest {
        photoRepository.result = AppResult.Failure(DataError.FileTooLarge)
        val viewModel = viewModel(appScope = this)

        viewModel.events.test {
            viewModel.onAction(ProfileAction.PhotoPicked("content://media/1"))
            testScheduler.advanceUntilIdle()

            assertEquals(ProfileEvent.ShowMessage(DataError.FileTooLarge.toUiText()), awaitItem())
        }
        assertFalse(viewModel.uiState.value.isUploadingPhoto)
    }

    @Test
    fun `sin conexion no intenta subir la foto`() = runTest {
        networkMonitor.isOnline.value = false
        val viewModel = viewModel(appScope = this)

        viewModel.events.test {
            viewModel.onAction(ProfileAction.PhotoPicked("content://media/1"))

            assertEquals(ProfileEvent.ShowMessage(DataError.Network.toUiText()), awaitItem())
        }
        assertTrue(photoRepository.uploads.isEmpty())
    }
}
