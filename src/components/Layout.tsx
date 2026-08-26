import type { ReactNode } from 'react';
import { LayoutDashboard, Users, Tent, ClipboardList, Receipt, Settings } from 'lucide-react';

export type Vista = 'panel' | 'clientes' | 'toldos' | 'alquileres' | 'recibos' | 'configuracion';

const NAVEGACION: Array<{
  vista: Vista;
  etiqueta: string;
  icono: React.ComponentType<{ className?: string }>;
}> = [
  { vista: 'panel', etiqueta: 'Panel', icono: LayoutDashboard },
  { vista: 'clientes', etiqueta: 'Clientes', icono: Users },
  { vista: 'toldos', etiqueta: 'Toldos', icono: Tent },
  { vista: 'alquileres', etiqueta: 'Alquileres', icono: ClipboardList },
  { vista: 'recibos', etiqueta: 'Recibos', icono: Receipt },
  { vista: 'configuracion', etiqueta: 'Configuración', icono: Settings }
];

interface LayoutProps {
  vista: Vista;
  alCambiarVista: (vista: Vista) => void;
  children: ReactNode;
}

/** Marca oficial del sistema. El launcher de Android conserva su icono independiente. */
function Marca() {
  return (
    <img
      src="/marca-elspot.png"
      alt="EL SPOT TOLDOS"
      className="h-24 w-full max-w-[190px] object-contain object-center"
    />
  );
}

export function Layout({ vista, alCambiarVista, children }: LayoutProps) {
  return (
    <div className="flex min-h-screen bg-[#0b1018]">
      {/* Barra lateral (escritorio) */}
      <aside className="fixed inset-y-0 hidden w-64 flex-col border-r border-white/5 bg-gradient-to-b from-[#0d1724] via-[#0a1018] to-black text-white shadow-2xl md:flex">
        <div className="flex h-32 items-center justify-center border-b border-white/5 px-4 py-4">
          <Marca />
        </div>
        <nav className="flex-1 space-y-1 p-3">
          {NAVEGACION.map((item) => (
            <button
              key={item.vista}
              onClick={() => alCambiarVista(item.vista)}
              className={`flex w-full items-center gap-3 rounded-xl px-4 py-2.5 text-sm font-medium transition ${
                vista === item.vista
                  ? 'bg-white/10 text-white shadow-sm ring-1 ring-white/10'
                  : 'text-gray-400 hover:bg-white/5 hover:text-white'
              }`}
            >
              <item.icono className="h-[18px] w-[18px] shrink-0" aria-hidden />
              {item.etiqueta}
            </button>
          ))}
        </nav>
        <div className="border-t border-white/5 px-5 py-4">
          <p className="text-[10px] uppercase tracking-[0.25em] text-gray-500">
            Alquiler de toldos
          </p>
        </div>
      </aside>

      {/* Contenido */}
      <div className="flex min-w-0 flex-1 flex-col md:ml-64">
        <header className="flex h-20 items-center justify-center border-b border-white/5 bg-[#0a1018] px-4 py-3 text-white shadow-md md:hidden">
          <img
            src="/marca-elspot.png"
            alt="EL SPOT TOLDOS"
            className="h-16 w-full max-w-[150px] object-contain object-center"
          />
        </header>
        <main className="flex-1 p-4 pb-24 md:p-6 md:pb-6">{children}</main>
      </div>

      {/* Navegación inferior (móvil) */}
      <nav className="fixed inset-x-0 bottom-0 z-40 flex overflow-x-auto border-t border-slate-800 bg-slate-950/95 shadow-[0_-4px_16px_rgba(0,0,0,0.4)] backdrop-blur md:hidden">
        {NAVEGACION.map((item) => (
          <button
            key={item.vista}
            onClick={() => alCambiarVista(item.vista)}
            className={`min-w-[78px] flex flex-1 flex-col items-center gap-1 py-2 text-[11px] font-medium transition ${
              vista === item.vista ? 'text-marca-400' : 'text-gray-500 hover:text-gray-300'
            }`}
          >
            <span
              className={`flex h-8 w-10 items-center justify-center rounded-full transition ${
                vista === item.vista
                  ? 'bg-gradient-to-br from-marca-500 to-sky-500 text-white shadow-md shadow-marca-500/30'
                  : ''
              }`}
            >
              <item.icono className="h-4 w-4" aria-hidden />
            </span>
            {item.etiqueta}
          </button>
        ))}
      </nav>
    </div>
  );
}