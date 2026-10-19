package com.example.huellitasya.core.model

import java.time.LocalDate

/** Mascota guardada en `users/{uid}/pets/{petId}` (spec 005). */
data class Pet(
    val id: String,
    val name: String,
    val species: PetSpecies,
    val breed: String,
    val birthDate: LocalDate,
    val sex: PetSex,
    val weightKg: Double,
    val photoUrl: String?,
)

/** Datos ya validados del formulario de mascota (registrar y editar). */
data class PetDraft(
    val name: String,
    val species: PetSpecies,
    val breed: String,
    val birthDate: LocalDate,
    val sex: PetSex,
    val weightKg: Double,
)

enum class PetSpecies { DOG, CAT, OTHER }

enum class PetSex { MALE, FEMALE }
