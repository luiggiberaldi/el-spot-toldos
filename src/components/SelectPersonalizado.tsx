import { useState, useRef, useEffect } from 'react';
import { ChevronDown, Check } from 'lucide-react';

/**
 * Select de diseño personalizado.
 * Reemplaza al desplegable nativo del navegador (cuadrado y desincronizado con el diseño)
 * por un combobox estilizado con dropdown redondeado, sombra y soporte básico de teclado.
 */
interface Opcion<T extends string> {
  valor: T;
  etiqueta: string;
  /** Icono lucide opcional mostrado junto a la etiqueta. */
  Icono?: React.ComponentType<{ className?: string }>;
}

interface SelectPersonalizadoProps<T extends string> {
  label?: string;
  valor: T;
  alCambiar: (valor: T) => void;
  opciones: Array<Opcion<T>>;
  obligatorio?: boolean;
  placeholder?: string;
}

export function SelectPersonalizado<T extends string>({
  label,
  valor,
  alCambiar,
  opciones,
  obligatorio,
  placeholder = 'Selecciona una opción'
}: SelectPersonalizadoProps<T>) {
  const [abierto, setAbierto] = useState(false);
  const contenedorRef = useRef<HTMLDivElement>(null);

  // Cierra el dropdown al hacer clic fuera o pulsar Escape.
  useEffect(() => {
    if (!abierto) return;
    const manejarClic = (e: MouseEvent) => {
      if (contenedorRef.current && !contenedorRef.current.contains(e.target as Node)) {
        setAbierto(false);
      }
    };
    const manejarTecla = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setAbierto(false);
    };
    document.addEventListener('mousedown', manejarClic);
    document.addEventListener('keydown', manejarTecla);
    return () => {
      document.removeEventListener('mousedown', manejarClic);
      document.removeEventListener('keydown', manejarTecla);
    };
  }, [abierto]);

  const seleccionada = opciones.find((o) => o.valor === valor);

  const elegir = (v: T) => {
    alCambiar(v);
    setAbierto(false);
  };

  return (
    <div className={label ? undefined : ''} ref={contenedorRef}>
      {label && (
        <label className="mb-1 block text-sm font-medium text-gray-300">
          {label}
          {obligatorio && <span className="text-red-400"> *</span>}
        </label>
      )}
      <div className="relative">
        <button
          type="button"
          onClick={() => setAbierto((v) => !v)}
          aria-haspopup="listbox"
          aria-expanded={abierto}
          className="input flex items-center justify-between gap-2 text-left"
        >
          <span className="flex items-center gap-2 overflow-hidden">
            {seleccionada?.Icono && <seleccionada.Icono className="h-4 w-4 shrink-0 text-marca-300" />}
            <span className="truncate">{seleccionada ? seleccionada.etiqueta : placeholder}</span>
          </span>
          <ChevronDown
            className={`h-4 w-4 shrink-0 text-gray-500 transition-transform ${
              abierto ? 'rotate-180' : ''
            }`}
          />
        </button>

        {abierto && (
          <ul
            role="listbox"
            className="anim-dropdown absolute z-50 mt-1 max-h-60 w-full overflow-auto rounded-xl border border-slate-700 bg-slate-900 py-1 shadow-xl shadow-black/50"
          >
            {opciones.map((opcion) => {
              const activa = opcion.valor === valor;
              return (
                <li key={opcion.valor} role="option" aria-selected={activa}>
                  <button
                    type="button"
                    onClick={() => elegir(opcion.valor)}
                    className={`flex w-full items-center gap-2 px-3 py-2 text-sm transition ${
                      activa ? 'bg-sky-500/15 text-sky-300' : 'text-gray-300 hover:bg-slate-800'
                    }`}
                  >
                    {opcion.Icono && <opcion.Icono className="h-4 w-4 shrink-0 text-gray-500" />}
                    <span className="flex-1 truncate text-left">{opcion.etiqueta}</span>
                    {activa && <Check className="h-4 w-4 shrink-0 text-marca-300" />}
                  </button>
                </li>
              );
            })}
          </ul>
        )}
      </div>
    </div>
  );
}
