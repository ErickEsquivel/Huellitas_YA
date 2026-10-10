# 003 — Design system

- **Estado:** Approved
- **Módulos:** :core:designsystem

## Objetivo

Centralizar tokens visuales y componentes reutilizables para que las pantallas generadas desde Figma (vía MCP) se armen con piezas existentes en lugar de duplicar estilos.

## Tokens

| Token | Valor | Uso |
|-------|-------|-----|
| `GreenPrimary` | `#588157` | Botones principales, ícono de pestaña activa, acentos |
| `GreenBackground` | `#C7EBC7` | Fondo curvo, íconos inactivos |
| `GreenAccent` | `#8DC68F` | Botón flotante "+" |
| `GrayText` | `#8E8E93` | Subtítulos |
| `LightGrayText` | `#AEAEB2` | Texto secundario |
| `SurfaceGray` | `#F2F2F2` | Tarjetas vacías / placeholders |
| `BorderSubtle` | `#33000000` | Bordes de tarjetas y botones |
| `ErrorRed` | `#D32F2F` | Errores de formulario |

- Espaciados en `HuellitasSpacing` (`xs=4, sm=8, md=16, lg=24, xl=32, xxl=48` dp).
- Tipografía: `Typography` de Material 3 en `Type.kt`; cuando se defina la fuente de Figma (Nunito) se configura ahí una sola vez.
- `HuellitasYATheme` usa un esquema de color de marca fijo (sin dynamic color) para que `MaterialTheme.colorScheme` refleje los colores de Figma.

## Catálogo de componentes (`core.designsystem.components`)

| Componente | Descripción |
|------------|-------------|
| `HuellitasButton` | Botón principal verde redondeado. Soporta `enabled` y `isLoading`. |
| `HuellitasTextButton` | Enlace de texto (p. ej. "¿Olvidaste tu contraseña?"). |
| `GoogleSignInButton` | Botón con borde "Continuar con Google". |
| `HuellitasTextField` | Campo con label, placeholder, mensaje de error y `KeyboardOptions`. |
| `HuellitasPasswordField` | `HuellitasTextField` con mostrar/ocultar contraseña. |
| `CurvedBackground` | Fondo con ola verde (parámetro `fraction` de altura). |
| `ScreenHeader` | Título + subtítulo opcional + campana de notificaciones. |
| `LoadingOverlay` | Capa semitransparente con indicador de progreso. |
| `AddPlaceholderCard` | Tarjeta gris con ícono "+" (agregar mascota/evento). |

## Reglas

- Un componente entra al design system cuando se usa en ≥ 2 pantallas o es base de formularios.
- Los componentes reciben `modifier: Modifier = Modifier` como primer parámetro opcional y no tienen tamaños fijos de ancho salvo que el diseño lo exija (preferir `fillMaxWidth()` desde el llamador).
- Sin lógica de negocio ni strings de features: los textos llegan por parámetro.
- Cada componente tiene un `@Preview`.

## Criterios de aceptación

- [ ] Ninguna pantalla redefine el header título + campana ni el fondo curvo con su propio `Canvas`.
- [ ] Los formularios de auth usan exclusivamente componentes del catálogo.
- [ ] Los colores de las pantallas provienen de tokens (no literales `Color(0x...)` en features).

## Fuera de alcance

- Modo oscuro de marca.
- Fuentes personalizadas (pendiente de exportar desde Figma).
