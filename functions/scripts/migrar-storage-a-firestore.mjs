// Copia a Firestore las fotos y los archivos que están en Storage (spec 008, "Archivos que ya
// están en Storage"), para que la app los siga mostrando en el modo gratuito.
//
// Se corre UNA vez, en el computador de alguien con acceso al proyecto, ANTES de quitar la
// facturación: en el plan Spark Storage ya no responde. No se despliega.
//
//   gcloud auth application-default login            # o GOOGLE_APPLICATION_CREDENTIALS=<clave>.json
//   node functions/scripts/migrar-storage-a-firestore.mjs            # simulación: solo lista
//   node functions/scripts/migrar-storage-a-firestore.mjs --aplicar  # copia y cambia las URLs
//
// Es idempotente: lo que ya tiene una URL `huellitas-media://` se salta. No borra nada de Storage.

import { createRequire } from 'node:module';

const require = createRequire(new URL('../package.json', import.meta.url));
const { initializeApp, applicationDefault } = require('firebase-admin/app');
const { getFirestore, FieldValue } = require('firebase-admin/firestore');
const { getStorage } = require('firebase-admin/storage');

const PROJECT_ID = 'huellitasya-3fd78';
const BUCKET = `${PROJECT_ID}.firebasestorage.app`;
const SCHEME = 'huellitas-media';
const CHUNK_BYTES = 900 * 1024;
const apply = process.argv.includes('--aplicar');

initializeApp({ credential: applicationDefault(), projectId: PROJECT_ID, storageBucket: BUCKET });
const db = getFirestore();
const bucket = getStorage().bucket();

const isStorageUrl = (url) => typeof url === 'string' && url.includes('firebasestorage');
const mediaUrl = (uid, mediaId, version) =>
  `${SCHEME}://users/${uid}/media/${mediaId}` + (version ? `?v=${version}` : '');

/** Mismo formato que `FirestoreMediaStore` en la app: chunks primero, metadatos al final. */
async function writeMedia(uid, mediaId, bytes, contentType) {
  const media = db.collection('users').doc(uid).collection('media').doc(mediaId);
  const count = Math.max(1, Math.ceil(bytes.length / CHUNK_BYTES));
  for (let i = 0; i < count; i++) {
    await media.collection('chunks').doc(String(i)).set({ data: bytes.subarray(i * CHUNK_BYTES, (i + 1) * CHUNK_BYTES) });
  }
  await media.set({
    contentType,
    sizeBytes: bytes.length,
    chunkCount: count,
    createdAt: FieldValue.serverTimestamp(),
  });
}

const totals = { copiados: 0, saltados: 0, faltantes: 0 };

/** Copia `storagePath` a `media/{mediaId}` y pone la URL nueva en `field` de `docRef`. */
async function migrate(docRef, field, uid, mediaId, storagePath, fallbackType, versioned) {
  const file = bucket.file(storagePath);
  const [exists] = await file.exists();
  if (!exists) {
    totals.faltantes++;
    console.warn(`  falta en Storage: ${storagePath}`);
    return;
  }
  const [metadata] = await file.getMetadata();
  const contentType = metadata.contentType || fallbackType;
  if (!apply) {
    totals.copiados++;
    console.log(`  copiaría ${storagePath} (${metadata.size} bytes) → media/${mediaId}`);
    return;
  }
  const [bytes] = await file.download();
  if (bytes.length >= 10 * 1024 * 1024) {
    totals.saltados++;
    console.warn(`  demasiado grande para Firestore: ${storagePath}`);
    return;
  }
  await writeMedia(uid, mediaId, bytes, contentType);
  await docRef.update({ [field]: mediaUrl(uid, mediaId, versioned ? Date.now() : null) });
  totals.copiados++;
  console.log(`  copiado ${storagePath} → media/${mediaId}`);
}

const users = await db.collection('users').get();
console.log(`${apply ? 'Copiando' : 'Simulación'}: ${users.size} cuentas`);

for (const user of users.docs) {
  const uid = user.id;
  console.log(`users/${uid}`);
  if (isStorageUrl(user.get('photoUrl'))) {
    await migrate(user.ref, 'photoUrl', uid, 'profile', `users/${uid}/profile.jpg`, 'image/jpeg', true);
  } else {
    totals.saltados++;
  }

  const pets = await user.ref.collection('pets').get();
  for (const pet of pets.docs) {
    if (isStorageUrl(pet.get('photoUrl'))) {
      await migrate(pet.ref, 'photoUrl', uid, `pet-${pet.id}`, `users/${uid}/pets/${pet.id}.jpg`, 'image/jpeg', true);
    }
    const records = await pet.ref.collection('records').get();
    for (const record of records.docs) {
      if (!isStorageUrl(record.get('fileUrl'))) {
        totals.saltados++;
        continue;
      }
      await migrate(
        record.ref,
        'fileUrl',
        uid,
        `record-${pet.id}-${record.id}`,
        `users/${uid}/pets/${pet.id}/records/${record.id}`,
        record.get('contentType') || 'application/octet-stream',
        false,
      );
    }
  }
}

console.log(`\n${apply ? 'Copiados' : 'Por copiar'}: ${totals.copiados} · saltados: ${totals.saltados} · faltan en Storage: ${totals.faltantes}`);
if (!apply) console.log('Nada cambió. Para copiar de verdad, vuelve a correrlo con --aplicar.');
