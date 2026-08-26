import type { ReactNode } from 'react';
import { X } from 'lucide-react';

interface ModalProps {
  titulo: string;
  alCerrar: () => void;
  children: ReactNode;
  anchoMaximo?: string;
}

export function Modal({ titulo, alCerrar, children, anchoMaximo = 'max-w-2xl' }: ModalProps) {
  return (
    <div
      className="anim-fondo fixed inset-0 z-50 flex items-end justify-center bg-black/50 backdrop-blur-[2px] sm:items-center sm:p-4"
      onClick={alCerrar}
    >
      <div
        className={`anim-modal w-full ${anchoMaximo} max-h-[92vh] overflow-y-auto rounded-t-2xl bg-slate-900 shadow-2xl sm:rounded-2xl`}
        onClick={(e) => e.stopPropagation()}
      >
        <div className="h-1 w-full bg-gradient-to-r from-marca-500 via-sky-500 to-cyan-400" />
        <div className="sticky top-0 z-10 flex items-center justify-between border-b border-slate-800 bg-slate-900 px-5 py-3">
          <h2 className="text-lg font-semibold text-white">{titulo}</h2>
          <button
            className="btn-secundario !px-2 !py-1"
            onClick={alCerrar}
            aria-label="Cerrar"
          >
            <X className="h-4 w-4" />
          </button>
        </div>
        <div className="p-5">{children}</div>
      </div>
    </div>
  );
}
