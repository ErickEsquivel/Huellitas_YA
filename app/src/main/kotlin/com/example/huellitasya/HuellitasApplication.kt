package com.example.huellitasya

import android.app.Application
import com.example.huellitasya.core.common.ApplicationScope
import com.example.huellitasya.core.domain.repository.BackendModeProvider
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class HuellitasApplication : Application() {

    @Inject
    lateinit var backendMode: BackendModeProvider

    @Inject
    @ApplicationScope
    lateinit var appScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        // Lee el interruptor `blaze_activo` al abrir la app (spec 008). Hasta que llegue, se usa
        // el último valor activo o, la primera vez, el modo gratuito.
        appScope.launch { backendMode.refresh() }
    }
}
