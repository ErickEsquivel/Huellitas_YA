# 006 — Registros médicos

- **Estado:** Implemented
- **Paquetes:** `feature.records`, `feature.pets` (perfil de la mascota), `navigation`, `core.model`, `core.domain`, `core.data`, `core.designsystem`, `core.ui`
- **Figma:** [Huellitas YA!](https://www.figma.com/design/02Bq5fbLLxTbaAD5hZ1sxS/Huellitas-YA-) ("Momento 3"). Capturas de referencia en [`figma/006-registros/`](figma/006-registros/).

| Pantalla | Carné de vacunas | Historial clínico | Archivos adicionales |
|---|---|---|---|
| Lista vacía (tarjeta "+") | 3193:3626 | 3193:3600 | 3193:3574 |
| Lista con archivos ("+" flotante) | 3193:3448, 3193:3378, 3193:3339 | 3193:3512 | 3193:3543 |
| Detalle (vista previa, descargar, "Eliminar Archivo") | 3193:3746, 3193:4088, 3193:4048 | 3193:3841 | 3193:4029 |
| Diálogo "¿Seguro deseas eliminarlo?" | 3279:1344, 3279:1345, 3273:1175 | 3294:2125 | 3294:2126 |
| "Eliminando…" | 3281:1849, 3281:1850, 3281:1851 | 3294:2211 | 3294:2212 |
| "Subir Archivo" | 3193:3730, 3273:889, 3273:927 | 3279:1209 | 3294:2222 |
| "Subiendo archivo…" | 3281:1697, 3281:1702, 3281:1714 | 3318:1326 | 3318:1352 |

Puntos de entrada: las tarjetas 89:44, 89:43 y 89:52 del perfil de la mascota (89:4).

## Objetivo

Que el dueño guarde los documentos de cada mascota (carné de vacunas, historial clínico y otros archivos) en la nube, los consulte desde el teléfono y los descargue o elimine cuando quiera.

## Historias de usuario

- Como dueño, quiero subir la foto o el PDF del carné de vacunas para tenerlo siempre a mano.
- Como dueño, quiero ver los documentos de cada categoría con su fecha, tipo y veterinario.
- Como dueño, quiero abrir un documento, verlo en grande y descargarlo al teléfono.
- Como dueño, quiero eliminar un documento que ya no sirve.

## Flujo de navegación

```
Perfil de la mascota ─tarjeta de categoría─► Lista de la categoría
Lista ─"+" (tarjeta o botón flotante)─► Subir Archivo ─elige archivo─► formulario ─"Subir"─► "Subiendo archivo…" ─► Lista
Lista ─tarjeta de archivo─► Detalle ─descargar─► Descargas del teléfono
Detalle ─"Eliminar Archivo"─► diálogo ─► "Eliminando…" ─► Lista
```

Todas estas pantallas ocultan la barra inferior. "<" vuelve a la pantalla anterior.

## Pantallas

**Cabecera común** (Figma 3193:3339):
- "<" y "Perfil de <nombre>".
- La foto de la mascota (120 dp) a la izquierda y, a su derecha, "raza  -  edad".
- "Registros Médicos - Registros Detallados" y el nombre de la categoría.
- La ola verde empieza detrás de la foto.

**Lista:**
- Una tarjeta por archivo, del más nuevo al más antiguo, con:
  - el ícono PDF (o una miniatura si es imagen);
  - el nombre del archivo;
  - "Fecha:dd/MM/yyyy | Tipo:<tipo>" y, si hay veterinario, "Veterinario:<nombre>".
- Sin archivos se ve la tarjeta gris con "+". Con archivos, un botón flotante verde "+" abajo a la derecha.
- Tocar una tarjeta abre el detalle.

**Subir Archivo:**
- La tarjeta grande "Subir Archivo" abre el selector del sistema (PDF o imágenes).
- Después se ve un formulario con los campos de las mascotas (spec 005):
  - Fecha: hoy por defecto, no futura;
  - Tipo: Vacunación, Consulta, Examen u Otro; en el Carné de vacunas viene elegido "Vacunación";
  - Veterinario: opcional, hasta 50 caracteres.
- Al subir se muestra "Subiendo archivo…" con una barra de progreso real y, al terminar, se vuelve a la lista.
- Sin red, Snackbar "Sin conexión. Revisa tu internet".

**Detalle:**
- Ícono y nombre del archivo, con el botón de descarga a la derecha.
- Vista previa grande: la imagen, o la primera página del PDF.
- "Eliminar Archivo" (rojo) abre el diálogo del cachorro (`DeleteConfirmDialog`, botón "Eliminar Archivo"); luego "Eliminando…" y vuelve a la lista.
- Descargar guarda el archivo en la carpeta Descargas del teléfono, con su notificación (`DownloadManager` con la `fileUrl`). Antes de Android 10 escribir ahí exige un permiso de almacenamiento, así que en esas versiones queda en la carpeta Download de la app.
- La vista previa del PDF la dibuja `PdfFirstPageDecoder` (`core.ui.pdf`), un decoder de Coil con `PdfRenderer`: Coil descarga y cachea el archivo igual que una imagen.
- Si el registro o la mascota se eliminan desde otro lado, el detalle vuelve a la lista o a Home.

**Perfil de la mascota:**
- Las tres tarjetas abren su categoría y muestran el conteo real: "1 Archivo", "3 Archivos".

## Desviaciones de Figma

- **Sin el botón de cambiar el tema** (la luna arriba a la derecha): la app no tiene tema oscuro.
- **Sin el botón gris "Guardar información"** de la lista: cada archivo se guarda al subirlo y "<" vuelve al perfil.
- **Formulario de datos al subir:** Figma muestra fecha, tipo y veterinario en la lista, pero no dónde se escriben.
- **Imágenes además de PDF:** las vistas previas de Figma son fotos de carnés. Una imagen se muestra con su miniatura en la lista.

## Reglas de validación

| Campo | Regla | Mensaje |
|---|---|---|
| Archivo | `application/pdf` o `image/*`, hasta 10 MB | "Elige un PDF o una imagen" / "El archivo es demasiado grande (máx. 10 MB)" |
| Fecha | Obligatoria, no futura ni anterior a 1990 | "Fecha no válida" |
| Tipo | Obligatorio | (campo resaltado) |
| Veterinario | Opcional, hasta 50 caracteres | "Máximo 50 caracteres" |

## Contratos

### Modelos (`core.model`)

```kotlin
enum class RecordCategory { VACCINES, HISTORY, FILES }
enum class RecordType { VACCINATION, CONSULTATION, EXAM, OTHER }

data class MedicalRecord(
    val id: String, val petId: String, val category: RecordCategory,
    val fileName: String, val contentType: String, val sizeBytes: Long,
    val date: LocalDate, val type: RecordType, val vet: String?, val fileUrl: String,
)

data class RecordDraft(val category: RecordCategory, val date: LocalDate, val type: RecordType, val vet: String?)
```

### Firestore: `users/{uid}/pets/{petId}/records/{recordId}`

| Campo | Tipo | Notas |
|---|---|---|
| `category` | string | `vaccines`, `history` o `files` |
| `fileName` | string | 1 a 120 caracteres |
| `contentType` | string | `application/pdf` o `image/*` |
| `sizeBytes` | number | > 0 y < 10 MB |
| `date` | string | `yyyy-MM-dd` |
| `type` | string | `vaccination`, `consultation`, `exam` u `other` |
| `vet` | string? | hasta 50 caracteres |
| `fileUrl` | string | URL de descarga de Storage (miniatura, vista previa y descarga) |
| `createdAt` | timestamp | `serverTimestamp()` |

- **Reglas:** solo el dueño lee, crea y borra; los registros no se editan.
- **Storage:** el archivo vive en `users/{uid}/pets/{petId}/records/{recordId}`, con las mismas reglas de dueño, tipo (`application/pdf` o `image/*`) y tamaño (< 10 MB), y el dueño puede borrarlo.
- **Al eliminar la cuenta:** `deleteAccount` (spec 004) ya borra los registros y sus archivos.
- **Al eliminar la mascota:** `DeletePetUseCase` borra antes sus registros y archivos, porque el cliente no borra subcolecciones.

### Domain (`core.domain`)

- `RecordRepository`:
  - `observeRecords(uid, petId)`;
  - `newRecordId(uid, petId)`;
  - `createRecord(uid, petId, recordId, draft, file, fileUrl)`;
  - `deleteRecord(uid, petId, recordId)`.
- `RecordFileRepository`:
  - `upload(uid, petId, recordId, localUri): Flow<UploadProgress>`;
  - `downloadUrl(...)`;
  - `delete(...)`.
- `RecordDownloader`: `download(url, fileName, contentType)`, implementado con `DownloadManager`.
- Use cases:
  - `ObserveRecordsUseCase`, `ObserveRecordCountsUseCase`, `ObserveRecordUseCase`;
  - `UploadRecordUseCase`, `DeleteRecordUseCase` (`@ApplicationScope`), `DownloadRecordUseCase`.

### Rutas

| Destino | Argumentos |
|---|---|
| `RecordListDestination` | `petId`, `category` |
| `UploadRecordDestination` | `petId`, `category` |
| `RecordDetailDestination` | `petId`, `recordId` |

## Reparto (equipo)

| Persona | Rama | Alcance |
|---|---|---|
| Erick | `feature/registros-lista` | Esta spec, modelo, datos y reglas, la lista y los conteos del perfil |
| Mateo | `feature/registros-subir` | Subir archivo: selector, formulario y progreso |
| Julian | `feature/registros-detalle` | Detalle, descargar, eliminar, limpieza al eliminar una mascota y la verificación en dispositivo |

## Criterios de aceptación

> Marcar cada casilla al verificarla en dispositivo.

- [x] Las tarjetas del perfil abren su categoría y muestran el número real de archivos.
- [x] Sin archivos, la lista muestra la tarjeta "+"; con archivos, una tarjeta por archivo con fecha, tipo y veterinario, y el "+" flotante.
- [x] Subir un PDF y una imagen funciona en las tres categorías, con la barra de progreso, y aparecen en la lista. (Desde la app: un PDF en Historial y una imagen en Archivos; las tres categorías usan la misma pantalla.)
- [x] Un archivo de otro tipo o de más de 10 MB se rechaza con su aviso y no se sube. (El selector solo ofrece PDF e imágenes; las reglas rechazan los demás tipos.)
- [x] El detalle muestra la imagen o la primera página del PDF; descargar lo deja en Descargas.
- [x] Eliminar pide confirmación, muestra "Eliminando…", vuelve a la lista y el archivo desaparece de Storage.
- [x] Eliminar una mascota borra también sus registros y archivos.
- [x] Sin conexión, la lista muestra la caché, y subir, descargar o eliminar muestran "Sin conexión. Revisa tu internet".
- [x] Las reglas impiden leer, crear o borrar registros de otra cuenta, y guardar tipos, tamaños o campos fuera del esquema.

> Julian verificó en el Vivo V2436 el detalle de un PDF (primera página) y de una imagen, la descarga a Descargas, eliminar (diálogo, "Eliminando…", vuelta a la lista y el archivo con 404 en Storage), eliminar la mascota (sin documento, registros ni archivos), un JPG de 11 MB rechazado y el modo avión en la lista, al subir, al descargar y al eliminar. Las reglas pasaron 19 pruebas por REST con una segunda cuenta.
>
> Mateo verificó la subida en el Vivo V2436: selector del sistema, formulario con el tipo por defecto de cada categoría, barra de avance y vuelta a la lista.
>
> Verificados en el Vivo V2436 (Erick): conteos del perfil y las listas vacía y con archivos (un PDF y una imagen creados por REST con las reglas desplegadas).

## Fuera de alcance

- Editar los datos de un registro ya subido (se elimina y se vuelve a subir).
- Compartir un documento con el veterinario.
- Tema oscuro.
