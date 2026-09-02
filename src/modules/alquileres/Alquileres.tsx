import { useMemo, useState } from 'react';
import { useAppStore } from '../../data/store';
import { generarId } from '../../lib/ids';
import { calcularSaldo, redondearMonto } from '../../lib/calculos';
import { formatearFechaCorta, formatearMonto, formatearMontoDual } from '../../lib/formato';
import { enlaceMapa, formatearCoordenadas, obtenerUbicacion } from '../../lib/geolocalizacion';
import { direccionDesdeUbicacion } from '../../lib/geocodificacion';
import { puedeCambiarEstadoAlquiler, unidadesDisponibles } from '../../lib/validaciones';
import { validarMontoPago } from '../../lib/pagos';
import type { Alquiler, DatosRecibo, EstadoAlquiler, EstadoRecibo, ModalidadAlquiler } from '../../types/modelos';
import { Modal } from '../../components/Modal';
import {
  CampoNumero,
  CampoSelect,
  CampoTexto,
  CampoTextoArea,
  SelectFiltro
} from '../../components/Campos';
import { EtiquetaAlquiler } from '../../components/Etiqueta';
import type { Vista } from '../../components/Layout';
import { Plus, X, MapPin, Receipt, CheckCircle2, Clock3, FileText, WalletCards, AlertCircle } from 'lucide-react';

const OPCIONES_ESTADO: Array<{ valor: EstadoAlquiler; etiqueta: string }> = [
  { valor: 'activo', etiqueta: 'Activo' },
  { valor: 'entregado', etiqueta: 'Entregado' },
  { valor: 'devuelto', etiqueta: 'Devuelto' },
  { valor: 'cancelado', etiqueta: 'Cancelado' }
];

const ETIQUETA_MODALIDAD: Record<ModalidadAlquiler, string> = {
  '12h': '12 horas',
  '24h': '24 horas'
};

type TipoMonto = 'total' | 'abono' | 'saldo' | 'otro';

const CONCEPTOS: Record<TipoMonto, string> = {
  total: 'Pago total del alquiler',
  abono: 'Abono del alquiler',
  saldo: 'Pendiente de pago del alquiler',
  otro: 'Pago del alquiler'
};

interface ItemFormulario {
  toldoId: string;
  cantidad: string;
  tarifa: string;
}

