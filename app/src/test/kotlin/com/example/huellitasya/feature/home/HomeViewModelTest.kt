package com.example.huellitasya.feature.home

import app.cash.turbine.test
import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.domain.error.DataError
import com.example.huellitasya.core.domain.usecase.ObservePetsUseCase
import com.example.huellitasya.core.testing.FakeAuthRepository
import com.example.huellitasya.core.testing.FakePetRepository
import com.example.huellitasya.core.testing.MainDispatcherRule
import com.example.huellitasya.core.testing.TestPet
import com.example.huellitasya.core.testing.TestUser
import com.example.huellitasya.core.ui.toUiText
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = FakeAuthRepository().apply { currentUser.value = TestUser }
    private val petRepository = FakePetRepository()

    private fun viewModel() = HomeViewModel(ObservePetsUseCase(authRepository, petRepository))

    @Test
    fun `sin mascotas muestra la lista vacia ya cargada`() = runTest {
        val state = viewModel().uiState.value

        assertTrue(state.pets.isEmpty())
        assertFalse(state.isLoading)
    }

    @Test
    fun `muestra las mascotas y se actualiza en vivo`() = runTest {
        val viewModel = viewModel()

        val second = TestPet.copy(id = "pet-2", name = "Michi")
        petRepository.pets.value = AppResult.Success(listOf(TestPet, second))

        assertEquals(listOf(TestPet, second), viewModel.uiState.value.pets)
    }

    @Test
    fun `un error de lectura se muestra como mensaje`() = runTest {
        petRepository.pets.value = AppResult.Failure(DataError.PermissionDenied)

        viewModel().events.test {
            assertEquals(HomeEvent.ShowMessage(DataError.PermissionDenied.toUiText()), awaitItem())
        }
    }
}
