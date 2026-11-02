package com.example.huellitasya.core.data.mapper

import com.example.huellitasya.core.domain.error.AuthError
import com.example.huellitasya.core.domain.error.DataError
import com.example.huellitasya.core.model.AuthProvider
import com.example.huellitasya.core.model.MedicalRecord
import com.example.huellitasya.core.model.Pet
import com.example.huellitasya.core.model.PetSex
import com.example.huellitasya.core.model.PetSpecies
import com.example.huellitasya.core.model.RecordCategory
import com.example.huellitasya.core.model.RecordType
import com.example.huellitasya.core.model.Reminder
import com.example.huellitasya.core.model.ReminderKind
import com.example.huellitasya.core.model.User
import com.example.huellitasya.core.model.UserProfile
import com.example.huellitasya.core.model.VaccineType
import com.example.huellitasya.core.model.VetReason
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.storage.StorageException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import java.util.Locale

internal fun FirebaseUser.toUser(): User = User(
    uid = uid,
    name = displayName.orEmpty(),
    email = email.orEmpty(),
    photoUrl = photoUrl?.toString(),
    provider = if (providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID }) {
        AuthProvider.GOOGLE
    } else {
        AuthProvider.EMAIL
    },
)

/** Tabla "Errores remotos" de las specs 002 y 004. El orden importa: WeakPassword hereda de InvalidCredentials. */
internal fun Throwable.toAuthError(): AuthError = when (this) {
    is FirebaseAuthRecentLoginRequiredException -> AuthError.RequiresRecentLogin
    is FirebaseAuthWeakPasswordException -> AuthError.WeakPassword
    is FirebaseAuthUserCollisionException -> AuthError.EmailAlreadyInUse
    is FirebaseAuthInvalidCredentialsException,
    is FirebaseAuthInvalidUserException -> AuthError.InvalidCredentials
    is FirebaseNetworkException -> AuthError.Network
    is FirebaseTooManyRequestsException -> AuthError.TooManyRequests
    else -> AuthError.Unknown
}

internal fun Throwable.toDataError(): DataError = when {
    this is FirebaseNetworkException -> DataError.Network
    this is FirebaseFirestoreException && code == FirebaseFirestoreException.Code.UNAVAILABLE -> DataError.Network
    this is FirebaseFirestoreException && code == FirebaseFirestoreException.Code.PERMISSION_DENIED -> DataError.PermissionDenied
    this is FirebaseFirestoreException && code == FirebaseFirestoreException.Code.NOT_FOUND -> DataError.NotFound
    this is StorageException && errorCode == StorageException.ERROR_RETRY_LIMIT_EXCEEDED -> DataError.Network
    this is StorageException && errorCode == StorageException.ERROR_NOT_AUTHORIZED -> DataError.PermissionDenied
    else -> DataError.Unknown
}

/**
 * Esquema `users/{uid}/pets/{petId}` de la spec 005. Si falta un dato obligatorio o está mal
 * formado, la mascota se omite (`null`) en lugar de mostrarse a medias.
 */
internal fun DocumentSnapshot.toPet(): Pet? {
    val name = getString("name")?.takeIf { it.isNotBlank() } ?: return null
    val breed = getString("breed")?.takeIf { it.isNotBlank() } ?: return null
    val birthDate = getString("birthDate")?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return null
    val weightKg = getDouble("weightKg") ?: return null
    return Pet(
        id = id,
        name = name,
        species = when (getString("species")) {
            "dog" -> PetSpecies.DOG
            "cat" -> PetSpecies.CAT
            else -> PetSpecies.OTHER
        },
        breed = breed,
        birthDate = birthDate,
        sex = if (getString("sex") == "female") PetSex.FEMALE else PetSex.MALE,
        weightKg = weightKg,
        photoUrl = getString("photoUrl")?.takeIf { it.isNotBlank() },
    )
}

/**
 * Esquema `users/{uid}/pets/{petId}/records/{recordId}` de la spec 006. Un registro con datos
 * obligatorios faltantes o mal formados se omite (`null`).
 */
