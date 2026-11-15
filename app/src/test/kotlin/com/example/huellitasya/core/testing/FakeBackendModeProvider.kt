package com.example.huellitasya.core.testing

import com.example.huellitasya.core.domain.repository.BackendMode
import com.example.huellitasya.core.domain.repository.BackendModeProvider

/**
 * Interruptor del modo sin facturación (spec 008) controlable desde los tests. Por defecto
 * [BackendMode.Blaze], el comportamiento de las specs 002 y 004.
 */
class FakeBackendModeProvider(var mode: BackendMode = BackendMode.Blaze) : BackendModeProvider {
    var refreshCount = 0
        private set

    override fun current(): BackendMode = mode

    override suspend fun refresh() {
        refreshCount++
    }
}
