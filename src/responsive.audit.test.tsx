/**
 * Auditoría de responsividad como pruebas — EL SPOT · Gestor de Toldos.
 *
 * Ejecuta la PWA real en Chromium (Edge del sistema) mediante Vitest Browser
 * y hace cumplir, de forma determinista:
 *
 *   R-OV · Ninguna vista desborda horizontalmente su viewport.
 *   R-TT · Ningún objetivo táctil visible es menor a 24×24 px (WCAG 2.5.8),
 *          con la excepción WCAG de enlaces verdaderamente inline.
 *   R-IN · Ningún campo de texto/número baja de 16 px (zoom automático de iOS).
 *
 * Se auditan las 6 vistas del sistema en 320 / 360 / 375 / 768 / 1280 px,
 * más el modal "Nuevo alquiler" (el formulario más complejo) en móvil.
 *
 * Ejecución:  npm run test:responsive
 * (requiere Edge o Chrome instalado; navegador alternativo:
 *  NAVERGADOR_AUDITORIA=chrome npm run test:responsive)
 */
import { beforeAll, describe, expect, it } from 'vitest';
import { page } from '@vitest/browser/context';
import './index.css';
import { CLAVE_ALMACENAMIENTO } from './data/store';
import type { DatosCompletos } from './data/store';

const ANCHOS = [320, 360, 375, 768, 1280] as const;
const VISTAS: Array<{ vista: string; etiqueta: string }> = [
  { vista: 'panel', etiqueta: 'Panel' },
  { vista: 'clientes', etiqueta: 'Clientes' },
  { vista: 'toldos', etiqueta: 'Toldos' },
  { vista: 'alquileres', etiqueta: 'Alquileres' },
  { vista: 'recibos', etiqueta: 'Recibos' },
  { vista: 'configuracion', etiqueta: 'Configuración' }
];

const TAP_MINIMO = 24;
const FUENTE_MINIMA = 16;

const esperar = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

/** Datos de prueba con todas las entidades pobladas (2 clientes, 2 toldos, 2 alquileres, 1 recibo). */
function datosSemilla(): DatosCompletos {
  const ahora = '2026-09-02T12:00:00.000Z';
  return {
    clientes: [
      { id: 'cl-1', nombre: 'María Pérez', cedula: 'V-12.345.678', telefono: '0412-1234567', email: '', direccion: 'Av. Principal, Los Dos Caminos', notas: '', creadoEn: ahora },
      { id: 'cl-2', nombre: 'José Rodríguez', cedula: 'V-8.765.432', telefono: '0414-7654321', email: '', direccion: 'Calle 5, Petare', notas: '', creadoEn: ahora }
    ],
    toldos: [
      { id: 'to-1', nombre: 'Toldo blanco 4x4', tamano: '4x4 m', tarifa: 40, tarifa12h: 20, unidades: 3, estado: 'alquilado', notas: '', creadoEn: ahora },
      { id: 'to-2', nombre: 'Toldo azul 3x3', tamano: '3x3 m', tarifa: 30, tarifa12h: 15, unidades: 2, estado: 'disponible', notas: '', creadoEn: ahora }
    ],
    alquileres: [
      { id: 'al-1', folio: 'ALQ-0001', clienteId: 'cl-1', items: [{ toldoId: 'to-1', cantidad: 2, tarifa: 40 }], modalidad: '24h', direccion: 'Av. Principal, Los Dos Caminos', lat: 10.49, lng: -66.85, montoTotal: 80, abono: 40, estado: 'activo', notas: '', creadoEn: ahora },
      { id: 'al-2', folio: 'ALQ-0002', clienteId: 'cl-2', items: [{ toldoId: 'to-2', cantidad: 1, tarifa: 30 }], modalidad: '12h', direccion: 'Calle 5, Petare', montoTotal: 30, abono: 30, estado: 'entregado', notas: '', creadoEn: ahora }
    ],
    recibos: [
      {
        id: 're-1', folio: 'REC-0001', alquilerId: 'al-1', emitidoEn: ahora, concepto: 'Abono del alquiler', monto: 40, estado: 'pagado',
        datos: {
          folio: 'REC-0001', emitidoEn: ahora, concepto: 'Abono del alquiler', estado: 'pagado', monto: 40,
          negocio: { nombre: 'EL SPOT', rif: 'J-123456789', telefono: '0412-1234567', direccion: 'Av. Siempre Viva', moneda: '$', logo: '', tasaBs: 36.5 },
          cliente: { nombre: 'María Pérez', cedula: 'V-12.345.678', telefono: '0412-1234567', direccion: 'Los Dos Caminos' },
          alquiler: { folio: 'ALQ-0001', items: [{ nombre: 'Toldo blanco 4x4', cantidad: 2, tarifa: 40 }], modalidad: '24h', direccion: 'Av. Principal', lat: 10.49, lng: -66.85, montoTotal: 80, abono: 40 }
        }
      }
    ],
    bitacora: [],
    config: {
      negocio: { nombre: 'EL SPOT', rif: 'J-123456789', telefono: '0412-1234567', direccion: 'Av. Siempre Viva', moneda: '$', logo: '' },
      tasaBs: 36.5,
      ultimoFolio: 1,
      ultimoFolioAlquiler: 2
    }
  };
}

