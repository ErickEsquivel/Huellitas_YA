package com.example.huellitasya.feature.records.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.huellitasya.core.model.RecordCategory
import com.example.huellitasya.feature.records.list.RecordListRoute
import kotlinx.serialization.Serializable

/**
 * Lista de una categoría de registros médicos (spec 006). Los nombres de las propiedades son las
 * claves que lee `RecordListViewModel`; `category` es el nombre de [RecordCategory].
 */
@Serializable
data class RecordListDestination(val petId: String, val category: String) {
    constructor(petId: String, category: RecordCategory) : this(petId, category.name)
}

/** @param onPetGone la mascota se eliminó: vuelve a Home. */
fun NavGraphBuilder.recordListScreen(
    onBack: () -> Unit,
    onAddClick: (petId: String, category: RecordCategory) -> Unit,
    onRecordClick: (petId: String, recordId: String) -> Unit,
    onPetGone: () -> Unit,
) {
    composable<RecordListDestination> {
        RecordListRoute(
            onBackClick = onBack,
            onAddClick = onAddClick,
            onRecordClick = onRecordClick,
            onPetGone = onPetGone,
        )
    }
}
