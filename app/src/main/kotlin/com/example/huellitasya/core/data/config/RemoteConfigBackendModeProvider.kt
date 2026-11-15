package com.example.huellitasya.core.data.config

import android.content.Context
import android.content.pm.ApplicationInfo
import com.example.huellitasya.core.domain.repository.BackendMode
import com.example.huellitasya.core.domain.repository.BackendModeProvider
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lee el interruptor `blaze_activo` de Remote Config (spec 008). Remote Config es gratis en el
 * plan Spark. El valor por defecto es `false`: sin poder leerlo, la app usa el modo gratuito.
 */
@Singleton
internal class RemoteConfigBackendModeProvider @Inject constructor(
    @param:ApplicationContext context: Context,
    private val remoteConfig: FirebaseRemoteConfig,
) : BackendModeProvider {

    init {
        val isDebuggable = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        remoteConfig.setConfigSettingsAsync(
            remoteConfigSettings {
                // En debug se lee en cada apertura para poder probar los dos modos.
                minimumFetchIntervalInSeconds = if (isDebuggable) 0 else FETCH_INTERVAL_SECONDS
            },
        )
        remoteConfig.setDefaultsAsync(mapOf(BLAZE_ACTIVE_KEY to false))
    }

    override fun current(): BackendMode =
        if (remoteConfig.getBoolean(BLAZE_ACTIVE_KEY)) BackendMode.Blaze else BackendMode.Free

    override suspend fun refresh() {
        try {
            remoteConfig.fetchAndActivate().await()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Sin red o con Remote Config caído se sigue con el último valor activo.
        }
    }

    private companion object {
        const val BLAZE_ACTIVE_KEY = "blaze_activo"
        const val FETCH_INTERVAL_SECONDS = 3_600L
    }
}
