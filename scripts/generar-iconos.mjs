/**
 * Genera los iconos de la PWA (192 y 512 px) dibujando una carpa/toldo
 * con un encoder PNG mínimo en Node (sin dependencias externas).
 */
import { deflateSync } from 'node:zlib';
import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = dirname(fileURLToPath(import.meta.url));

// --- Encoder PNG mínimo -------------------------------------------------

const CRC_TABLE = (() => {
  const tabla = new Int32Array(256);
  for (let n = 0; n < 256; n++) {
    let c = n;
    for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
    tabla[n] = c;
  }
  return tabla;
})();

function crc32(buf) {
  let c = 0xffffffff;
  for (let i = 0; i < buf.length; i++) c = CRC_TABLE[(c ^ buf[i]) & 0xff] ^ (c >>> 8);
  return (c ^ 0xffffffff) >>> 0;
}

function chunk(tipo, datos) {
  const longitud = Buffer.alloc(4);
  longitud.writeUInt32BE(datos.length);
  const tipoBuf = Buffer.from(tipo, 'ascii');
  const crc = Buffer.alloc(4);
  crc.writeUInt32BE(crc32(Buffer.concat([tipoBuf, datos])));
  return Buffer.concat([longitud, tipoBuf, datos, crc]);
}

function crearPng(tamano, rgba) {
  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(tamano, 0);
  ihdr.writeUInt32BE(tamano, 4);
  ihdr[8] = 8; // profundidad de bits
  ihdr[9] = 6; // color type: RGBA
  const filas = Buffer.alloc(tamano * (1 + tamano * 4));
  for (let y = 0; y < tamano; y++) {
    filas[y * (1 + tamano * 4)] = 0; // filtro "ninguno"
    rgba.copy(filas, y * (1 + tamano * 4) + 1, y * tamano * 4, (y + 1) * tamano * 4);
  }
  const idat = deflateSync(filas);
  return Buffer.concat([
    Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]),
    chunk('IHDR', ihdr),
    chunk('IDAT', idat),
    chunk('IEND', Buffer.alloc(0))
  ]);
}

// --- Dibujo de la carpa -------------------------------------------------

const FONDO = [15, 118, 110, 255]; // teal marca-800
const CARPA = [255, 255, 255, 255]; // blanco
const PUERTA = [15, 118, 110, 255]; // fondo (recorte de la entrada)

function dibujarCarpa(tamano) {
  const buf = Buffer.alloc(tamano * tamano * 4);
  const poner = (x, y, color) => {
    if (x < 0 || y < 0 || x >= tamano || y >= tamano) return;
    const i = (y * tamano + x) * 4;
    buf[i] = color[0];
    buf[i + 1] = color[1];
    buf[i + 2] = color[2];
    buf[i + 3] = color[3];
  };
  // Punto dentro de un triángulo (barycentric)
  const enTriangulo = (px, py, a, b, c) => {
    const d1 = (px - b.x) * (a.y - b.y) - (a.x - b.x) * (py - b.y);
    const d2 = (px - c.x) * (b.y - c.y) - (b.x - c.x) * (py - c.y);
    const d3 = (px - a.x) * (c.y - a.y) - (c.x - a.x) * (py - a.y);
    const tieneNeg = d1 < 0 || d2 < 0 || d3 < 0;
    const tienePos = d1 > 0 || d2 > 0 || d3 > 0;
    return !(tieneNeg && tienePos);
  };

  const punto = (fx, fy) => ({ x: Math.round(fx * tamano), y: Math.round(fy * tamano) });
  const carpa = [punto(0.5, 0.18), punto(0.12, 0.88), punto(0.88, 0.88)];
  const puerta = [punto(0.5, 0.48), punto(0.36, 0.88), punto(0.64, 0.88)];

  for (let y = 0; y < tamano; y++) {
    for (let x = 0; x < tamano; x++) {
      if (enTriangulo(x + 0.5, y + 0.5, carpa[0], carpa[1], carpa[2])) {
        poner(x, y, enTriangulo(x + 0.5, y + 0.5, puerta[0], puerta[1], puerta[2]) ? PUERTA : CARPA);
      } else {
        poner(x, y, FONDO);
      }
    }
  }
  return buf;
}

// --- Escritura ----------------------------------------------------------

const directorio = join(__dirname, '..', 'public', 'icons');
mkdirSync(directorio, { recursive: true });
for (const tamano of [192, 512]) {
  writeFileSync(join(directorio, `icon-${tamano}.png`), crearPng(tamano, dibujarCarpa(tamano)));
  console.log(`icon-${tamano}.png generado`);
}
