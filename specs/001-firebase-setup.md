# 001 — Firebase setup

- **Estado:** Implemented
- **Paquetes:** `core.data` (en `:app`)

## Objetivo

Conectar la app al proyecto de Firebase existente (Auth + Firestore) sin publicar credenciales en el repositorio.

## Pasos de configuración (una vez por desarrollador)

1. **`google-services.json`**
   - Consola de Firebase → Configuración del proyecto → Tus apps → app Android con paquete `com.example.huellitasya` → descargar `google-services.json`.
   - Copiarlo en `app/google-services.json`.
   - Está en `.gitignore`: **nunca** se commitea. Se comparte por un canal privado del equipo.
   - Sin este archivo el build falla en la tarea `processDebugGoogleServices` con `File google-services.json is missing`.
   - Va en `app/`, no en la raíz del proyecto.
   - Si el archivo no incluye un cliente OAuth web (`oauth_client` vacío porque Google no está habilitado o falta la SHA-1), no se genera `R.string.default_web_client_id`. La app compila y el login con correo funciona, pero "Continuar con Google" muestra un error y registra un aviso en Logcat (tag `GoogleSignIn`). Habilita Google (paso 3), registra la SHA-1 (paso 2) y vuelve a descargar el archivo.

2. **Huellas SHA (necesarias para Google Sign-In)**
   - Ejecutar `./gradlew signingReport` (o `gradlew.bat signingReport` en Windows) y copiar SHA-1 y SHA-256 de la variante `debug`.
   - Agregarlas en la consola: Configuración del proyecto → app Android → "Agregar huella digital".
   - Cada desarrollador tiene su propio keystore debug, así que cada uno debe registrar sus huellas.
   - Tras agregar huellas, volver a descargar `google-services.json` (incluye el cliente OAuth).

3. **Authentication** (ya hecho en `huellitasya-3fd78`; solo hace falta en un proyecto nuevo):
   - Pulsar **Comenzar**.
   - En Método de acceso, habilitar Correo electrónico/contraseña.
   - En Método de acceso, habilitar Google (define el correo de soporte). El "ID de cliente web" que se crea es el que la app usa como `serverClientId`; el plugin de Google Services lo genera como recurso `R.string.default_web_client_id`.
   - En Plantillas, poner el idioma de la plantilla en **Español**. La app además llama a `FirebaseAuth.setLanguageCode("es")`, así el correo de recuperación de contraseña llega en español.

4. **Firestore Database**: base `(default)` en **`southamerica-east1`** (São Paulo, la región más cercana a Colombia), modo producción.
   - La región no se puede cambiar después.
   - Crearla requiere el rol Owner del proyecto: la cuenta con rol Editor recibe `403 The caller does not have permission`.

5. **Reglas de seguridad**: el repo trae `firebase.json` y `.firebaserc` (proyecto por defecto `huellitasya-3fd78`). Para publicar [`firebase/firestore.rules`](../firebase/firestore.rules):

   ```bash
   firebase deploy --only firestore:rules
   ```

### Por CLI (alternativa a la consola para los pasos 1 y 2)

```bash
firebase apps:android:sha:create <APP_ID> <SHA-1 o SHA-256>
firebase apps:sdkconfig ANDROID <APP_ID> -o app/google-services.json
```

El `APP_ID` de la app Android es `1:498312883676:android:15060b8cb85e28d2b3b319`.

## Contratos

- Plugin `com.google.gms.google-services` aplicado en `:app`.
- Dependencias Firebase mediante BoM (`firebase-auth`, `firebase-firestore`).
- Solo el paquete `core.data` importa clases `com.google.firebase.*`.

## Criterios de aceptación

- [x] `git status` no muestra `app/google-services.json` después de copiarlo.
- [x] `./gradlew assembleDebug` compila con el archivo presente.
- [x] Authentication tiene habilitados Correo/contraseña y Google, con plantillas en español.
- [x] `google-services.json` incluye el cliente OAuth web (`client_type` 3) y el de Android con la SHA-1 registrada.
- [ ] Las reglas de Firestore impiden leer o escribir `users/{uid}` de otro usuario.

## Fuera de alcance

- Entornos separados (dev/prod) con flavors.
- Firebase Emulator Suite.
- Cloud Functions, Storage, Messaging.
