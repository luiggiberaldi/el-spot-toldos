import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type {
  Alquiler,
  BitacoraEntrada,
  Cliente,
  Config,
  Recibo,
  Toldo
} from '../types/modelos';
import { numeroDeFolio, siguienteFolio, siguienteFolioAlquiler } from '../lib/folio';
import { calcularEstadosToldos } from '../lib/estados';
import {
  puedeCambiarEstadoAlquiler,
  unidadesOcupadas,
  validarAlquiler,
  validarToldo
} from '../lib/validaciones';
import { generarId } from '../lib/ids';
import { abonoPosterior, validarMontoPago } from '../lib/pagos';

/**
 * Tienda central de la aplicación (Zustand).
 * Toda la información se persiste automáticamente en localStorage bajo la
 * clave CLAVE_ALMACENAMIENTO, por lo que la app funciona sin conexión.
 */

export const CLAVE_ALMACENAMIENTO = 'gestor-toldos-v1';

export const CONFIG_POR_DEFECTO: Config = {
  negocio: {
    nombre: 'EL SPOT',
    rif: '',
    telefono: '',
    direccion: '',
    moneda: '$',
    logo: ''
  },
  /** Tasa de cambio manual: cuántos Bs vale 1 $ (0 = no mostrar equivalente). */
  tasaBs: 0,
  ultimoFolio: 0,
  ultimoFolioAlquiler: 0
};

/** Conjunto completo de datos, usado también para los respaldos. */
export interface DatosCompletos {
  clientes: Cliente[];
  toldos: Toldo[];
  alquileres: Alquiler[];
  recibos: Recibo[];
  bitacora: BitacoraEntrada[];
  config: Config;
}

interface EstadoApp extends DatosCompletos {
  agregarCliente: (cliente: Cliente) => void;
  actualizarCliente: (cliente: Cliente) => void;
  eliminarCliente: (id: string) => void;

  agregarToldo: (toldo: Toldo) => void;
  actualizarToldo: (toldo: Toldo) => void;
  eliminarToldo: (id: string) => void;

  /** Asigna automáticamente el folio correlativo de alquiler. */
  agregarAlquiler: (alquiler: Alquiler) => void;
  actualizarAlquiler: (alquiler: Alquiler) => void;
  eliminarAlquiler: (id: string) => void;

  /** Asigna automáticamente el folio correlativo del recibo. */
  agregarRecibo: (recibo: Recibo) => void;
  /** Emite el recibo y registra el abono dentro de la misma actualización. */
  emitirRecibo: (recibo: Recibo, registrarAbono: boolean) => string;
  eliminarRecibo: (id: string) => void;

  actualizarConfig: (config: Config) => void;
  restaurarDatos: (datos: DatosCompletos) => void;
  restablecerTodo: () => void;
}

