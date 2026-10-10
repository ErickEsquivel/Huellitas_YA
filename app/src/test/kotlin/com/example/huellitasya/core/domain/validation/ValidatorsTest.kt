package com.example.huellitasya.core.domain.validation

import com.example.huellitasya.core.domain.error.ValidationError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ValidatorsTest {

    private val validateName = ValidateNameUseCase()
    private val validateEmail = ValidateEmailUseCase()
    private val validatePassword = ValidatePasswordUseCase()

    @Test
    fun `nombre vacio o con espacios es EmptyName`() {
        assertEquals(ValidationError.EmptyName, validateName(""))
        assertEquals(ValidationError.EmptyName, validateName("   "))
    }

    @Test
    fun `nombre de un caracter es NameTooShort`() {
        assertEquals(ValidationError.NameTooShort, validateName(" A "))
    }

    @Test
    fun `nombre valido no tiene error`() {
        assertNull(validateName("Santiago Cruz"))
    }

    @Test
    fun `correo vacio es EmptyEmail`() {
        assertEquals(ValidationError.EmptyEmail, validateEmail(" "))
    }

    @Test
    fun `correo sin dominio es InvalidEmail`() {
        assertEquals(ValidationError.InvalidEmail, validateEmail("sa.company@"))
        assertEquals(ValidationError.InvalidEmail, validateEmail("sa.company@hotmail"))
        assertEquals(ValidationError.InvalidEmail, validateEmail("sa company@hotmail.com"))
    }

    @Test
    fun `correo valido con espacios alrededor no tiene error`() {
        assertNull(validateEmail("  Sa.company@hotmail.com "))
    }

    @Test
    fun `contrasena de registro aplica las reglas de Figma en orden`() {
        assertEquals(ValidationError.EmptyPassword, validatePassword.forRegistration(""))
        assertEquals(ValidationError.PasswordTooShort, validatePassword.forRegistration("Abc12"))
        assertEquals(ValidationError.PasswordNeedsUppercase, validatePassword.forRegistration("huellitas1"))
        assertEquals(ValidationError.PasswordNeedsDigitOrSymbol, validatePassword.forRegistration("Huellitas"))
    }

    @Test
    fun `contrasena de registro acepta numero o caracter especial`() {
        assertNull(validatePassword.forRegistration("Huellitas1"))
        assertNull(validatePassword.forRegistration("Huellitas!"))
    }

    @Test
    fun `contrasena de login solo exige que no este vacia`() {
        assertEquals(ValidationError.EmptyPassword, validatePassword.forLogin(""))
        assertNull(validatePassword.forLogin("x"))
    }

    @Test
    fun `confirmacion vacia o distinta`() {
        assertEquals(ValidationError.EmptyPassword, validatePassword.confirmation("Huellitas1", ""))
        assertEquals(ValidationError.PasswordsDoNotMatch, validatePassword.confirmation("Huellitas1", "Huellitas2"))
        assertNull(validatePassword.confirmation("Huellitas1", "Huellitas1"))
    }
}
