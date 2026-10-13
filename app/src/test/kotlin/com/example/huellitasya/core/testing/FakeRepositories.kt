package com.example.huellitasya.core.testing

import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.domain.error.AuthError
import com.example.huellitasya.core.domain.error.DataError
import com.example.huellitasya.core.domain.repository.AuthRepository
import com.example.huellitasya.core.domain.repository.UserRepository
import com.example.huellitasya.core.model.AuthProvider
import com.example.huellitasya.core.model.User
import kotlinx.coroutines.flow.MutableStateFlow

val TestUser = User(
    uid = "uid-123",
    name = "Santiago Cruz",
    email = "sa.company@hotmail.com",
    photoUrl = null,
    provider = AuthProvider.EMAIL,
)

/**
 * [AuthRepository] en memoria. Cada operación devuelve el resultado configurado y, si es
 * exitosa, actualiza [currentUser] igual que lo haría Firebase.
 */
class FakeAuthRepository : AuthRepository {
    override val currentUser = MutableStateFlow<User?>(null)

    var signInResult: AppResult<User, AuthError> = AppResult.Success(TestUser)
    var registerResult: AppResult<User, AuthError> = AppResult.Success(TestUser)
    var googleResult: AppResult<User, AuthError> = AppResult.Success(TestUser.copy(provider = AuthProvider.GOOGLE))

    val signInCalls = mutableListOf<Pair<String, String>>()
    val registerCalls = mutableListOf<Triple<String, String, String>>()
    val googleTokens = mutableListOf<String>()
    var signOutCount = 0
        private set

    override suspend fun signInWithEmail(email: String, password: String): AppResult<User, AuthError> {
        signInCalls += email to password
        return signInResult.alsoSignIn()
    }

    override suspend fun registerWithEmail(name: String, email: String, password: String): AppResult<User, AuthError> {
        registerCalls += Triple(name, email, password)
        return registerResult.alsoSignIn()
    }

    override suspend fun signInWithGoogle(idToken: String): AppResult<User, AuthError> {
        googleTokens += idToken
        return googleResult.alsoSignIn()
    }

    override suspend fun signOut() {
        signOutCount++
        currentUser.value = null
    }

    private fun AppResult<User, AuthError>.alsoSignIn(): AppResult<User, AuthError> {
        if (this is AppResult.Success) currentUser.value = data
        return this
    }
}

class FakeUserRepository : UserRepository {
    var result: AppResult<Unit, DataError> = AppResult.Success(Unit)
    val createdUsers = mutableListOf<User>()

    override suspend fun createIfMissing(user: User): AppResult<Unit, DataError> {
        createdUsers += user
        return result
    }
}
