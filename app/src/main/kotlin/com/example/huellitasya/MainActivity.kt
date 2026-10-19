package com.example.huellitasya

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.huellitasya.core.designsystem.HuellitasYATheme
import com.example.huellitasya.navigation.HuellitasApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // El splash se mantiene hasta saber si hay sesión y dónde empieza (Home o Presentación, spec 005),
        // así no se ve una pantalla un instante antes de la correcta.
        installSplashScreen().setKeepOnScreenCondition {
            viewModel.uiState.value == MainUiState.Loading
        }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val googleClientId = googleServerClientId()
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            HuellitasYATheme {
                HuellitasApp(
                    uiState = uiState,
                    googleServerClientId = googleClientId,
                )
            }
        }
    }

    /**
     * `default_web_client_id` lo genera el plugin de Google Services solo si `google-services.json`
     * incluye el cliente OAuth web (proveedor Google habilitado + SHA-1 registrada). Se busca por
     * nombre para que la app compile y funcione con correo aunque Google aún no esté configurado.
     */
    @SuppressLint("DiscouragedApi")
    private fun googleServerClientId(): String? =
        resources.getIdentifier("default_web_client_id", "string", packageName)
            .takeIf { it != 0 }
            ?.let(::getString)
}
