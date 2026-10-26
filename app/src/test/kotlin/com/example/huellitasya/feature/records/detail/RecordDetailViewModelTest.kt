package com.example.huellitasya.feature.records.detail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.huellitasya.R
import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.domain.error.DataError
import com.example.huellitasya.core.domain.usecase.DeleteRecordUseCase
import com.example.huellitasya.core.domain.usecase.DownloadRecordUseCase
import com.example.huellitasya.core.domain.usecase.ObservePetRecordsUseCase
import com.example.huellitasya.core.domain.usecase.ObservePetUseCase
import com.example.huellitasya.core.domain.usecase.ObserveRecordUseCase
import com.example.huellitasya.core.testing.FakeAuthRepository
import com.example.huellitasya.core.testing.FakeNetworkMonitor
import com.example.huellitasya.core.testing.FakePetRepository
import com.example.huellitasya.core.testing.FakeRecordDownloader
import com.example.huellitasya.core.testing.FakeRecordFileRepository
import com.example.huellitasya.core.testing.FakeRecordRepository
import com.example.huellitasya.core.testing.MainDispatcherRule
import com.example.huellitasya.core.testing.TestPet
import com.example.huellitasya.core.testing.TestRecord
import com.example.huellitasya.core.testing.TestUser
import com.example.huellitasya.core.ui.UiText
import com.example.huellitasya.core.ui.toUiText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RecordDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = FakeAuthRepository().apply { currentUser.value = TestUser }
    private val petRepository = FakePetRepository().apply { pets.value = AppResult.Success(listOf(TestPet)) }
    private val recordRepository = FakeRecordRepository().apply { records.value = AppResult.Success(listOf(TestRecord)) }
    private val fileRepository = FakeRecordFileRepository()
    private val downloader = FakeRecordDownloader()
    private val networkMonitor = FakeNetworkMonitor()

    private fun viewModel(appScope: CoroutineScope) = RecordDetailViewModel(
        savedStateHandle = SavedStateHandle(
            mapOf(RecordDetailViewModel.PET_ID_ARG to TestPet.id, RecordDetailViewModel.RECORD_ID_ARG to TestRecord.id),
        ),
        observePet = ObservePetUseCase(authRepository, petRepository),
        observeRecord = ObserveRecordUseCase(ObservePetRecordsUseCase(authRepository, recordRepository)),
        deleteRecord = DeleteRecordUseCase(authRepository, recordRepository, fileRepository, appScope),
        downloadRecord = DownloadRecordUseCase(downloader),
        networkMonitor = networkMonitor,
    )

    @Test
    fun `muestra la mascota y el registro`() = runTest {
        val state = viewModel(this).uiState.value

        assertEquals(TestPet, state.pet)
        assertEquals(TestRecord, state.record)
    }

    @Test
    fun `descargar encola el archivo y avisa`() = runTest {
        val viewModel = viewModel(this)

        viewModel.events.test {
            viewModel.onAction(RecordDetailAction.Download)

            assertEquals(
                RecordDetailEvent.ShowMessage(UiText.Resource(R.string.records_download_started, TestRecord.fileName)),
                awaitItem(),
            )
        }
        assertEquals(listOf(TestRecord.fileUrl to TestRecord.fileName), downloader.downloads)
    }

    @Test
    fun `sin conexion no descarga`() = runTest {
        networkMonitor.isOnline.value = false
        val viewModel = viewModel(this)

        viewModel.events.test {
            viewModel.onAction(RecordDetailAction.Download)
            assertEquals(RecordDetailEvent.ShowMessage(DataError.Network.toUiText()), awaitItem())
        }
        assertTrue(downloader.downloads.isEmpty())
    }

    @Test
    fun `eliminar pide confirmacion, borra archivo y documento y vuelve una sola vez`() = runTest {
        val viewModel = viewModel(this)

        viewModel.onAction(RecordDetailAction.DeleteClick)
        assertTrue(viewModel.uiState.value.showDeleteDialog)

        viewModel.events.test {
            viewModel.onAction(RecordDetailAction.ConfirmDelete)
            testScheduler.advanceUntilIdle()

            assertEquals(RecordDetailEvent.Deleted, awaitItem())
            expectNoEvents()
        }
        assertEquals(listOf(TestRecord.id), fileRepository.deletedFiles)
        assertEquals(listOf(TestRecord.id), recordRepository.deleted)
    }

    @Test
    fun `si falla el borrado avisa y sigue en el detalle`() = runTest {
        fileRepository.deleteResult = AppResult.Failure(DataError.PermissionDenied)
        val viewModel = viewModel(this)

        viewModel.events.test {
            viewModel.onAction(RecordDetailAction.ConfirmDelete)
            testScheduler.advanceUntilIdle()

            assertEquals(RecordDetailEvent.ShowMessage(DataError.PermissionDenied.toUiText()), awaitItem())
        }
        assertFalse(viewModel.uiState.value.isDeleting)
        assertEquals(TestRecord, viewModel.uiState.value.record)
    }

    @Test
    fun `si el registro se elimina en otro lado vuelve a la lista`() = runTest {
        val viewModel = viewModel(this)

        viewModel.events.test {
            recordRepository.records.value = AppResult.Success(emptyList())
            assertEquals(RecordDetailEvent.Deleted, awaitItem())
        }
    }

    @Test
    fun `si la mascota se elimina vuelve a Home`() = runTest {
        val viewModel = viewModel(this)

        viewModel.events.test {
            petRepository.pets.value = AppResult.Success(emptyList())
            assertEquals(RecordDetailEvent.PetGone, awaitItem())
        }
    }
}
