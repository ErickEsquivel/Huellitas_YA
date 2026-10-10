# Huellitas YA!

App Android para organizar vacunas, historial médico y eventos de tus mascotas.

## Configuración del SDK y del proyecto
- Nombre del proyecto: Huellitas YA!
- Paquete / namespace: `com.example.huellitasya`
- minSdk: 24 (Android 7.0 Nougat)
- compileSdk / targetSdk: 37

## Herramientas de construcción y versiones
- IDE: Android Studio 2026.1.4
- Android Gradle Plugin (AGP): 9.4.1
- Gradle Wrapper: 9.6.0
- Kotlin: 2.2.10
- Compatibilidad Java: Java 11

## Stack
- UI: Jetpack Compose + Material 3 (Compose BOM 2026.02.01), tipografía Nunito.
- Arquitectura: MVVM, Hilt, Navigation Compose type-safe, multi-módulo con convention plugins (`build-logic/`).
- Backend: Firebase Authentication (correo/contraseña y Google) y Cloud Firestore.

## Módulos
| Módulo | Contenido |
|--------|-----------|
| `:app` | Punto de entrada, gate de sesión y navegación raíz |
| `:core:model`, `:core:common`, `:core:domain` | Modelos, resultados tipados, repositorios (interfaces), use cases y validaciones (Kotlin puro) |
| `:core:data` | Implementaciones con Firebase + módulos Hilt |
| `:core:designsystem` | Tema, tokens y componentes Compose |
| `:core:ui` | Utilidades de UI compartidas |
| `:core:testing` | Fakes y utilidades para tests |
| `:feature:*` | auth, home, calendar, profile, settings |

## Primeros pasos
1. Pide al equipo el archivo `google-services.json` (o descárgalo de la consola de Firebase) y cópialo en `app/`. **No se sube al repositorio.**
2. Registra la huella SHA-1 de tu keystore debug en Firebase (`./gradlew signingReport`) para que funcione el inicio de sesión con Google.
3. `./gradlew assembleDebug` y ejecuta la app desde Android Studio.

El detalle de cada paso está en [`specs/001-firebase-setup.md`](specs/001-firebase-setup.md).

## Especificaciones
El comportamiento de cada feature está definido en [`specs/`](specs/README.md). Los mockups están en [Figma](https://www.figma.com/design/02Bq5fbLLxTbaAD5hZ1sxS/Huellitas-YA-).

## Tests
```bash
./gradlew :core:domain:test testDebugUnitTest
```
