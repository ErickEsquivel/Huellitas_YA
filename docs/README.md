# Guía técnica de Huellitas YA!

Esta guía explica cómo está hecha la app: qué hay en cada carpeta, cómo se conectan sus partes y qué pasa, línea por línea, desde que tocas un botón hasta que el dato llega a Firebase.

Está escrita para el equipo y para sustentar el proyecto. Usa lenguaje simple y enlaza el código real, así que cada fragmento se puede abrir en su archivo.

## Cómo leerla

| Orden | Documento | Qué aprendes |
|---|---|---|
| 1 | [01 — Estructura del proyecto](01-estructura-del-proyecto.md) | Para qué sirve cada carpeta y cada archivo importante |
| 2 | [02 — Capas y conceptos](02-capas-y-conceptos.md) | ViewModel, estado, modelos, dominio, casos de uso, repositorios, Hilt, navegación |
| 3 | [03 — Firebase y datos](03-firebase-y-datos.md) | Cómo se guarda y se consulta todo: Firestore, Storage, Auth, Functions, reglas y avisos |
| 4 | [04 — Flujo: crear un recordatorio](04-flujo-crear-recordatorio.md) | Calendario → Firestore → aviso en el teléfono |
| 5 | [05 — Flujo: registrar una mascota](05-flujo-registrar-mascota.md) | Formulario → foto en Storage → documento en Firestore |
| 6 | [06 — Flujo: iniciar sesión](06-flujo-iniciar-sesion.md) | Login → Firebase Auth → perfil → cambio de pantallas |
| 7 | [07 — Flujo: subir un registro médico](07-flujo-subir-registro.md) | Selector de archivo → Storage con barra de avance → Firestore |
| 8 | [08 — Conceptos del curso](08-conceptos-del-curso.md) | Cada unidad de *Android Basics with Compose* y dónde aparece en la app |

También existe [Arquitectura y Firebase CLI](arquitectura-y-firebase-cli.md), un resumen general de las capas y de cómo se publican las reglas y las funciones.

> **Cómo leer el código de la guía:** los fragmentos están copiados del proyecto. Los comentarios que empiezan con `// ←` los agrega esta guía para explicar cada línea y no están en el archivo original. `...` indica líneas omitidas.

## La app en una página

Huellitas YA! es una app Android hecha con **Kotlin** y **Jetpack Compose**. Guarda los datos en **Firebase**: Auth para las cuentas, Firestore para los datos, Storage para los archivos y Cloud Functions para el backend.

El código está ordenado en **capas**. Cada capa solo habla con la de abajo:

```text
┌────────────────────────────────────────────────────────────┐
│ feature/   Pantallas (Compose) + ViewModel                 │  lo que ve el usuario
├────────────────────────────────────────────────────────────┤
│ core/domain/   Casos de uso, validaciones, interfaces      │  las reglas de la app
├────────────────────────────────────────────────────────────┤
│ core/data/   Repositorios con Firebase                     │  cómo se guardan los datos
├────────────────────────────────────────────────────────────┤
│ Firebase: Auth · Firestore · Storage · Functions           │  la nube
└────────────────────────────────────────────────────────────┘
```

Toda acción del usuario sigue el mismo camino de ida y vuelta:

```text
Ida:    Screen ─acción─► ViewModel ─► Caso de uso ─► Repositorio ─► Firebase
Vuelta: Firebase ─Flow/AppResult─► Repositorio ─► Caso de uso ─► ViewModel ─UiState─► Screen
```

- **Screen:** dibuja el estado y avisa lo que toca el usuario.
- **ViewModel:** guarda el estado de la pantalla y decide qué hacer.
- **Caso de uso:** una regla de negocio concreta, como "crear un recordatorio".
- **Repositorio:** la única parte que conoce Firebase.

Las rutas de esta guía son relativas a `app/src/main/kotlin/com/example/huellitasya/` salvo que digan otra cosa.
