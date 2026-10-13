package com.example.huellitasya.feature.profile.personaldata

import app.cash.turbine.test
import com.example.huellitasya.R
import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.designsystem.components.FieldStatus
import com.example.huellitasya.core.domain.error.DataError
import com.example.huellitasya.core.domain.error.ValidationError
import com.example.huellitasya.core.domain.usecase.ObserveUserProfileUseCase
import com.example.huellitasya.core.domain.usecase.UpdateUserProfileUseCase
import com.example.huellitasya.core.domain.validation.ValidateAddressUseCase
import com.example.huellitasya.core.domain.validation.ValidateBirthDateUseCase
import com.example.huellitasya.core.domain.validation.ValidateEmailUseCase
import com.example.huellitasya.core.domain.validation.ValidateNameUseCase
import com.example.huellitasya.core.domain.validation.ValidatePhoneUseCase
import com.example.huellitasya.core.model.Field
import com.example.huellitasya.core.model.ProfileChanges
import com.example.huellitasya.core.testing.FakeAuthRepository
import com.example.huellitasya.core.testing.FakeNetworkMonitor
import com.example.huellitasya.core.testing.FakeUserRepository
import com.example.huellitasya.core.testing.MainDispatcherRule
import com.example.huellitasya.core.testing.TestProfile
import com.example.huellitasya.core.testing.TestUser
import com.example.huellitasya.core.ui.FieldFeedback
import com.example.huellitasya.core.ui.MissingFieldsFeedback
import com.example.huellitasya.core.ui.UiText
import com.example.huellitasya.core.ui.toUiText
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class PersonalDataViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = FakeAuthRepository().apply { currentUser.value = TestUser }
    private val userRepository = FakeUserRepository().apply {
        profile.value = AppResult.Success(TestProfile.copy(phone = "+573124456311"))
    }
    private val networkMonitor = FakeNetworkMonitor()

    private fun viewModel() = PersonalDataViewModel(
        observeUserProfile = ObserveUserProfileUseCase(authRepository, userRepository),
        updateUserProfile = UpdateUserProfileUseCase(authRepository, userRepository),
        networkMonitor = networkMonitor,
        validateName = ValidateNameUseCase(),
        validateEmail = ValidateEmailUseCase(),
        validatePhone = ValidatePhoneUseCase(),
        validateAddress = ValidateAddressUseCase(),
        validateBirthDate = ValidateBirthDateUseCase(),
    )

    private fun PersonalDataViewModel.startEditing() = onAction(PersonalDataAction.ToggleEditing)

    @Test
    fun `muestra el perfil guardado en modo lectura`() {
        val state = viewModel().uiState.value

        assertEquals(TestProfile.name, state.name)
        assertEquals("+573124456311", state.phone)
        assertEquals("", state.address)
        assertFalse(state.isEditing)
        assertFalse(state.hasChanges)
    }

    @Test
    fun `el boton sin el lapiz abre la edicion sin guardar`() {
        val viewModel = viewModel()

        viewModel.onAction(PersonalDataAction.Save)

        assertTrue(viewModel.uiState.value.isEditing)
        assertTrue(userRepository.updates.isEmpty())
    }

    @Test
    fun `editar un campo activa el boton verde`() {
        val viewModel = viewModel()
        viewModel.startEditing()

        viewModel.onAction(PersonalDataAction.AddressChanged("Calle 1"))

        assertTrue(viewModel.uiState.value.hasChanges)
    }

    @Test
    fun `volver a escribir el valor original no cuenta como cambio`() {
        val viewModel = viewModel()
        viewModel.startEditing()

        viewModel.onAction(PersonalDataAction.PhoneChanged("+57 312 445 6311"))

        assertFalse(viewModel.uiState.value.hasChanges)
    }

    @Test
    fun `el lapiz durante la edicion descarta los cambios`() {
        val viewModel = viewModel()
        viewModel.startEditing()
        viewModel.onAction(PersonalDataAction.NameChanged("Otro nombre"))

        viewModel.startEditing()

        val state = viewModel.uiState.value
        assertFalse(state.isEditing)
        assertEquals(TestProfile.name, state.name)
        assertFalse(state.hasChanges)
    }

    @Test
    fun `datos invalidos muestran avisos y no escriben en Firestore`() {
        val viewModel = viewModel()
        viewModel.startEditing()
        viewModel.onAction(PersonalDataAction.NameChanged(""))
        viewModel.onAction(PersonalDataAction.PhoneChanged("12ab"))

        viewModel.onAction(PersonalDataAction.Save)

        val state = viewModel.uiState.value
        assertEquals(FieldFeedback(FieldStatus.Warning), state.nameFeedback)
        assertEquals(FieldFeedback(FieldStatus.Warning, ValidationError.InvalidPhone.toUiText()), state.phoneFeedback)
        assertEquals(MissingFieldsFeedback, state.formFeedback)
        assertTrue(userRepository.updates.isEmpty())
    }

    @Test
    fun `guardar envia solo lo que cambio y sale del modo edicion`() = runTest {
        val viewModel = viewModel()
        viewModel.startEditing()
        viewModel.onAction(PersonalDataAction.PhoneChanged(""))
        viewModel.onAction(PersonalDataAction.AddressChanged("  Calle 1 "))
        viewModel.onAction(PersonalDataAction.BirthDateChanged(LocalDate.of(2005, 1, 15)))

        viewModel.events.test {
            viewModel.onAction(PersonalDataAction.Save)

            assertEquals(
                PersonalDataEvent.ShowMessage(UiText.Resource(R.string.personal_data_saved)),
                awaitItem(),
            )
        }

        assertEquals(
            TestProfile.uid to ProfileChanges(
                phone = Field.Clear,
                address = Field.Set("Calle 1"),
                birthDate = Field.Set(LocalDate.of(2005, 1, 15)),
            ),
            userRepository.updates.single(),
        )
        val state = viewModel.uiState.value
        assertFalse(state.isEditing)
        assertFalse(state.isSaving)
        assertEquals("Calle 1", state.address)
    }

    @Test
    fun `el telefono se guarda normalizado`() {
        val viewModel = viewModel()
        viewModel.startEditing()
        viewModel.onAction(PersonalDataAction.PhoneChanged("+57 300-123-4567"))

        viewModel.onAction(PersonalDataAction.Save)

        assertEquals(ProfileChanges(phone = Field.Set("+573001234567")), userRepository.updates.single().second)
        assertEquals("+573001234567", viewModel.uiState.value.phone)
    }

    @Test
    fun `sin conexion avisa y no intenta guardar`() = runTest {
        networkMonitor.isOnline.value = false
        val viewModel = viewModel()
        viewModel.startEditing()
        viewModel.onAction(PersonalDataAction.AddressChanged("Calle 1"))

        viewModel.events.test {
            viewModel.onAction(PersonalDataAction.Save)

            assertEquals(PersonalDataEvent.ShowMessage(DataError.Network.toUiText()), awaitItem())
        }
        assertTrue(userRepository.updates.isEmpty())
        assertTrue(viewModel.uiState.value.isEditing)
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun `si falla Firestore se queda en edicion con el mensaje`() = runTest {
        userRepository.updateResult = AppResult.Failure(DataError.PermissionDenied)
        val viewModel = viewModel()
        viewModel.startEditing()
        viewModel.onAction(PersonalDataAction.AddressChanged("Calle 1"))

        viewModel.events.test {
            viewModel.onAction(PersonalDataAction.Save)

            assertEquals(PersonalDataEvent.ShowMessage(DataError.PermissionDenied.toUiText()), awaitItem())
        }
        assertTrue(viewModel.uiState.value.isEditing)
        assertEquals("Calle 1", viewModel.uiState.value.address)
    }

    @Test
    fun `un cambio remoto no pisa lo que se esta editando`() {
        val viewModel = viewModel()
        viewModel.startEditing()
        viewModel.onAction(PersonalDataAction.AddressChanged("Calle 1"))

        userRepository.profile.value = AppResult.Success(TestProfile.copy(name = "Cambiado en otro equipo"))

        val state = viewModel.uiState.value
        assertEquals("Calle 1", state.address)
        assertEquals(TestProfile.name, state.name)
        assertEquals("Cambiado en otro equipo", state.saved?.name)
    }
}
