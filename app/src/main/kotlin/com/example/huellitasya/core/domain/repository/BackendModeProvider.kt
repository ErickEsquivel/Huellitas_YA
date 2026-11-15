package com.example.huellitasya.core.domain.repository

/**
 * Con qué backend trabaja la app (spec 008). Lo decide el interruptor `blaze_activo` de Remote
 * Config, no la app: el plan de facturación no se puede consultar desde el cliente.
 */
enum class BackendMode {
    /** Plan Blaze: Cloud Functions y Storage (specs 002, 004, 005 y 006). */
    Blaze,

    /** Plan Spark: solo Auth y Firestore, con las alternativas gratuitas de la spec 008. */
    Free,
}

interface BackendModeProvider {
    /** El modo activo. Si el interruptor no se pudo leer, [BackendMode.Free], que funciona en los dos planes. */
    fun current(): BackendMode

    /** Descarga y activa el interruptor. Si falla (p. ej. sin red), se queda con el último valor activo. */
    suspend fun refresh()
}
