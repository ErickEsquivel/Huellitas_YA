# 001 — Firebase setup

- **Estado:** Approved
- **Módulos:** :app, :core:data

## Objetivo

Conectar la app al proyecto de Firebase existente (Auth + Firestore) sin publicar credenciales en el repositorio.

## Pasos de configuración (una vez por desarrollador)

1. **`google-services.json`**
   - Consola de Firebase → Configuración del proyecto → Tus apps → app Android con paquete `com.example.huellitasya` → descargar `google-services.json`.
   - Copiarlo en `app/google-services.json`.
   - Está en `.gitignore`: **nunca** se commitea. Se comparte por un canal privado del equipo.
   - Sin este archivo el build falla en la tarea `processDebugGoogleServices` con `File google-services.json is missing`.

2. **Huellas SHA (necesarias para Google Sign-In)**
   - Ejecutar `./gradlew signingReport` (o `gradlew.bat signingReport` en Windows) y copiar SHA-1 y SHA-256 de la variante `debug`.
   - Agregarlas en la consola: Configuración del proyecto → app Android → "Agregar huella digital".
   - Cada desarrollador tiene su propio keystore debug, así que cada uno debe registrar sus huellas.
   - Tras agregar huellas, volver a descargar `google-services.json` (incluye el cliente OAuth).

3. **Authentication** → Método de acceso → habilitar:
   - Correo electrónico/contraseña.
   - Google (define el correo de soporte). El "ID de cliente web" que se crea es el que la app usa como `serverClientId`; el plugin de Google Services lo genera como recurso `R.string.default_web_client_id`.

4. **Firestore Database** → crear base (modo producción) en la región más cercana (p. ej. `southamerica-east1` o `us-east1`).
   - Publicar las reglas de [`firebase/firestore.rules`](../firebase/firestore.rules) (pegar en la pestaña Reglas de la consola o `firebase deploy --only firestore:rules`).

## Contratos

- Plugin `com.google.gms.google-services` aplicado **solo** en `:app`.
- Dependencias Firebase mediante BoM, solo en `:core:data` (`firebase-auth`, `firebase-firestore`).
- Ningún otro módulo importa clases `com.google.firebase.*`.

## Criterios de aceptación

- [ ] `git status` no muestra `app/google-services.json` después de copiarlo.
- [ ] `./gradlew assembleDebug` compila con el archivo presente.
- [ ] Las reglas de Firestore impiden leer o escribir `users/{uid}` de otro usuario.

## Fuera de alcance

- Entornos separados (dev/prod) con flavors.
- Firebase Emulator Suite.
- Cloud Functions, Storage, Messaging.
