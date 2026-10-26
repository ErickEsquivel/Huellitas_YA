package com.example.huellitasya.feature.records.upload

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.domain.error.DataError
import com.example.huellitasya.core.domain.error.ValidationError
import com.example.huellitasya.core.domain.usecase.InspectRecordFileUseCase
import com.example.huellitasya.core.domain.usecase.ObservePetUseCase
import com.example.huellitasya.core.domain.usecase.UploadRecordUseCase
import com.example.huellitasya.core.domain.validation.ValidateRecordFileUseCase
import com.example.huellitasya.core.domain.validation.ValidateRecordFormUseCase
import com.example.huellitasya.core.model.LocalFileInfo
import com.example.huellitasya.core.model.RecordCategory
import com.example.huellitasya.core.model.RecordType
import com.example.huellitasya.core.testing.FakeAuthRepository
import com.example.huellitasya.core.testing.FakeNetworkMonitor
import com.example.huellitasya.core.testing.FakePetRepository
import com.example.huellitasya.core.testing.FakeRecordFileRepository
import com.example.huellitasya.core.testing.FakeRecordRepository
import com.example.huellitasya.core.testing.MainDispatcherRule
import com.example.huellitasya.core.testing.TestLocalFile
import com.example.huellitasya.core.testing.TestPet
import com.example.huellitasya.core.testing.TestUser
import com.example.huellitasya.core.ui.toFieldFeedback
import com.example.huellitasya.core.ui.toUiText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class UploadRecordViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = FakeAuthRepository().apply { currentUser.value = TestUser }
    private val petRepository = FakePetRepository().apply { pets.value = AppResult.Success(listOf(TestPet)) }
    private val recordRepository = FakeRecordRepository()
    private val fileRepository = FakeRecordFileRepository()
    private val networkMonitor = FakeNetworkMonitor()

    private fun viewModel(appScope: CoroutineScope, category: RecordCategory = RecordCategory.VACCINES) = UploadRecordViewModel(
        savedStateHandle = SavedStateHandle(
            mapOf(UploadRecordViewModel.PET_ID_ARG to TestPet.id, UploadRecordViewModel.CATEGORY_ARG to category.name),
        ),
        observePet = ObservePetUseCase(authRepository, petRepository),
        inspectFile = InspectRecordFileUseCase(fileRepository),
        uploadRecord = UploadRecordUseCase(authRepository, recordRepository, fileRepository, appScope),
        validateFile = ValidateRecordFileUseCase(),
        validateForm = ValidateRecordFormUseCase(),
        networkMonitor = networkMonitor,
    )

    @Test
    fun `el tipo por defecto depende de la categoria`() = runTest {
        assertEquals(RecordType.VACCINATION, viewModel(this, RecordCategory.VACCINES).uiState.value.type)
        assertEquals(RecordType.CONSULTATION, viewModel(this, RecordCategory.HISTORY).uiState.value.type)
        assertEquals(RecordType.OTHER, viewModel(this, RecordCategory.FILES).uiState.value.type)
    }

    @Test
    fun `elegir un archivo valido lo muestra con el formulario`() = runTest {
        val viewModel = viewModel(this)

        viewModel.onAction(UploadRecordAction.FilePicked("content://carnet"))

        assertEquals("content://carnet", viewModel.uiState.value.localUri)
        assertEquals(TestLocalFile, viewModel.uiState.value.file)
    }

    @Test
    fun `un archivo de otro tipo se rechaza con su aviso`() = runTest {
        fileRepository.inspectResult = AppResult.Success(LocalFileInfo("notas.docx", "application/msword", 1_000))
        val viewModel = viewModel(this)

        viewModel.events.test {
            viewModel.onAction(UploadRecordAction.FilePicked("content://notas"))
            assertEquals(UploadRecordEvent.ShowMessage(ValidationError.UnsupportedFileType.toUiText()), awaitItem())
        }
        assertNull(viewModel.uiState.value.file)
    }

    @Test
    fun `un veterinario demasiado largo muestra su aviso y no sube`() = runTest {
        val viewModel = viewModel(this)
        viewModel.onAction(UploadRecordAction.FilePicked("content://carnet"))
        viewModel.onAction(UploadRecordAction.VetChanged("a".repeat(51)))

        viewModel.onAction(UploadRecordAction.Upload)

        assertEquals(ValidationError.VetTooLong.toFieldFeedback(), viewModel.uiState.value.vetFeedback)
        assertTrue(fileRepository.uploads.isEmpty())
    }

    @Test
    fun `subir guarda el registro con los datos del formulario y vuelve a la lista`() = runTest {
        val viewModel = viewModel(this)
        viewModel.onAction(UploadRecordAction.FilePicked("content://carnet"))
        viewModel.onAction(UploadRecordAction.DateChanged(LocalDate.of(2026, 9, 10)))
        viewModel.onAction(UploadRecordAction.TypeChanged(RecordType.EXAM))
        viewModel.onAction(UploadRecordAction.VetChanged("  Dr. Pérez "))

        viewModel.events.test {
            viewModel.onAction(UploadRecordAction.Upload)
            testScheduler.advanceUntilIdle()

            assertEquals(UploadRecordEvent.Uploaded, awaitItem())
        }
        val draft = recordRepository.created.single().second
        assertEquals(RecordCategory.VACCINES, draft.category)
        assertEquals(LocalDate.of(2026, 9, 10), draft.date)
        assertEquals(RecordType.EXAM, draft.type)
        assertEquals("Dr. Pérez", draft.vet)
        // La barra sigue visible hasta que la pantalla se cierra.
        assertEquals(1f, viewModel.uiState.value.progress)
    }

    @Test
    fun `sin conexion avisa y no sube`() = runTest {
        networkMonitor.isOnline.value = false
        val viewModel = viewModel(this)
        viewModel.onAction(UploadRecordAction.FilePicked("content://carnet"))

        viewModel.events.test {
            viewModel.onAction(UploadRecordAction.Upload)
            testScheduler.advanceUntilIdle()

            assertEquals(UploadRecordEvent.ShowMessage(DataError.Network.toUiText()), awaitItem())
        }
        assertTrue(fileRepository.uploads.isEmpty())
        assertFalse(viewModel.uiState.value.isUploading)
    }
}
