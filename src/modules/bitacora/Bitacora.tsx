import { useMemo, useState } from 'react';
import { ClipboardCheck, Search } from 'lucide-react';
import { useAppStore } from '../../data/store';
import { formatearFechaHora } from '../../lib/formato';
import type { BitacoraEntrada } from '../../types/modelos';
import { SelectFiltro } from '../../components/Campos';

const TIPOS = [
  { valor: 'todos', etiqueta: 'Todos los tipos' },
  { valor: 'Nuevo', etiqueta: 'Nuevos' },
  { valor: 'Cambio', etiqueta: 'Cambios' },
  { valor: 'Corrección', etiqueta: 'Correcciones' }
] as const;

function colorTipo(tipo: BitacoraEntrada['tipo']): string {
  if (tipo === 'Nuevo') return 'bg-cyan-500/15 text-cyan-300';
  if (tipo === 'Corrección') return 'bg-amber-500/15 text-amber-300';
  return 'bg-blue-500/15 text-blue-300';
}

export function Bitacora() {
  const bitacora = useAppStore((state) => state.bitacora);
  const [busqueda, setBusqueda] = useState('');
  const [tipo, setTipo] = useState<(typeof TIPOS)[number]['valor']>('todos');

  const filtradas = useMemo(() => {
    const termino = busqueda.trim().toLowerCase();
    return bitacora.filter((entrada) => {
      const coincideTipo = tipo === 'todos' || entrada.tipo === tipo;
      const texto = `${entrada.entidad} ${entrada.descripcion}`.toLowerCase();
      return coincideTipo && (!termino || texto.includes(termino));
    });
  }, [bitacora, busqueda, tipo]);

  return (
    <div className="space-y-4">
      <div>
        <h1 className="titulo-pagina">Bitácora</h1>
        <p className="text-sm text-gray-400">
          Historial auditable de cambios, reparaciones y operaciones del sistema.
        </p>
      </div>

      <div className="flex flex-wrap gap-2">
        <label className="relative min-w-[220px] flex-1 sm:max-w-md">
          <span className="sr-only">Buscar en la bitácora</span>
          <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-500" />
          <input
            className="input pl-9"
            type="search"
            placeholder="Buscar entidad o descripción…"
            value={busqueda}
            onChange={(event) => setBusqueda(event.target.value)}
          />
        </label>
        <div className="w-48 shrink-0">
          <SelectFiltro
            valor={tipo}
            alCambiar={setTipo}
            opciones={[...TIPOS]}
          />
        </div>
      </div>

      {filtradas.length === 0 ? (
        <div className="tarjeta flex flex-col items-center gap-2 py-12 text-center text-sm text-gray-400">
          <ClipboardCheck className="h-9 w-9 text-marca-400" aria-hidden />
          <p className="font-medium text-gray-200">
            {bitacora.length === 0 ? 'Aún no hay movimientos' : 'No hay coincidencias'}
          </p>
          <p>
            {bitacora.length === 0
              ? 'Las altas, ediciones, reparaciones, pagos y respaldos aparecerán aquí.'
              : 'Prueba con otro tipo o término de búsqueda.'}
          </p>
        </div>
      ) : (
        <ol className="space-y-2">
          {filtradas.map((entrada) => (
            <li key={entrada.id} className="tarjeta flex gap-3">
              <span className={`mt-0.5 inline-flex h-fit shrink-0 rounded-full px-2.5 py-1 text-xs font-semibold ${colorTipo(entrada.tipo)}`}>
                {entrada.tipo}
              </span>
              <div className="min-w-0">
                <p className="font-medium text-white">{entrada.entidad}</p>
                <p className="text-sm text-gray-300">{entrada.descripcion}</p>
                <time className="text-xs text-gray-500" dateTime={entrada.fecha}>
                  {formatearFechaHora(entrada.fecha)}
                </time>
              </div>
            </li>
          ))}
        </ol>
      )}
    </div>
  );
}