/** Módulo de gestión de alquileres. */
export function Alquileres({ navegar }: { navegar: (vista: Vista) => void }) {
  const clientes = useAppStore((s) => s.clientes);
  const toldos = useAppStore((s) => s.toldos);
  const alquileres = useAppStore((s) => s.alquileres);
  const moneda = useAppStore((s) => s.config.negocio.moneda);
  const tasaBs = useAppStore((s) => s.config.tasaBs);
  const agregarAlquiler = useAppStore((s) => s.agregarAlquiler);
  const actualizarAlquiler = useAppStore((s) => s.actualizarAlquiler);
  const eliminarAlquiler = useAppStore((s) => s.eliminarAlquiler);

  const [filtroEstado, setFiltroEstado] = useState<EstadoAlquiler | 'todos'>('todos');
  const [busqueda, setBusqueda] = useState('');
  const [detalle, setDetalle] = useState<Alquiler | null>(null);
  const [formularioAbierto, setFormularioAbierto] = useState(false);
  const [editando, setEditando] = useState<Alquiler | null>(null);
  const [emitirDe, setEmitirDe] = useState<Alquiler | null>(null);
  const [errorOperacion, setErrorOperacion] = useState('');
  const [confirmarEliminar, setConfirmarEliminar] = useState<Alquiler | null>(null);

  const nombreCliente = (id: string) =>
    clientes.find((c) => c.id === id)?.nombre ?? 'Cliente eliminado';

  // Memoizado: evita re-filtrar todo el listado en cada render (patrón de Bitacora).
  const filtrados = useMemo(() => {
    const termino = busqueda.toLowerCase();
    return alquileres.filter((a) => {
      const coincideEstado = filtroEstado === 'todos' || a.estado === filtroEstado;
      const texto = `${a.folio} ${nombreCliente(a.clienteId)} ${a.direccion}`.toLowerCase();
      return coincideEstado && texto.includes(termino);
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps -- nombreCliente depende solo de `clientes`
  }, [alquileres, filtroEstado, busqueda, clientes]);

  const abrirNuevo = () => {
    setEditando(null);
    setDetalle(null);
    setFormularioAbierto(true);
  };

  const abrirEdicion = (alquiler: Alquiler) => {
    setEditando(alquiler);
    setDetalle(null);
    setFormularioAbierto(true);
  };

  const eliminar = (alquiler: Alquiler) => {
    try {
      eliminarAlquiler(alquiler.id);
      setErrorOperacion('');
      setDetalle(null);
      setConfirmarEliminar(null);
    } catch (error) {
      setErrorOperacion(error instanceof Error ? error.message : 'No se pudo eliminar el alquiler.');
    }
  };

  return (
    <div className="space-y-4">
      {errorOperacion && (
        <div className="rounded-xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-300" role="alert">
          {errorOperacion}
        </div>
      )}

      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="titulo-pagina">Alquileres</h1>
          <p className="text-sm text-gray-400">
            {alquileres.length} alquiler{alquileres.length === 1 ? '' : 'es'} registrado
            {alquileres.length === 1 ? '' : 's'}.
          </p>
        </div>
        <button className="btn-primario" onClick={abrirNuevo}>
          <Plus className="h-4 w-4" />
          Nuevo alquiler
        </button>
      </div>

      <div className="flex flex-wrap gap-2">
        <input
          className="input max-w-xs"
          type="search"
          placeholder="Buscar por folio, cliente o dirección…"
          value={busqueda}
          onChange={(e) => setBusqueda(e.target.value)}
        />
        <div className="w-44 shrink-0">
          <SelectFiltro
            valor={filtroEstado as never}
            alCambiar={(v) => setFiltroEstado(v as EstadoAlquiler | 'todos')}
            opciones={[
              { valor: 'todos' as never, etiqueta: 'Todos los estados' },
              ...OPCIONES_ESTADO
            ]}
          />
        </div>
      </div>

      {filtrados.length === 0 ? (
        <div className="tarjeta text-sm text-gray-400">
          {alquileres.length === 0
            ? 'Aún no hay alquileres. Pulsa "+ Nuevo alquiler" para crear el primero.'
            : 'No se encontraron alquileres con ese filtro.'}
        </div>
      ) : (
        <ul className="space-y-2">
          {filtrados.map((alquiler) => {
            const saldo = calcularSaldo(alquiler.montoTotal, alquiler.abono);
            return (
              <li key={alquiler.id} className="tarjeta flex flex-wrap items-center justify-between gap-3">
                <div className="min-w-0">
                  <p className="font-medium text-white">
                    {alquiler.folio} · {nombreCliente(alquiler.clienteId)}
                  </p>
                  <p className="text-xs text-gray-400">
                    {ETIQUETA_MODALIDAD[alquiler.modalidad] ?? '24 horas'}
                    {alquiler.direccion && ` · ${alquiler.direccion}`}
                  </p>
                  <p className="text-xs text-gray-400">
                    Total: {formatearMontoDual(alquiler.montoTotal, moneda, tasaBs)} · Pendiente:{' '}
                    <span className={saldo > 0 ? 'font-semibold text-red-400' : 'font-semibold text-cyan-300'}>
                      {formatearMontoDual(saldo, moneda, tasaBs)}
                    </span>
                  </p>
                </div>
                <div className="flex shrink-0 items-center gap-2">
                  <EtiquetaAlquiler estado={alquiler.estado} />
                  <button
                    className="btn-secundario !px-3 !py-1.5"
                    onClick={() => setDetalle(alquiler)}
                  >
                    Ver
                  </button>
                </div>
              </li>
            );
          })}
        </ul>
      )}

      {formularioAbierto && (
        <FormularioAlquiler
          editando={editando}
          clientes={clientes}
          toldos={toldos}
          alquileres={alquileres}
          alCerrar={() => setFormularioAbierto(false)}
          alGuardar={(alquiler) => {
            try {
              if (editando) actualizarAlquiler(alquiler);
              else agregarAlquiler(alquiler);
              setErrorOperacion('');
              setFormularioAbierto(false);
            } catch (error) {
              setErrorOperacion(error instanceof Error ? error.message : 'No se pudo guardar el alquiler.');
            }
          }}
          navegar={navegar}
        />
      )}

      {detalle && (
        <Modal titulo={`Alquiler ${detalle.folio}`} alCerrar={() => setDetalle(null)}>
          <DetalleAlquiler
            alquiler={detalle}
            nombreCliente={nombreCliente(detalle.clienteId)}
            moneda={moneda}
            alEditar={() => abrirEdicion(detalle)}
            alEmitirRecibo={() => {
              setEmitirDe(detalle);
              setDetalle(null);
            }}
            alEliminar={() => {
              setConfirmarEliminar(detalle);
              setDetalle(null);
            }}
          />
        </Modal>
      )}

      {confirmarEliminar && (
        <Modal titulo="Eliminar alquiler" alCerrar={() => setConfirmarEliminar(null)}>
          <div className="space-y-4">
            <p className="text-sm text-gray-300">¿Eliminar el alquiler <strong className="text-white">{confirmarEliminar.folio}</strong>? Los recibos emitidos se conservarán.</p>
            <p className="text-xs text-amber-300">Esta acción libera las unidades del inventario y no se puede deshacer.</p>
            <div className="flex justify-end gap-2">
              <button className="btn-secundario" onClick={() => setConfirmarEliminar(null)}>Cancelar</button>
              <button className="btn-peligro" onClick={() => eliminar(confirmarEliminar)}>Eliminar alquiler</button>
            </div>
          </div>
        </Modal>
      )}

      {emitirDe && (
        <EmitirRecibo
          alquiler={emitirDe}
          alCerrar={() => setEmitirDe(null)}
          navegar={navegar}
        />
      )}
    </div>
  );
}

/* ------------------------------------------------------------------ */
/* Formulario de alquiler (crear / editar)                             */
/* ------------------------------------------------------------------ */

function FormularioAlquiler({
  editando,
  clientes,
  toldos,
  alquileres,
  alCerrar,
  alGuardar,
  navegar
}: {
  editando: Alquiler | null;
  clientes: ReturnType<typeof useAppStore.getState>['clientes'];
  toldos: ReturnType<typeof useAppStore.getState>['toldos'];
  alquileres: ReturnType<typeof useAppStore.getState>['alquileres'];
  alCerrar: () => void;
  alGuardar: (alquiler: Alquiler) => void;
  navegar: (vista: Vista) => void;
}) {
  const moneda = useAppStore((s) => s.config.negocio.moneda);
  const tasaBs = useAppStore((s) => s.config.tasaBs);

  const [clienteId, setClienteId] = useState(editando?.clienteId ?? '');
  const [items, setItems] = useState<ItemFormulario[]>(
    editando
      ? editando.items.map((it) => ({
          toldoId: it.toldoId,
          cantidad: String(it.cantidad),
          tarifa: String(it.tarifa)
        }))
      : []
  );
  const [modalidad, setModalidad] = useState<ModalidadAlquiler>(editando?.modalidad ?? '24h');
  const [direccion, setDireccion] = useState(editando?.direccion ?? '');
  const [lat, setLat] = useState<number | null>(editando?.lat ?? null);
  const [lng, setLng] = useState<number | null>(editando?.lng ?? null);
  const [abono, setAbono] = useState(editando ? String(editando.abono) : '');
  const [estado, setEstado] = useState<EstadoAlquiler>(editando?.estado ?? 'activo');
  const [notas, setNotas] = useState(editando?.notas ?? '');
  const [error, setError] = useState('');
  const [capturando, setCapturando] = useState(false);
  const [errorGps, setErrorGps] = useState('');

  const subtotal = items.reduce(
    (acc, item) =>
      acc +
      (Number(item.tarifa.replace(',', '.')) || 0) *
        (Number.parseInt(item.cantidad, 10) || 0),
    0
  );
  // La línea ya contiene el precio de la modalidad seleccionada.
  const total = redondearMonto(subtotal);
  const abonoNumero = Number(abono.replace(',', '.')) || 0;
  const abonoExcedeTotal = Number.isFinite(abonoNumero) && abonoNumero > total;
  const saldo = calcularSaldo(total, abonoNumero);

  const cambiarAbono = (valor: string) => {
    setAbono(valor);
    if (Number.isFinite(Number(valor.replace(',', '.'))) && Number(valor.replace(',', '.')) > total) {
      setError(`El abono no puede superar el monto a pagar de ${formatearMonto(total, moneda)}.`);
    } else if (error.startsWith('El abono no puede superar')) {
      setError('');
    }
  };

  const agregarItem = () =>
    setItems([...items, { toldoId: '', cantidad: '1', tarifa: '' }]);

  const cambiarItem = (indice: number, parcial: Partial<ItemFormulario>) =>
    setItems(items.map((item, i) => (i === indice ? { ...item, ...parcial } : item)));

  const quitarItem = (indice: number) =>
    setItems(items.filter((_, i) => i !== indice));

  const seleccionarToldo = (indice: number, toldoId: string) => {
    const toldo = toldos.find((t) => t.id === toldoId);
    const precio = toldo
      ? modalidad === '12h'
        ? toldo.tarifa12h ?? toldo.tarifa * 0.5
        : toldo.tarifa
      : null;
    cambiarItem(indice, { toldoId, tarifa: precio === null ? '' : String(redondearMonto(precio)) });
  };

  const cambiarModalidad = (nuevaModalidad: ModalidadAlquiler) => {
    setModalidad(nuevaModalidad);
    setItems((actuales) => actuales.map((item) => {
      const toldo = toldos.find((t) => t.id === item.toldoId);
      if (!toldo) return item;
      const precio = nuevaModalidad === '12h'
        ? toldo.tarifa12h ?? toldo.tarifa * 0.5
        : toldo.tarifa;
      return { ...item, tarifa: String(redondearMonto(precio)) };
    }));
  };

  const capturarUbicacion = async () => {
    setCapturando(true);
    setErrorGps('');
    try {
      const ubicacion = await obtenerUbicacion();
      setLat(ubicacion.lat);
      setLng(ubicacion.lng);
      const direccionDetectada = await direccionDesdeUbicacion(ubicacion);
      if (direccionDetectada && !direccion.trim()) setDireccion(direccionDetectada);
    } catch (e) {
      setErrorGps(e instanceof Error ? e.message : 'No se pudo obtener la ubicación.');
    } finally {
      setCapturando(false);
    }
  };

  const guardar = () => {
    if (!clienteId) {
      setError('Selecciona un cliente.');
      return;
    }
    const lineaValida = (item: ItemFormulario) =>
      Boolean(item.toldoId) &&
      Number.isFinite(Number(item.tarifa.replace(',', '.'))) &&
      Number(item.tarifa.replace(',', '.')) >= 0 &&
      Number.isInteger(Number(item.cantidad)) &&
      Number(item.cantidad) > 0;
    if (items.some((item) => !lineaValida(item))) {
      setError('Revisa cada línea: selecciona un toldo, cantidad y tarifa válidas.');
      return;
    }
    const itemsValidos = items.filter(lineaValida);
    if (itemsValidos.length === 0) {
      setError('Agrega al menos un toldo con cantidad y tarifa válidas.');
      return;
    }
    if (!direccion.trim() && (lat === null || lng === null)) {
      setError('Indica la dirección del evento o captura la ubicación GPS.');
      return;
    }
    if ((lat === null) !== (lng === null)) {
      setError('La ubicación GPS debe incluir latitud y longitud.');
      return;
    }
    const abonoValido = Number(abono.replace(',', '.'));
    if (!Number.isFinite(abonoValido) || abonoValido < 0 || abonoValido > total) {
      setError(`El abono debe ser válido y no puede superar el monto a pagar de ${formatearMonto(total, moneda)}.`);
      return;
    }
    if (new Set(itemsValidos.map((item) => item.toldoId)).size !== itemsValidos.length) {
      setError('No repitas el mismo toldo en varias líneas.');
      return;
    }
    const inventario = new Map(toldos.map((toldo) => [toldo.id, toldo]));
    const ocupaInventario = estado === 'activo' || estado === 'entregado';
    if (
      ocupaInventario &&
      itemsValidos.some((item) => {
        const toldo = inventario.get(item.toldoId);
        return !toldo || Number(item.cantidad) > unidadesDisponibles(toldo, alquileres, editando?.id);
      })
    ) {
      setError('La cantidad solicitada supera las unidades disponibles.');
      return;
    }
    const datosBase = {
      clienteId,
      items: itemsValidos.map((item) => ({
        toldoId: item.toldoId,
        cantidad: Number.parseInt(item.cantidad, 10),
        tarifa: Number(item.tarifa.replace(',', '.'))
      })),
      modalidad,
      direccion: direccion.trim(),
      lat: lat ?? undefined,
      lng: lng ?? undefined,
      montoTotal: total,
      abono: redondearMonto(abonoValido),
      estado,
      notas: notas.trim()
    };
    if (editando) {
      alGuardar({ ...editando, ...datosBase });
    } else {
      alGuardar({ id: generarId(), folio: '', ...datosBase, creadoEn: new Date().toISOString() });
    }
  };

  return (
    <Modal
      titulo={editando ? `Editar alquiler ${editando.folio}` : 'Nuevo alquiler'}
      alCerrar={alCerrar}
      anchoMaximo="max-w-3xl"
    >
      <div className="space-y-5 pb-1">
        {/* Cliente */}
        <CampoSelect
          label="Cliente"
          valor={clienteId}
          alCambiar={setClienteId}
          obligatorio
          opciones={[
            { valor: '', etiqueta: '— Selecciona un cliente —' },
            ...clientes.map((c) => ({ valor: c.id, etiqueta: c.nombre }))
          ]}
        />
        {clientes.length === 0 && (
          <p className="text-xs text-amber-400">
            No hay clientes registrados.{' '}
            <button
              className="inline-block min-h-6 py-1 align-baseline font-medium underline"
              onClick={() => {
                alCerrar();
                navegar('clientes');
              }}
            >
              Crear un cliente aquí
            </button>
          </p>
        )}

        {/* Toldos del alquiler */}
        <div className="rounded-2xl border border-slate-800/70 bg-slate-900/50 p-3 sm:p-4">
          <div className="mb-3 flex items-center justify-between">
            <p className="text-sm font-semibold text-gray-200">
              Toldos alquilados <span className="text-red-400">*</span>
            </p>
            {toldos.length > 0 && (
              <button className="btn-secundario !px-2.5 !py-1 text-xs" onClick={agregarItem}>
                <Plus className="h-3.5 w-3.5" />
                Agregar
              </button>
            )}
          </div>
          <div className="space-y-3">
            {items.map((item, indice) => (
              <div
                key={indice}
                className="rounded-xl border border-slate-800/80 bg-slate-900/70 p-2.5 sm:p-3"
              >
                <div className="flex items-center gap-2">
                  <span className="text-xs font-semibold text-marca-300">
                    Toldo {indice + 1}
                  </span>
                  <span className="flex-1" />
                  <button
                    className="btn-secundario !h-8 !w-8 !px-0 !py-0 text-red-400"
                    onClick={() => quitarItem(indice)}
                    aria-label="Quitar toldo"
                  >
                    <X className="h-4 w-4" />
                  </button>
                </div>
                <div className="mt-2 grid grid-cols-12 items-end gap-2">
                  <div className="col-span-12 sm:col-span-6">
                    <CampoSelect
                      label=""
                      valor={item.toldoId}
                      alCambiar={(toldoId) => seleccionarToldo(indice, toldoId)}
                      placeholder="— Selecciona un toldo —"
                      opciones={[
                        { valor: '', etiqueta: '— Selecciona —' },
                        ...toldos
                          .filter((toldo) =>
                            toldo.id === item.toldoId ||
                            (toldo.estado !== 'en_reparacion' &&
                              toldo.estado !== 'retirado' &&
                              unidadesDisponibles(toldo, alquileres, editando?.id) > 0)
                          )
                          .map((t) => ({
                            valor: t.id,
                            etiqueta: `${t.nombre} (${t.tamano || 'sin tamaño'} · ${unidadesDisponibles(t, alquileres, editando?.id)} disp.)`
                          }))
                      ]}
                    />
                  </div>
                  <div className="col-span-3 sm:col-span-2">
                    <CampoNumero
                      label="Cant."
                      valor={item.cantidad}
                      alCambiar={(v) => cambiarItem(indice, { cantidad: v })}
                      paso="1"
                      min={1}
                    />
                  </div>
                  <div className="col-span-6 sm:col-span-3">
                    <CampoNumero
                      label={modalidad === '12h' ? 'Precio 12h' : 'Precio 24h'}
                      valor={item.tarifa}
                      alCambiar={(v) => cambiarItem(indice, { tarifa: v })}
                      min={0}
                    />
                  </div>
                  <div className="col-span-3 flex sm:col-span-1" />
                </div>
              </div>
            ))}
          </div>
          {toldos.length === 0 && (
            <p className="mt-2 text-xs text-amber-400">
            No hay toldos en el inventario.{' '}
            <button
              className="inline-block min-h-6 py-1 align-baseline font-medium underline"
              onClick={() => {
                alCerrar();
                navegar('toldos');
              }}
            >
              Crear un toldo aquí
            </button>
            </p>
          )}
        </div>

        {/* Modalidad de duración */}
        <div>
          <CampoSelect
            label="Modalidad de alquiler"
            valor={modalidad}
            alCambiar={cambiarModalidad}
            obligatorio
            opciones={[
              { valor: '24h', etiqueta: '24 horas (tarifa completa)' },
              { valor: '12h', etiqueta: '12 horas (mitad de tarifa)' }
            ]}
          />
          {modalidad === '12h' && (
            <p className="mt-1 text-xs text-marca-400">
              La modalidad de 12 horas cobra la mitad de la tarifa base de cada toldo.
            </p>
          )}
        </div>

        {/* Dirección + GPS */}
        <div className="rounded-2xl border border-slate-800/70 bg-slate-900/50 p-3 sm:p-4">
          <div className="mb-3 flex items-center gap-2">
            <MapPin className="h-4 w-4 text-marca-400" />
            <p className="text-sm font-semibold text-gray-200">Entrega y ubicación</p>
          </div>
          <CampoTexto
            label="Dirección del evento"
            valor={direccion}
            alCambiar={setDireccion}
            placeholder="Calle, urbanización, referencia…"
          />
          <div className="mt-3">
            {lat === null || lng === null ? (
              <button
                className="btn-secundario w-full justify-center !py-2.5"
                onClick={capturarUbicacion}
                disabled={capturando}
              >
                <MapPin className="h-4 w-4 text-marca-400" />
                {capturando ? 'Capturando…' : 'Capturar ubicación GPS'}
              </button>
            ) : (
              <div className="flex flex-wrap items-center gap-2 rounded-xl border border-marca-500/30 bg-marca-500/10 px-3 py-2.5">
                <MapPin className="h-4 w-4 shrink-0 text-marca-300" />
                <span className="text-sm font-medium text-marca-200">
                  {formatearCoordenadas(lat, lng)}
                </span>
                <span className="flex-1" />
                <a
                  className="text-sm font-medium text-marca-400 underline"
                  href={enlaceMapa(lat, lng)}
                  target="_blank"
                  rel="noreferrer"
                >
                  Mapas
                </a>
                <button
                  className="text-sm text-red-400 hover:underline"
                  onClick={() => {
                    setLat(null);
                    setLng(null);
                  }}
                >
                  Quitar
                </button>
              </div>
            )}
            {errorGps && <p className="mt-2 text-xs text-red-400">{errorGps}</p>}
          </div>
        </div>

        {/* Montos y estado operativo */}
        <div className="flex items-center gap-2">
          <WalletCards className="h-4 w-4 text-cyan-300" />
          <p className="text-sm font-semibold text-gray-200">Pago y estado</p>
        </div>
        <div className="grid gap-4 sm:grid-cols-2">
          <div>
            <CampoNumero
              label="Abono recibido"
              valor={abono}
              alCambiar={cambiarAbono}
              min={0}
              placeholder="0.00"
            />
            <p className={`mt-1 text-xs ${abonoExcedeTotal ? 'font-medium text-red-400' : 'text-gray-500'}`}>
              Máximo permitido: {formatearMonto(total, moneda)}
            </p>
            <div className="mt-2 flex flex-wrap gap-2" aria-label="Atajos para definir el abono">
              <button
                type="button"
                className="btn-secundario !px-3 !py-1.5 text-xs"
                onClick={() => cambiarAbono(String(redondearMonto(total * 0.5)))}
              >
                50% del total
              </button>
              <button
                type="button"
                className="btn-secundario !px-3 !py-1.5 text-xs"
                onClick={() => cambiarAbono(String(redondearMonto(total)))}
              >
                Total completo
              </button>
            </div>
            <p className="mt-1 text-xs text-gray-500">
              Puedes usar un atajo o escribir cualquier monto recibido.
            </p>
          </div>
          <div>
            <CampoSelect
              label="Estado del alquiler"
              valor={estado}
              alCambiar={setEstado}
              opciones={OPCIONES_ESTADO.filter((opcion) =>
                !editando || opcion.valor === editando.estado || puedeCambiarEstadoAlquiler(editando.estado, opcion.valor)
              ).filter((opcion) => editando || opcion.valor === 'activo')}
            />
            <p className="mt-1 text-xs leading-4 text-gray-500">
              Indica en qué etapa está el servicio: activo, entregado, devuelto o cancelado. No representa el estado del pago.
            </p>
          </div>
        </div>

        <div className="overflow-hidden rounded-2xl border border-slate-800/70">
          <div className="flex items-center gap-2 bg-slate-800/50 px-4 py-2.5">
            <WalletCards className="h-4 w-4 text-cyan-300" />
            <p className="text-sm font-semibold text-gray-200">Resumen del alquiler</p>
          </div>
          <div className="divide-y divide-slate-800/70 bg-slate-900/50 px-4 py-1 text-sm">
            {modalidad === '12h' && (
              <div className="flex items-center justify-between py-2.5">
                <span className="text-gray-400">Subtotal (24h)</span>
                <span className="font-medium text-gray-300">{formatearMonto(subtotal, moneda)}</span>
              </div>
            )}
            <div className="flex items-center justify-between py-2.5">
              <span className="text-gray-400">Total{modalidad === '12h' ? ' (−50%)' : ''}</span>
              <span className="font-semibold text-white">{formatearMontoDual(total, moneda, tasaBs)}</span>
            </div>
            <div className="flex items-center justify-between py-2.5">
              <span className="text-gray-400">Abono</span>
              <span className="font-medium text-gray-300">{formatearMontoDual(abonoNumero, moneda, tasaBs)}</span>
            </div>
            <div className="flex items-center justify-between py-2.5">
              <span className="text-gray-400">Pendiente</span>
              <span className={`font-bold ${saldo > 0 ? 'text-amber-300' : 'text-emerald-300'}`}>
                {formatearMontoDual(saldo, moneda, tasaBs)}
              </span>
            </div>
          </div>
        </div>

        <CampoTextoArea
          label="Notas"
          valor={notas}
          alCambiar={setNotas}
          placeholder="Condiciones de entrega, observaciones…"
        />

        {error && <p className="text-sm font-medium text-red-400">{error}</p>}

        <div className="sticky bottom-0 -mx-5 mt-2 flex items-center justify-end gap-2 border-t border-slate-800 bg-slate-900/95 px-5 py-3 backdrop-blur">
          <button className="btn-secundario" onClick={alCerrar}>
            Cancelar
          </button>
          <button className="btn-primario min-w-[8.5rem]" onClick={guardar}>
            Guardar alquiler
          </button>
        </div>
      </div>
    </Modal>
  );
}

/* ------------------------------------------------------------------ */
/* Detalle de un alquiler                                              */
/* ------------------------------------------------------------------ */

function DetalleAlquiler({
  alquiler,
  nombreCliente,
  moneda,
  alEditar,
  alEmitirRecibo,
  alEliminar
}: {
  alquiler: Alquiler;
  nombreCliente: string;
  moneda: string;
  alEditar: () => void;
  alEmitirRecibo: () => void;
  alEliminar: () => void;
}) {
  const toldos = useAppStore((s) => s.toldos);
  const tasaBs = useAppStore((s) => s.config.tasaBs);
  const saldo = calcularSaldo(alquiler.montoTotal, alquiler.abono);
  const nombreToldo = (id: string) => toldos.find((t) => t.id === id)?.nombre ?? 'Toldo eliminado';
  const fila = (clave: string, valor: string) => (
    <p className="text-sm">
      <span className="font-medium text-gray-400">{clave}: </span>
      <span className="text-white">{valor}</span>
    </p>
  );

  return (
    <div className="space-y-4">
      <div className="flex items-center gap-2">
        <EtiquetaAlquiler estado={alquiler.estado} />
        <span className="text-sm text-gray-400">
          Creado el {formatearFechaCorta(alquiler.creadoEn)}
        </span>
      </div>

      <div className="space-y-1.5">
        {fila('Cliente', nombreCliente)}
        {fila('Folio', alquiler.folio)}
        {fila('Modalidad', ETIQUETA_MODALIDAD[alquiler.modalidad] ?? '24 horas')}
        {fila('Dirección', alquiler.direccion || '—')}
        {alquiler.lat !== undefined && alquiler.lng !== undefined && (
          <p className="text-sm">
            <span className="font-medium text-gray-400">GPS: </span>
            <a
              className="text-marca-400 underline"
              href={enlaceMapa(alquiler.lat, alquiler.lng)}
              target="_blank"
              rel="noreferrer"
            >
              {formatearCoordenadas(alquiler.lat, alquiler.lng)}
            </a>
          </p>
        )}
      </div>

      <div>
        <p className="label">Toldos</p>
        <ul className="space-y-1">
          {alquiler.items.map((item, i) => (
            <li key={i} className="text-sm text-gray-300">
              {item.cantidad} × {nombreToldo(item.toldoId)} —{' '}
              {formatearMonto(
                item.tarifa * item.cantidad,
                moneda
              )}
              {alquiler.modalidad === '12h' && (
                <span className="text-xs text-marca-400"> (12h)</span>
              )}
            </li>
          ))}
        </ul>
      </div>

      <div className="rounded-lg bg-marca-500/10 px-4 py-3 text-sm">
        <p className="flex justify-between">
          <span className="text-gray-400">Total</span>
          <span className="font-semibold text-white">{formatearMontoDual(alquiler.montoTotal, moneda, tasaBs)}</span>
        </p>
        <p className="flex justify-between">
          <span className="text-gray-400">Abono</span>
          <span className="text-gray-200">{formatearMontoDual(alquiler.abono, moneda, tasaBs)}</span>
        </p>
        <p className="flex justify-between">
          <span className="text-gray-400">Pendiente</span>
          <span className={`font-bold ${saldo > 0 ? 'text-red-400' : 'text-cyan-300'}`}>
            {formatearMontoDual(saldo, moneda, tasaBs)}
          </span>
        </p>
      </div>

      {alquiler.notas && <p className="text-sm text-gray-400">{alquiler.notas}</p>}

      <div className="flex flex-wrap justify-end gap-2">
        <button className="btn-secundario text-red-400" onClick={alEliminar}>
          Eliminar
        </button>
        <button className="btn-secundario" onClick={alEditar}>
          Editar
        </button>
        <button className="btn-primario" onClick={alEmitirRecibo}>
          <Receipt className="h-4 w-4" />
          Emitir recibo
        </button>
      </div>
    </div>
  );
}

/* ------------------------------------------------------------------ */
/* Emisión de recibo                                                   */
/* ------------------------------------------------------------------ */

function EmitirRecibo({
  alquiler,
  alCerrar,
  navegar
}: {
  alquiler: Alquiler;
  alCerrar: () => void;
  navegar: (vista: Vista) => void;
}) {
  const clientes = useAppStore((s) => s.clientes);
  const toldos = useAppStore((s) => s.toldos);
  const moneda = useAppStore((s) => s.config.negocio.moneda);
  const tasaBs = useAppStore((s) => s.config.tasaBs);
  const [tipoMonto, setTipoMonto] = useState<TipoMonto>('saldo');
  const [montoOtro, setMontoOtro] = useState('');
  const [concepto, setConcepto] = useState(CONCEPTOS.saldo);
  const [estadoRecibo, setEstadoRecibo] = useState<EstadoRecibo>('pagado');
  const [error, setError] = useState('');
  const [emitido, setEmitido] = useState<string | null>(null);

  const emitirRecibo = useAppStore((s) => s.emitirRecibo);

  const monto = useMemo(() => {
    switch (tipoMonto) {
      case 'total':
        return alquiler.montoTotal;
      case 'abono':
        return alquiler.abono;
      case 'saldo':
        return calcularSaldo(alquiler.montoTotal, alquiler.abono);
      case 'otro':
        return parseFloat(montoOtro) || 0;
    }
  }, [tipoMonto, montoOtro, alquiler]);

  const cambiarTipo = (tipo: TipoMonto) => {
    setTipoMonto(tipo);
    setConcepto(CONCEPTOS[tipo]);
  };

  const emitir = () => {
    const pendienteActual = calcularSaldo(alquiler.montoTotal, alquiler.abono);
    if (pendienteActual <= 0 && estadoRecibo === 'pagado' && tipoMonto !== 'abono') {
      setError('Este alquiler ya está pagado completamente. Selecciona “Abono ya recibido” para emitir un comprobante.');
      return;
    }
    const montoFinal = redondearMonto(monto);
    const errorPago = validarMontoPago(
      montoFinal,
      alquiler.montoTotal,
      alquiler.abono,
      estadoRecibo === 'pagado' && tipoMonto !== 'abono'
    );
    if (errorPago) {
      setError(errorPago);
      return;
    }
    const config = useAppStore.getState().config;
    const folioPreview = `REC-${String(config.ultimoFolio + 1).padStart(4, '0')}`;
    const cliente = clientes.find((c) => c.id === alquiler.clienteId);
    const datos: DatosRecibo = {
      folio: folioPreview,
      emitidoEn: new Date().toISOString(),
      concepto: concepto.trim() || CONCEPTOS.saldo,
      estado: estadoRecibo,
      monto: montoFinal,
      negocio: { ...config.negocio, tasaBs: config.tasaBs },
      cliente: {
        nombre: cliente?.nombre ?? 'Cliente eliminado',
        cedula: cliente?.cedula ?? '',
        telefono: cliente?.telefono ?? '',
        direccion: cliente?.direccion ?? ''
      },
      alquiler: {
        folio: alquiler.folio,
        items: alquiler.items.map((item) => ({
          nombre: toldos.find((t) => t.id === item.toldoId)?.nombre ?? 'Toldo eliminado',
          cantidad: item.cantidad,
          tarifa: item.tarifa
        })),
        modalidad: alquiler.modalidad,
        fechaInicio: alquiler.fechaInicio,
        fechaFin: alquiler.fechaFin,
        tiempoUso: alquiler.tiempoUso,
        direccion: alquiler.direccion,
        lat: alquiler.lat,
        lng: alquiler.lng,
        montoTotal: alquiler.montoTotal,
        abono: alquiler.abono
      }
    };
    try {
      const folio = emitirRecibo(
        {
          id: generarId(),
          folio: folioPreview,
          alquilerId: alquiler.id,
          emitidoEn: datos.emitidoEn,
          concepto: datos.concepto,
          monto: montoFinal,
          estado: estadoRecibo,
          datos
        },
        estadoRecibo === 'pagado' && tipoMonto !== 'abono'
      );
      setError('');
      setEmitido(folio);
    } catch (error) {
      setError(error instanceof Error ? error.message : 'No se pudo emitir el recibo.');
    }
  };

  const opciones: Array<{ valor: TipoMonto; etiqueta: string }> = [
    { valor: 'saldo', etiqueta: `Pendiente de pago (${formatearMonto(calcularSaldo(alquiler.montoTotal, alquiler.abono), moneda)})` },
    { valor: 'total', etiqueta: `Monto total (${formatearMonto(alquiler.montoTotal, moneda)})` },
    { valor: 'abono', etiqueta: `Abono ya recibido (${formatearMonto(alquiler.abono, moneda)})` },
    { valor: 'otro', etiqueta: 'Otro monto…' }
  ];

  if (emitido) {
    return (
      <Modal titulo={`Recibo ${emitido} emitido`} alCerrar={alCerrar}>
        <div className="space-y-4">
          <div className="rounded-xl border border-cyan-500/30 bg-cyan-500/10 p-4 text-center">
            <CheckCircle2 className="mx-auto h-10 w-10 text-cyan-300" aria-hidden />
            <p className="mt-2 font-semibold text-cyan-300">
              Recibo {emitido} emitido correctamente.
            </p>
            <p className="mt-1 text-sm text-cyan-300">
              Puedes verlo, descargarlo y enviarlo por WhatsApp desde el módulo de Recibos.
            </p>
          </div>
          <div className="flex flex-wrap justify-end gap-2">
            <button className="btn-secundario" onClick={alCerrar}>
              Cerrar
            </button>
            <button
              className="btn-primario"
              onClick={() => {
                alCerrar();
                navegar('recibos');
              }}
            >
              <Receipt className="h-4 w-4" />
              Ir a Recibos
            </button>
          </div>
        </div>
      </Modal>
    );
  }

  return (
    <Modal titulo="Emitir recibo" alCerrar={alCerrar} anchoMaximo="max-w-xl">
      <div className="space-y-5">          <div className="flex items-start justify-between gap-4 rounded-2xl border border-slate-700/70 bg-slate-950/45 px-4 py-3">

          <div className="flex min-w-0 items-center gap-3">
            <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-marca-500/15 text-marca-300">
              <Receipt className="h-5 w-5" aria-hidden />
            </div>
            <div className="min-w-0">
              <p className="truncate font-semibold text-white">{alquiler.folio}</p>
              <p className="truncate text-sm text-gray-400">Recibo para el alquiler seleccionado</p>
            </div>
          </div>
          <div className="shrink-0 text-right">
            <p className="text-[11px] font-medium uppercase tracking-wide text-gray-500">Pendiente actual</p>
            <p className={`text-base font-bold ${calcularSaldo(alquiler.montoTotal, alquiler.abono) > 0 ? 'text-amber-300' : 'text-cyan-300'}`}>
              {formatearMontoDual(calcularSaldo(alquiler.montoTotal, alquiler.abono), moneda, tasaBs)}
            </p>
          </div>
        </div>

        <div>
          <div className="mb-2 flex items-end justify-between gap-3">
            <div>
              <p className="text-sm font-semibold text-white">¿Qué monto deseas cobrar?</p>
              <p className="text-xs text-gray-500">Elige una opción para llenar el recibo automáticamente.</p>
            </div>
            <WalletCards className="h-5 w-5 shrink-0 text-marca-400" aria-hidden />
          </div>
          <div className="grid gap-2 sm:grid-cols-2">
            {opciones.map((opcion) => {
              const activa = tipoMonto === opcion.valor;
              const montoOpcion = opcion.valor === 'total'
                ? alquiler.montoTotal
                : opcion.valor === 'abono'
                  ? alquiler.abono
                  : opcion.valor === 'saldo'
                    ? calcularSaldo(alquiler.montoTotal, alquiler.abono)
                    : null;
              return (
                <button
                  key={opcion.valor}
                  type="button"
                  aria-pressed={activa}
                  onClick={() => cambiarTipo(opcion.valor)}
                  className={`group flex min-h-[72px] items-center gap-3 rounded-xl border px-3 py-3 text-left transition focus:outline-none focus:ring-2 focus:ring-marca-500/40 ${
                    activa
                      ? 'border-marca-400/70 bg-marca-500/15 text-white shadow-sm shadow-marca-500/10'
                      : 'border-slate-700/70 bg-slate-900/70 text-gray-300 hover:border-marca-500/40 hover:bg-slate-800'
                  }`}
                >
                  <span className={`flex h-9 w-9 shrink-0 items-center justify-center rounded-lg ${activa ? 'bg-marca-500/20 text-marca-300' : 'bg-slate-800 text-gray-500 group-hover:text-marca-300'}`}>
                    {opcion.valor === 'saldo' ? <Clock3 className="h-4 w-4" aria-hidden /> : opcion.valor === 'abono' ? <CheckCircle2 className="h-4 w-4" aria-hidden /> : opcion.valor === 'otro' ? <FileText className="h-4 w-4" aria-hidden /> : <Receipt className="h-4 w-4" aria-hidden />}
                  </span>
                  <span className="min-w-0 flex-1">
                    <span className="block text-sm font-medium">{opcion.valor === 'saldo' ? 'Pendiente de pago' : opcion.valor === 'total' ? 'Total del alquiler' : opcion.valor === 'abono' ? 'Abono ya recibido' : 'Otro monto'}</span>
                    <span className="mt-0.5 block truncate text-xs text-gray-500">{montoOpcion === null ? 'Escribe un monto' : formatearMonto(montoOpcion, moneda)}</span>
                  </span>
                  <span className={`h-4 w-4 rounded-full border ${activa ? 'border-marca-300 bg-marca-400 ring-2 ring-marca-400/20' : 'border-slate-600'}`} aria-hidden />
                </button>
              );
            })}
          </div>
        </div>

        {tipoMonto === 'otro' && (
          <CampoNumero
            label="Monto personalizado"
            valor={montoOtro}
            alCambiar={setMontoOtro}
            min={0}
            obligatorio
            placeholder="0.00"
          />
        )}

        <CampoTexto
          label="Concepto del recibo"
          valor={concepto}
          alCambiar={setConcepto}
          placeholder="Pago del alquiler"
        />

        <div>
          <div className="mb-2 flex items-end justify-between gap-3">
            <div>
              <p className="text-sm font-semibold text-white">Estado del pago</p>
              <p className="text-xs text-gray-500">Define si el monto ya fue recibido.</p>
            </div>
            <span className="text-xs text-gray-500">Obligatorio</span>
          </div>
          <div className="grid gap-2 sm:grid-cols-2">
            <button
              type="button"
              aria-pressed={estadoRecibo === 'pagado'}
              onClick={() => setEstadoRecibo('pagado')}
              className={`flex items-start gap-3 rounded-xl border p-3 text-left transition focus:outline-none focus:ring-2 focus:ring-marca-500/40 ${estadoRecibo === 'pagado' ? 'border-cyan-400/60 bg-cyan-400/10' : 'border-slate-700/70 bg-slate-900/70 hover:border-cyan-400/40'}`}
            >
              <CheckCircle2 className={`mt-0.5 h-5 w-5 shrink-0 ${estadoRecibo === 'pagado' ? 'text-cyan-300' : 'text-gray-500'}`} aria-hidden />
              <span>
                <span className="block text-sm font-semibold text-white">Pagado</span>
                <span className="mt-0.5 block text-xs leading-4 text-gray-500">Registra este monto como abono.</span>
              </span>
            </button>
            <button
              type="button"
              aria-pressed={estadoRecibo === 'por_pagar'}
              onClick={() => setEstadoRecibo('por_pagar')}
              className={`flex items-start gap-3 rounded-xl border p-3 text-left transition focus:outline-none focus:ring-2 focus:ring-marca-500/40 ${estadoRecibo === 'por_pagar' ? 'border-amber-400/60 bg-amber-400/10' : 'border-slate-700/70 bg-slate-900/70 hover:border-amber-400/40'}`}
            >
              <Clock3 className={`mt-0.5 h-5 w-5 shrink-0 ${estadoRecibo === 'por_pagar' ? 'text-amber-300' : 'text-gray-500'}`} aria-hidden />
              <span>
                <span className="block text-sm font-semibold text-white">Por pagar</span>
                <span className="mt-0.5 block text-xs leading-4 text-gray-500">Conserva el monto pendiente para cobrarlo después.</span>
              </span>
            </button>
          </div>
        </div>

        <div className="rounded-2xl border border-marca-400/20 bg-gradient-to-br from-marca-500/15 via-slate-900/80 to-slate-950/70 px-4 py-3">
          <div className="flex items-center justify-between gap-4">
            <div>
              <p className="text-xs font-medium uppercase tracking-wide text-marca-200/80">Monto del recibo</p>
              <p className="mt-1 text-xl font-bold text-white">{formatearMontoDual(redondearMonto(monto), moneda, tasaBs)}</p>
            </div>
            <div className={`rounded-full px-2.5 py-1 text-xs font-semibold ${monto <= 0 ? 'bg-slate-800 text-gray-400' : estadoRecibo === 'pagado' ? 'bg-cyan-400/15 text-cyan-300' : 'bg-amber-400/15 text-amber-300'}`}>
              {monto <= 0 ? 'Selecciona un monto' : estadoRecibo === 'pagado' ? 'Se registrará' : 'Quedará pendiente'}
            </div>
          </div>
        </div>

        {calcularSaldo(alquiler.montoTotal, alquiler.abono) <= 0 && (
          <div className="flex items-start gap-2 rounded-xl border border-cyan-400/25 bg-cyan-400/10 px-3 py-2.5 text-xs text-cyan-200" role="status">
            <CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0" aria-hidden />
            <span>Este alquiler está pagado completamente. Para generar un comprobante, selecciona “Abono ya recibido”.</span>
          </div>
        )}
        {monto <= 0 && (
          <div className="flex items-start gap-2 rounded-xl border border-amber-400/25 bg-amber-400/10 px-3 py-2.5 text-xs text-amber-200" role="status">
            <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" aria-hidden />
            <span>No hay un monto disponible para emitir este recibo. Registra primero un abono o usa “Otro monto”.</span>
          </div>
        )}
        {error && <p className="flex items-center gap-2 text-sm font-medium text-red-400" role="alert"><AlertCircle className="h-4 w-4 shrink-0" aria-hidden />{error}</p>}

        <div className="sticky bottom-[-20px] z-10 -mx-5 -mb-5 flex flex-col-reverse gap-2 border-t border-slate-800 bg-slate-900/95 px-5 py-4 backdrop-blur sm:flex-row sm:justify-end">
          <button type="button" className="btn-secundario" onClick={alCerrar}>Cancelar</button>
          <button type="button" className="btn-primario" onClick={emitir} disabled={monto <= 0}>
            <Receipt className="h-4 w-4" aria-hidden />
            Confirmar y emitir
          </button>
        </div>
      </div>
    </Modal>
  );
}
