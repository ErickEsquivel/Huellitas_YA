# 01 — Estructura del proyecto

Este documento recorre las carpetas del proyecto y dice para qué sirve cada una. La app es **un solo módulo Gradle** (`:app`), ordenado por paquetes.

## Vista general

```text
Huellitas_YA/
├── app/                      La app Android
│   ├── build.gradle.kts      Dependencias y configuración de compilación
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml   Permisos, Activity y Application
│       │   ├── kotlin/com/example/huellitasya/   Todo el código Kotlin
│       │   └── res/                  Textos, imágenes, íconos y temas
│       └── test/             Pruebas unitarias (corren en la computadora)
├── firebase/                 Reglas de seguridad de Firestore y Storage
├── functions/                Backend: Cloud Functions en TypeScript
├── specs/                    Especificaciones: qué debe hacer cada módulo
├── docs/                     Esta guía
├── gradle/libs.versions.toml Catálogo con las versiones de las librerías
├── firebase.json, .firebaserc  Configuración de la Firebase CLI
└── CLAUDE.md                 Reglas del proyecto para quien escribe código
```

> `app/google-services.json` conecta la app con Firebase. **No está en el repositorio** (es privado): cada integrante lo descarga de la consola de Firebase.

## Código Kotlin: `app/src/main/kotlin/com/example/huellitasya/`

### Raíz: arranque de la app

| Archivo | Para qué sirve |
|---|---|
| [`HuellitasApplication.kt`](../app/src/main/kotlin/com/example/huellitasya/HuellitasApplication.kt) | Clase `Application` con `@HiltAndroidApp`: enciende la inyección de dependencias (Hilt) para toda la app. |
| [`MainActivity.kt`](../app/src/main/kotlin/com/example/huellitasya/MainActivity.kt) | La única `Activity`. Muestra el splash, aplica el tema y dibuja `HuellitasApp`. También recibe el toque en un aviso de recordatorio y pide abrir el Calendario. |
| [`MainViewModel.kt`](../app/src/main/kotlin/com/example/huellitasya/MainViewModel.kt) | Decide qué mostrar según la sesión: cargando, sin sesión (pantallas de login) o con sesión (app principal). Además mantiene los avisos de recordatorios al día. |

### `navigation/`: cómo se pasa de una pantalla a otra

| Archivo | Para qué sirve |
|---|---|
| [`HuellitasApp.kt`](../app/src/main/kotlin/com/example/huellitasya/navigation/HuellitasApp.kt) | Arma los dos "mapas" de navegación (`NavHost`): el de autenticación y el principal. Conecta cada pantalla con su destino. |
| [`HuellitasBottomBar.kt`](../app/src/main/kotlin/com/example/huellitasya/navigation/HuellitasBottomBar.kt) | La barra inferior con Home, Calendario, Perfil y Configuración. |
| [`TopLevelDestination.kt`](../app/src/main/kotlin/com/example/huellitasya/navigation/TopLevelDestination.kt) | La lista de esas cuatro pestañas, con su ícono. |

### `core/`: lo que comparten todas las pantallas

