// Extracción determinista de versionCode/versionName desde el build de Android.
// El flujo de GitHub Actions la usa para (a) detectar si hay release nueva y
// (b) generar el tag. android-app/app/build.gradle.kts es la ÚNICA fuente de
// verdad de la versión: subir el versionCode (y versionName) es lo único que
// dispara una release nueva.
//
// Uso:
//   node scripts/version_utils.mjs                → JSON {versionCode, versionName}
//   node scripts/version_utils.mjs cmp <code>     → "mayor"|"igual"|"menor" vs el versionCode de update.json
//   node scripts/version_utils.mjs release-tag    → "v1.0.2"
//   node scripts/version_utils.mjs release-name   → "1.0.2"
//   node scripts/version_utils.mjs debe-publicar  → "publicar"|"omitir" (compara versionName
//                                                   contra el tag v* más alto publicado)
//
// Nota de diseño: el versionCode y el versionName se mantienen en paralelo por
// convención (ambos se suben juntos), pero NO son comparables entre sí
// (v1.0.2 ≠ código 102). La puerta de CI compara versiones semánticas:
// se publica solo si versionName > última release publicada (tag vX.Y.Z).

import { execFileSync } from 'node:child_process';
import { readFileSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const RAIZ = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const ARCHIVO_BUILD = path.join(RAIZ, 'android-app', 'app', 'build.gradle.kts');
const ARCHIVO_MANIFIESTO = path.join(RAIZ, 'update.json');

/** Lee android-app/app/build.gradle.kts y devuelve {versionCode, versionName}. */
export function versionDeBuild() {
  const texto = readFileSync(ARCHIVO_BUILD, 'utf8');
  const versionCode = texto.match(/^\s*versionCode\s*=\s*(\d+)\s*$/m)?.[1];
  const versionName = texto.match(/^\s*versionName\s*=\s*"([^"]+)"\s*$/m)?.[1];
  if (!versionCode || !versionName) {
    throw new Error(
      `No se pudo extraer versionCode/versionName de ${ARCHIVO_BUILD}. ` +
        `Formato esperado: líneas "versionCode = N" y versionName = "X.Y.Z".`,
    );
  }
  return { versionCode: Number(versionCode), versionName };
}

/** Lee update.json (manifiesto del autoupdate). */
export function manifiestoActual() {
  return JSON.parse(readFileSync(ARCHIVO_MANIFIESTO, 'utf8'));
}

/** "1.0.2" → [1, 0, 2]. Lanza si no es semver de 3 componentes. */
export function parsearVersion(texto) {
  const m = String(texto).trim().match(/^v?(\d+)\.(\d+)\.(\d+)$/);
  if (!m) throw new Error(`Versión no reconocida: "${texto}" (se esperaba X.Y.Z)`);
  return [Number(m[1]), Number(m[2]), Number(m[3])];
}

/** Comparación lexicográfica de tuplas: -1, 0 o 1. */
export function compararVersiones(a, b) {
  for (let i = 0; i < 3; i++) {
    if (a[i] !== b[i]) return a[i] < b[i] ? -1 : 1;
  }
  return 0;
}

/**
 * Última release publicada en GitHub: el tag vX.Y.Z con la versión más alta.
 * Devuelve {tag, version: [maj, min, patch]} o null si no hay releases (o si
 * `gh` no está disponible / sin credenciales).
 */
export function ultimoTagPublicado() {
  try {
    const salida = execFileSync(
      'gh',
      ['release', 'list', '--repo', 'luiggiberaldi/el-spot-toldos', '--limit', '100', '--json', 'tagName'],
      { encoding: 'utf8', stdio: ['ignore', 'pipe', 'ignore'] },
    );
    const releases = JSON.parse(salida)
      .map((r) => {
        try {
          return { tag: r.tagName, version: parsearVersion(r.tagName) };
        } catch {
          return null; // tags que no siguen vX.Y.Z se ignoran
        }
      })
      .filter(Boolean)
      .sort((a, b) => compararVersiones(b.version, a.version));
    return releases[0] ?? null;
  } catch {
    return null;
  }
}

/** ¿La versión del build es más nueva que la última release publicada? */
export function debePublicar() {
  const build = versionDeBuild();
  const ultimo = ultimoTagPublicado();
  if (!ultimo) return true;
  return compararVersiones(parsearVersion(build.versionName), ultimo.version) > 0;
}

// ---- CLI -------------------------------------------------------------------
// Siempre sale con código 0: el veredicto viaja por stdout, no por exit code,
// para que el paso de CI no falle en el camino "omitir".

const [comando, argumento] = process.argv.slice(2);

if (comando === 'cmp') {
  const codigo = Number(argumento);
  if (!Number.isFinite(codigo)) {
    console.error('Uso: node scripts/version_utils.mjs cmp <versionCode>');
    process.exit(2);
  }
  const actual = versionDeBuild();
  console.log(
    actual.versionCode > codigo ? 'mayor' : actual.versionCode === codigo ? 'igual' : 'menor',
  );
} else if (comando === 'release-tag') {
  console.log(`v${versionDeBuild().versionName}`);
} else if (comando === 'release-name') {
  console.log(versionDeBuild().versionName);
} else if (comando === 'debe-publicar') {
  console.log(debePublicar() ? 'publicar' : 'omitir');
} else if (!comando) {
  console.log(JSON.stringify(versionDeBuild()));
} else {
  console.error(`Comando desconocido: ${comando}`);
  process.exit(2);
}
