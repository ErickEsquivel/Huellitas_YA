package com.example.huellitasya.feature.pets.detail

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
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
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.huellitasya.R
import com.example.huellitasya.core.designsystem.DarkGrayText
import com.example.huellitasya.core.designsystem.GreenBackground
import com.example.huellitasya.core.designsystem.GreenCard
import com.example.huellitasya.core.designsystem.GreenPrimary
import com.example.huellitasya.core.designsystem.HuellitasYATheme
import com.example.huellitasya.core.designsystem.RecordCountGreen
import com.example.huellitasya.core.designsystem.components.HuellitasBackButton
import com.example.huellitasya.core.designsystem.components.HuellitasButton
import com.example.huellitasya.core.model.Pet
import com.example.huellitasya.core.model.PetSex
import com.example.huellitasya.core.model.PetSpecies
import com.example.huellitasya.core.model.RecordCategory
import com.example.huellitasya.core.ui.ObserveAsEvents
import com.example.huellitasya.core.ui.breedAndAge
import com.example.huellitasya.core.ui.weightLabel
import com.example.huellitasya.feature.pets.components.labelRes
import com.example.huellitasya.core.designsystem.components.greenWave
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** "06 de junio de 2026" (spec 005). */
private val LongDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-CO"))

@Composable
internal fun PetDetailRoute(
    onBackClick: () -> Unit,
    onEditClick: (petId: String) -> Unit,
    onRecordsClick: (petId: String, category: RecordCategory) -> Unit,
    onPetGone: () -> Unit,
    viewModel: PetDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is PetDetailEvent.ShowMessage -> scope.launch {
                snackbarHostState.showSnackbar(event.message.asString(context))
            }
            PetDetailEvent.PetGone -> onPetGone()
        }
    }

    PetDetailScreen(
        pet = uiState.pet,
        recordCounts = uiState.recordCounts,
        snackbarHostState = snackbarHostState,
        onBackClick = onBackClick,
        onEditClick = { onEditClick(viewModel.petId) },
        onRecordsClick = { onRecordsClick(viewModel.petId, it) },
    )
}

/** Figma "PERRO 1" (54:30). */
@Composable
internal fun PetDetailScreen(
    pet: Pet?,
    recordCounts: Map<RecordCategory, Int>,
    snackbarHostState: SnackbarHostState,
    onBackClick: () -> Unit,
    onEditClick: () -> Unit,
    onRecordsClick: (RecordCategory) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                HuellitasBackButton(
                    onClick = onBackClick,
                    contentDescription = stringResource(R.string.pets_detail_back),
                    modifier = Modifier.align(Alignment.TopStart).padding(top = 8.dp, start = 11.dp),
                )
                Text(
                    text = pet?.let { stringResource(R.string.pets_detail_title, it.name) }.orEmpty(),
                    color = Color.Black,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                )
            }
            if (pet != null) {
                PetDetailContent(pet = pet, recordCounts = recordCounts, onEditClick = onEditClick, onRecordsClick = onRecordsClick)
            }
        }

        // Sin barra inferior, el aviso debe quedar encima de la barra de navegación.
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding(),
        )
    }
}