export const useAppStore = create<EstadoApp>()(
  persist(
    (set) => ({
      clientes: [],
      toldos: [],
      alquileres: [],
      recibos: [],
      bitacora: [],
      config: CONFIG_POR_DEFECTO,

      agregarCliente: (cliente) =>
        set((estado) => ({
          clientes: [cliente, ...estado.clientes],
          bitacora: [entradaBitacora('Nuevo', 'Cliente', `Cliente creado: ${cliente.nombre}`), ...estado.bitacora]
        })),

      actualizarCliente: (cliente) =>
        set((estado) => ({
          clientes: estado.clientes.map((c) => (c.id === cliente.id ? cliente : c)),
          bitacora: [entradaBitacora('Cambio', 'Cliente', `Cliente actualizado: ${cliente.nombre}`), ...estado.bitacora]
        })),

      eliminarCliente: (id) =>
        set((estado) => ({
          clientes: estado.clientes.filter((c) => c.id !== id),
          bitacora: [entradaBitacora('Corrección', 'Cliente', `Cliente eliminado: ${id}`), ...estado.bitacora]
        })),

      agregarToldo: (toldo) =>
        set((estado) => {
          const normalizado = { ...toldo, unidades: Math.trunc(toldo.unidades || 1) };
          const error = validarToldo(normalizado, estado.alquileres);
          if (error) throw new Error(error);
          return {
            toldos: [normalizado, ...estado.toldos],
            bitacora: [entradaBitacora('Nuevo', 'Toldo', `Toldo creado: ${normalizado.nombre}`), ...estado.bitacora]
          };
        }),

      actualizarToldo: (toldo) =>
        set((estado) => {
          const normalizado = { ...toldo, unidades: Math.trunc(toldo.unidades || 1) };
          const error = validarToldo(normalizado, estado.alquileres);
          if (error) throw new Error(error);
          const toldos = estado.toldos.map((t) => (t.id === normalizado.id ? normalizado : t));
          return {
            toldos: calcularEstadosToldos(estado.alquileres, toldos),
            bitacora: [entradaBitacora('Cambio', 'Toldo', `Toldo actualizado: ${normalizado.nombre}`), ...estado.bitacora]
          };
        }),

      eliminarToldo: (id) =>
        set((estado) => {
          if (unidadesOcupadas(id, estado.alquileres) > 0) {
            throw new Error('No puedes eliminar un toldo con unidades asignadas a un alquiler activo.');
          }
          return {
            toldos: estado.toldos.filter((t) => t.id !== id),
            bitacora: [entradaBitacora('Corrección', 'Toldo', `Toldo eliminado: ${id}`), ...estado.bitacora]
          };
        }),

      agregarAlquiler: (alquiler) =>
        set((estado) => {
          const candidato = { ...alquiler, modalidad: alquiler.modalidad ?? '24h' as const };
          const error = validarAlquiler(candidato, estado.clientes, estado.toldos, estado.alquileres);
          if (error) throw new Error(error);
          const folio = siguienteFolioAlquiler(estado.config.ultimoFolioAlquiler);
          const nuevo = { ...candidato, folio };
          const alquileres = [nuevo, ...estado.alquileres];
          return {
            alquileres,
            toldos: calcularEstadosToldos(alquileres, estado.toldos),
            config: {
              ...estado.config,
              ultimoFolioAlquiler: estado.config.ultimoFolioAlquiler + 1
            },
            bitacora: [entradaBitacora('Nuevo', 'Alquiler', `Alquiler creado: ${folio}`), ...estado.bitacora]
          };
        }),

      actualizarAlquiler: (alquiler) =>
        set((estado) => {
          const actual = estado.alquileres.find((item) => item.id === alquiler.id);
          if (!actual) throw new Error('El alquiler que intentas editar ya no existe.');
          if (!puedeCambiarEstadoAlquiler(actual.estado, alquiler.estado)) {
            throw new Error(`No se puede cambiar un alquiler ${actual.estado} a ${alquiler.estado}.`);
          }
          const error = validarAlquiler(alquiler, estado.clientes, estado.toldos, estado.alquileres);
          if (error) throw new Error(error);
          const alquileres = estado.alquileres.map((a) =>
            a.id === alquiler.id ? alquiler : a
          );
          return {
            alquileres,
            toldos: calcularEstadosToldos(alquileres, estado.toldos),
            bitacora: [entradaBitacora('Cambio', 'Alquiler', `Alquiler actualizado: ${alquiler.folio}`), ...estado.bitacora]
          };
        }),

      eliminarAlquiler: (id) =>
        set((estado) => {
          const alquiler = estado.alquileres.find((item) => item.id === id);
          const alquileres = estado.alquileres.filter((a) => a.id !== id);
          return {
            alquileres,
            toldos: calcularEstadosToldos(alquileres, estado.toldos),
            bitacora: [entradaBitacora('Corrección', 'Alquiler', `Alquiler eliminado: ${alquiler?.folio ?? id}`), ...estado.bitacora]
          };
        }),

      agregarRecibo: (recibo) =>
        set((estado) => {
          const alquiler = estado.alquileres.find((item) => item.id === recibo.alquilerId);
          if (!alquiler) throw new Error('No se encontró el alquiler del recibo.');
          if (alquiler.estado === 'cancelado') throw new Error('No se puede emitir un recibo de un alquiler cancelado.');
          if (!Number.isFinite(recibo.monto) || recibo.monto <= 0 || recibo.monto > alquiler.montoTotal) {
            throw new Error('El monto del recibo debe ser mayor que 0 y no superar el total.');
          }
          const folio = siguienteFolio(estado.config.ultimoFolio);
          const nuevo: Recibo = { ...recibo, folio, datos: { ...recibo.datos, folio } };
          return {
            recibos: [nuevo, ...estado.recibos],
            config: { ...estado.config, ultimoFolio: estado.config.ultimoFolio + 1 },
            bitacora: [entradaBitacora('Nuevo', 'Recibo', `Recibo emitido: ${folio}`), ...estado.bitacora]
          };
        }),

      emitirRecibo: (recibo, registrarAbono) => {
        let folioAsignado = '';
        set((estado) => {
          const alquiler = estado.alquileres.find((item) => item.id === recibo.alquilerId);
          if (!alquiler) throw new Error('No se encontró el alquiler del recibo.');
          if (alquiler.estado === 'cancelado') throw new Error('No se puede emitir un recibo de un alquiler cancelado.');
          const estadoRecibo = recibo.estado ?? (registrarAbono ? 'pagado' : 'por_pagar');
          const registraPago = registrarAbono;
          const errorPago = validarMontoPago(recibo.monto, alquiler.montoTotal, alquiler.abono, registraPago);
          if (errorPago) throw new Error(errorPago);
          const folio = siguienteFolio(estado.config.ultimoFolio);
          folioAsignado = folio;
          const abonoDespues = abonoPosterior(alquiler.montoTotal, alquiler.abono, recibo.monto, registraPago);
          const nuevo: Recibo = {
            ...recibo,
            folio,
            estado: estadoRecibo,
            datos: {
              ...recibo.datos,
              folio,
              estado: estadoRecibo,
              alquiler: { ...recibo.datos.alquiler, abono: abonoDespues }
            }
          };
          const alquileres = registraPago
            ? estado.alquileres.map((item) =>
                item.id === alquiler.id ? { ...item, abono: abonoDespues } : item
              )
            : estado.alquileres;
          return {
            recibos: [nuevo, ...estado.recibos],
            alquileres,
            config: { ...estado.config, ultimoFolio: estado.config.ultimoFolio + 1 },
            bitacora: [entradaBitacora('Nuevo', 'Recibo', `Recibo emitido: ${folio}`), ...estado.bitacora]
          };
        });
        return folioAsignado;
      },

      eliminarRecibo: (id) =>
        set((estado) => ({
          recibos: estado.recibos.filter((r) => r.id !== id),
          bitacora: [entradaBitacora('Corrección', 'Recibo', `Recibo eliminado: ${id}`), ...estado.bitacora]
        })),

      actualizarConfig: (config) => {
        if (!config.negocio.nombre.trim()) throw new Error('El nombre del negocio es obligatorio.');
        if (!Number.isFinite(config.tasaBs) || config.tasaBs < 0) throw new Error('La tasa debe ser un número mayor o igual a 0.');
        set((estado) => ({
          config: { ...config, negocio: { ...config.negocio, nombre: config.negocio.nombre.trim(), moneda: '$' } },
          bitacora: [entradaBitacora('Cambio', 'Configuración', 'Configuración del negocio actualizada'), ...estado.bitacora]
        }));
      },

      restaurarDatos: (datos) =>
        set(() => {
          const configBase: Config = {
            ...CONFIG_POR_DEFECTO,
            ...(datos.config ?? {}),
            negocio: { ...CONFIG_POR_DEFECTO.negocio, ...(datos.config?.negocio ?? {}), moneda: '$' },
            tasaBs: Number.isFinite(datos.config?.tasaBs) && (datos.config?.tasaBs ?? 0) >= 0 ? datos.config!.tasaBs : 0,
            ultimoFolioAlquiler: datos.config?.ultimoFolioAlquiler ?? 0
          };
          const toldos = (datos.toldos ?? []).map((toldo) => ({
            ...toldo,
            unidades: Number.isInteger(toldo.unidades) && toldo.unidades > 0 ? toldo.unidades : 1
          }));
          const alquileres = (datos.alquileres ?? []).map((a) =>
            a.modalidad ? a : { ...a, modalidad: '24h' as const }
          );
          const recibos = datos.recibos ?? [];
          const bitacora = datos.bitacora ?? [];
          const ultimoFolio = Math.max(configBase.ultimoFolio, ...recibos.map((r) => numeroDeFolio(r.folio)), 0);
          const ultimoFolioAlquiler = Math.max(configBase.ultimoFolioAlquiler, ...alquileres.map((a) => numeroDeFolio(a.folio)), 0);
          return {
            clientes: datos.clientes ?? [],
            toldos: calcularEstadosToldos(alquileres, toldos),
            alquileres,
            recibos,
            config: { ...configBase, ultimoFolio, ultimoFolioAlquiler },
            bitacora: [entradaBitacora('Cambio', 'Respaldo', 'Respaldo restaurado'), ...bitacora]
          };
        }),

      restablecerTodo: () =>
        set({
          clientes: [],
          toldos: [],
          alquileres: [],
          recibos: [],
          config: CONFIG_POR_DEFECTO,
          bitacora: [entradaBitacora('Corrección', 'Sistema', 'Todos los datos fueron restablecidos')]
        })
    }),
    {
      name: CLAVE_ALMACENAMIENTO,
      version: 2,
      migrate: (persistedState) => {
        const estado = persistedState as Partial<DatosCompletos> | undefined;
        const toldos = (estado?.toldos ?? []).map((toldo) => ({
          ...toldo,
          unidades: Number.isInteger(toldo.unidades) && toldo.unidades > 0 ? toldo.unidades : 1
        }));
        const alquileres = (estado?.alquileres ?? []).map((alquiler) =>
          alquiler.modalidad ? alquiler : { ...alquiler, modalidad: '24h' as const }
        );
        return {
          clientes: estado?.clientes ?? [],
          toldos: calcularEstadosToldos(alquileres, toldos),
          alquileres,
          recibos: estado?.recibos ?? [],
          bitacora: estado?.bitacora ?? [],
          config: {
            ...CONFIG_POR_DEFECTO,
            ...(estado?.config ?? {}),
            negocio: { ...CONFIG_POR_DEFECTO.negocio, ...(estado?.config?.negocio ?? {}), moneda: '$' },
            tasaBs: Number.isFinite(estado?.config?.tasaBs) ? Math.max(0, estado?.config?.tasaBs ?? 0) : 0
          }
        } as Partial<EstadoApp>;
      }
    }
  )
);

function entradaBitacora(
  tipo: BitacoraEntrada['tipo'],
  entidad: string,
  descripcion: string
): BitacoraEntrada {
  return { id: generarId(), fecha: new Date().toISOString(), tipo, entidad, descripcion };
}
