package com.example.huellitasya.feature.pets.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.huellitasya.R
import com.example.huellitasya.core.designsystem.components.FieldMessage
import com.example.huellitasya.core.designsystem.components.FieldStatus
import com.example.huellitasya.core.designsystem.components.HuellitasDatePickerDialog
import com.example.huellitasya.core.designsystem.components.IconCardClickableField
import com.example.huellitasya.core.designsystem.components.IconCardDropdownField
import com.example.huellitasya.core.designsystem.components.IconCardTextField
import com.example.huellitasya.core.designsystem.components.IconCardTextStartNarrow
import com.example.huellitasya.core.domain.validation.ValidatePetBirthDateUseCase
import com.example.huellitasya.core.model.PetSex
import com.example.huellitasya.core.model.PetSpecies
import com.example.huellitasya.core.ui.FieldFeedback
import com.example.huellitasya.feature.pets.form.PetFormAction
import com.example.huellitasya.feature.pets.form.PetFormState
import java.time.format.DateTimeFormatter

/** Figma muestra la fecha como `06    /    06    /2026`. */
private val BirthDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd  /  MM  /yyyy")

/**
 * Campos del formulario de mascota (Figma 19:63), compartidos por Registrar y Editar:
 * Nombre, Especie, Raza, Fecha de nacimiento y, en una fila, Sexo y Peso. Debajo, el aviso
 * del formulario ("Rellene todas las casillas indicadas").
 */
@Composable
internal fun PetFormFields(
    form: PetFormState,
    onAction: (PetFormAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        IconCardTextField(
            value = form.name,
            onValueChange = { onAction(PetFormAction.NameChanged(it)) },
            label = stringResource(R.string.pets_field_name),
            icon = painterResource(R.drawable.ic_field_person),
            iconSize = 40.dp,
            status = form.nameFeedback.statusOrDefault,
            message = form.nameFeedback?.message?.asString(),
            keyboardOptions = TextKeyboard,
        )
        IconCardDropdownField(
            selected = form.species,
            options = PetSpecies.entries,
            optionLabel = { stringResource(it.labelRes) },
            onSelected = { onAction(PetFormAction.SpeciesChanged(it)) },
            label = stringResource(R.string.pets_field_species),
            icon = painterResource(R.drawable.ic_pets_species),
            iconSize = 60.dp,
            status = form.speciesFeedback.statusOrDefault,
        )
        IconCardTextField(
            value = form.breed,
            onValueChange = { onAction(PetFormAction.BreedChanged(it)) },
            label = stringResource(R.string.pets_field_breed),
            icon = painterResource(R.drawable.ic_pets_breed),
            iconSize = 60.dp,
            status = form.breedFeedback.statusOrDefault,
            message = form.breedFeedback?.message?.asString(),
            keyboardOptions = TextKeyboard,
        )
        IconCardClickableField(
            valueText = form.birthDate?.format(BirthDateFormatter),
            onClick = { showDatePicker = true },
            label = stringResource(R.string.pets_field_birth_date),
            icon = painterResource(R.drawable.ic_field_calendar),
            iconSize = 40.dp,
            status = form.birthDateFeedback.statusOrDefault,
            message = form.birthDateFeedback?.message?.asString(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            IconCardDropdownField(
                selected = form.sex,
                options = PetSex.entries,
                optionLabel = { stringResource(it.labelRes) },
                onSelected = { onAction(PetFormAction.SexChanged(it)) },
                label = stringResource(R.string.pets_field_sex),
                icon = painterResource(R.drawable.ic_pets_sex),
                iconSize = 44.dp,
                textStart = IconCardTextStartNarrow,
                status = form.sexFeedback.statusOrDefault,
                modifier = Modifier.weight(1f),
            )
            IconCardTextField(
                value = form.weight,
                onValueChange = { value -> onAction(PetFormAction.WeightChanged(value.filter { it.isDigit() || it == '.' || it == ',' })) },
                label = stringResource(R.string.pets_field_weight),
                icon = painterResource(R.drawable.ic_pets_weight),
                iconSize = 50.dp,
                textStart = IconCardTextStartNarrow,
                suffix = stringResource(R.string.pets_weight_unit),
                status = form.weightFeedback.statusOrDefault,
                message = form.weightFeedback?.message?.asString(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                modifier = Modifier.weight(1f),
            )
        }
        form.formFeedback?.message?.let { message ->
            FieldMessage(text = message.asString(), status = form.formFeedback.status)
        }
    }

    if (showDatePicker) {
        HuellitasDatePickerDialog(
            initialDate = form.birthDate,
            minDate = ValidatePetBirthDateUseCase.MIN_DATE,
            confirmText = stringResource(R.string.pets_date_accept),
            cancelText = stringResource(R.string.pets_date_cancel),
            onDateSelected = { onAction(PetFormAction.BirthDateChanged(it)) },
            onDismiss = { showDatePicker = false },
        )
    }
}

private val TextKeyboard = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next)

private val FieldFeedback?.statusOrDefault: FieldStatus get() = this?.status ?: FieldStatus.Default

internal val PetSpecies.labelRes: Int
    get() = when (this) {
        PetSpecies.DOG -> R.string.pets_species_dog
        PetSpecies.CAT -> R.string.pets_species_cat
        PetSpecies.OTHER -> R.string.pets_species_other
    }

internal val PetSex.labelRes: Int
    get() = when (this) {
        PetSex.MALE -> R.string.pets_sex_male
        PetSex.FEMALE -> R.string.pets_sex_female
    }