| Carpeta | Para qué sirve | Ejemplos |
|---|---|---|
| [`core/model/`](../app/src/main/kotlin/com/example/huellitasya/core/model) | **Modelos**: las clases de datos de la app. Kotlin puro, sin Android ni Firebase. | `Pet`, `Reminder`, `MedicalRecord`, `User`, `SessionState` |
| [`core/common/`](../app/src/main/kotlin/com/example/huellitasya/core/common) | Piezas básicas de uso general. | `AppResult` (éxito o error), `@ApplicationScope` |
| [`core/domain/repository/`](../app/src/main/kotlin/com/example/huellitasya/core/domain/repository) | **Interfaces de repositorio**: qué operaciones existen (guardar, leer, borrar), sin decir cómo. | `PetRepository`, `ReminderRepository`, `NetworkMonitor` |
| [`core/domain/usecase/`](../app/src/main/kotlin/com/example/huellitasya/core/domain/usecase) | **Casos de uso**: una acción de negocio cada uno. | `CreateReminderUseCase`, `RegisterPetUseCase`, `SignInWithEmailUseCase` |
| [`core/domain/validation/`](../app/src/main/kotlin/com/example/huellitasya/core/domain/validation) | **Validaciones** de formularios (correo, contraseña, nombre, peso, fechas…). | `Validators.kt` |
| [`core/domain/error/`](../app/src/main/kotlin/com/example/huellitasya/core/domain/error) | Los **tipos de error** que puede devolver cada operación. | `AuthError`, `DataError`, `ValidationError` |
| [`core/data/repository/`](../app/src/main/kotlin/com/example/huellitasya/core/data/repository) | **Implementaciones con Firebase** de las interfaces del dominio. Es el único lugar que habla con Firebase. | `FirestorePetRepository`, `FirebaseAuthRepository`, `FirebaseStorageRecordFileRepository` |
| [`core/data/mapper/`](../app/src/main/kotlin/com/example/huellitasya/core/data/mapper) | Convierte documentos de Firestore en modelos (y al revés) y excepciones de Firebase en errores de la app. | `FirebaseMappers.kt` |
| [`core/data/di/`](../app/src/main/kotlin/com/example/huellitasya/core/data/di) | **Módulos de Hilt**: dicen qué implementación usar para cada interfaz y cómo crear Firebase. | `DataModule.kt`, `TimeModule.kt` |
| [`core/data/network/`](../app/src/main/kotlin/com/example/huellitasya/core/data/network) | Detecta si hay internet. | `ConnectivityNetworkMonitor` |
| [`core/data/image/`](../app/src/main/kotlin/com/example/huellitasya/core/data/image) | Reduce y comprime las fotos antes de subirlas. | `JpegCompressor` |
| [`core/data/notifications/`](../app/src/main/kotlin/com/example/huellitasya/core/data/notifications) | Programa y muestra los avisos de recordatorios con WorkManager. | `WorkManagerReminderNotifier`, `ReminderWorker` |
| [`core/data/download/`](../app/src/main/kotlin/com/example/huellitasya/core/data/download) | Descarga archivos de registros a la carpeta Descargas. | `DownloadManagerRecordDownloader` |
| [`core/designsystem/`](../app/src/main/kotlin/com/example/huellitasya/core/designsystem) | **Sistema de diseño**: colores, tipografía (Nunito), tema y componentes visuales reutilizables. No tiene lógica. | `Color.kt`, `HuellitasButton`, `IconCardField`, `DeleteConfirmDialog` |
| [`core/ui/`](../app/src/main/kotlin/com/example/huellitasya/core/ui) | Ayudas para la UI que usan varias pantallas. | `UiText`, `ObserveAsEvents`, mensajes de error, Google Sign-In, vista previa de PDF |

### `feature/`: una carpeta por módulo de la app

Cada pantalla tiene su `XScreen.kt` (lo que se ve) y su `XViewModel.kt` (la lógica). Cada módulo tiene una carpeta `navigation/` con sus destinos.

