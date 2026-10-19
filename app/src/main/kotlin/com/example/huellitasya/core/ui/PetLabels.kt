package com.example.huellitasya.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.example.huellitasya.R
import com.example.huellitasya.core.model.Pet
import com.example.huellitasya.core.model.PetAge
import com.example.huellitasya.core.model.ageOn
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.util.Locale

/** "3 meses", "1 año", "12 días" (spec 005). */
@Composable
fun PetAge.label(): String = when (this) {
    is PetAge.Days -> pluralStringResource(R.plurals.pets_age_days, value, value)
    is PetAge.Months -> pluralStringResource(R.plurals.pets_age_months, value, value)
    is PetAge.Years -> pluralStringResource(R.plurals.pets_age_years, value, value)
}

/** "Golden Retriever  -  3 meses": subtítulo de la mascota en Home y en su perfil (Figma 19:100, 54:30). */
@Composable
fun Pet.breedAndAge(today: LocalDate = LocalDate.now()): String =
    stringResource(R.string.pets_breed_age, breed, ageOn(today).label())

/** "5 kg", "4,5 kg": sin decimales si es entero y con coma decimal. */
@Composable
fun Pet.weightLabel(): String = stringResource(R.string.pets_weight_value, WeightFormat.format(weightKg))

private val WeightFormat = DecimalFormat("0.##", DecimalFormatSymbols(Locale.forLanguageTag("es-CO")))
