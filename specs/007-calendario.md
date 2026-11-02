# 007 — Calendario y recordatorios

- **Estado:** Approved
- **Paquetes:** `feature.calendar`, `feature.home` (campana), `feature.pets` (eliminar mascota), `navigation`, `core.model`, `core.domain`, `core.data`, `core.designsystem`
- **Figma:** [Huellitas YA!](https://www.figma.com/design/02Bq5fbLLxTbaAD5hZ1sxS/Huellitas-YA-). Capturas de referencia en [`figma/007-calendario/`](figma/007-calendario/).

| Pantalla | Frames |
|---|---|
| Calendario vacío (mes, "+" y tarjeta "+") | 3444:828 |
| Calendario con eventos | 3241:1074 |
| Hoja "¿Qué deseas programar?…" | 3241:1717 |
| Nuevo · Vacunas | 3362:1491, 3241:1151, 3241:1202, 3359:1411, 3362:1554 |
| Nuevo · Veterinario | 3241:1291, 3367:1623, 3241:1347, 3367:1733, 3367:1782 |
| Nuevo · Otro | 3241:1248, 3367:1909, 3367:1970, 3367:2019 |
| Detalle y edición | 3241:1487, 3376:1050, 3376:963, 3241:1570, 3376:831, 3241:2322 |
| Detalle de solo lectura (no se implementa) | 3241:1530, 3241:1447, 3241:2241 |
| Diálogo "¿Eliminar recordatorio?" | 3241:1665, 3241:1613, 3241:2287 |
| Notificaciones | 3263:914, 3263:954, 3241:1036, 3263:1034 |

Puntos de entrada:
- la pestaña Calendario de la barra inferior;
- la campana de Home y de Calendario, que abre Notificaciones;
- tocar un aviso del teléfono, que abre el Calendario.

## Objetivo

Que el dueño programe vacunas, citas veterinarias y otros recordatorios para cada mascota. Los ve marcados en el mes y en una lista, los cambia de fecha u hora, los elimina, y el teléfono le avisa a la hora indicada.

## Historias de usuario

- Como dueño, quiero ver en el calendario qué días tienen eventos y de qué mascota.
- Como dueño, quiero programar una vacuna, una cita con el veterinario u otro recordatorio para una mascota.
- Como dueño, quiero cambiar la fecha o la hora de un recordatorio, o eliminarlo.
- Como dueño, quiero que el teléfono me avise cuando llegue la hora.
- Como dueño, quiero ver todos los próximos recordatorios juntos desde la campana.

## Flujo de navegación

```
Calendario ─"+" (botón o tarjeta)─► hoja "¿Qué deseas programar?…" ─Vacunas | Veterinario | Otro─► Nuevo recordatorio ─"Agregar Recordatorio"─► Calendario
Calendario ─tarjeta de evento─► Detalle ─"Actualizar información"─► Calendario
Detalle ─"Eliminar recordatorio" / "Eliminar cita"─► diálogo ─► "Eliminando…" ─► Calendario
Home / Calendario ─campana─► Notificaciones ─tarjeta─► Detalle
Aviso del teléfono ─toque─► Calendario
```

El Calendario muestra la barra inferior. Nuevo recordatorio, Detalle y Notificaciones la ocultan, y "<" vuelve atrás.

## Pantallas

**Calendario:**
- Cabecera "Calendario" / "Gestiona los eventos de tu mascota" y la campana.
- Mes en una tarjeta con borde verde, con "<" y ">" para cambiar de mes. La semana empieza el lunes y los nombres van en español.
- Hoy se marca con un círculo gris. Un día con eventos lleva un anillo del color de la mascota; si hay varias mascotas, el de la primera.
- El botón "+" sobre el borde inferior abre la hoja.
- "Próximos eventos": recordatorios desde ahora en adelante, agrupados por mascota. Solo esta lista se desplaza; la cabecera y el mes quedan fijos (Figma 3336:1114). Cada grupo lleva un punto y el nombre en el color de la mascota. Cada tarjeta muestra el ícono del tipo, el título, la hora y la fecha.
- Sin próximos eventos, se muestra la tarjeta "+".

**Hoja "¿Qué deseas programar?…":** tres opciones, Vacunas (jeringa), Veterinario (corazón) y Otro (lista). Se cierra tocando fuera.

**Nuevo recordatorio** (título "Vacunas", "Veterinario" u "Otro"):
1. "Selecciona la mascota": una tarjeta por mascota, con foto y nombre. La elegida va en verde oscuro.
2. Según el tipo:
   - **Vacunas:** "Selecciona la vacuna que se le aplicará": Rabia, Parvovirus, Leptospirosis u Otros.
   - **Veterinario:** "Selecciona el motivo de la cita": Consulta General, Desparasitación, Estética y Peluquería, Cirugía y Especialidad u Otros.
   - **Otro:** "Agrega el nombre del recordatorio", un texto.
   - "Otros" (en Vacunas y en Veterinario) pide además el nombre.
3. "Selecciona la fecha y hora": Fecha y Hora, con sus selectores.

"Agregar Recordatorio" queda gris hasta que el formulario está completo. Muestra "Guardando…" y vuelve al Calendario.

**Detalle:**
- Ícono y título del recordatorio.
- Tarjeta verde con el nombre y la foto de la mascota, y los campos Fecha y Hora editables.
- "Actualizar información" queda gris hasta que hay un cambio.
- "Eliminar recordatorio" (o "Eliminar cita" si es del veterinario) abre el diálogo del cachorro y luego muestra "Eliminando…".

**Notificaciones:** "<", "Notificaciones" / "Veamos como están tus mascotas", y las tarjetas de los próximos recordatorios en el color de su mascota. Tocar una abre el Detalle.

**Aviso del teléfono:** a la hora del recordatorio llega una notificación con el título y el nombre de la mascota.

## Desviaciones de Figma

- **Sin el botón de cambiar el tema:** la app no tiene tema oscuro.
- **Detalle siempre editable:** Figma muestra además una versión de solo lectura ("Hora de vacuna" / "Fecha de recordatorio"). Aquí los campos se editan directamente, como en Editar mascota.
- **El color sale de la mascota:** Figma pinta "Chequeo veterinario" de rosa. Aquí cada mascota tiene un color fijo, asignado por orden de registro, y lo usan sus anillos, su grupo y sus tarjetas.
- **Lista del paso 2 dentro de la sección:** Figma abre las vacunas o los motivos sobre toda la pantalla, con un fondo difuminado. Aquí se despliegan dentro de la caja verde del paso 2.
- **Hoja con tarjetas más bajas:** las opciones de "¿Qué deseas programar?…" miden 100 dp en lugar de 121, para que la hoja quepa en pantallas pequeñas.
- **Calendario en español:** Figma usa "September 2026" y "Mon…Sun". Aquí "Septiembre 2026" y "Lun…Dom".

## Reglas de validación

- Mascota: obligatoria.
- Vacuna o motivo: obligatorio en Vacunas y Veterinario.
- Nombre: entre 1 y 50 caracteres. Obligatorio en Otro y en "Otros".
- Fecha y hora: obligatorias y en el futuro.
- Al editar solo cambian la fecha y la hora, y deben seguir en el futuro.

## Contratos

### Modelo (`core.model`)

```kotlin
enum class ReminderKind { VACCINE, VET, OTHER }
enum class VaccineType { RABIES, PARVOVIRUS, LEPTOSPIROSIS, OTHER }
enum class VetReason { GENERAL, DEWORMING, GROOMING, SURGERY, OTHER }

data class Reminder(
    val id: String, val petId: String, val kind: ReminderKind,
    val vaccineType: VaccineType?, val vetReason: VetReason?,
    val customTitle: String?, val dateTime: LocalDateTime,
)

data class ReminderDraft(
    val petId: String, val kind: ReminderKind,
    val vaccineType: VaccineType?, val vetReason: VetReason?,
    val customTitle: String?, val dateTime: LocalDateTime,
)
```

### Firestore: `users/{uid}/reminders/{reminderId}`

Va al nivel del usuario y no dentro de la mascota, porque el Calendario lee las de todas sus mascotas con una sola consulta.

| Campo | Tipo | Notas |
|---|---|---|
| `petId` | string | |
| `kind` | string | `vaccine`, `vet` u `other` |
| `subtype` | string? | Vacunas: `rabies`, `parvovirus`, `leptospirosis` u `other`. Veterinario: `general`, `deworming`, `grooming`, `surgery` u `other`. Otro: ausente |
| `title` | string? | 1 a 50 caracteres; obligatorio con `kind == other` o `subtype == other` |
| `dateTime` | timestamp | |
| `createdAt` | timestamp | `serverTimestamp()` |
| `updatedAt` | timestamp | `serverTimestamp()` |

- **Reglas:** solo el dueño lee, crea, edita y borra. Al editar solo pueden cambiar `dateTime` y `updatedAt`.
- **Al eliminar la mascota:** `DeletePetUseCase` borra sus recordatorios y cancela sus avisos.
- **Al eliminar la cuenta:** `deleteAccount` (spec 004) ya borra todo `users/{uid}`.

### Dominio (`core.domain`)

- `ReminderRepository`:
  - `observeReminders(uid)`, ordenados por `dateTime`;
  - `newReminderId(uid)`;
  - `createReminder(uid, id, draft)`;
  - `updateDateTime(uid, id, dateTime)`;
  - `deleteReminder(uid, id)`.
- `ReminderNotifier`:
  - `sync(reminders: List<ScheduledReminder>)`: deja programados exactamente esos avisos y cancela el resto (una lista vacía cancela todos);
  - se implementa con WorkManager: un trabajo único `reminder-<id>`, que sobrevive al reinicio;
  - puede llegar unos minutos tarde en modo de ahorro de batería.
- Casos de uso:
  - `ObserveRemindersUseCase`, `ObserveUpcomingRemindersUseCase`;
  - `CreateReminderUseCase`, `UpdateReminderUseCase` y `DeleteReminderUseCase` (`@ApplicationScope`);
  - `ObserveReminderUseCase`;
  - `SyncReminderNotificationsUseCase`: se lanza una vez desde `MainViewModel`. Con cada cambio de los recordatorios o de las mascotas programa los futuros, y sin sesión cancela todos. Así un solo camino cubre crear, editar, eliminar (también desde otro dispositivo) y eliminar una mascota. Los avisos se reprograman al abrir la app.

### Rutas

| Destino | Argumentos |
|---|---|
| `CalendarDestination` | — |
| `NewReminderDestination` | `kind` |
| `ReminderDetailDestination` | `reminderId` |
| `NotificationsDestination` | — |

## Reparto (equipo)

| Persona | Rama | Alcance |
|---|---|---|
| Erick | `feature/calendario-mes` | Esta spec, modelo, datos y reglas, y la pantalla Calendario |
| Mateo | `feature/calendario-nuevo` | Hoja "¿Qué deseas programar?…", Nuevo recordatorio, validaciones y selector de hora |
| Julian | `feature/calendario-detalle` | Detalle, editar, eliminar, Notificaciones, avisos del teléfono, limpieza al eliminar una mascota y la verificación en el teléfono |

## Criterios de aceptación

> Erick verificó los 3 primeros en el teléfono. El resto tiene tests unitarios y queda para el recorrido final en el Vivo, al cerrar el módulo.

> Marcar cada casilla al verificarla en el teléfono.

- [x] El Calendario muestra el mes actual con hoy marcado, y "<" / ">" cambian de mes.
- [x] Los días con recordatorios llevan el anillo del color de su mascota.
- [x] "Próximos eventos" agrupa los recordatorios futuros por mascota; sin eventos, muestra la tarjeta "+".
- [ ] Se puede crear una vacuna, una cita con el veterinario y un recordatorio "Otro". El botón queda gris hasta completar el formulario.
- [ ] Una fecha u hora pasada se rechaza con su aviso.
- [ ] El Detalle permite cambiar la fecha y la hora; "Actualizar información" se activa solo con cambios.
- [ ] Eliminar pide confirmación, muestra "Eliminando…" y el recordatorio desaparece del Calendario.
- [ ] La campana de Home y del Calendario abre Notificaciones con los próximos recordatorios.
- [ ] A la hora programada llega un aviso al teléfono, aun con la app cerrada. Al eliminar o cambiar el recordatorio, el aviso anterior se cancela.
- [ ] Eliminar una mascota borra también sus recordatorios.
- [ ] Sin conexión se ven los datos guardados, y crear, editar o eliminar muestran "Sin conexión. Revisa tu internet".
- [ ] Las reglas impiden leer o escribir recordatorios de otra cuenta, guardar campos fuera del esquema y editar algo distinto de la fecha.

> Verificados en el Vivo V2436 (Erick): mes con hoy y flechas, anillos por mascota, "Próximos eventos" agrupados (sin el pasado) y la tarjeta "+" sin eventos, con 2 mascotas y 4 recordatorios sembrados por REST. Reglas: 18 de 18 pruebas por REST.

## Fuera de alcance

- Recordatorios que se repiten (anuales o mensuales).
- Avisos con anticipación (por ejemplo, un día antes).
- Compartir un recordatorio con otra persona.
- Tema oscuro.
