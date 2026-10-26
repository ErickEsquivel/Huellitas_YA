# 003 — Design system

- **Estado:** Implemented
- **Paquetes:** `core.designsystem`, `core.ui`
- **Figma:** [Huellitas YA!](https://www.figma.com/design/02Bq5fbLLxTbaAD5hZ1sxS/Huellitas-YA-) (frame `ERRORES LAYOUTS` para los estados de campos)

## Objetivo

Centralizar tokens visuales y componentes reutilizables para que las pantallas generadas desde Figma (vía MCP) se armen con piezas existentes en lugar de duplicar estilos.

## Tokens (`Color.kt`)

| Token | Valor | Uso |
|-------|-------|-----|
| `GreenPrimary` | `#588157` | Botón "Comenzar", ícono de pestaña activa, bordes verdes |
| `GreenAction` | `#57AF80` | Botones de acción de auth cuando el formulario está listo |
| `GreenHighlight` | `#59AD57` | Acentos de título ("YA!", "!") |
| `GreenBackground` | `#C7EBC7` | Fondo curvo, íconos inactivos, huella decorativa |
| `GreenAccent` | `#8DC68F` | Botón flotante "+" |
| `MintSurface` | `#E5F5ED` | Botón secundario suave ("Regístrate", al 50 %) |
| `ButtonIncomplete` | `#676E68` | Botón principal mientras faltan datos |
| `GrayText` | `#8E8E93` | Subtítulos |
| `LightGrayText` | `#AEAEB2` | Texto secundario |
| `SurfaceGray` | `#F2F2F2` | Tarjetas vacías / placeholders |
| `BorderSubtle` | `#33000000` | Bordes de campos, tarjetas y botones |
| `LinkBlue` | `#0077FF` | Enlaces ("¿Se te olvidó la contraseña?", "Términos") |
| `ErrorRed` | `#FF0000` | Campo con error (borde e ícono X) |
| `WarningOrange` | `#80FF6A00` | Campo con advertencia (borde) |
| `GreenCard` | `#80CC80` | Tarjetas de mascota en Home y de "Registros Medicos" (spec 005) |
| `PinkSurface` | `#FCE4EC` | Recuadro de la foto en la tarjeta de mascota |
| `DarkGrayText` | `#707070` | Etiquetas y valores del perfil de la mascota |
| `RecordCountGreen` | `#005716` | Conteo de archivos en "Registros Medicos" |
| `DestructiveRed` | `#B50004` | Botón "Eliminar mascota" y "Eliminar Archivo" |
| `GreenDark` | `#3C6642` | Botón flotante "+" de los registros médicos |
| `RecordCardSurface` | `#3393B98F` | Tarjetas de archivo de los registros médicos |

- Espaciados en `HuellitasSpacing` (`xs=4, sm=8, md=16, lg=24, xl=32, xxl=48` dp).
- Tipografía: **Nunito** (Regular 400, Bold 700, ExtraBold 800) en `res/font`, con licencia SIL OFL 1.1. `Typography` aplica Nunito a todos los estilos, así que cualquier `Text` la hereda.
- `HuellitasYATheme` usa un esquema de color de marca fijo (sin dynamic color), para que `MaterialTheme.colorScheme` refleje los colores de Figma.

## Catálogo de componentes (`core.designsystem.components`)

| Componente | Descripción |
|------------|-------------|
| `HuellitasButton` | Botón principal redondeado. Soporta `containerColor`, `enabled` e `isLoading`. |
| `HuellitasOptionButton` | Botón de 319×46 con ícono a la izquierda y texto alineado a 87 dp ("Continuar con correo", "Regístrate"). |
| `GoogleOptionButton` | Variante blanca con borde verde y logo de Google. |
| `HuellitasTextButton` | Enlace de texto. |
| `HuellitasTextField` | Etiqueta arriba + caja de 48 dp + `FieldStatus` (`Default`/`Warning`/`Error`) + mensaje opcional. |
| `HuellitasPasswordField` | `HuellitasTextField` con mostrar/ocultar contraseña. |
| `FieldMessage` | Ícono (alerta o X) + mensaje bajo un campo o un formulario. |
| `CurvedBackground` | Fondo con ola verde. |
| `Modifier.greenWave()` | Ola verde que empieza en el borde superior del contenido y llega hasta el final de la pantalla aunque el contenido sea corto (formularios de mascota, perfil de la mascota, registros médicos). |
| `ScreenHeader` | Título + subtítulo opcional + campana de notificaciones (`ic_bell`, la de Figma 99:731). |
| `LoadingOverlay` | Capa semitransparente con indicador de progreso. |
| `ProgressOverlay` | Desenfoca la pantalla y la cubre con el spinner de Figma y un texto ("Guardando...", "Actualizando...", "Eliminando..."; spec 005). `SpinnerWithLabel` es el spinner solo ("Conectando..."). |
| `AppTitle` | Logo + "Huellitas YA!" con el "YA!" en verde (Bienvenido y Presentación). |
| `HuellitasDatePickerDialog` | Selector de fecha de nacimiento (Datos Personales y mascotas), en UTC y con "Borrar" opcional. |
| `HuellitasBackButton` | Flecha "<" de 16×14 dp de los formularios y de Datos Personales. |
| `PasswordConfirmDialog` | Diálogo "Confirma tu contraseña" antes de una operación sensible (cambiar el correo, eliminar la cuenta). |
| `PasswordRequirementRow` | Fila de la checklist de contraseña (✗ naranja / ✓ verde), en "Crear nueva contraseña" y Seguridad. |
| `DeleteConfirmDialog` | "¿Seguro deseas eliminarlo?" con el cachorro (Figma 3050:1515): eliminar una mascota o la cuenta. Recibe los textos y el del botón. |
| `AddPlaceholderCard` | Tarjeta gris con ícono "+" (agregar mascota/evento). |

En `core.ui`, `FieldFeedback(status, message: UiText?)` es lo que los ViewModels asignan a cada campo. Los mapeos `ValidationError.toFieldFeedback()` y `AuthError.toFieldFeedback()` aplican las reglas de la spec 002.

## Assets de Figma

- Se descargan con el MCP de Figma (`get_design_context`) y se guardan sin modificar el contenido:
  - Fotos y PNG se convierten a WebP y van en `drawable-nodpi/`.
  - Los SVG se convierten a vector drawable.
- Todos los recursos van en `app/src/main/res`. Los que solo usa una feature llevan su prefijo (p. ej. `img_auth_*`, `ic_settings_*`).
- **Ícono de la app:** XML en capas vectoriales, sin imágenes. El logo de Figma (`PRESENTACION`, nodo 46:60) es un PNG; se vectorizó (contornos con huecos, degradado verde de Figma) y vive en `drawable/ic_launcher_foreground.xml`.
  - `mipmap-anydpi-v26/ic_launcher*.xml`: ícono adaptativo con `drawable/ic_launcher_background` (fondo blanco), `ic_launcher_foreground` (logo de 60 dp dentro del círculo seguro de 66 dp) e `ic_launcher_monochrome` (silueta para los íconos temáticos).
  - `mipmap-anydpi/ic_launcher*.xml`: versión legacy para API 24-25, también vectorial (cuadrado redondeado o círculo blanco + logo).
  - El splash usa el mismo `ic_launcher_foreground`.
  - El path del logo es largo, así que esos vectores llevan `tools:ignore="VectorPath"`: el sistema cachea el ícono y lo dibuja una sola vez.
- **Vectores con trazo:** Figma exporta los SVG con `overflow="visible"`, pero Android recorta lo que sale del viewport. Si el trazo llega al borde, se agrega margen al viewport (ver `ic_back_chevron`).

## Reglas

- Un componente entra al design system cuando se usa en ≥ 2 pantallas o es base de formularios.
- Los componentes reciben `modifier: Modifier = Modifier` y el llamador puede cambiar el ancho por defecto.
- Sin lógica de negocio ni strings de features: los textos llegan por parámetro.
- Cada componente tiene un `@Preview`.

## Criterios de aceptación

- [x] Ninguna pantalla redefine el header título + campana ni el fondo curvo con su propio `Canvas`.
- [x] Los formularios de auth usan exclusivamente componentes del catálogo.
- [x] Los colores de las pantallas de auth provienen de tokens.

## Fuera de alcance

- Modo oscuro de marca.
- Reemplazar los literales de color que quedan en Home, Calendario, Perfil y Configuración (se revisarán cuando se rehagan desde Figma).
