package com.example.huellitasya.feature.records.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.huellitasya.core.model.RecordCategory
import com.example.huellitasya.feature.records.detail.RecordDetailRoute
import com.example.huellitasya.feature.records.list.RecordListRoute
import com.example.huellitasya.feature.records.upload.UploadRecordRoute
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

/** "Subir Archivo" de una categoría. Las propiedades son las claves que lee `UploadRecordViewModel`. */
@Serializable
data class UploadRecordDestination(val petId: String, val category: String) {
    constructor(petId: String, category: RecordCategory) : this(petId, category.name)
}

/** @param onUploaded el archivo quedó guardado: vuelve a la lista. */
fun NavGraphBuilder.uploadRecordScreen(
    onBack: () -> Unit,
    onUploaded: () -> Unit,
) {
    composable<UploadRecordDestination> {
        UploadRecordRoute(onBackClick = onBack, onUploaded = onUploaded)
    }
}

/** Detalle de un registro. Las propiedades son las claves que lee `RecordDetailViewModel`. */
@Serializable
data class RecordDetailDestination(val petId: String, val recordId: String)

/**
 * @param onDeleted el registro se eliminó: vuelve a la lista.
 * @param onPetGone la mascota se eliminó: vuelve a Home.
 */
fun NavGraphBuilder.recordDetailScreen(
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    onPetGone: () -> Unit,
) {
    composable<RecordDetailDestination> {
        RecordDetailRoute(onBackClick = onBack, onDeleted = onDeleted, onPetGone = onPetGone)
    }
}