| Carpeta | Módulo | Pantallas |
|---|---|---|
| [`feature/auth/`](../app/src/main/kotlin/com/example/huellitasya/feature/auth) | Autenticación (spec 002) | Bienvenido (`chooser`), Iniciar sesión (`login`), Crear cuenta (`register`), Recuperar contraseña (`forgotpassword` → `verifycode` → `newpassword`) y el aviso "Sin conexión" (`connectivity`) |
| [`feature/home/`](../app/src/main/kotlin/com/example/huellitasya/feature/home) | Inicio (spec 005) | Home con la tarjeta de cada mascota |
| [`feature/pets/`](../app/src/main/kotlin/com/example/huellitasya/feature/pets) | Mascotas (spec 005) | Presentación (`intro`), Registrar (`register`), Perfil (`detail`), Editar (`edit`), y el formulario compartido (`form`, `components`) |
| [`feature/records/`](../app/src/main/kotlin/com/example/huellitasya/feature/records) | Registros médicos (spec 006) | Lista por categoría (`list`), Subir archivo (`upload`), Detalle (`detail`) |
| [`feature/calendar/`](../app/src/main/kotlin/com/example/huellitasya/feature/calendar) | Calendario (spec 007) | Calendario, Nuevo recordatorio (`newreminder`), Detalle (`detail`), Notificaciones (`notifications`) |
| [`feature/profile/`](../app/src/main/kotlin/com/example/huellitasya/feature/profile) | Perfil (spec 004) | Perfil, Datos personales (`personaldata`), Verificar correo (`verifyemail`) |
| [`feature/settings/`](../app/src/main/kotlin/com/example/huellitasya/feature/settings) | Configuración (spec 004) | Configuración, Seguridad (`security`), eliminar cuenta |

## Recursos: `app/src/main/res/`

| Carpeta | Para qué sirve |
|---|---|
| [`values/`](../app/src/main/res/values) | Textos en español, separados por área: `strings_auth.xml`, `strings_pets.xml`, `strings_records.xml`, `strings_calendar.xml`, `strings_errors.xml`… También el tema (`themes.xml`). Ningún texto visible está escrito directamente en el código. |
| `drawable/` | Íconos en formato vectorial (`.xml`). Cada uno lleva el prefijo de su módulo: `ic_calendar_*`, `ic_records_*`, `ic_pets_*`. |
| `drawable-nodpi/` | Imágenes de Figma en `.webp` (fotos de cabecera, ilustraciones). |
| `font/` | La fuente Nunito. |

## Pruebas: `app/src/test/kotlin/com/example/huellitasya/`

Repiten la estructura de `main`. Hay unas 240 pruebas que corren en la computadora, sin teléfono ni Firebase.

| Carpeta | Para qué sirve |
|---|---|
| [`core/testing/`](../app/src/test/kotlin/com/example/huellitasya/core/testing) | **Repositorios falsos** (*fakes*) que reemplazan a Firebase en las pruebas (`FakePetRepository`, `FakeReminderRepository`…) y `MainDispatcherRule`, que permite probar corrutinas. |
| `core/domain/…` | Pruebas de validaciones y casos de uso. |
| `feature/…` | Pruebas de cada ViewModel: qué estado muestra y qué eventos envía ante cada acción. |

Se corren con `./gradlew testDebugUnitTest`.

## Fuera de la app

| Carpeta o archivo | Para qué sirve |
|---|---|
| [`firebase/firestore.rules`](../firebase/firestore.rules) | Quién puede leer y escribir cada documento, y qué forma deben tener los datos. |
| [`firebase/storage.rules`](../firebase/storage.rules) | Lo mismo para los archivos: fotos y registros médicos. |
| [`functions/src/`](../functions/src) | Backend en TypeScript: códigos de recuperación de contraseña, cambio de correo y eliminación de cuenta. Las pruebas son los archivos `*.test.ts`. |
| [`specs/`](../specs) | Una especificación por módulo (000 a 007), con los frames de Figma, las decisiones, los contratos y los criterios de aceptación. Es la fuente de verdad: primero se escribe la spec y después el código. |
| [`gradle/libs.versions.toml`](../gradle/libs.versions.toml) | Las versiones de todas las librerías: Compose, Firebase, Hilt, Coil, WorkManager… |
| [`app/build.gradle.kts`](../app/build.gradle.kts) | Dice qué librerías usa la app y cómo se compila (minSdk 24, Java 11). |
| [`app/src/main/AndroidManifest.xml`](../app/src/main/AndroidManifest.xml) | Permisos (red y notificaciones), la `Activity` principal y la `Application`. |
