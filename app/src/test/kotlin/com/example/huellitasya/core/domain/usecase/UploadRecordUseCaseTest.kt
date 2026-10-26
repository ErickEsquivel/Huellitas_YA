package com.example.huellitasya.core.domain.usecase

import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.domain.error.DataError
import com.example.huellitasya.core.model.RecordCategory
import com.example.huellitasya.core.model.RecordDraft
import com.example.huellitasya.core.model.RecordType
import com.example.huellitasya.core.model.UploadProgress
import com.example.huellitasya.core.testing.FakeAuthRepository
import com.example.huellitasya.core.testing.FakeRecordFileRepository
import com.example.huellitasya.core.testing.FakeRecordRepository
import com.example.huellitasya.core.testing.TestLocalFile
import com.example.huellitasya.core.testing.TestPet
import com.example.huellitasya.core.testing.TestUser
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class UploadRecordUseCaseTest {

    private val authRepository = FakeAuthRepository().apply { currentUser.value = TestUser }
    private val recordRepository = FakeRecordRepository()
    private val fileRepository = FakeRecordFileRepository()
    private val draft = RecordDraft(RecordCategory.VACCINES, LocalDate.of(2026, 9, 10), RecordType.VACCINATION, vet = null)

    private fun TestScope.upload() = UploadRecordUseCase(authRepository, recordRepository, fileRepository, backgroundScope)

    @Test
    fun `sube el archivo, informa el avance y crea el registro con su URL`() = runTest {
        val progress = mutableListOf<Float>()

        val result = upload()(TestPet.id, draft, "content://carnet", TestLocalFile) { progress += it }

        assertEquals(AppResult.Success(Unit), result)
        assertEquals(listOf(0.5f, 1f), progress)
        assertEquals("record-1" to "content://carnet", fileRepository.uploads.single())
        val (recordId, savedDraft, file) = recordRepository.created.single()
        assertEquals("record-1", recordId)
        assertEquals(draft, savedDraft)
        assertEquals(TestLocalFile, file)
        assertEquals("https://storage.test/record", (recordRepository.records.value as AppResult.Success).data.single().fileUrl)
    }

    @Test
    fun `si la subida falla no se crea el registro`() = runTest {
        fileRepository.uploadEvents = listOf(
            AppResult.Success(UploadProgress.InProgress(0.2f)),
            AppResult.Failure(DataError.Network),
        )

        val result = upload()(TestPet.id, draft, "content://carnet", TestLocalFile) {}

        assertEquals(AppResult.Failure(DataError.Network), result)
        assertTrue(recordRepository.created.isEmpty())
    }

    @Test
    fun `si no se puede crear el registro se borra el archivo subido`() = runTest {
        recordRepository.writeResult = AppResult.Failure(DataError.PermissionDenied)

        val result = upload()(TestPet.id, draft, "content://carnet", TestLocalFile) {}

        assertEquals(AppResult.Failure(DataError.PermissionDenied), result)
        assertEquals(listOf("record-1"), fileRepository.deletedFiles)
    }

    @Test
    fun `sin sesion no se sube nada`() = runTest {
        authRepository.currentUser.value = null

        val result = upload()(TestPet.id, draft, "content://carnet", TestLocalFile) {}

        assertEquals(AppResult.Failure(DataError.PermissionDenied), result)
        assertTrue(fileRepository.uploads.isEmpty())
    }
}
