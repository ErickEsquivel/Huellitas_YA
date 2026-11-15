package com.example.huellitasya.core.domain.usecase

import com.example.huellitasya.core.domain.repository.BackendMode
import com.example.huellitasya.core.domain.repository.BackendModeProvider
import javax.inject.Inject

/**
 * Modo del backend (spec 008). Las pantallas lo consultan para elegir entre código y enlace;
 * los repositorios lo consultan para elegir la implementación.
 */
class GetBackendModeUseCase @Inject constructor(
    private val provider: BackendModeProvider,
) {
    operator fun invoke(): BackendMode = provider.current()
}
