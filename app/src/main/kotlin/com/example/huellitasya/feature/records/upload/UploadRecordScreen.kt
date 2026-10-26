package com.example.huellitasya.feature.records.upload

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.huellitasya.R
import com.example.huellitasya.core.designsystem.BorderSubtle
import com.example.huellitasya.core.designsystem.GreenBackground
import com.example.huellitasya.core.designsystem.GreenPrimary
import com.example.huellitasya.core.designsystem.HuellitasYATheme
import com.example.huellitasya.core.designsystem.OverlayScrim
import com.example.huellitasya.core.designsystem.RecordCardSurface
import com.example.huellitasya.core.designsystem.UploadProgressEnd
import com.example.huellitasya.core.designsystem.UploadProgressStart
import com.example.huellitasya.core.designsystem.UploadProgressText
import com.example.huellitasya.core.designsystem.components.FieldStatus
import com.example.huellitasya.core.designsystem.components.HuellitasButton
import com.example.huellitasya.core.designsystem.components.HuellitasDatePickerDialog
import com.example.huellitasya.core.designsystem.components.IconCardClickableField
import com.example.huellitasya.core.designsystem.components.IconCardDropdownField
import com.example.huellitasya.core.designsystem.components.IconCardTextField
import com.example.huellitasya.core.domain.validation.ValidatePetBirthDateUseCase
import com.example.huellitasya.core.model.LocalFileInfo
import com.example.huellitasya.core.model.PDF_CONTENT_TYPE
import com.example.huellitasya.core.model.Pet
import com.example.huellitasya.core.model.PetSex
import com.example.huellitasya.core.model.PetSpecies
import com.example.huellitasya.core.model.RecordCategory
import com.example.huellitasya.core.model.RecordType
import com.example.huellitasya.core.ui.FieldFeedback
import com.example.huellitasya.core.ui.ObserveAsEvents
import com.example.huellitasya.feature.records.components.RecordDateFormatter
import com.example.huellitasya.feature.records.components.RecordsHeader
import com.example.huellitasya.feature.records.components.labelRes
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.ceil

/** PDF o cualquier imagen (spec 006). */
private val PickableTypes = arrayOf(PDF_CONTENT_TYPE, "image/*")

@Composable
internal fun UploadRecordRoute(
    onBackClick: () -> Unit,
    onUploaded: () -> Unit,
    viewModel: UploadRecordViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        // `null` = el usuario cerró el selector sin elegir: no pasa nada.
        uri?.let { viewModel.onAction(UploadRecordAction.FilePicked(it.toString())) }
    }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is UploadRecordEvent.ShowMessage -> scope.launch {
                snackbarHostState.showSnackbar(event.message.asString(context))
            }
            UploadRecordEvent.Uploaded -> onUploaded()
        }
    }

    UploadRecordScreen(
        state = uiState,
        category = viewModel.category,
        snackbarHostState = snackbarHostState,
        onAction = viewModel::onAction,
        onBackClick = onBackClick,
        onPickFile = { filePicker.launch(PickableTypes) },
    )
}

/**
 * "Subir Archivo" (Figma 3193:3730): la tarjeta grande abre el selector; con el archivo elegido se
 * piden fecha, tipo y veterinario (desviación de Figma, spec 006). Al subir, la barra de Figma
 * 3281:1697 muestra el avance real.
 */
@Composable
internal fun UploadRecordScreen(
    state: UploadRecordUiState,
    category: RecordCategory,
    snackbarHostState: SnackbarHostState,
    onAction: (UploadRecordAction) -> Unit,
    onBackClick: () -> Unit,
    onPickFile: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (state.isUploading && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Modifier.blur(5.dp) else Modifier),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                RecordsHeader(pet = state.pet, category = category, onBackClick = onBackClick)
                val file = state.file
                if (file == null) {
                    UploadCard(onClick = onPickFile)
                } else {
                    SelectedFileCard(file = file, localUri = state.localUri, onClick = onPickFile)
                    IconCardClickableField(
                        valueText = state.date.format(RecordDateFormatter),
                        onClick = { showDatePicker = true },
                        label = stringResource(R.string.records_field_date),
                        icon = painterResource(R.drawable.ic_field_calendar),
                        iconSize = 40.dp,
                        status = state.dateFeedback.statusOrDefault,
                        message = state.dateFeedback?.message?.asString(),
                    )
                    IconCardDropdownField(
                        selected = state.type,
                        options = RecordType.entries,
                        optionLabel = { stringResource(it.labelRes) },
                        onSelected = { onAction(UploadRecordAction.TypeChanged(it)) },
                        label = stringResource(R.string.records_field_type),
                        icon = painterResource(R.drawable.ic_records_type),
                        iconSize = 40.dp,
                    )
                    IconCardTextField(
                        value = state.vet,
                        onValueChange = { onAction(UploadRecordAction.VetChanged(it)) },
                        label = stringResource(R.string.records_field_vet),
                        icon = painterResource(R.drawable.ic_field_person),
                        iconSize = 40.dp,
                        status = state.vetFeedback.statusOrDefault,
                        message = state.vetFeedback?.message?.asString(),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    HuellitasButton(
                        text = stringResource(R.string.records_upload_button),
                        onClick = {
                            focusManager.clearFocus()
                            onAction(UploadRecordAction.Upload)
                        },
                        containerColor = GreenPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.size(width = 279.dp, height = 48.dp),
                    )
                }
            }
        }

        state.progress?.let { UploadingOverlay(progress = it) }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding(),
        )
    }

    if (showDatePicker) {
        HuellitasDatePickerDialog(
            initialDate = state.date,
            minDate = ValidatePetBirthDateUseCase.MIN_DATE,
            confirmText = stringResource(R.string.records_date_accept),
            cancelText = stringResource(R.string.records_date_cancel),
            onDateSelected = { onAction(UploadRecordAction.DateChanged(it)) },
            onDismiss = { showDatePicker = false },
        )
    }
}

