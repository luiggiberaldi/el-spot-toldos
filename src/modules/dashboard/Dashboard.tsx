import { useEffect, useState } from 'react';
import { useAppStore } from '../../data/store';
import { calcularSaldo } from '../../lib/calculos';
import { formatearBsEquivalente, formatearMonto, formatearMontoDual } from '../../lib/formato';
import { unidadesDisponibles } from '../../lib/validaciones';
import type { Vista } from '../../components/Layout';
import { EtiquetaAlquiler } from '../../components/Etiqueta';
import { ClipboardList, Coins, Hourglass, Tent, Plus, ChevronRight, Save, CheckCircle2, AlertCircle } from 'lucide-react';
import type { LucideIcon } from 'lucide-react';

/** Panel de control: resumen de alquileres, ingresos y disponibilidad. */
export function Dashboard({ navegar }: { navegar: (vista: Vista) => void }) {
  const clientes = useAppStore((s) => s.clientes);
  const toldos = useAppStore((s) => s.toldos);
  const alquileres = useAppStore((s) => s.alquileres);
  const recibos = useAppStore((s) => s.recibos);
  const moneda = useAppStore((s) => s.config.negocio.moneda);
  const config = useAppStore((s) => s.config);
  const tasaBs = config.tasaBs;
  const actualizarConfig = useAppStore((s) => s.actualizarConfig);
  const [tasaRapida, setTasaRapida] = useState(tasaBs ? String(tasaBs) : '');
  const [tasaGuardada, setTasaGuardada] = useState(false);
  const [errorTasa, setErrorTasa] = useState('');

  useEffect(() => {
    setTasaRapida(tasaBs ? String(tasaBs) : '');
  }, [tasaBs]);

  const guardarTasa = () => {
    const valor = Number.parseFloat(tasaRapida.replace(',', '.'));
    if (!Number.isFinite(valor) || valor < 0) {
      setErrorTasa('Indica una tasa válida mayor o igual a 0.');
      setTasaGuardada(false);
      return;
    }
    try {
      actualizarConfig({ ...config, tasaBs: valor });
      setErrorTasa('');
      setTasaGuardada(true);
      window.setTimeout(() => setTasaGuardada(false), 2500);
    } catch (error) {
      setErrorTasa(error instanceof Error ? error.message : 'No se pudo guardar la tasa.');
      setTasaGuardada(false);
    }
  };

  const activos = alquileres.filter((a) => a.estado === 'activo');
  const hoy = new Date();
  const ingresosMes = recibos
    .filter((r) => {
      const fecha = new Date(r.emitidoEn);
      return fecha.getMonth() === hoy.getMonth() && fecha.getFullYear() === hoy.getFullYear();
    })
    .reduce((acc, r) => acc + r.monto, 0);
  const pendiente = alquileres
    .filter((a) => a.estado === 'activo' || a.estado === 'entregado')
    .reduce((acc, a) => acc + calcularSaldo(a.montoTotal, a.abono), 0);
  const disponibles = toldos.reduce((total, toldo) => total + unidadesDisponibles(toldo, alquileres), 0);
  const capacidadTotal = toldos.reduce((total, toldo) => total + Math.max(1, toldo.unidades || 1), 0);

  const nombreCliente = (id: string) =>
    clientes.find((c) => c.id === id)?.nombre ?? 'Cliente eliminado';

  const recientes = [...alquileres]
    .sort((a, b) => b.creadoEn.localeCompare(a.creadoEn))
    .slice(0, 5);

  const tarjetas: Array<{
    etiqueta: string;
    valor: string;
    /** Equivalente en Bs (solo tarjetas de dinero). */
    equivalente?: string;
    icono: LucideIcon;
    tono: string;
  }> = [
    { etiqueta: 'Alquileres activos', valor: String(activos.length), icono: ClipboardList, tono: 'bg-blue-500/15 text-blue-400' },
    { etiqueta: 'Ingresos del mes', valor: formatearMonto(ingresosMes, moneda), equivalente: formatearBsEquivalente(ingresosMes, tasaBs), icono: Coins, tono: 'bg-cyan-500/15 text-cyan-300' },
    { etiqueta: 'Pendiente de cobro', valor: formatearMonto(pendiente, moneda), equivalente: formatearBsEquivalente(pendiente, tasaBs), icono: Hourglass, tono: 'bg-amber-500/15 text-amber-400' },
    { etiqueta: 'Unidades disponibles', valor: `${disponibles} / ${capacidadTotal}`, icono: Tent, tono: 'bg-marca-500/15 text-marca-400' }
  ];

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="titulo-pagina">Panel de control</h1>
          <p className="text-sm text-slate-400">Resumen de tu negocio de alquiler de toldos.</p>
        </div>
        <button className="btn-primario" onClick={() => navegar('alquileres')}>
          <Plus className="h-4 w-4" />
          Nuevo alquiler
        </button>
      </div>

      <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
        {tarjetas.map((tarjeta) => (
          <div key={tarjeta.etiqueta} className="tarjeta">
            <span className={`inline-flex h-10 w-10 items-center justify-center rounded-xl ${tarjeta.tono}`}>
              <tarjeta.icono className="h-5 w-5" aria-hidden />
            </span>
            <p className="mt-3 text-2xl font-bold text-white">{tarjeta.valor}</p>
            {tarjeta.equivalente && (
              <p className="text-xs font-medium text-slate-400">{tarjeta.equivalente}</p>
            )}
            <p className="text-xs text-gray-500">{tarjeta.etiqueta}</p>
          </div>
        ))}
      </div>

      <section className="tarjeta border-marca-500/25 bg-gradient-to-br from-marca-500/10 via-slate-900/80 to-slate-950/70">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div className="flex items-start gap-3">
            <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-marca-500/15 text-marca-300">
              <Coins className="h-5 w-5" aria-hidden />
            </span>
            <div>
              <h2 className="font-semibold text-white">Tasa de cambio</h2>
              <p className="mt-0.5 text-sm text-slate-400">Actualiza el equivalente en bolívares desde el inicio.</p>
            </div>
          </div>
          <div className="rounded-xl border border-slate-700/70 bg-slate-950/55 px-4 py-2 text-right">
            <p className="text-[10px] font-medium uppercase tracking-[0.16em] text-slate-500">Conversión actual</p>
            <p className="mt-0.5 text-base font-bold text-white">$ 1 = {formatearMonto(tasaBs, 'Bs')}</p>
          </div>
        </div>
        <div className="mt-4 flex flex-col gap-3 sm:flex-row sm:items-end">
          <div className="w-full sm:max-w-xs">
            <label className="label" htmlFor="tasa-rapida">Nueva tasa (Bs por 1 $)</label>
            <input
              id="tasa-rapida"
              className="input"
              type="number"
              inputMode="decimal"
              min={0}
              step="0.01"
              placeholder="Ej.: 36,50"
              value={tasaRapida}
              onChange={(e) => {
                setTasaRapida(e.target.value);
                setErrorTasa('');
                setTasaGuardada(false);
              }}
              onKeyDown={(e) => {
                if (e.key === 'Enter') guardarTasa();
              }}
            />
          </div>
          <button className="btn-primario sm:mb-0" onClick={guardarTasa}>
            <Save className="h-4 w-4" aria-hidden />
            Guardar tasa
          </button>
          {tasaGuardada && (
            <span className="inline-flex items-center gap-1.5 pb-2 text-sm font-medium text-cyan-300" role="status">
              <CheckCircle2 className="h-4 w-4" aria-hidden />
              Tasa actualizada
            </span>
          )}
        </div>
        {errorTasa && (
          <p className="mt-2 flex items-center gap-1.5 text-sm text-red-300" role="alert">
            <AlertCircle className="h-4 w-4 shrink-0" aria-hidden />
            {errorTasa}
          </p>
        )}
      </section>

      <div className="tarjeta">
        <div className="mb-3 flex items-center justify-between">
          <h2 className="font-semibold text-white">Alquileres recientes</h2>
          <button
            className="inline-flex min-h-6 items-center gap-1 py-1 text-sm font-medium text-marca-400 hover:text-marca-300"
            onClick={() => navegar('alquileres')}
          >
            Ver todos
            <ChevronRight className="h-4 w-4" />
          </button>
        </div>
        {recientes.length === 0 ? (
          <p className="text-sm text-slate-400">
            Aún no hay alquileres. Crea el primero desde el módulo de Alquileres.
          </p>
        ) : (
          <ul className="divide-y divide-slate-800">
            {recientes.map((a) => (
              <li key={a.id} className="flex items-center justify-between gap-3 py-2.5">
                <div className="min-w-0">
                  <p className="truncate text-sm font-medium text-white">
                    {nombreCliente(a.clienteId)}
                  </p>
                  <p className="text-xs text-slate-400">
                    {a.folio} · {a.modalidad === '12h' ? '12 horas' : '24 horas'}
                  </p>
                </div>
                <div className="flex shrink-0 items-center gap-3">
                  <p className="text-sm font-semibold text-white">
                    {formatearMontoDual(a.montoTotal, moneda, tasaBs)}
                  </p>
                  <EtiquetaAlquiler estado={a.estado} />
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}