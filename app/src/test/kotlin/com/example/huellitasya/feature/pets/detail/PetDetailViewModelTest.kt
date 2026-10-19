package com.example.huellitasya.feature.pets.detail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.domain.error.DataError
import com.example.huellitasya.core.domain.usecase.ObservePetUseCase
import com.example.huellitasya.core.testing.FakeAuthRepository
import com.example.huellitasya.core.testing.FakePetRepository
import com.example.huellitasya.core.testing.MainDispatcherRule
import com.example.huellitasya.core.testing.TestPet
import com.example.huellitasya.core.testing.TestUser
import com.example.huellitasya.core.ui.toUiText
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PetDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = FakeAuthRepository().apply { currentUser.value = TestUser }
    private val petRepository = FakePetRepository().apply { pets.value = AppResult.Success(listOf(TestPet)) }

    private fun viewModel() = PetDetailViewModel(
        savedStateHandle = SavedStateHandle(mapOf(PetDetailViewModel.PET_ID_ARG to TestPet.id)),
        observePet = ObservePetUseCase(authRepository, petRepository),
    )

    @Test
    fun `muestra la mascota y se actualiza en vivo`() = runTest {
        val viewModel = viewModel()
        assertEquals(TestPet, viewModel.uiState.value.pet)

        petRepository.pets.value = AppResult.Success(listOf(TestPet.copy(weightKg = 6.5)))

        assertEquals(6.5, viewModel.uiState.value.pet?.weightKg)
    }

    @Test
    fun `si la mascota se elimina vuelve a Home`() = runTest {
        val viewModel = viewModel()

        viewModel.events.test {
            petRepository.pets.value = AppResult.Success(emptyList())
            assertEquals(PetDetailEvent.PetGone, awaitItem())
        }
    }

    @Test
    fun `otros errores se muestran como mensaje`() = runTest {
        petRepository.pets.value = AppResult.Failure(DataError.Network)

        viewModel().events.test {
            assertEquals(PetDetailEvent.ShowMessage(DataError.Network.toUiText()), awaitItem())
        }
    }
}
