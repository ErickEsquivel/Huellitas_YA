package com.example.huellitasya.core.domain.usecase

import app.cash.turbine.test
import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.domain.error.DataError
import com.example.huellitasya.core.model.Field
import com.example.huellitasya.core.model.ProfileChanges
import com.example.huellitasya.core.domain.error.AuthError
import com.example.huellitasya.core.domain.error.EmailChangeError
import com.example.huellitasya.core.testing.FakeAccountRepository
import com.example.huellitasya.core.testing.FakeAuthRepository
import com.example.huellitasya.core.testing.FakeEmailChangeRepository
import com.example.huellitasya.core.testing.FakeProfilePhotoRepository
import com.example.huellitasya.core.testing.FakeUserRepository
import com.example.huellitasya.core.testing.TestProfile
import com.example.huellitasya.core.testing.TestUser
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileUseCasesTest {

    private val authRepository = FakeAuthRepository()
    private val userRepository = FakeUserRepository()

    @Test
    fun `sin sesion no se observa ningun perfil`() = runTest {
        ObserveUserProfileUseCase(authRepository, userRepository)().test {
            expectNoEvents()
        }
    }

    @Test
    fun `con sesion emite el perfil de Firestore`() = runTest {
        authRepository.currentUser.value = TestUser

        ObserveUserProfileUseCase(authRepository, userRepository)().test {
            assertEquals(AppResult.Success(TestProfile), awaitItem())
        }
    }

    @Test
    fun `si el documento aun no existe se muestran los datos de la sesion`() = runTest {
        authRepository.currentUser.value = TestUser.copy(name = "Nombre de Auth")
        userRepository.profile.value = AppResult.Failure(DataError.NotFound)

        ObserveUserProfileUseCase(authRepository, userRepository)().test {
            val profile = (awaitItem() as AppResult.Success).data
            assertEquals("Nombre de Auth", profile.name)
            assertEquals(null, profile.phone)
        }
    }

    @Test
    fun `otros errores de lectura se propagan`() = runTest {
        authRepository.currentUser.value = TestUser
        userRepository.profile.value = AppResult.Failure(DataError.PermissionDenied)

        ObserveUserProfileUseCase(authRepository, userRepository)().test {
            assertEquals(AppResult.Failure(DataError.PermissionDenied), awaitItem())
        }
    }

    @Test
    fun `cambiar el nombre tambien lo actualiza en Auth`() = runTest {
        val update = UpdateUserProfileUseCase(authRepository, userRepository)

        val result = update(TestProfile.uid, ProfileChanges(name = "Santiago C."))

        assertEquals(AppResult.Success(Unit), result)
        assertEquals(listOf("Santiago C."), authRepository.displayNames)
    }

    @Test
    fun `cambiar solo el telefono no toca Auth`() = runTest {
        val update = UpdateUserProfileUseCase(authRepository, userRepository)

        update(TestProfile.uid, ProfileChanges(phone = Field.Set("+573124456311")))

        assertEquals(TestProfile.uid to ProfileChanges(phone = Field.Set("+573124456311")), userRepository.updates.single())
        assertTrue(authRepository.displayNames.isEmpty())
    }

    @Test
    fun `sin cambios no se escribe nada`() = runTest {
        val result = UpdateUserProfileUseCase(authRepository, userRepository)(TestProfile.uid, ProfileChanges())

        assertEquals(AppResult.Success(Unit), result)
        assertTrue(userRepository.updates.isEmpty())
    }

    @Test
    fun `si falla Firestore no se actualiza el nombre en Auth`() = runTest {
        userRepository.updateResult = AppResult.Failure(DataError.Network)

        val result = UpdateUserProfileUseCase(authRepository, userRepository)(TestProfile.uid, ProfileChanges(name = "Otro"))

        assertEquals(AppResult.Failure(DataError.Network), result)
        assertTrue(authRepository.displayNames.isEmpty())
    }

    @Test
    fun `cambiar la foto la sube y guarda la URL en el perfil`() = runTest {
        val photoRepository = FakeProfilePhotoRepository()
        val changePhoto = ChangeProfilePhotoUseCase(photoRepository, userRepository, backgroundScope)

        val result = changePhoto(TestProfile.uid, "content://media/1")

        assertEquals(AppResult.Success(Unit), result)
        assertEquals(TestProfile.uid to "content://media/1", photoRepository.uploads.single())
        assertEquals(listOf("https://storage.test/users/uid-123/profile.jpg"), userRepository.photoUrls)
    }

    @Test
    fun `si la subida falla no se toca el perfil`() = runTest {
        val photoRepository = FakeProfilePhotoRepository().apply { result = AppResult.Failure(DataError.FileTooLarge) }
        val changePhoto = ChangeProfilePhotoUseCase(photoRepository, userRepository, backgroundScope)

        val result = changePhoto(TestProfile.uid, "content://media/1")

        assertEquals(AppResult.Failure(DataError.FileTooLarge), result)
        assertTrue(userRepository.photoUrls.isEmpty())
    }

    @Test
    fun `eliminar la cuenta cierra la sesion local`() = runTest {
        authRepository.currentUser.value = TestUser
        val accountRepository = FakeAccountRepository()

        val result = DeleteAccountUseCase(accountRepository, authRepository, backgroundScope)()

        assertEquals(AppResult.Success(Unit), result)
        assertEquals(1, accountRepository.deleteCount)
        assertEquals(1, authRepository.signOutCount)
    }

    @Test
    fun `si el backend falla la sesion sigue abierta`() = runTest {
        authRepository.currentUser.value = TestUser
        val accountRepository = FakeAccountRepository().apply { result = AppResult.Failure(AuthError.Network) }

        val result = DeleteAccountUseCase(accountRepository, authRepository, backgroundScope)()

        assertEquals(AppResult.Failure(AuthError.Network), result)
        assertEquals(0, authRepository.signOutCount)
        assertEquals(TestUser, authRepository.currentUser.value)
    }

    @Test
    fun `confirmar el correo recarga la sesion para tener el correo nuevo`() = runTest {
        val emailChangeRepository = FakeEmailChangeRepository()

        val result = ConfirmEmailChangeUseCase(emailChangeRepository, authRepository)("4821")

        assertEquals(AppResult.Success("nuevo@correo.com"), result)
        assertEquals(listOf("4821"), emailChangeRepository.confirmedCodes)
        assertEquals(1, authRepository.reloadCount)
    }

    @Test
    fun `un codigo incorrecto no recarga la sesion`() = runTest {
        val emailChangeRepository = FakeEmailChangeRepository().apply {
            confirmResult = AppResult.Failure(EmailChangeError.InvalidCode)
        }

        val result = ConfirmEmailChangeUseCase(emailChangeRepository, authRepository)("0000")

        assertEquals(AppResult.Failure(EmailChangeError.InvalidCode), result)
        assertEquals(0, authRepository.reloadCount)
    }

    @Test
    fun `pedir el codigo recorta el correo`() = runTest {
        val emailChangeRepository = FakeEmailChangeRepository()

        RequestEmailChangeCodeUseCase(emailChangeRepository)("  nuevo@correo.com ")

        assertEquals(listOf("nuevo@correo.com"), emailChangeRepository.requestedEmails)
    }

    @Test
    fun `cambiar la contrasena reautentica, la cambia y envia el aviso`() = runTest {
        val accountRepository = FakeAccountRepository()

        val result = ChangePasswordUseCase(authRepository, accountRepository)("Actual1", "Nueva2025")

        assertEquals(AppResult.Success(Unit), result)
        assertEquals(listOf("Actual1"), authRepository.reauthPasswords)
        assertEquals(listOf("Nueva2025"), authRepository.updatedPasswords)
        assertEquals(1, accountRepository.passwordNoticeCount)
    }

    @Test
    fun `con la contrasena actual incorrecta no se cambia nada`() = runTest {
        authRepository.reauthResult = AppResult.Failure(AuthError.InvalidCredentials)
        val accountRepository = FakeAccountRepository()

        val result = ChangePasswordUseCase(authRepository, accountRepository)("Mala", "Nueva2025")

        assertEquals(AppResult.Failure(AuthError.InvalidCredentials), result)
        assertTrue(authRepository.updatedPasswords.isEmpty())
        assertEquals(0, accountRepository.passwordNoticeCount)
    }

    @Test
    fun `si el aviso no sale el cambio de contrasena sigue siendo valido`() = runTest {
        val accountRepository = FakeAccountRepository().apply { notifyResult = AppResult.Failure(AuthError.Network) }

        val result = ChangePasswordUseCase(authRepository, accountRepository)("Actual1", "Nueva2025")

        assertEquals(AppResult.Success(Unit), result)
    }
}
