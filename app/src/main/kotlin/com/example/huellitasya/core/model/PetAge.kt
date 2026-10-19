package com.example.huellitasya.core.model

import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit

/**
 * Edad de una mascota para "raza · edad" (spec 005): días si tiene menos de un mes, meses si
 * tiene menos de un año, y años desde ahí.
 */
sealed interface PetAge {
    val value: Int

    data class Days(override val value: Int) : PetAge
    data class Months(override val value: Int) : PetAge
    data class Years(override val value: Int) : PetAge

    companion object {
        fun between(birthDate: LocalDate, today: LocalDate): PetAge {
            // Una fecha futura (reloj del teléfono atrasado) se muestra como recién nacida.
            if (birthDate.isAfter(today)) return Days(0)
            val period = Period.between(birthDate, today)
            return when {
                period.years > 0 -> Years(period.years)
                period.months > 0 -> Months(period.months)
                else -> Days(ChronoUnit.DAYS.between(birthDate, today).toInt())
            }
        }
    }
}

fun Pet.ageOn(today: LocalDate): PetAge = PetAge.between(birthDate, today)
