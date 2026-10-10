package com.example.huellitasya.core.common

import javax.inject.Qualifier

/**
 * `CoroutineScope` que vive tanto como la aplicación. Se usa para trabajo que no debe
 * cancelarse cuando la pantalla que lo inició desaparece (p. ej. crear el perfil en
 * Firestore justo después de autenticarse, momento en que la UI cambia de grafo).
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ApplicationScope
