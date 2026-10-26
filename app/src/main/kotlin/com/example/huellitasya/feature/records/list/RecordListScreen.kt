package com.example.huellitasya.feature.records.list

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.huellitasya.R
import com.example.huellitasya.core.designsystem.GreenDark
import com.example.huellitasya.core.designsystem.HuellitasYATheme
import com.example.huellitasya.core.designsystem.components.AddPlaceholderCard
import com.example.huellitasya.core.model.MedicalRecord
import com.example.huellitasya.core.model.Pet
import com.example.huellitasya.core.model.PetSex
import com.example.huellitasya.core.model.PetSpecies
import com.example.huellitasya.core.model.RecordCategory
import com.example.huellitasya.core.model.RecordType
import com.example.huellitasya.core.ui.ObserveAsEvents
import com.example.huellitasya.feature.records.components.RecordCard
import com.example.huellitasya.feature.records.components.RecordsHeader
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
internal fun RecordListRoute(
    onBackClick: () -> Unit,
    onAddClick: (petId: String, category: RecordCategory) -> Unit,
    onRecordClick: (petId: String, recordId: String) -> Unit,
    onPetGone: () -> Unit,
    viewModel: RecordListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is RecordListEvent.ShowMessage -> scope.launch {
                snackbarHostState.showSnackbar(event.message.asString(context))
            }
            RecordListEvent.PetGone -> onPetGone()
        }
    }

    RecordListScreen(
        state = uiState,
        category = viewModel.category,
        snackbarHostState = snackbarHostState,
        onBackClick = onBackClick,
        onAddClick = { onAddClick(viewModel.petId, viewModel.category) },
        onRecordClick = { onRecordClick(viewModel.petId, it) },
    )
}

/**
 * Lista de una categoría (Figma: vacía 3193:3626, con archivos 3193:3339). Sin el botón "Guardar
 * información" ni el del tema (desviaciones, spec 006).
 */
@Composable
internal fun RecordListScreen(
    state: RecordListUiState,
    category: RecordCategory,
    snackbarHostState: SnackbarHostState,
    onBackClick: () -> Unit,
    onAddClick: () -> Unit,
    onRecordClick: (recordId: String) -> Unit,
) {
    val hasRecords = state.records.isNotEmpty()
    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        // Column y no LazyColumn: la cabecera dibuja la ola y no debe desaparecer al desplazar.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            RecordsHeader(pet = state.pet, category = category, onBackClick = onBackClick)
            if (!state.isLoading) {
                if (hasRecords) {
                    state.records.forEach { record ->
                        key(record.id) {
                            RecordCard(record = record, onClick = { onRecordClick(record.id) })
                        }
                    }
                } else {
                    AddPlaceholderCard(
                        contentDescription = stringResource(R.string.records_add),
                        onClick = onAddClick,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        if (hasRecords) {
            AddRecordButton(
                onClick = onAddClick,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 19.dp, bottom = 24.dp),
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding(),
        )
    }
}

/** Botón flotante "+" (Figma 3273:1006): 56 × 54 dp, verde oscuro, esquinas de 15 dp. */
@Composable
private fun AddRecordButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(width = 56.dp, height = 54.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(GreenDark)
            .clickable(onClickLabel = stringResource(R.string.records_add), role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_records_add),
            contentDescription = stringResource(R.string.records_add),
            modifier = Modifier.size(16.dp),
        )
    }
}

private val PreviewPet = Pet("pet-1", "Maximiliano Tercero", PetSpecies.DOG, "Golden Retriever", LocalDate.now().minusMonths(3), PetSex.MALE, 5.0, null)

@Preview(showBackground = true, widthDp = 412, heightDp = 917)
@Composable
private fun RecordListScreenPreview() {
    HuellitasYATheme {
        RecordListScreen(
            state = RecordListUiState(
                pet = PreviewPet,
                records = listOf(
                    MedicalRecord("1", "pet-1", RecordCategory.VACCINES, "Vacuna_Rabia_2026.pdf", "application/pdf", 1, LocalDate.of(2026, 9, 10), RecordType.VACCINATION, "Dr. A. Ramírez", ""),
                    MedicalRecord("2", "pet-1", RecordCategory.VACCINES, "Vacuna_Polivalente_2026.pdf", "application/pdf", 1, LocalDate.of(2026, 9, 10), RecordType.VACCINATION, null, ""),
                ),
                isLoading = false,
            ),
            category = RecordCategory.VACCINES,
            snackbarHostState = SnackbarHostState(),
            onBackClick = {},
            onAddClick = {},
            onRecordClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917)
@Composable
private fun RecordListScreenEmptyPreview() {
    HuellitasYATheme {
        RecordListScreen(
            state = RecordListUiState(pet = PreviewPet, isLoading = false),
            category = RecordCategory.HISTORY,
            snackbarHostState = SnackbarHostState(),
            onBackClick = {},
            onAddClick = {},
            onRecordClick = {},
        )
    }
}
