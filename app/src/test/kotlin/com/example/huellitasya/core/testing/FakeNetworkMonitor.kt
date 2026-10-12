package com.example.huellitasya.core.testing

import com.example.huellitasya.core.domain.repository.NetworkMonitor
import kotlinx.coroutines.flow.MutableStateFlow

/** [NetworkMonitor] controlable desde los tests: cambia [isOnline] para simular la red. */
class FakeNetworkMonitor(initiallyOnline: Boolean = true) : NetworkMonitor {
    override val isOnline = MutableStateFlow(initiallyOnline)
}
