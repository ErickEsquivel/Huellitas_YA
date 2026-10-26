package com.example.huellitasya.feature.records.list

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.domain.error.DataError
import com.example.huellitasya.core.domain.usecase.ObservePetRecordsUseCase
import com.example.huellitasya.core.domain.usecase.ObservePetUseCase
import com.example.huellitasya.core.domain.usecase.ObserveRecordsUseCase
import com.example.huellitasya.core.model.RecordCategory
import com.example.huellitasya.core.testing.FakeAuthRepository
import com.example.huellitasya.core.testing.FakePetRepository
import com.example.huellitasya.core.testing.FakeRecordRepository
import com.example.huellitasya.core.testing.MainDispatcherRule
import com.example.huellitasya.core.testing.TestPet
import com.example.huellitasya.core.testing.TestRecord
import com.example.huellitasya.core.testing.TestUser
import com.example.huellitasya.core.ui.toUiText
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RecordListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = FakeAuthRepository().apply { currentUser.value = TestUser }
    private val petRepository = FakePetRepository().apply { pets.value = AppResult.Success(listOf(TestPet)) }
    private val recordRepository = FakeRecordRepository()

    private fun viewModel(category: RecordCategory = RecordCategory.VACCINES) = RecordListViewModel(
        savedStateHandle = SavedStateHandle(
            mapOf(RecordListViewModel.PET_ID_ARG to TestPet.id, RecordListViewModel.CATEGORY_ARG to category.name),
        ),
        observePet = ObservePetUseCase(authRepository, petRepository),
        observeRecords = ObserveRecordsUseCase(ObservePetRecordsUseCase(authRepository, recordRepository)),
    )

    @Test
    fun `sin archivos la lista queda vacia y cargada, con la mascota de la cabecera`() = runTest {
        val state = viewModel().uiState.value

        assertEquals(TestPet, state.pet)
        assertTrue(state.records.isEmpty())
        assertFalse(state.isLoading)
    }

    @Test
    fun `solo muestra los archivos de su categoria y se actualiza en vivo`() = runTest {
        val viewModel = viewModel(RecordCategory.HISTORY)
        val history = TestRecord.copy(id = "record-2", category = RecordCategory.HISTORY)

        recordRepository.records.value = AppResult.Success(listOf(TestRecord, history))

        assertEquals(listOf(history), viewModel.uiState.value.records)
    }

    @Test
    fun `un error de lectura se muestra como mensaje`() = runTest {
        recordRepository.records.value = AppResult.Failure(DataError.Network)

        viewModel().events.test {
            assertEquals(RecordListEvent.ShowMessage(DataError.Network.toUiText()), awaitItem())
        }
    }

    @Test
    fun `si la mascota se elimina vuelve a Home`() = runTest {
        val viewModel = viewModel()

        viewModel.events.test {
            petRepository.pets.value = AppResult.Success(emptyList())
            assertEquals(RecordListEvent.PetGone, awaitItem())
        }
    }
}