function describir(el: Element): string {
  const clase = typeof el.className === 'string' ? el.className : '';
  return `<${el.tagName.toLowerCase()}${clase ? ` class="${clase.slice(0, 60)}"` : ''}> "${(el.textContent ?? '').trim().slice(0, 25)}"`;
}

function visible(el: Element): boolean {
  const r = el.getBoundingClientRect();
  if (r.width === 0 && r.height === 0) return false;
  const cs = getComputedStyle(el);
  return cs.display !== 'none' && cs.visibility !== 'hidden';
}

/** ¿El elemento o un ancestro recorta con scroll horizontal intencional (p. ej. la barra inferior)? */
function dentroDeScrollIntencional(el: Element): boolean {
  let actual: Element | null = el;
  while (actual && actual !== document.body) {
    const cs = getComputedStyle(actual);
    if (/(auto|scroll)/.test(cs.overflowX) && actual.scrollWidth > actual.clientWidth + 1) return true;
    actual = actual.parentElement;
  }
  return false;
}

/**
 * R-OV: elementos cuyo borde sobrepasa el ancho del viewport. Se ignoran los
 * elementos dentro de un contenedor con scroll horizontal intencional (el
 * patrón documentado de la barra inferior en <380 px, regla R11 de la
 * auditoría); el desbordamiento de la propia página sí falla siempre.
 */
function ofensoresDeOverflow(vw: number): string[] {
  const d = document.documentElement;
  const hallados: string[] = [];
  if (d.scrollWidth > vw + 1) hallados.push(`<html> scrollWidth=${d.scrollWidth} > viewport=${vw}`);
  document.querySelectorAll('body *').forEach((el) => {
    const r = el.getBoundingClientRect();
    if (r.width > 0 && (r.right > vw + 1 || r.left < -1)) {
      if (!dentroDeScrollIntencional(el)) hallados.push(`${describir(el)} right=${Math.round(r.right)} left=${Math.round(r.left)}`);
    }
  });
  return hallados;
}

/** R-TT: objetivos interactivos visibles por debajo del mínimo, salvo enlaces inline (excepción WCAG 2.5.8). */
function objetivosPequeños(): string[] {
  const hallados: string[] = [];
  document.querySelectorAll('button, a[href], input, textarea, [role="button"]').forEach((el) => {
    if (!visible(el)) return;
    const r = el.getBoundingClientRect();
    const cs = getComputedStyle(el);
    // Excepción WCAG "Inline": el enlace fluye con el texto del párrafo.
    if (cs.display === 'inline') return;
    if (r.width < TAP_MINIMO || r.height < TAP_MINIMO) {
      hallados.push(`${describir(el)} ${Math.round(r.width)}×${Math.round(r.height)} px`);
    }
  });
  return hallados;
}

/** R-IN: campos de texto/número con fuente menor a 16 px (iOS hace zoom al enfocarlos). */
function inputsPequeños(): string[] {
  const hallados: string[] = [];
  document.querySelectorAll('input:not([type="hidden"]):not([type="checkbox"]):not([type="radio"]), textarea').forEach((el) => {
    if (!visible(el)) return;
    const fs = parseFloat(getComputedStyle(el).fontSize);
    if (fs < FUENTE_MINIMA) hallados.push(`${describir(el)} font-size=${fs} px`);
  });
  return hallados;
}

