package com.example.huellitasya.core.domain.usecase

import app.cash.turbine.test
import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.domain.error.DataError
import com.example.huellitasya.core.model.RecordCategory
import com.example.huellitasya.core.testing.FakeAuthRepository
import com.example.huellitasya.core.testing.FakeRecordRepository
import com.example.huellitasya.core.testing.TestPet
import com.example.huellitasya.core.testing.TestRecord
import com.example.huellitasya.core.testing.TestUser
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RecordUseCasesTest {

    private val authRepository = FakeAuthRepository()
    private val recordRepository = FakeRecordRepository()
    private val observePetRecords = ObservePetRecordsUseCase(authRepository, recordRepository)

    private val history = TestRecord.copy(id = "record-2", category = RecordCategory.HISTORY, fileName = "Historial.pdf")
    private val vaccine2 = TestRecord.copy(id = "record-3", fileName = "Polivalente.pdf")

    @Test
    fun `sin sesion no se observan registros`() = runTest {
        observePetRecords(TestPet.id).test {
            expectNoEvents()
        }
    }

    @Test
    fun `la lista de una categoria solo trae sus registros`() = runTest {
        authRepository.currentUser.value = TestUser
        recordRepository.records.value = AppResult.Success(listOf(TestRecord, history, vaccine2))

        ObserveRecordsUseCase(observePetRecords)(TestPet.id, RecordCategory.VACCINES).test {
            assertEquals(AppResult.Success(listOf(TestRecord, vaccine2)), awaitItem())
        }
    }

    @Test
    fun `cuenta los archivos de cada categoria, con cero en las vacias`() = runTest {
        authRepository.currentUser.value = TestUser
        recordRepository.records.value = AppResult.Success(listOf(TestRecord, history, vaccine2))

        ObserveRecordCountsUseCase(observePetRecords)(TestPet.id).test {
            val expected = mapOf(RecordCategory.VACCINES to 2, RecordCategory.HISTORY to 1, RecordCategory.FILES to 0)
            assertEquals(AppResult.Success(expected), awaitItem())
        }
    }

    @Test
    fun `los errores de lectura se propagan`() = runTest {
        authRepository.currentUser.value = TestUser
        recordRepository.records.value = AppResult.Failure(DataError.PermissionDenied)

        ObserveRecordCountsUseCase(observePetRecords)(TestPet.id).test {
            assertEquals(AppResult.Failure(DataError.PermissionDenied), awaitItem())
        }
    }
}
