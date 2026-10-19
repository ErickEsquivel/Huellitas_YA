package com.example.huellitasya.core.domain.usecase

import app.cash.turbine.test
import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.domain.error.DataError
import com.example.huellitasya.core.testing.FakeAuthRepository
import com.example.huellitasya.core.testing.FakePetPhotoRepository
import com.example.huellitasya.core.testing.FakePetRepository
import com.example.huellitasya.core.testing.TestPet
import com.example.huellitasya.core.testing.TestPetDraft
import com.example.huellitasya.core.testing.TestUser
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PetUseCasesTest {

    private val authRepository = FakeAuthRepository()
    private val petRepository = FakePetRepository()
    private val photoRepository = FakePetPhotoRepository()

    @Test
    fun `sin sesion no se observan mascotas`() = runTest {
        ObservePetsUseCase(authRepository, petRepository)().test {
            expectNoEvents()
        }
    }

    @Test
    fun `con sesion emite las mascotas del usuario`() = runTest {
        authRepository.currentUser.value = TestUser
        petRepository.pets.value = AppResult.Success(listOf(TestPet))

        ObservePetsUseCase(authRepository, petRepository)().test {
            assertEquals(AppResult.Success(listOf(TestPet)), awaitItem())
        }
    }

    @Test
    fun `observar una mascota la emite y avisa si se elimino`() = runTest {
        authRepository.currentUser.value = TestUser
        petRepository.pets.value = AppResult.Success(listOf(TestPet))

        ObservePetUseCase(authRepository, petRepository)(TestPet.id).test {
            assertEquals(AppResult.Success(TestPet), awaitItem())

            petRepository.pets.value = AppResult.Success(emptyList())
            assertEquals(AppResult.Failure(DataError.NotFound), awaitItem())
        }
    }

    @Test
    fun `registrar sin foto crea el documento sin photoUrl`() = runTest {
        authRepository.currentUser.value = TestUser

        val result = registerPet()(TestPetDraft, photoUri = null)

        assertEquals(AppResult.Success(Unit), result)
        assertTrue(photoRepository.uploads.isEmpty())
        val created = petRepository.created.single()
        assertEquals(TestUser.uid, created.uid)
        assertEquals(TestPetDraft, created.draft)
        assertEquals(null, created.photoUrl)
    }

    @Test
    fun `registrar con foto la sube con el mismo id y guarda su URL`() = runTest {
        authRepository.currentUser.value = TestUser

        registerPet()(TestPetDraft, photoUri = "content://foto")

        val (uid, petId, uri) = photoRepository.uploads.single()
        val created = petRepository.created.single()
        assertEquals(TestUser.uid, uid)
        assertEquals(created.petId, petId)
        assertEquals("content://foto", uri)
        assertEquals("https://storage.test/pet.jpg", created.photoUrl)
    }

    @Test
    fun `si la foto falla no se crea la mascota`() = runTest {
        authRepository.currentUser.value = TestUser
        photoRepository.uploadResult = AppResult.Failure(DataError.FileTooLarge)

        val result = registerPet()(TestPetDraft, photoUri = "content://foto")

        assertEquals(AppResult.Failure(DataError.FileTooLarge), result)
        assertTrue(petRepository.created.isEmpty())
    }

    @Test
    fun `sin sesion no se registra nada`() = runTest {
        val result = registerPet()(TestPetDraft, photoUri = null)

        assertEquals(AppResult.Failure(DataError.PermissionDenied), result)
        assertTrue(petRepository.created.isEmpty())
    }

    private fun TestScope.registerPet() =
        RegisterPetUseCase(authRepository, petRepository, photoRepository, backgroundScope)
}
