package com.example.huellitasya.feature.records.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.example.huellitasya.R
import com.example.huellitasya.core.designsystem.BorderSubtle
import com.example.huellitasya.core.designsystem.DestructiveRed
import com.example.huellitasya.core.designsystem.HuellitasYATheme
import com.example.huellitasya.core.designsystem.RecordCardSurface
import com.example.huellitasya.core.designsystem.components.DeleteConfirmDialog
import com.example.huellitasya.core.designsystem.components.HuellitasButton
import com.example.huellitasya.core.designsystem.components.ProgressOverlay
import com.example.huellitasya.core.model.MedicalRecord
import com.example.huellitasya.core.model.Pet
import com.example.huellitasya.core.model.PetSex
import com.example.huellitasya.core.model.PetSpecies
import com.example.huellitasya.core.model.RecordCategory
import com.example.huellitasya.core.model.RecordType
import com.example.huellitasya.core.ui.ObserveAsEvents
import com.example.huellitasya.core.ui.pdf.PdfFirstPageDecoder
import com.example.huellitasya.feature.records.components.RecordFileIcon
import com.example.huellitasya.feature.records.components.RecordsHeader
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
internal fun RecordDetailRoute(
    onBackClick: () -> Unit,
    onDeleted: () -> Unit,
    onPetGone: () -> Unit,
    viewModel: RecordDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is RecordDetailEvent.ShowMessage -> scope.launch {
                snackbarHostState.showSnackbar(event.message.asString(context))
            }
            RecordDetailEvent.Deleted -> onDeleted()
            RecordDetailEvent.PetGone -> onPetGone()
        }
    }

    RecordDetailScreen(
        state = uiState,
        snackbarHostState = snackbarHostState,
        onBackClick = onBackClick,
        onAction = viewModel::onAction,
    )
}

/**
 * Detalle de un archivo (Figma 3193:3746): tarjeta con el ícono, el nombre, la descarga y la vista
 * previa grande; debajo, "Eliminar Archivo". Sin el botón del tema (desviación, spec 006).
 */
@Composable
internal fun RecordDetailScreen(
    state: RecordDetailUiState,
    snackbarHostState: SnackbarHostState,
    onBackClick: () -> Unit,
    onAction: (RecordDetailAction) -> Unit,
) {
    ProgressOverlay(isVisible = state.isDeleting, text = stringResource(R.string.records_deleting)) {
        Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                RecordsHeader(pet = state.pet, category = state.record?.category, onBackClick = onBackClick)
                state.record?.let { record ->
                    RecordPreviewCard(record = record, onDownloadClick = { onAction(RecordDetailAction.Download) })
                    Spacer(modifier = Modifier.height(8.dp))
                    HuellitasButton(
                        text = stringResource(R.string.records_delete),
                        onClick = { onAction(RecordDetailAction.DeleteClick) },
                        containerColor = DestructiveRed,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.align(Alignment.CenterHorizontally).width(279.dp),
                    )
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding(),
            )
        }
    }

    if (state.showDeleteDialog) {
        DeleteConfirmDialog(
            title = stringResource(R.string.records_delete_title),
            message = stringResource(R.string.records_delete_message),
            confirmText = stringResource(R.string.records_delete),
            closeDescription = stringResource(R.string.records_delete_close),
            imageDescription = stringResource(R.string.records_delete_image_description),
            onConfirm = { onAction(RecordDetailAction.ConfirmDelete) },
            onDismiss = { onAction(RecordDetailAction.DismissDelete) },
        )
    }
}

private val CardShape = RoundedCornerShape(10.dp)

/** Tarjeta de 360 dp de ancho en Figma: cabecera del archivo y vista previa de 338 × 244 dp. */
@Composable
private fun RecordPreviewCard(record: MedicalRecord, onDownloadClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(RecordCardSurface)
            .border(1.dp, BorderSubtle, CardShape)
            .padding(start = 11.dp, end = 4.dp, top = 8.dp, bottom = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RecordFileIcon(record = record, size = 40.dp, modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = record.fileName,
                color = Color.Black,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onDownloadClick) {
                Image(
                    painter = painterResource(R.drawable.ic_records_download),
                    contentDescription = stringResource(R.string.records_download, record.fileName),
                    modifier = Modifier.size(width = 31.dp, height = 27.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        RecordPreview(
            record = record,
            modifier = Modifier.padding(end = 7.dp).fillMaxWidth().aspectRatio(338f / 300f),
        )
    }
}

/** La imagen, o la primera página del PDF dibujada por [PdfFirstPageDecoder], ajustada a su caja. */
@Composable
private fun RecordPreview(record: MedicalRecord, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val request = remember(record.fileUrl, record.isPdf) {
        ImageRequest.Builder(context)
            .data(record.fileUrl)
            .apply { if (record.isPdf) decoderFactory(PdfFirstPageDecoder.Factory()) }
            .build()
    }
    AsyncImage(
        model = request,
        contentDescription = stringResource(R.string.records_preview, record.fileName),
        contentScale = ContentScale.Fit,
        modifier = modifier,
    )
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917)
@Composable
private fun RecordDetailScreenPreview() {
    HuellitasYATheme {
        RecordDetailScreen(
            state = RecordDetailUiState(
                pet = Pet("pet-1", "Maximiliano Tercero", PetSpecies.DOG, "Golden Retriever", LocalDate.now().minusMonths(3), PetSex.MALE, 5.0, null),
                record = MedicalRecord("1", "pet-1", RecordCategory.VACCINES, "Carné_Vacunación.pdf", "application/pdf", 1, LocalDate.of(2026, 9, 10), RecordType.VACCINATION, null, ""),
            ),
            snackbarHostState = SnackbarHostState(),
            onBackClick = {},
            onAction = {},
        )
    }
}
