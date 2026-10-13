package com.example.huellitasya.core.domain.validation

import com.example.huellitasya.core.domain.error.ValidationError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class ValidatorsTest {

    private val validateName = ValidateNameUseCase()
    private val validateEmail = ValidateEmailUseCase()
    private val validatePassword = ValidatePasswordUseCase()
    private val validatePhone = ValidatePhoneUseCase()
    private val validateAddress = ValidateAddressUseCase()
    private val validateBirthDate = ValidateBirthDateUseCase()

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

    @Test
    fun `requisitos de contrasena uno por uno`() {
        assertEquals(
            PasswordRequirements(hasMinLength = false, hasUppercase = false, hasDigitOrSymbol = false, matchesConfirmation = false),
            validatePassword.requirements("", ""),
        )
        assertEquals(
            PasswordRequirements(hasMinLength = true, hasUppercase = true, hasDigitOrSymbol = true, matchesConfirmation = true),
            validatePassword.requirements("Huellitas!", "Huellitas!"),
        )
        assertEquals(false, validatePassword.requirements("Huellitas1", "Huellitas2").matchesConfirmation)
    }

    @Test
    fun `nombre de mas de 50 caracteres es NameTooLong`() {
        assertEquals(ValidationError.NameTooLong, validateName("a".repeat(51)))
        assertNull(validateName("a".repeat(50)))
    }

    @Test
    fun `telefono vacio es valido porque es opcional`() {
        assertNull(validatePhone(""))
        assertNull(validatePhone("   "))
    }

    @Test
    fun `telefono con prefijo espacios y guiones es valido`() {
        assertNull(validatePhone("+57 312-445-6311"))
        assertNull(validatePhone("3124456"))
    }

    @Test
    fun `telefono con letras o con pocos o demasiados digitos es InvalidPhone`() {
        assertEquals(ValidationError.InvalidPhone, validatePhone("312abc4456"))
        assertEquals(ValidationError.InvalidPhone, validatePhone("123456"))
        assertEquals(ValidationError.InvalidPhone, validatePhone("1234567890123456"))
        assertEquals(ValidationError.InvalidPhone, validatePhone("57+3124456"))
    }

    @Test
    fun `el telefono se guarda sin espacios ni guiones`() {
        assertEquals("+573124456311", validatePhone.normalize("+57 312-445 6311"))
    }

    @Test
    fun `direccion de mas de 100 caracteres es AddressTooLong`() {
        assertEquals(ValidationError.AddressTooLong, validateAddress("a".repeat(101)))
        assertNull(validateAddress("61a Sur27 Cra. 104a Bis"))
    }

    @Test
    fun `fecha de nacimiento futura o anterior a 1900 no es valida`() {
        val today = LocalDate.of(2026, 10, 12)
        assertEquals(ValidationError.InvalidBirthDate, validateBirthDate(LocalDate.of(2026, 10, 13), today))
        assertEquals(ValidationError.InvalidBirthDate, validateBirthDate(LocalDate.of(1899, 12, 31), today))
        assertNull(validateBirthDate(LocalDate.of(2005, 1, 15), today))
        assertNull(validateBirthDate(today, today))
        assertNull(validateBirthDate(null, today))
    }
}
