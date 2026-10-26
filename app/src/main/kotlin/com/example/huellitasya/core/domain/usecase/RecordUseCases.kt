package com.example.huellitasya.core.domain.usecase

import com.example.huellitasya.core.common.AppResult
import com.example.huellitasya.core.common.map
import com.example.huellitasya.core.domain.error.DataError
import com.example.huellitasya.core.domain.repository.AuthRepository
import com.example.huellitasya.core.domain.repository.RecordRepository
import com.example.huellitasya.core.model.MedicalRecord
import com.example.huellitasya.core.model.RecordCategory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Registros de la mascota del usuario con sesión, en vivo (spec 006). Sin sesión no emite nada. */
class ObservePetRecordsUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val recordRepository: RecordRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(petId: String): Flow<AppResult<List<MedicalRecord>, DataError>> =
        authRepository.currentUser
            .distinctUntilChanged { old, new -> old?.uid == new?.uid }
            .flatMapLatest { user -> if (user == null) emptyFlow() else recordRepository.observeRecords(user.uid, petId) }
}

/** Los registros de una categoría, del más nuevo al más antiguo (la lista de la spec 006). */
class ObserveRecordsUseCase @Inject constructor(
    private val observePetRecords: ObservePetRecordsUseCase,
) {
    operator fun invoke(petId: String, category: RecordCategory): Flow<AppResult<List<MedicalRecord>, DataError>> =
        observePetRecords(petId).map { result -> result.map { records -> records.filter { it.category == category } } }
}

/** Cuántos archivos hay en cada categoría: el conteo de las tarjetas del perfil de la mascota. */
class ObserveRecordCountsUseCase @Inject constructor(
    private val observePetRecords: ObservePetRecordsUseCase,
) {
    operator fun invoke(petId: String): Flow<AppResult<Map<RecordCategory, Int>, DataError>> =
        observePetRecords(petId).map { result ->
            result.map { records -> RecordCategory.entries.associateWith { category -> records.count { it.category == category } } }
        }
}