private val CardShape = RoundedCornerShape(10.dp)

/** Tarjeta grande "Subir Archivo" (Figma 3273:825): 412 dp de alto, ícono de 116 dp. */
@Composable
private fun UploadCard(onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(412.dp)
            .clip(CardShape)
            .background(RecordCardSurface)
            .border(1.dp, BorderSubtle, CardShape)
            .clickable(role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.img_records_upload),
            contentDescription = null,
            modifier = Modifier.size(116.dp),
        )
        Spacer(modifier = Modifier.height(22.dp))
        Text(
            text = stringResource(R.string.records_upload_title),
            color = Color.Black,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

/** El archivo elegido: ícono PDF o la imagen, nombre y tamaño. Tocarlo elige otro. */
@Composable
private fun SelectedFileCard(file: LocalFileInfo, localUri: String?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 94.dp)
            .clip(CardShape)
            .background(RecordCardSurface)
            .border(1.dp, BorderSubtle, CardShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(70.dp), contentAlignment = Alignment.Center) {
            if (file.contentType == PDF_CONTENT_TYPE || localUri == null) {
                Image(painter = painterResource(R.drawable.ic_records_pdf), contentDescription = null, modifier = Modifier.fillMaxSize())
            } else {
                AsyncImage(
                    model = localUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.padding(7.dp).fillMaxSize().clip(RoundedCornerShape(8.dp)),
                )
            }
        }
        Spacer(modifier = Modifier.width(6.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                color = Color.Black,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(text = fileSizeLabel(file.sizeBytes), color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(
                text = stringResource(R.string.records_upload_change),
                color = Color.Black.copy(alpha = 0.5f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/** "1,2 MB" o "350 KB"; un archivo de menos de 1 KB se muestra como "1 KB", nunca "0 KB". */
@Composable
private fun fileSizeLabel(bytes: Long): String {
    val kb = bytes / 1024.0
    return if (kb < 1024) {
        stringResource(R.string.records_size_kb, ceil(kb).coerceAtLeast(1.0).toLong().toString())
    } else {
        stringResource(R.string.records_size_mb, "%.1f".format(kb / 1024))
    }
}

/**
 * "Subiendo archivo…" (Figma 3273:859): barra de 312 × 35 dp con el degradado de Figma, cuyo
 * relleno sigue el avance real, sobre la pantalla desenfocada. Bloquea los toques.
 */
@Composable
private fun UploadingOverlay(progress: Float) {
    val animated by animateFloatAsState(targetValue = progress.coerceIn(0f, 1f), label = "uploadProgress")
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OverlayScrim)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {}),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(width = 312.dp, height = 35.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(GreenBackground),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animated)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Brush.horizontalGradient(listOf(UploadProgressStart, UploadProgressEnd))),
                )
            }
            Spacer(modifier = Modifier.height(26.dp))
            Text(
                text = stringResource(R.string.records_uploading),
                color = UploadProgressText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private val FieldFeedback?.statusOrDefault: FieldStatus get() = this?.status ?: FieldStatus.Default

private val PreviewPet = Pet("pet-1", "Maximiliano Tercero", PetSpecies.DOG, "Golden Retriever", LocalDate.now().minusMonths(3), PetSex.MALE, 5.0, null)

@Preview(showBackground = true, widthDp = 412, heightDp = 917)
@Composable
private fun UploadRecordScreenPreview() {
    HuellitasYATheme {
        UploadRecordScreen(
            state = UploadRecordUiState(pet = PreviewPet),
            category = RecordCategory.VACCINES,
            snackbarHostState = SnackbarHostState(),
            onAction = {},
            onBackClick = {},
            onPickFile = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917)
@Composable
private fun UploadRecordScreenFormPreview() {
    HuellitasYATheme {
        UploadRecordScreen(
            state = UploadRecordUiState(
                pet = PreviewPet,
                localUri = "content://carnet",
                file = LocalFileInfo("Carnet_Vacunación.pdf", PDF_CONTENT_TYPE, 1_250_000),
                type = RecordType.VACCINATION,
                vet = "Dr. A. Ramírez",
                progress = 0.6f,
            ),
            category = RecordCategory.VACCINES,
            snackbarHostState = SnackbarHostState(),
            onAction = {},
            onBackClick = {},
            onPickFile = {},
        )
    }
}
