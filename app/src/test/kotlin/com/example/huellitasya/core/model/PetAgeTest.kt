package com.example.huellitasya.core.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class PetAgeTest {

    private val today = LocalDate.of(2026, 10, 19)

    @Test
    fun `menos de un mes se cuenta en dias`() {
        assertEquals(PetAge.Days(0), PetAge.between(today, today))
        assertEquals(PetAge.Days(29), PetAge.between(LocalDate.of(2026, 9, 20), today))
    }

    @Test
    fun `desde un mes y antes del año se cuenta en meses`() {
        assertEquals(PetAge.Months(1), PetAge.between(LocalDate.of(2026, 9, 19), today))
        assertEquals(PetAge.Months(4), PetAge.between(LocalDate.of(2026, 6, 6), today))
        assertEquals(PetAge.Months(11), PetAge.between(LocalDate.of(2025, 10, 20), today))
    }

    @Test
    fun `desde un año se cuenta en años`() {
        assertEquals(PetAge.Years(1), PetAge.between(LocalDate.of(2025, 10, 19), today))
        assertEquals(PetAge.Years(6), PetAge.between(LocalDate.of(2020, 1, 1), today))
    }

    @Test
    fun `una fecha futura se muestra como recien nacida`() {
        assertEquals(PetAge.Days(0), PetAge.between(today.plusDays(3), today))
    }
}
