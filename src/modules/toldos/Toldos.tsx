import { useState } from 'react';
import { useAppStore } from '../../data/store';
import { generarId } from '../../lib/ids';
import { formatearMontoDual } from '../../lib/formato';
import type { EstadoToldo, Toldo } from '../../types/modelos';
import { Modal } from '../../components/Modal';
import { CampoNumero, CampoSelect, CampoTexto, CampoTextoArea } from '../../components/Campos';
import { EtiquetaToldo } from '../../components/Etiqueta';
import { Plus } from 'lucide-react';
import { unidadesOcupadas } from '../../lib/validaciones';
import { capitalizarPalabras } from '../../lib/venezuela';

const OPCIONES_ESTADO: Array<{ valor: EstadoToldo; etiqueta: string }> = [
  { valor: 'disponible', etiqueta: 'Disponible' },
  { valor: 'alquilado', etiqueta: 'Alquilado' },
  { valor: 'en_reparacion', etiqueta: 'En reparación' },
  { valor: 'retirado', etiqueta: 'Retirado' }
];

/** Módulo de inventario de toldos. */
export function Toldos() {
  const toldos = useAppStore((s) => s.toldos);
  const alquileres = useAppStore((s) => s.alquileres);
  const agregarToldo = useAppStore((s) => s.agregarToldo);
  const actualizarToldo = useAppStore((s) => s.actualizarToldo);
  const eliminarToldo = useAppStore((s) => s.eliminarToldo);
  const moneda = useAppStore((s) => s.config.negocio.moneda);
  const tasaBs = useAppStore((s) => s.config.tasaBs);

  const [modalAbierto, setModalAbierto] = useState(false);
  const [editando, setEditando] = useState<Toldo | null>(null);
  const [error, setError] = useState('');
  const [confirmarEliminar, setConfirmarEliminar] = useState<Toldo | null>(null);

  const [nombre, setNombre] = useState('');
  const [tamano, setTamano] = useState('');
  const [tarifa, setTarifa] = useState('');
  const [tarifa12h, setTarifa12h] = useState('');
  const [unidades, setUnidades] = useState('1');
  const [estado, setEstado] = useState<EstadoToldo>('disponible');

  const unidadesOcupadasActuales = editando ? unidadesOcupadas(editando.id, alquileres) : 0;
  const estadoAutomatico = unidadesOcupadasActuales > 0;
  const [notas, setNotas] = useState('');

  const abrirNuevo = () => {
    setEditando(null);
    setNombre('');
    setTamano('');
    setTarifa('');
    setTarifa12h('');
    setUnidades('1');
    setEstado('disponible');
    setNotas('');
    setError('');
    setModalAbierto(true);
  };

  const abrirEditar = (toldo: Toldo) => {
    setEditando(toldo);
    setNombre(toldo.nombre);
    setTamano(toldo.tamano);
    setTarifa(String(toldo.tarifa));
    setTarifa12h(String(toldo.tarifa12h ?? toldo.tarifa * 0.5));
    setUnidades(String(toldo.unidades || 1));
    setEstado(toldo.estado);
    setNotas(toldo.notas);
    setError('');
    setModalAbierto(true);
  };

  const guardar = () => {
    if (!nombre.trim()) {
      setError('El nombre es obligatorio.');
      return;
    }
    const tarifaNumero = parseFloat(tarifa.replace(',', '.'));
    const tarifa12hNumero = parseFloat(tarifa12h.replace(',', '.'));
    if (!Number.isFinite(tarifaNumero) || tarifaNumero < 0 || !Number.isFinite(tarifa12hNumero) || tarifa12hNumero < 0) {
      setError('Indica precios válidos para 12 y 24 horas.');
      return;
    }
    const unidadesNumero = Number.parseInt(unidades, 10);
    if (!Number.isInteger(unidadesNumero) || unidadesNumero < 1) {
      setError('Las unidades deben ser un número entero mayor que 0.');
      return;
    }
    try {
      if (editando) {
        actualizarToldo({
          ...editando,
          nombre: capitalizarPalabras(nombre),
          tamano: tamano.trim(),
          tarifa: tarifaNumero,
          tarifa12h: tarifa12hNumero,
          unidades: unidadesNumero,
          estado,
          notas: notas.trim()
        });
      } else {
        agregarToldo({
          id: generarId(),
          nombre: capitalizarPalabras(nombre),
          tamano: tamano.trim(),
          tarifa: tarifaNumero,
          tarifa12h: tarifa12hNumero,
          unidades: unidadesNumero,
          estado,
          notas: notas.trim(),
          creadoEn: new Date().toISOString()
        });
      }
      setError('');
      setModalAbierto(false);
    } catch (error) {
      setError(error instanceof Error ? error.message : 'No se pudo guardar el toldo.');
    }
  };

  const eliminar = (toldo: Toldo) => {
    try {
      eliminarToldo(toldo.id);
      setError('');
      setConfirmarEliminar(null);
    } catch (error) {
      setError(error instanceof Error ? error.message : 'No se pudo eliminar el toldo.');
    }
  };

  const opcionesEstado = OPCIONES_ESTADO.filter((opcion) => {
    if (opcion.valor === 'alquilado') return estadoAutomatico;
    if (estadoAutomatico) return opcion.valor === estado;
    return true;
  });

  return (
    <div className="space-y-4">
      {error && !modalAbierto && (
        <div className="rounded-xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-300" role="alert">
          {error}
        </div>
      )}
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="titulo-pagina">Toldos</h1>
          <p className="text-sm text-gray-400">
            {toldos.length} toldo{toldos.length === 1 ? '' : 's'} en el inventario.
          </p>
        </div>
        <button className="btn-primario" onClick={abrirNuevo}>
          <Plus className="h-4 w-4" />
          Nuevo toldo
        </button>
      </div>          {toldos.length === 0 ? (
        <div className="tarjeta text-sm text-gray-400">
          Aún no hay toldos. Pulsa "+ Nuevo toldo" para registrar el primero.
        </div>
      ) : (
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {toldos.map((toldo) => (
            <div key={toldo.id} className="tarjeta flex flex-col gap-2">
              <div className="flex items-start justify-between gap-2">
                <div>
                  <p className="font-semibold text-white">{capitalizarPalabras(toldo.nombre)}</p>
                  {toldo.tamano && <p className="text-xs text-gray-400">{capitalizarPalabras(toldo.tamano)}</p>}
                  <p className="text-xs text-gray-400">{toldo.unidades || 1} Unidad{(toldo.unidades || 1) === 1 ? '' : 'es'}</p>
                </div>
                <EtiquetaToldo estado={toldo.estado} />
              </div>
              <p className="text-lg font-bold text-marca-400">                  {formatearMontoDual(toldo.tarifa, moneda, tasaBs)}
                <span className="text-xs font-normal text-gray-400"> / 24h</span>
              </p>
              <p className="text-sm font-semibold text-sky-300">
                {formatearMontoDual(toldo.tarifa12h ?? toldo.tarifa * 0.5, moneda, tasaBs)}
                <span className="text-xs font-normal text-gray-400"> / 12h</span>

              </p>
              {toldo.notas && <p className="text-xs text-gray-400">{toldo.notas}</p>}
              <div className="mt-auto flex gap-2 pt-1">
                <button className="btn-secundario !px-3 !py-1.5" onClick={() => abrirEditar(toldo)}>
                  Editar
                </button>
                <button
                  className="btn-secundario !px-3 !py-1.5 text-red-400 hover:bg-red-500/10"
                  onClick={() => setConfirmarEliminar(toldo)}
                >
                  Eliminar
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {confirmarEliminar && (
        <Modal titulo="Eliminar toldo" alCerrar={() => setConfirmarEliminar(null)}>
          <div className="space-y-4">
            <p className="text-sm text-gray-300">¿Eliminar <strong className="text-white">{confirmarEliminar.nombre}</strong>? Los alquileres históricos se conservarán.</p>
            <div className="flex justify-end gap-2">
              <button className="btn-secundario" onClick={() => setConfirmarEliminar(null)}>Cancelar</button>
              <button className="btn-peligro" onClick={() => eliminar(confirmarEliminar)}>Eliminar toldo</button>
            </div>
          </div>
        </Modal>
      )}

      {modalAbierto && (
        <Modal titulo={editando ? 'Editar toldo' : 'Nuevo toldo'} alCerrar={() => setModalAbierto(false)}>
          <div className="space-y-4">
            <CampoTexto
              label="Nombre"
              valor={nombre}
              alCambiar={setNombre}
              obligatorio
              placeholder="Ej.: Toldo blanco 4x4"
            />
            <div className="grid gap-4 sm:grid-cols-2">
              <CampoTexto
                label="Tamaño"
                valor={tamano}
                alCambiar={setTamano}
                placeholder="Ej.: 4x4 m"
              />
              <CampoNumero
                label="Precio 24 horas"
                valor={tarifa}
                alCambiar={setTarifa}
                obligatorio
                min={0}
                placeholder="0.00"
              />
              <CampoNumero
                label="Precio 12 horas"
                valor={tarifa12h}
                alCambiar={setTarifa12h}
                obligatorio
                min={0}
                placeholder="0.00"
              />
              <CampoNumero
                label="Unidades"
                valor={unidades}
                alCambiar={setUnidades}
                obligatorio
                paso="1"
                min={1}
                placeholder="1"
              />
            </div>
            {estadoAutomatico ? (
              <div className="rounded-xl border border-cyan-400/20 bg-cyan-400/5 px-3 py-2.5">
                <p className="label">Estado</p>
                <div className="flex items-center justify-between gap-3">
                  <span className="text-sm font-medium text-cyan-200">Alquilado</span>
                  <span className="text-xs text-gray-400">{unidadesOcupadasActuales} Unidad{unidadesOcupadasActuales === 1 ? '' : 'es'} en Pedidos Activos</span>
                </div>
                <p className="mt-1 text-xs text-gray-400">Se actualiza automáticamente mientras exista un alquiler activo o entregado.</p>
              </div>
            ) : (
              <CampoSelect
                label="Estado"
                valor={estado}
                alCambiar={setEstado}
                opciones={opcionesEstado}
              />
            )}
            <CampoTextoArea
              label="Notas"
              valor={notas}
              alCambiar={setNotas}
              placeholder="Material, condiciones, observaciones…"
            />
            {error && <p className="text-sm font-medium text-red-400">{error}</p>}
            <div className="flex justify-end gap-2">
              <button className="btn-secundario" onClick={() => setModalAbierto(false)}>
                Cancelar
              </button>
              <button className="btn-primario" onClick={guardar}>
                Guardar
              </button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
}
