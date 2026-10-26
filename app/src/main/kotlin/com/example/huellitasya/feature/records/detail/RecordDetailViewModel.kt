package com.example.huellitasya.feature.records.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.huellitasya.R
import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.domain.error.DataError
import com.example.huellitasya.core.domain.repository.NetworkMonitor
import com.example.huellitasya.core.domain.usecase.DeleteRecordUseCase
import com.example.huellitasya.core.domain.usecase.DownloadRecordUseCase
import com.example.huellitasya.core.domain.usecase.ObservePetUseCase
import com.example.huellitasya.core.domain.usecase.ObserveRecordUseCase
import com.example.huellitasya.core.model.MedicalRecord
import com.example.huellitasya.core.model.Pet
import com.example.huellitasya.core.ui.UiText
import com.example.huellitasya.core.ui.toUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RecordDetailUiState(
    val pet: Pet? = null,
    val record: MedicalRecord? = null,
    val showDeleteDialog: Boolean = false,
    val isDeleting: Boolean = false,
)

sealed interface RecordDetailAction {
    data object Download : RecordDetailAction
    data object DeleteClick : RecordDetailAction
    data object DismissDelete : RecordDetailAction
    data object ConfirmDelete : RecordDetailAction
}

sealed interface RecordDetailEvent {
    data class ShowMessage(val message: UiText) : RecordDetailEvent

    /** El registro se eliminó (aquí o desde otro dispositivo): la Route vuelve a la lista. */
    data object Deleted : RecordDetailEvent

    /** La mascota ya no existe: la Route vuelve a Home. */
    data object PetGone : RecordDetailEvent
}

/** Detalle de un registro médico (spec 006, Figma 3193:3746): vista previa, descargar y eliminar. */
@HiltViewModel
class RecordDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observePet: ObservePetUseCase,
    observeRecord: ObserveRecordUseCase,
    private val deleteRecord: DeleteRecordUseCase,
    private val downloadRecord: DownloadRecordUseCase,
    private val networkMonitor: NetworkMonitor,
) : ViewModel() {

    /** Argumentos de `RecordDetailDestination`. */
    private val petId: String = checkNotNull(savedStateHandle[PET_ID_ARG]) { "Falta la mascota" }
    private val recordId: String = checkNotNull(savedStateHandle[RECORD_ID_ARG]) { "Falta el registro" }

    private val _uiState = MutableStateFlow(RecordDetailUiState())
    val uiState: StateFlow<RecordDetailUiState> = _uiState.asStateFlow()

    private val _events = Channel<RecordDetailEvent>(Channel.BUFFERED)
    val events: Flow<RecordDetailEvent> = _events.receiveAsFlow()

    /** Se sale de la pantalla una sola vez, aunque lleguen a la vez el borrado y el `NotFound`. */
    private var isLeaving = false

    init {
        viewModelScope.launch {
            observePet(petId).collect { result ->
                when (result) {
                    is AppResult.Success -> _uiState.update { it.copy(pet = result.data) }
                    is AppResult.Failure -> if (result.error == DataError.NotFound) leave(RecordDetailEvent.PetGone)
                }
            }
        }
        viewModelScope.launch {
            observeRecord(petId, recordId).collect { result ->
                when (result) {
                    is AppResult.Success -> _uiState.update { it.copy(record = result.data) }
                    is AppResult.Failure -> when {
                        // Al eliminarlo aquí, el listener avisa antes de que termine el borrado.
                        result.error == DataError.NotFound -> if (!_uiState.value.isDeleting) leave(RecordDetailEvent.Deleted)
                        else -> _events.send(RecordDetailEvent.ShowMessage(result.error.toUiText()))
                    }
                }
            }
        }
    }

    fun onAction(action: RecordDetailAction) {
        when (action) {
            RecordDetailAction.Download -> download()
            RecordDetailAction.DeleteClick -> _uiState.update {
                if (it.isDeleting || it.record == null) it else it.copy(showDeleteDialog = true)
            }
            RecordDetailAction.DismissDelete -> _uiState.update { it.copy(showDeleteDialog = false) }
            RecordDetailAction.ConfirmDelete -> delete()
        }
    }

    private fun download() {
        val record = _uiState.value.record ?: return
        viewModelScope.launch {
            if (!isOnline()) return@launch
            val message = when (val result = downloadRecord(record)) {
                is AppResult.Success -> UiText.Resource(R.string.records_download_started, record.fileName)
                is AppResult.Failure -> result.error.toUiText()
            }
            _events.send(RecordDetailEvent.ShowMessage(message))
        }
    }

    private fun delete() {
        if (_uiState.value.isDeleting) return
        _uiState.update { it.copy(showDeleteDialog = false, isDeleting = true) }
        viewModelScope.launch {
            if (!isOnline()) {
                _uiState.update { it.copy(isDeleting = false) }
                return@launch
            }
            when (val result = deleteRecord(petId, recordId)) {
                is AppResult.Success -> leave(RecordDetailEvent.Deleted)
                is AppResult.Failure -> {
                    _uiState.update { it.copy(isDeleting = false) }
                    _events.send(RecordDetailEvent.ShowMessage(result.error.toUiText()))
                }
            }
        }
    }

    private suspend fun leave(event: RecordDetailEvent) {
        if (isLeaving) return
        isLeaving = true
        _events.send(event)
    }

    /** Sin red, Firestore encola la escritura y no termina: se avisa y no se intenta. */
    private suspend fun isOnline(): Boolean {
        val online = networkMonitor.isOnline.first()
        if (!online) _events.send(RecordDetailEvent.ShowMessage(DataError.Network.toUiText()))
        return online
    }

    companion object {
        const val PET_ID_ARG = "petId"
        const val RECORD_ID_ARG = "recordId"
    }
}