@Composable
private fun PetDetailContent(
    pet: Pet,
    recordCounts: Map<RecordCategory, Int>,
    onEditClick: () -> Unit,
    onRecordsClick: (RecordCategory) -> Unit,
) {
    Spacer(modifier = Modifier.height(8.dp))
    PetDetailPhoto(photoUrl = pet.photoUrl, contentDescription = stringResource(R.string.pets_photo_description))
    Spacer(modifier = Modifier.height(16.dp))
    Text(text = pet.breedAndAge(), color = Color.Black, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
    Spacer(modifier = Modifier.height(32.dp))

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PetDetailRow(
            icon = R.drawable.ic_pets_calendar,
            label = stringResource(R.string.pets_detail_birth_date),
            value = pet.birthDate.format(LongDateFormatter),
        )
        PetDetailRow(icon = R.drawable.ic_pets_scale, label = stringResource(R.string.pets_detail_weight), value = pet.weightLabel())
        PetDetailRow(
            icon = R.drawable.ic_pets_sex,
            label = stringResource(R.string.pets_detail_sex),
            value = stringResource(pet.sex.labelRes),
            iconAlpha = 0.7f,
        )
    }

    // La ola y los registros van juntos: la ola siempre empieza sobre "Registros Medicos".
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp)
            .greenWave(horizontalBleed = 24.dp)
            .padding(top = 56.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.pets_detail_records),
            color = Color.Black,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(24.dp))
        // Las tarjetas ocupan casi todo el ancho, como en Figma (10 dp de margen).
        Row(
            modifier = Modifier.fillMaxWidth().bleedHorizontally(14.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            MedicalRecordCard(
                icon = R.drawable.ic_pets_record_vaccines,
                title = stringResource(R.string.pets_record_vaccines),
                count = recordCounts[RecordCategory.VACCINES] ?: 0,
                onClick = { onRecordsClick(RecordCategory.VACCINES) },
                modifier = Modifier.weight(1f),
            )
            MedicalRecordCard(
                icon = R.drawable.ic_pets_record_history,
                title = stringResource(R.string.pets_record_history),
                count = recordCounts[RecordCategory.HISTORY] ?: 0,
                onClick = { onRecordsClick(RecordCategory.HISTORY) },
                modifier = Modifier.weight(1f),
            )
            MedicalRecordCard(
                icon = R.drawable.ic_pets_record_files,
                title = stringResource(R.string.pets_record_files),
                count = recordCounts[RecordCategory.FILES] ?: 0,
                onClick = { onRecordsClick(RecordCategory.FILES) },
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(56.dp))
        HuellitasButton(
            text = stringResource(R.string.pets_detail_edit),
            onClick = onEditClick,
            containerColor = GreenPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.size(width = 279.dp, height = 48.dp),
        )
    }
}

/** Ensancha el contenido [extra] a cada lado, por encima del padding del contenedor. */
private fun Modifier.bleedHorizontally(extra: Dp): Modifier = layout { measurable, constraints ->
    val extraPx = extra.roundToPx()
    val width = constraints.maxWidth + 2 * extraPx
    val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
    layout(constraints.maxWidth, placeable.height) { placeable.place(-extraPx, 0) }
}

/** Foto circular de 120 dp sobre el círculo verde (Figma 56:88). Sin foto, el ícono de la mascota. */
@Composable
private fun PetDetailPhoto(photoUrl: String?, contentDescription: String) {
    Box(
        modifier = Modifier.size(120.dp).clip(CircleShape).background(GreenBackground),
        contentAlignment = Alignment.Center,
    ) {
        if (photoUrl == null) {
            Image(
                painter = painterResource(R.drawable.ic_pets_breed),
                contentDescription = contentDescription,
                modifier = Modifier.size(80.dp),
            )
        } else {
            AsyncImage(
                model = photoUrl,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** Fila "ícono · etiqueta · valor" (Figma: etiqueta a 98 dp y valor a 222 dp del borde). */
@Composable
private fun PetDetailRow(@DrawableRes icon: Int, label: String, value: String, iconAlpha: Float = 1f) {
    Row(modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = DarkGrayText.copy(alpha = iconAlpha),
            modifier = Modifier.width(50.dp).height(43.dp).padding(horizontal = 4.dp),
        )
        Spacer(modifier = Modifier.width(20.dp))
        Text(
            text = label,
            color = DarkGrayText,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.width(110.dp),
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(text = value, color = DarkGrayText, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
    }
}

/** Tarjeta verde de "Registros Medicos" (Figma 56:144): abre su categoría (spec 006) y cuenta sus archivos. */
@Composable
private fun MedicalRecordCard(
    @DrawableRes icon: Int,
    title: String,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .height(149.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(GreenCard)
            .clickable(onClickLabel = title, role = Role.Button, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(painter = painterResource(icon), contentDescription = null, tint = Color.White, modifier = Modifier.size(51.dp))
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = title,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            lineHeight = 14.sp,
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = pluralStringResource(R.plurals.pets_record_count, count, count),
            color = RecordCountGreen,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917)
@Composable
private fun PetDetailScreenPreview() {
    HuellitasYATheme {
        PetDetailScreen(
            pet = Pet(
                id = "pet-1",
                name = "Maximiliano Tercero",
                species = PetSpecies.DOG,
                breed = "Golden Retriever",
                birthDate = LocalDate.now().minusMonths(3),
                sex = PetSex.MALE,
                weightKg = 5.0,
                photoUrl = null,
            ),
            recordCounts = mapOf(RecordCategory.VACCINES to 3, RecordCategory.HISTORY to 1),
            snackbarHostState = SnackbarHostState(),
            onBackClick = {},
            onEditClick = {},
            onRecordsClick = {},
        )
    }
}
