package com.example.huellitasya.core.testing

import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.domain.error.AuthError
import com.example.huellitasya.core.domain.error.DataError
import com.example.huellitasya.core.domain.error.EmailChangeError
import com.example.huellitasya.core.domain.repository.AccountRepository
import com.example.huellitasya.core.domain.repository.AuthRepository
import com.example.huellitasya.core.domain.repository.ConfirmedEmailChange
import com.example.huellitasya.core.domain.repository.EmailChangeRepository
import com.example.huellitasya.core.domain.repository.ProfilePhotoRepository
import com.example.huellitasya.core.domain.repository.UserRepository
import com.example.huellitasya.core.model.AuthProvider
import com.example.huellitasya.core.model.Field
import com.example.huellitasya.core.model.ProfileChanges
import com.example.huellitasya.core.model.User
import com.example.huellitasya.core.model.UserProfile
import kotlinx.coroutines.flow.Flow
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

    var reauthResult: AppResult<Unit, AuthError> = AppResult.Success(Unit)
    val reauthPasswords = mutableListOf<String>()
    val reauthGoogleTokens = mutableListOf<String>()

    override suspend fun reauthenticateWithPassword(password: String): AppResult<Unit, AuthError> {
        reauthPasswords += password
        return reauthResult
    }

    override suspend fun reauthenticateWithGoogle(idToken: String): AppResult<Unit, AuthError> {
        reauthGoogleTokens += idToken
        return reauthResult
    }

    var updatePasswordResult: AppResult<Unit, AuthError> = AppResult.Success(Unit)
    val updatedPasswords = mutableListOf<String>()
    val customTokens = mutableListOf<String>()

    override suspend fun updatePassword(newPassword: String): AppResult<Unit, AuthError> {
        updatedPasswords += newPassword
        return updatePasswordResult
    }

    override suspend fun signInWithCustomToken(token: String): AppResult<User, AuthError> {
        customTokens += token
        val user = (currentUser.value ?: TestUser)
        currentUser.value = user
        return AppResult.Success(user)
    }

    var updateDisplayNameResult: AppResult<Unit, AuthError> = AppResult.Success(Unit)
    val displayNames = mutableListOf<String>()

    override suspend fun updateDisplayName(name: String): AppResult<Unit, AuthError> {
        displayNames += name
        return updateDisplayNameResult
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

val TestProfile = UserProfile(
    uid = TestUser.uid,
    name = TestUser.name,
    email = TestUser.email,
    phone = null,
    address = null,
    birthDate = null,
    photoUrl = null,
    provider = AuthProvider.EMAIL,
)

/**
 * [UserRepository] en memoria. [profile] hace de documento `users/{uid}`: `updateProfile`
 * lo modifica y lo emite, igual que el snapshot listener de Firestore.
 */
class FakeUserRepository : UserRepository {
    var result: AppResult<Unit, DataError> = AppResult.Success(Unit)
    val createdUsers = mutableListOf<User>()

    val profile = MutableStateFlow<AppResult<UserProfile, DataError>>(AppResult.Success(TestProfile))
    var updateResult: AppResult<Unit, DataError> = AppResult.Success(Unit)
    val updates = mutableListOf<Pair<String, ProfileChanges>>()
    val syncedEmails = mutableListOf<Pair<String, String>>()

    override suspend fun createIfMissing(user: User): AppResult<Unit, DataError> {
        createdUsers += user
        return result
    }

    override fun observeProfile(uid: String): Flow<AppResult<UserProfile, DataError>> = profile

    override suspend fun updateProfile(uid: String, changes: ProfileChanges): AppResult<Unit, DataError> {
        updates += uid to changes
        val current = (profile.value as? AppResult.Success)?.data
        if (updateResult is AppResult.Success && current != null) {
            profile.value = AppResult.Success(current.applying(changes))
        }
        return updateResult
    }

    val photoUrls = mutableListOf<String>()

    override suspend fun updatePhotoUrl(uid: String, url: String): AppResult<Unit, DataError> {
        photoUrls += url
        val current = (profile.value as? AppResult.Success)?.data
        if (updateResult is AppResult.Success && current != null) {
            profile.value = AppResult.Success(current.copy(photoUrl = url))
        }
        return updateResult
    }

    override suspend fun syncEmail(uid: String, email: String): AppResult<Unit, DataError> {
        syncedEmails += uid to email
        return AppResult.Success(Unit)
    }

    private fun UserProfile.applying(changes: ProfileChanges) = copy(
        name = changes.name ?: name,
        phone = changes.phone.applyTo(phone),
        address = changes.address.applyTo(address),
        birthDate = changes.birthDate.applyTo(birthDate),
    )

    private fun <T> Field<T>?.applyTo(current: T?): T? = when (this) {
        null -> current
        is Field.Set -> value
        Field.Clear -> null
    }
}

class FakeProfilePhotoRepository : ProfilePhotoRepository {
    var result: AppResult<String, DataError> = AppResult.Success("https://storage.test/users/uid-123/profile.jpg")
    val uploads = mutableListOf<Pair<String, String>>()

    override suspend fun upload(uid: String, localUri: String): AppResult<String, DataError> {
        uploads += uid to localUri
        return result
    }
}

class FakeAccountRepository : AccountRepository {
    var result: AppResult<Unit, AuthError> = AppResult.Success(Unit)
    var deleteCount = 0
        private set

    override suspend fun deleteAccount(): AppResult<Unit, AuthError> {
        deleteCount++
        return result
    }

    var notifyResult: AppResult<Unit, AuthError> = AppResult.Success(Unit)
    var passwordNoticeCount = 0
        private set

    override suspend fun notifyPasswordChanged(): AppResult<Unit, AuthError> {
        passwordNoticeCount++
        return notifyResult
    }
}

class FakeEmailChangeRepository : EmailChangeRepository {
    /** Resultados en orden para cada pedido de código; el último se repite. */
    var requestResults: List<AppResult<Unit, EmailChangeError>> = listOf(AppResult.Success(Unit))
    val requestedEmails = mutableListOf<String>()
    var confirmResult: AppResult<ConfirmedEmailChange, EmailChangeError> =
        AppResult.Success(ConfirmedEmailChange("nuevo@correo.com", "custom-token"))
    val confirmedCodes = mutableListOf<String>()

    override suspend fun requestCode(newEmail: String): AppResult<Unit, EmailChangeError> {
        requestedEmails += newEmail
        return requestResults[minOf(requestedEmails.size, requestResults.size) - 1]
    }

    override suspend fun confirm(code: String): AppResult<ConfirmedEmailChange, EmailChangeError> {
        confirmedCodes += code
        return confirmResult
    }
}