/** Navega a una vista pulsando el botón de navegación visible con la etiqueta dada. */
async function navegarA(etiqueta: string): Promise<void> {
  const boton = [...document.querySelectorAll('button')].find(
    (b) => b.textContent?.trim() === etiqueta && visible(b)
  ) as HTMLButtonElement | undefined;
  if (!boton) throw new Error(`Botón de navegación "${etiqueta}" no encontrado`);
  boton.click();
  await esperar(180);
}

async function cambiarViewport(ancho: number): Promise<void> {
  await page.viewport(ancho, 800);
  await esperar(160);
}

beforeAll(async () => {
  localStorage.setItem(CLAVE_ALMACENAMIENTO, JSON.stringify({ state: datosSemilla(), version: 2 }));
  const [{ default: App }, ReactDOM, React] = await Promise.all([
    import('./App'),
    import('react-dom/client'),
    import('react')
  ]);
  const contenedor = document.createElement('div');
  contenedor.id = 'raiz-auditoria';
  document.body.appendChild(contenedor);
  ReactDOM.createRoot(contenedor).render(React.createElement(App));
  await esperar(400);
  await cambiarViewport(375);
});

describe('Auditoría de responsividad (navegador real)', () => {
  it('R-OV: ninguna vista desborda horizontalmente en 320/360/375/768/1280 px', async () => {
    const violaciones: string[] = [];
    for (const ancho of ANCHOS) {
      await cambiarViewport(ancho);
      for (const { etiqueta } of VISTAS) {
        await navegarA(etiqueta);
        for (const ofensor of ofensoresDeOverflow(ancho)) {
          violaciones.push(`[${ancho}px · ${etiqueta}] ${ofensor}`);
        }
      }
    }
    expect(violaciones).toEqual([]);
  });

  it('R-TT: ningún objetivo táctil visible es menor a 24×24 px (WCAG 2.5.8)', async () => {
    const violaciones: string[] = [];
    for (const ancho of ANCHOS) {
      await cambiarViewport(ancho);
      for (const { etiqueta } of VISTAS) {
        await navegarA(etiqueta);
        for (const ofensor of objetivosPequeños()) {
          violaciones.push(`[${ancho}px · ${etiqueta}] ${ofensor}`);
        }
      }
    }
    expect(violaciones).toEqual([]);
  });

  it('R-IN: ningún campo de texto/número baja de 16 px (zoom automático de iOS)', async () => {
    const violaciones: string[] = [];
    for (const ancho of [320, 375, 1280] as const) {
      await cambiarViewport(ancho);
      for (const { etiqueta } of VISTAS) {
        await navegarA(etiqueta);
        for (const ofensor of inputsPequeños()) {
          violaciones.push(`[${ancho}px · ${etiqueta}] ${ofensor}`);
        }
      }
    }
    expect(violaciones).toEqual([]);
  });

  it('Modal "Nuevo alquiler": sin overflow, táctiles ≥24px e inputs ≥16px en 320 y 375 px', async () => {
    const violaciones: string[] = [];
    for (const ancho of [320, 375] as const) {
      await cambiarViewport(ancho);
      await navegarA('Alquileres');
      const abrir = [...document.querySelectorAll('main button')].find(
        (b) => b.textContent?.includes('Nuevo alquiler')
      ) as HTMLButtonElement | undefined;
      if (!abrir) throw new Error('Botón "Nuevo alquiler" no encontrado');
      abrir.click();
      await esperar(250);
      const modal = document.querySelector('.anim-modal');
      if (!modal) throw new Error('El modal de nuevo alquiler no se abrió');
      const r = modal.getBoundingClientRect();
      if (r.width > ancho + 1) violaciones.push(`[${ancho}px] modal ${Math.round(r.width)} px > viewport ${ancho} px`);
      for (const ofensor of ofensoresDeOverflow(ancho)) violaciones.push(`[${ancho}px · modal] ${ofensor}`);
      for (const ofensor of objetivosPequeños()) violaciones.push(`[${ancho}px · modal] ${ofensor}`);
      for (const ofensor of inputsPequeños()) violaciones.push(`[${ancho}px · modal] ${ofensor}`);
      const cerrar = modal.querySelector('button[aria-label="Cerrar"]');
      (cerrar as HTMLButtonElement | null)?.click();
      await esperar(200);
    }
    expect(violaciones).toEqual([]);
  });
});
