package com.example.huellitasya.core.domain.usecase

import app.cash.turbine.test
import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.domain.error.AuthError
import com.example.huellitasya.core.domain.error.DataError
import com.example.huellitasya.core.model.SessionState
import com.example.huellitasya.core.testing.FakeAuthRepository
import com.example.huellitasya.core.testing.FakeUserRepository
import com.example.huellitasya.core.testing.TestUser
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthUseCasesTest {

    private val authRepository = FakeAuthRepository()
    private val userRepository = FakeUserRepository()

    @Test
    fun `login exitoso recorta el correo y asegura el perfil`() = runTest {
        val signIn = SignInWithEmailUseCase(authRepository, userRepository, backgroundScope)

        val result = signIn("  sa.company@hotmail.com ", "secreto")

        assertEquals(AppResult.Success(TestUser), result)
        assertEquals("sa.company@hotmail.com" to "secreto", authRepository.signInCalls.single())
        assertEquals(listOf(TestUser), userRepository.createdUsers)
        assertEquals(listOf(TestUser.uid to TestUser.email), userRepository.syncedEmails)
    }

    @Test
    fun `login fallido no crea perfil`() = runTest {
        authRepository.signInResult = AppResult.Failure(AuthError.InvalidCredentials)
        val signIn = SignInWithEmailUseCase(authRepository, userRepository, backgroundScope)

        val result = signIn("sa.company@hotmail.com", "mala")

        assertEquals(AppResult.Failure(AuthError.InvalidCredentials), result)
        assertTrue(userRepository.createdUsers.isEmpty())
    }

    @Test
    fun `registro crea el perfil con el nombre recortado`() = runTest {
        val register = RegisterUseCase(authRepository, userRepository, backgroundScope)

        register(" Santiago Cruz ", "sa.company@hotmail.com", "Huellitas1")

        assertEquals(Triple("Santiago Cruz", "sa.company@hotmail.com", "Huellitas1"), authRepository.registerCalls.single())
        assertEquals(1, userRepository.createdUsers.size)
    }

    @Test
    fun `si falla crear el perfil el registro sigue siendo exitoso`() = runTest {
        userRepository.result = AppResult.Failure(DataError.Network)
        val register = RegisterUseCase(authRepository, userRepository, backgroundScope)

        val result = register("Santiago", "sa.company@hotmail.com", "Huellitas1")

        assertTrue(result is AppResult.Success)
    }

    @Test
    fun `google autentica con el token y asegura el perfil`() = runTest {
        val signInWithGoogle = SignInWithGoogleUseCase(authRepository, userRepository, backgroundScope)

        signInWithGoogle("id-token")

        assertEquals(listOf("id-token"), authRepository.googleTokens)
        assertEquals(1, userRepository.createdUsers.size)
    }

    @Test
    fun `la sesion refleja el usuario autenticado`() = runTest {
        val observeSession = ObserveSessionUseCase(authRepository)

        observeSession().test {
            assertEquals(SessionState.SignedOut, awaitItem())
            authRepository.currentUser.value = TestUser
            assertEquals(SessionState.SignedIn(TestUser), awaitItem())
            SignOutUseCase(authRepository)()
            assertEquals(SessionState.SignedOut, awaitItem())
        }
    }
}
