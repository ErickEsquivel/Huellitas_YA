# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Proyecto

"Huellitas YA!": app Android 100 % Jetpack Compose + Material 3. MVVM + Hilt + Firebase (Auth + Firestore), en **un solo módulo Gradle (`:app`)** organizado por paquetes.
- Versiones: Kotlin 2.2.10, AGP 9.4.1 (Kotlin integrado, sin plugin `kotlin-android`), Gradle 9.6.0, Java 11, minSdk 24, compile/targetSdk 37.
- El código, los comentarios y los textos de UI están en español.

**Specs primero:** `specs/` es la fuente de verdad del comportamiento (arquitectura, Firebase, auth, design system). Antes de implementar una feature, lee o actualiza su spec. Si cambias el comportamiento, actualiza la spec en el mismo cambio.

## Comandos

En Windows usar `gradlew.bat` (o `./gradlew` desde Git Bash).

```bash
./gradlew assembleDebug                                    # APK debug (requiere app/google-services.json)
./gradlew testDebugUnitTest                                # Todos los unit tests
./gradlew testDebugUnitTest --tests "*LoginViewModelTest"  # Un test/clase
./gradlew lint
./gradlew signingReport                                    # SHA-1/SHA-256 para registrar en Firebase
```

- `app/google-services.json` (en `app/`, no en la raíz) **no** está en el repo (`.gitignore`). Sin él falla `processDebugGoogleServices`. Nunca lo commitees.
- Si el archivo no trae un cliente OAuth web, no existe `R.string.default_web_client_id`. `MainActivity` lo busca por nombre con `getIdentifier`, así que la app compila y solo falla el login con Google. Los pasos de configuración están en `specs/001-firebase-setup.md`.
- Si Android Studio está abierto, Windows puede bloquear `app/build` ("Unable to delete directory"). Cierra el IDE o redirige el build dir con un init script.
- Las dependencias van en `gradle/libs.versions.toml` y se declaran en `app/build.gradle.kts`.

## Arquitectura (`app/src/main/kotlin/com/example/huellitasya/`)

```
MainActivity, HuellitasApplication, MainViewModel   splash + gate de sesión, @HiltAndroidApp
navigation/          HuellitasApp (NavHost según sesión), barra inferior, TopLevelDestination
core/model|common|domain   Modelos, AppResult/AppError, @ApplicationScope, repos (interfaces), use cases, validadores
core/data            Firebase (FirebaseAuthRepository, FirestoreUserRepository) + módulos Hilt (di/)
core/designsystem    Tema (Nunito, tokens de color), componentes Compose sin lógica
core/ui              UiText, FieldFeedback, mapeo error→mensaje, ObserveAsEvents
feature/auth|home|calendar|profile|settings
```

Tests en `app/src/test/kotlin/...` con la misma estructura. `core/testing/` tiene `FakeAuthRepository`, `FakeUserRepository` y `MainDispatcherRule`.

**Reglas de capas.** El compilador no las impone porque todo es un solo módulo, así que respétalas a mano (spec 000):
- `feature.*` nunca importa `core.data` ni otra feature.
- `core.model`, `core.common` y `core.domain` no importan Android ni Firebase.
- Firebase solo se usa en `core.data`.

**Recursos:** hay un único `R` (`com.example.huellitasya.R`). Los strings van en `res/values/strings_<área>.xml` y los recursos de una feature llevan su prefijo (`img_auth_*`, `ic_settings_*`).

**Patrón MVVM por pantalla:**
- `XUiState` (data class) y `XAction` (sealed).
- `XEvent` para efectos de una sola vez, emitidos por `Channel` y recogidos con `ObserveAsEvents`.
- `XViewModel` (`@HiltViewModel`) depende solo de use cases.
- `XRoute` es el composable stateful (`hiltViewModel()` + `collectAsStateWithLifecycle`).
- `XScreen` es stateless, con `@Preview`. Es lo único que se rehace al aplicar un diseño de Figma.
- Los objetos de navegación se llaman `XDestination`, porque `XRoute` ya es el composable. Cada feature expone `NavGraphBuilder.xScreen()` o `authGraph()` en su paquete `navigation/`.

**Sesión y navegación (lo que no es obvio):**
- `HuellitasApp` elige el NavHost según `SessionState`: auth o principal.
- Login, registro y logout **no navegan**: Firebase cambia la sesión y se reemplaza el NavHost completo, con lo que el historial queda limpio.
- Por eso los use cases de inicio de sesión corren en `@ApplicationScope`: el ViewModel que los llamó se destruye en cuanto cambia el grafo, y así el perfil `users/{uid}` (`createIfMissing`) se escribe igual.
- En tests de ViewModel, pasa el `TestScope` (`appScope = this`) como ese scope. `advanceUntilIdle()` no ejecuta trabajo de `backgroundScope`.

**Errores:**
- Los repositorios devuelven `AppResult<D, AuthError|DataError>` en lugar de lanzar excepciones.
- La UI muestra `FieldFeedback(FieldStatus.Warning|Error, UiText)` bajo cada campo, con un aviso agrupado para campos vacíos. Los errores generales van a un Snackbar.

**Google Sign-In:** `GoogleIdTokenRequester` (Credential Manager, en `feature.auth.google`) necesita un Activity. `MainActivity` le pasa `default_web_client_id` a `authGraph()`.

## Figma

- Archivo: https://www.figma.com/design/02Bq5fbLLxTbaAD5hZ1sxS/Huellitas-YA- . Los IDs de los frames están en cada spec.
- Antes de `get_design_context`, carga la guía `figma-design-to-code`.
- Mapea el diseño a los tokens y componentes existentes de `core.designsystem`, sin literales de color.
- Assets descargados:
  - PNG → WebP en `drawable-nodpi/`.
  - SVG → vector drawable.
- Las medidas de Figma están en un frame de 412 dp de ancho. `AuthHeroSpec` reproduce el recorte de las fotos de cabecera escalando a ese ancho.
