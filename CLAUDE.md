# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Proyecto

"Huellitas YA!": app Android 100 % Jetpack Compose + Material 3. MVVM, Hilt, multi-módulo Gradle y Firebase (Auth + Firestore).
- Versiones: Kotlin 2.2.10, AGP 9.4.1 (Kotlin integrado, sin plugin `kotlin-android`), Gradle 9.6.0, Java 11, minSdk 24, compile/targetSdk 37.
- El código, los comentarios y los textos de UI están en español.

**Specs primero:** `specs/` es la fuente de verdad del comportamiento (arquitectura, Firebase, auth, design system). Antes de implementar una feature, lee o actualiza su spec. Si cambias el comportamiento, actualiza la spec en el mismo cambio.

## Comandos

En Windows usar `gradlew.bat` (o `./gradlew` desde Git Bash).

```bash
./gradlew assembleDebug                                   # APK debug (requiere app/google-services.json)
./gradlew :core:domain:test testDebugUnitTest             # Todos los unit tests (módulos JVM usan `test`)
./gradlew :feature:auth:testDebugUnitTest --tests "*LoginViewModelTest"   # Un test/clase
./gradlew :core:domain:test --tests "*ValidatorsTest"
./gradlew lint
./gradlew signingReport                                   # SHA-1/SHA-256 para registrar en Firebase
```

- `app/google-services.json` **no** está en el repo (`.gitignore`). Sin él falla `processDebugGoogleServices`, y sin un cliente OAuth web no existe `R.string.default_web_client_id`. Los pasos están en `specs/001-firebase-setup.md`. Nunca lo commitees.
- Si Android Studio está abierto, Windows puede bloquear `app/build` ("Unable to delete directory"). Cierra el IDE o redirige el build dir de `:app` con un init script.

## Arquitectura

```
:app                MainActivity (splash + gate de sesión), HuellitasApplication (@HiltAndroidApp), navegación raíz
:core:model         Modelos puros (User, SessionState)                       ─┐
:core:common        AppResult/AppError, @ApplicationScope                     ├ Kotlin/JVM puro (sin Android)
:core:domain        Interfaces de repositorio, use cases, validadores         ─┘
:core:data          Firebase (FirebaseAuthRepository, FirestoreUserRepository) + módulos Hilt
:core:designsystem  Tema (Nunito, tokens de color), componentes Compose sin lógica
:core:ui            UiText, FieldFeedback, mapeo error→mensaje, ObserveAsEvents
:core:testing       Fakes de repositorios + MainDispatcherRule (solo testImplementation)
:feature:auth|home|calendar|profile|settings
build-logic/        Convention plugins: huellitas.android.{application,library,compose,feature}, huellitas.hilt, huellitas.jvm.library
```

- **Reglas de dependencia:** una feature nunca depende de `:core:data` ni de otra feature. `huellitas.android.feature` ya agrega designsystem, ui, domain, model, common, navigation, Hilt y `:core:testing` para los tests.
- Las dependencias van en `gradle/libs.versions.toml`.
- Entre módulos se usa `project(":core:x")`, no `projects.*`: el nombre del root ("Huellitas YA!") no genera accessors.

**Patrón MVVM por pantalla:**
- `XUiState` (data class) y `XAction` (sealed).
- `XEvent` para efectos de una sola vez, emitidos por `Channel` y recogidos con `ObserveAsEvents`.
- `XViewModel` (`@HiltViewModel`) depende solo de use cases.
- `XRoute` es el composable stateful (`hiltViewModel()` + `collectAsStateWithLifecycle`).
- `XScreen` es stateless, con `@Preview`. Es lo único que se rehace al aplicar un diseño de Figma.
- Los objetos de navegación se llaman `XDestination`, porque `XRoute` ya es el composable. Cada feature expone `NavGraphBuilder.xScreen()` o `authGraph()` en `navigation/`.

**Sesión y navegación (lo que no es obvio):**
- `HuellitasApp` elige el NavHost según `SessionState`: auth o principal.
- Login, registro y logout **no navegan**: Firebase cambia la sesión y se reemplaza el NavHost completo, con lo que el historial queda limpio.
- Por eso los use cases de inicio de sesión corren en `@ApplicationScope`: el ViewModel que los llamó se destruye en cuanto cambia el grafo, y así el perfil `users/{uid}` (`createIfMissing`) se escribe igual.
- En tests de ViewModel, pasa el `TestScope` (`appScope = this`) como ese scope. `advanceUntilIdle()` no ejecuta trabajo de `backgroundScope`.

**Errores:**
- Los repositorios devuelven `AppResult<D, AuthError|DataError>` en lugar de lanzar excepciones.
- La UI muestra `FieldFeedback(FieldStatus.Warning|Error, UiText)` bajo cada campo, con un aviso agrupado para campos vacíos. Los errores generales van a un Snackbar.

**Google Sign-In:** `GoogleIdTokenRequester` (Credential Manager) vive en `:feature:auth`, porque necesita un Activity. `:app` le pasa `default_web_client_id` a `authGraph()`.

## Figma

- Archivo: https://www.figma.com/design/02Bq5fbLLxTbaAD5hZ1sxS/Huellitas-YA- . Los IDs de los frames están en cada spec.
- Antes de `get_design_context`, carga la guía `figma-design-to-code`.
- Mapea el diseño a los tokens y componentes existentes de `:core:designsystem`, sin literales de color.
- Assets descargados:
  - PNG → WebP en `drawable-nodpi/`.
  - SVG → vector drawable.
  - Van en el módulo de la feature que los usa, o en designsystem si se comparten.
- Las medidas de Figma están en un frame de 412 dp de ancho. `AuthHeroSpec` reproduce el recorte de las fotos de cabecera escalando a ese ancho.