internal fun DocumentSnapshot.toMedicalRecord(petId: String): MedicalRecord? {
    val category = when (getString("category")) {
        "vaccines" -> RecordCategory.VACCINES
        "history" -> RecordCategory.HISTORY
        "files" -> RecordCategory.FILES
        else -> return null
    }
    val fileName = getString("fileName")?.takeIf { it.isNotBlank() } ?: return null
    val contentType = getString("contentType") ?: return null
    val date = getString("date")?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return null
    val fileUrl = getString("fileUrl")?.takeIf { it.isNotBlank() } ?: return null
    return MedicalRecord(
        id = id,
        petId = petId,
        category = category,
        fileName = fileName,
        contentType = contentType,
        sizeBytes = getLong("sizeBytes") ?: 0L,
        date = date,
        type = when (getString("type")) {
            "vaccination" -> RecordType.VACCINATION
            "consultation" -> RecordType.CONSULTATION
            "exam" -> RecordType.EXAM
            else -> RecordType.OTHER
        },
        vet = getString("vet")?.takeIf { it.isNotBlank() },
        fileUrl = fileUrl,
    )
}

/** Esquema `users/{uid}` de las specs 002 y 004. Un dato mal formado se trata como no registrado. */
internal fun DocumentSnapshot.toUserProfile(): UserProfile = UserProfile(
    uid = id,
    name = getString("name").orEmpty(),
    email = getString("email").orEmpty(),
    phone = getString("phone")?.takeIf { it.isNotBlank() },
    address = getString("address")?.takeIf { it.isNotBlank() },
    birthDate = getString("birthDate")?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
    photoUrl = getString("photoUrl")?.takeIf { it.isNotBlank() },
    provider = if (getString("provider") == "google") AuthProvider.GOOGLE else AuthProvider.EMAIL,
)

/** Recordatorio de `users/{uid}/reminders/{id}` (spec 007). `null` si el documento está incompleto. */
internal fun DocumentSnapshot.toReminder(): Reminder? {
    val petId = getString("petId")?.takeIf { it.isNotBlank() } ?: return null
    val kind = when (getString("kind")) {
        "vaccine" -> ReminderKind.VACCINE
        "vet" -> ReminderKind.VET
        "other" -> ReminderKind.OTHER
        else -> return null
    }
    val subtype = getString("subtype")
    val dateTime = getTimestamp("dateTime")?.toLocalDateTime() ?: return null
    return Reminder(
        id = id,
        petId = petId,
        kind = kind,
        vaccineType = if (kind == ReminderKind.VACCINE) VaccineType.entries.firstOrNull { it.wireValue() == subtype } ?: return null else null,
        vetReason = if (kind == ReminderKind.VET) VetReason.entries.firstOrNull { it.wireValue() == subtype } ?: return null else null,
        customTitle = getString("title"),
        dateTime = dateTime,
    )
}

internal fun ReminderKind.wireValue(): String = when (this) {
    ReminderKind.VACCINE -> "vaccine"
    ReminderKind.VET -> "vet"
    ReminderKind.OTHER -> "other"
}

internal fun VaccineType.wireValue(): String = name.lowercase(Locale.ROOT)

internal fun VetReason.wireValue(): String = when (this) {
    VetReason.GENERAL -> "general"
    VetReason.DEWORMING -> "deworming"
    VetReason.GROOMING -> "grooming"
    VetReason.SURGERY -> "surgery"
    VetReason.OTHER -> "other"
}

/** Valor de `subtype`: la vacuna o el motivo; `null` en los recordatorios "Otro". */
internal fun subtypeValue(vaccineType: VaccineType?, vetReason: VetReason?): String? =
    vaccineType?.wireValue() ?: vetReason?.wireValue()

/** Las fechas se guardan como instantes en la zona del teléfono. */
internal fun LocalDateTime.toTimestamp(): Timestamp = Timestamp(Date.from(atZone(ZoneId.systemDefault()).toInstant()))

internal fun Timestamp.toLocalDateTime(): LocalDateTime = LocalDateTime.ofInstant(toDate().toInstant(), ZoneId.systemDefault())
