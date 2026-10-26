package com.example.huellitasya.feature.records.upload

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.domain.error.DataError
import com.example.huellitasya.core.domain.repository.NetworkMonitor
import com.example.huellitasya.core.domain.usecase.InspectRecordFileUseCase
import com.example.huellitasya.core.domain.usecase.ObservePetUseCase
import com.example.huellitasya.core.domain.usecase.UploadRecordUseCase
import com.example.huellitasya.core.domain.validation.ValidateRecordFileUseCase
import com.example.huellitasya.core.domain.validation.ValidateRecordFormUseCase
import com.example.huellitasya.core.model.LocalFileInfo
import com.example.huellitasya.core.model.Pet
import com.example.huellitasya.core.model.RecordCategory
import com.example.huellitasya.core.model.RecordDraft
import com.example.huellitasya.core.model.RecordType
import com.example.huellitasya.core.ui.FieldFeedback
import com.example.huellitasya.core.ui.UiText
import com.example.huellitasya.core.ui.toFieldFeedback
import com.example.huellitasya.core.ui.toUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class UploadRecordUiState(
    val pet: Pet? = null,
    /** Archivo elegido (`content://`) y lo que se sabe de él; `null` hasta elegir uno. */
    val localUri: String? = null,
    val file: LocalFileInfo? = null,
    val date: LocalDate = LocalDate.now(),
    val type: RecordType = RecordType.OTHER,
    val vet: String = "",
    val dateFeedback: FieldFeedback? = null,
    val vetFeedback: FieldFeedback? = null,
    /** Avance de la subida (0–1); `null` cuando no se está subiendo. */
    val progress: Float? = null,
) {
    val isUploading: Boolean get() = progress != null
}

sealed interface UploadRecordAction {
    /** El usuario eligió un archivo en el selector del sistema (`content://`). */
    data class FilePicked(val uri: String) : UploadRecordAction
    data class DateChanged(val value: LocalDate) : UploadRecordAction
    data class TypeChanged(val value: RecordType) : UploadRecordAction
    data class VetChanged(val value: String) : UploadRecordAction
    data object Upload : UploadRecordAction
}

sealed interface UploadRecordEvent {
    data class ShowMessage(val message: UiText) : UploadRecordEvent

    /** El archivo quedó guardado: la Route vuelve a la lista. */
    data object Uploaded : UploadRecordEvent
}

/** "Subir Archivo" (spec 006, Figma 3193:3730 y 3281:1697): elige el archivo, pide sus datos y lo sube. */
@HiltViewModel
class UploadRecordViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observePet: ObservePetUseCase,
    private val inspectFile: InspectRecordFileUseCase,
    private val uploadRecord: UploadRecordUseCase,
    private val validateFile: ValidateRecordFileUseCase,
    private val validateForm: ValidateRecordFormUseCase,
    private val networkMonitor: NetworkMonitor,
) : ViewModel() {

    /** Argumentos de `UploadRecordDestination`. */
    private val petId: String = checkNotNull(savedStateHandle[PET_ID_ARG]) { "Falta la mascota" }
    val category: RecordCategory = RecordCategory.valueOf(checkNotNull(savedStateHandle[CATEGORY_ARG]) { "Falta la categoría" })

    private val _uiState = MutableStateFlow(UploadRecordUiState(type = defaultType(category)))
    val uiState: StateFlow<UploadRecordUiState> = _uiState.asStateFlow()

    private val _events = Channel<UploadRecordEvent>(Channel.BUFFERED)
    val events: Flow<UploadRecordEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            observePet(petId).collect { result ->
                if (result is AppResult.Success) _uiState.update { it.copy(pet = result.data) }
            }
        }
    }

    fun onAction(action: UploadRecordAction) {
        if (_uiState.value.isUploading) return
        when (action) {
            is UploadRecordAction.FilePicked -> pickFile(action.uri)
            is UploadRecordAction.DateChanged -> _uiState.update { it.copy(date = action.value, dateFeedback = null) }
            is UploadRecordAction.TypeChanged -> _uiState.update { it.copy(type = action.value) }
            is UploadRecordAction.VetChanged -> _uiState.update { it.copy(vet = action.value, vetFeedback = null) }
            UploadRecordAction.Upload -> upload()
        }
    }

    private fun pickFile(uri: String) {
        viewModelScope.launch {
            when (val result = inspectFile(uri)) {
                is AppResult.Success -> {
                    val error = validateFile(result.data)
                    if (error != null) {
                        showMessage(error.toUiText())
                    } else {
                        _uiState.update { it.copy(localUri = uri, file = result.data) }
                    }
                }
                is AppResult.Failure -> showMessage(result.error.toUiText())
            }
        }
    }

    private fun upload() {
        val state = _uiState.value
        val localUri = state.localUri ?: return
        val file = state.file ?: return

        val dateError = validateForm.date(state.date)
        val vetError = validateForm.vet(state.vet)
        if (dateError != null || vetError != null) {
            _uiState.update { it.copy(dateFeedback = dateError?.toFieldFeedback(), vetFeedback = vetError?.toFieldFeedback()) }
            return
        }

        _uiState.update { it.copy(progress = 0f) }
        viewModelScope.launch {
            // Sin red, Storage reintenta sin fin: mejor avisar y no intentarlo.
            if (!networkMonitor.isOnline.first()) {
                _uiState.update { it.copy(progress = null) }
                showMessage(DataError.Network.toUiText())
                return@launch
            }
            val draft = RecordDraft(category, state.date, state.type, state.vet.trim().ifEmpty { null })
            val result = uploadRecord(petId, draft, localUri, file) { fraction ->
                _uiState.update { it.copy(progress = fraction) }
            }
            when (result) {
                is AppResult.Success -> _events.send(UploadRecordEvent.Uploaded)
                is AppResult.Failure -> {
                    _uiState.update { it.copy(progress = null) }
                    showMessage(result.error.toUiText())
                }
            }
        }
    }

    private suspend fun showMessage(message: UiText) {
        _events.send(UploadRecordEvent.ShowMessage(message))
    }

    companion object {
        const val PET_ID_ARG = "petId"
        const val CATEGORY_ARG = "category"

        /** En el Carné de vacunas casi todo es una vacunación; en el historial, una consulta. */
        fun defaultType(category: RecordCategory): RecordType = when (category) {
            RecordCategory.VACCINES -> RecordType.VACCINATION
            RecordCategory.HISTORY -> RecordType.CONSULTATION
            RecordCategory.FILES -> RecordType.OTHER
        }
    }
}
