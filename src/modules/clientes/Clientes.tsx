import { useMemo, useState } from 'react';
import { useAppStore } from '../../data/store';
import { generarId } from '../../lib/ids';
import { formatearCedulaVenezolana, formatearTelefonoVenezolano } from '../../lib/venezuela';
import type { Cliente } from '../../types/modelos';
import { Modal } from '../../components/Modal';
import {
  CampoTexto,
  CampoTextoArea
} from '../../components/Campos';
import { Plus } from 'lucide-react';

function capitalizarPalabras(valor: string): string {
  return valor.replace(/(^|[\s-])([\p{L}])/gu, (_, separador: string, letra: string) => `${separador}${letra.toLocaleUpperCase('es-VE')}`);
}

/** Módulo de gestión de clientes. */
export function Clientes() {
  const clientes = useAppStore((s) => s.clientes);
  const alquileres = useAppStore((s) => s.alquileres);
  const agregarCliente = useAppStore((s) => s.agregarCliente);
  const actualizarCliente = useAppStore((s) => s.actualizarCliente);
  const eliminarCliente = useAppStore((s) => s.eliminarCliente);

  const [busqueda, setBusqueda] = useState('');
  const [modalAbierto, setModalAbierto] = useState(false);
  const [editando, setEditando] = useState<Cliente | null>(null);
  const [error, setError] = useState('');

  const [nombre, setNombre] = useState('');
  const [cedula, setCedula] = useState('');
  const [telefono, setTelefono] = useState('');
  const [direccion, setDireccion] = useState('');
  const [notas, setNotas] = useState('');

  const abrirNuevo = () => {
    setEditando(null);
    setNombre('');
    setCedula('');
    setTelefono('');
    setDireccion('');
    setNotas('');
    setError('');
    setModalAbierto(true);
  };

  const abrirEditar = (cliente: Cliente) => {
    setEditando(cliente);
    setNombre(cliente.nombre);
    setCedula(cliente.cedula);
    setTelefono(cliente.telefono);
    setDireccion(cliente.direccion);
    setNotas(cliente.notas);
    setError('');
    setModalAbierto(true);
  };

  const guardar = () => {
    if (!nombre.trim()) {
      setError('El nombre es obligatorio.');
      return;
    }
    try {
      if (editando) {
        actualizarCliente({
          ...editando,
          nombre: nombre.trim(),
          cedula: formatearCedulaVenezolana(cedula),
          telefono: formatearTelefonoVenezolano(telefono),
          email: editando.email,
          direccion: direccion.trim(),
          notas: notas.trim()
        });
      } else {
        agregarCliente({
          id: generarId(),
          nombre: nombre.trim(),
          cedula: formatearCedulaVenezolana(cedula),
          telefono: formatearTelefonoVenezolano(telefono),
          email: '',
          direccion: direccion.trim(),
          notas: notas.trim(),
          creadoEn: new Date().toISOString()
        });
      }
      setError('');
      setModalAbierto(false);
    } catch (error) {
      setError(error instanceof Error ? error.message : 'No se pudo guardar el cliente.');
    }
  };

  const eliminar = (cliente: Cliente) => {
    const tieneAlquileres = alquileres.some((a) => a.clienteId === cliente.id);
    const aviso = tieneAlquileres
      ? `"${cliente.nombre}" tiene alquileres registrados. ¿Eliminarlo de todos modos? (Los alquileres se conservarán sin el cliente.)`
      : `¿Eliminar a "${cliente.nombre}"?`;
    if (window.confirm(aviso)) {
      try {
        eliminarCliente(cliente.id);
        setError('');
      } catch (error) {
        setError(error instanceof Error ? error.message : 'No se pudo eliminar el cliente.');
      }
    }
  };

  // Memoizado: evita re-filtrar todo el listado en cada render (patrón de Bitacora).
  const filtrados = useMemo(() => {
    const termino = busqueda.toLowerCase();
    return clientes.filter((c) =>
      `${c.nombre} ${c.cedula} ${c.telefono}`
        .toLowerCase()
        .includes(termino)
    );
  }, [clientes, busqueda]);

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="titulo-pagina">Clientes</h1>
          <p className="text-sm text-gray-400">
            {clientes.length} cliente{clientes.length === 1 ? '' : 's'} registrado
            {clientes.length === 1 ? '' : 's'}.
          </p>
        </div>
        <button className="btn-primario" onClick={abrirNuevo}>
          <Plus className="h-4 w-4" />
          Nuevo cliente
        </button>
      </div>

      <input
        className="input max-w-md"
        type="search"
        placeholder="Buscar por nombre, cédula o teléfono…"
        value={busqueda}
        onChange={(e) => setBusqueda(e.target.value)}
      />

      {error && !modalAbierto && (
        <div className="rounded-xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-300" role="alert">
          {error}
        </div>
      )}

      {filtrados.length === 0 ? (
        <div className="tarjeta text-sm text-gray-400">
          {clientes.length === 0
            ? 'Aún no hay clientes. Pulsa "+ Nuevo cliente" para registrar el primero.'
            : 'No se encontraron clientes con esa búsqueda.'}
        </div>
      ) : (
        <ul className="space-y-2">
          {filtrados.map((cliente) => (
            <li key={cliente.id} className="tarjeta flex items-center justify-between gap-3">
              <div className="min-w-0">
                <p className="font-medium text-white">{capitalizarPalabras(cliente.nombre)}</p>
                <p className="truncate text-xs text-gray-400">
                  {[cliente.cedula && `Cédula/RIF: ${cliente.cedula}`, cliente.telefono && `Tel: ${cliente.telefono}`]
                    .filter(Boolean)
                    .join(' · ')}
                </p>
                {cliente.direccion && (
                  <p className="truncate text-xs text-gray-400">{capitalizarPalabras(cliente.direccion)}</p>
                )}
              </div>
              <div className="flex shrink-0 gap-2">
                <button className="btn-secundario !px-3 !py-1.5" onClick={() => abrirEditar(cliente)}>
                  Editar
                </button>
                <button
                  className="btn-secundario !px-3 !py-1.5 text-red-400 hover:bg-red-500/10"
                  onClick={() => eliminar(cliente)}
                >
                  Eliminar
                </button>
              </div>
            </li>
          ))}
        </ul>
      )}

      {modalAbierto && (
        <Modal
          titulo={editando ? 'Editar cliente' : 'Nuevo cliente'}
          alCerrar={() => setModalAbierto(false)}
        >
          <div className="space-y-4">
            <CampoTexto
              label="Nombre"
              valor={nombre}
              alCambiar={setNombre}
              obligatorio
              placeholder="Ej.: María Pérez"
            />
            <div className="grid gap-4 sm:grid-cols-2">
              <CampoTexto
                label="Cédula / RIF"
                valor={cedula}
                alCambiar={setCedula}
                placeholder="Ej.: V-12.345.678"
              />
              <CampoTexto
                label="Teléfono"
                valor={telefono}
                alCambiar={setTelefono}
                tipo="tel"
                placeholder="Ej.: 0412-1234567"
              />
            </div>
            <CampoTexto
              label="Dirección"
              valor={direccion}
              alCambiar={setDireccion}
              placeholder="Dirección de residencia"
            />
            <CampoTextoArea
              label="Notas"
              valor={notas}
              alCambiar={setNotas}
              placeholder="Referencias, observaciones…"
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
