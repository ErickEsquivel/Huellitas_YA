package com.example.huellitasya.core.data.repository

import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.data.media.FirestoreMediaStore
import com.example.huellitasya.core.domain.error.AuthError
import com.example.huellitasya.core.domain.repository.AccountRepository
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Modo gratuito de [AccountRepository] (spec 008): sin la Cloud Function `deleteAccount`, la app
 * borra desde el cliente lo que el usuario tiene en Firestore y después su usuario de Auth.
 *
 * El cliente no puede borrar una subcolección de una vez, así que se recorre: archivos guardados en
 * Firestore, recordatorios, registros de cada mascota, mascotas y al final `users/{uid}`. Es
 * idempotente: al reintentar, lo que ya no existe simplemente no aparece en las consultas.
 *
 * Los archivos de Storage no se tocan: en el plan Spark Storage no responde (spec 008).
 */
internal class ClientAccountRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val mediaStore: FirestoreMediaStore,
) : AccountRepository {

    override suspend fun deleteAccount(): AppResult<Unit, AuthError> {
        val user = auth.currentUser ?: return AppResult.Failure(AuthError.RequiresRecentLogin)
        return try {
            deleteUserData(user.uid)
            user.delete().await()
            AppResult.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (_: FirebaseAuthRecentLoginRequiredException) {
            AppResult.Failure(AuthError.RequiresRecentLogin)
        } catch (_: FirebaseNetworkException) {
            AppResult.Failure(AuthError.Network)
        } catch (e: FirebaseFirestoreException) {
            AppResult.Failure(if (e.code == FirebaseFirestoreException.Code.UNAVAILABLE) AuthError.Network else AuthError.Unknown)
        } catch (_: Exception) {
            AppResult.Failure(AuthError.Unknown)
        }
    }

    /** Sin Cloud Functions no hay quien envíe el aviso por correo: no se envía (spec 008). */
    override suspend fun notifyPasswordChanged(): AppResult<Unit, AuthError> = AppResult.Success(Unit)

    private suspend fun deleteUserData(uid: String) {
        mediaStore.deleteAll(uid)
        val userDocument = firestore.collection(USERS).document(uid)
        val documents = buildList {
            addAll(userDocument.collection(REMINDERS).get().await().documents.map { it.reference })
            for (pet in userDocument.collection(PETS).get().await().documents) {
                addAll(pet.reference.collection(RECORDS).get().await().documents.map { it.reference })
                add(pet.reference)
            }
            add(userDocument)
        }
        deleteInBatches(documents)
    }

    /** En orden y en lotes: `users/{uid}` va al final, así un fallo a la mitad deja la cuenta reintentable. */
    private suspend fun deleteInBatches(documents: List<DocumentReference>) {
        documents.chunked(MAX_BATCH_WRITES).forEach { chunk ->
            firestore.batch().apply { chunk.forEach(::delete) }.commit().await()
        }
    }

    private companion object {
        const val USERS = "users"
        const val PETS = "pets"
        const val RECORDS = "records"
        const val REMINDERS = "reminders"

        /** Límite de escrituras por lote de Firestore. */
        const val MAX_BATCH_WRITES = 500
    }
}
