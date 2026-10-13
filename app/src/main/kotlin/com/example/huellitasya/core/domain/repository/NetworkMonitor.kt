package com.example.huellitasya.core.domain.repository

import kotlinx.coroutines.flow.Flow

/** Estado de la conexión a internet del dispositivo. */
interface NetworkMonitor {
    /** `true` mientras haya una red con acceso a internet verificado. Emite al cambiar. */
    val isOnline: Flow<Boolean>
}
