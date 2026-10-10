# Specs — Huellitas YA!

Las specs son la fuente de verdad de **qué** debe hacer cada feature antes de escribir código. Una feature no se implementa sin una spec en estado `Approved`, y su PR debe marcar los criterios de aceptación cumplidos.

## Convenciones

- Nombre: `NNN-nombre-en-kebab.md` (número correlativo, nunca se reutiliza).
- Estados: `Draft` → `Approved` → `Implemented` (y `Deprecated` si se reemplaza).
- Los criterios de aceptación se escriben como checklist verificable (`- [ ]`), redactados como comportamiento observable.
- Si la implementación obliga a cambiar el comportamiento, primero se actualiza la spec.
- Los mockups viven en Figma; la spec enlaza el frame correspondiente (el MCP de Figma se usa con ese link para generar la UI).

## Índice

| Spec | Estado |
|------|--------|
| [000 — Arquitectura](000-arquitectura.md) | Approved |
| [001 — Firebase setup](001-firebase-setup.md) | Approved |
| [002 — Autenticación](002-auth.md) | Approved |
| [003 — Design system](003-design-system.md) | Approved |

## Plantilla

```markdown
# NNN — Título

- **Estado:** Draft
- **Módulos:** :feature:x, :core:y
- **Figma:** <link al frame>

## Objetivo
Qué problema resuelve y para quién.

## Historias de usuario
- Como <rol>, quiero <acción> para <beneficio>.

## Criterios de aceptación
- [ ] Comportamiento observable 1
- [ ] Comportamiento observable 2

## Contratos
Modelos, rutas de navegación, colecciones de Firestore, errores.

## Fuera de alcance
Lo que explícitamente NO cubre esta spec.
```
