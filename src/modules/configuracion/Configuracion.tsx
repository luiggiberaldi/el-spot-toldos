import { useRef, useState } from 'react';
import { useAppStore } from '../../data/store';
import { descargarRespaldo, esRespaldoValido } from '../../data/respaldo';
import type { DatosNegocio } from '../../types/modelos';
import { formatearCedulaVenezolana, formatearTelefonoVenezolano } from '../../lib/venezuela';
import { CampoNumero, CampoTexto } from '../../components/Campos';
import { Download, Upload, Trash2, Check } from 'lucide-react';

/** Módulo de configuración: datos del negocio y respaldo de datos. */
export function Configuracion() {
  const config = useAppStore((s) => s.config);
  const actualizarConfig = useAppStore((s) => s.actualizarConfig);
  const restaurarDatos = useAppStore((s) => s.restaurarDatos);
  const restablecerTodo = useAppStore((s) => s.restablecerTodo);

  const [negocio, setNegocio] = useState<DatosNegocio>(config.negocio);
  const [tasa, setTasa] = useState(config.tasaBs ? String(config.tasaBs) : '');
  const [guardado, setGuardado] = useState(false);
  const [errorOperacion, setErrorOperacion] = useState('');
  const [confirmacion, setConfirmacion] = useState('');
  const inputImportar = useRef<HTMLInputElement>(null);

  const guardar = () => {
    const tasaNumero = parseFloat(tasa.replace(',', '.'));
    try {
      actualizarConfig({
        ...config,
        // La moneda principal es siempre el dólar ($); el Bs se calcula con la tasa.
        negocio: {
          ...negocio,
          rif: formatearCedulaVenezolana(negocio.rif),
          telefono: formatearTelefonoVenezolano(negocio.telefono),
          moneda: '$'
        },
        tasaBs: isNaN(tasaNumero) || tasaNumero < 0 ? 0 : tasaNumero
      });
      setErrorOperacion('');
      setConfirmacion('Configuración guardada correctamente.');
      setGuardado(true);
    } catch (error) {
      setErrorOperacion(error instanceof Error ? error.message : 'No se pudo guardar la configuración.');
    }
    window.setTimeout(() => setGuardado(false), 2500);
  };

  const importar = (archivo: File | undefined) => {
    if (!archivo) return;
    const lector = new FileReader();
    lector.onload = () => {
      try {
        const datos = JSON.parse(String(lector.result));
        if (!esRespaldoValido(datos)) {
          setErrorOperacion('El archivo no es un respaldo válido de Gestor de Toldos.');
          return;
        }
        if (
          window.confirm(
            'Esto reemplazará todos los datos actuales por los del respaldo. ¿Continuar?'
          )
        ) {
          try {
            restaurarDatos(datos.datos);
            setNegocio(datos.datos.config.negocio);
            setTasa(datos.datos.config.tasaBs ? String(datos.datos.config.tasaBs) : '');
            setErrorOperacion('');
          } catch (error) {
            setErrorOperacion(error instanceof Error ? error.message : 'No se pudo restaurar el respaldo.');
          }
        }
      } catch {
        setErrorOperacion('No se pudo leer el archivo. Asegúrate de que sea un respaldo JSON.');
      }
    };
    lector.readAsText(archivo);
    if (inputImportar.current) inputImportar.current.value = '';
  };

  const restablecer = () => {
    if (!window.confirm('¿Restablecer todos los datos? Se borrará clientes, toldos, alquileres y recibos.')) return;
    if (!window.confirm('Esta acción es irreversible. Antes de continuar, exporta un respaldo. ¿Restablecer de todos modos?')) return;
    restablecerTodo();
    setNegocio({
      nombre: 'EL SPOT',
      rif: '',
      telefono: '',
      direccion: '',
      moneda: '$',
      logo: ''
    });
    setTasa('');
    setErrorOperacion('Datos restablecidos. La app vuelve a su estado inicial.');
  };

  return (
    <div className="max-w-2xl space-y-6">
      <div>
        <h1 className="titulo-pagina">Configuración</h1>
        <p className="text-sm text-gray-400">
          Datos de tu negocio y gestión de respaldos. Estos datos aparecen en los recibos.
        </p>
      </div>

      {/* Datos del negocio */}
      <section className="tarjeta space-y-4">
        <h2 className="font-semibold text-white">Datos del negocio</h2>

        <CampoTexto
          label="Nombre del negocio"
          valor={negocio.nombre}
          alCambiar={(v) => setNegocio({ ...negocio, nombre: v })}
          obligatorio
          placeholder="Ej.: Toldos El Sol"
        />
        <div className="grid gap-4 sm:grid-cols-2">
          <CampoTexto
            label="RIF / Cédula jurídica"
            valor={negocio.rif}
            alCambiar={(v) => setNegocio({ ...negocio, rif: v })}
            placeholder="Opcional"
          />
          <CampoTexto
            label="Teléfono"
            valor={negocio.telefono}
            alCambiar={(v) => setNegocio({ ...negocio, telefono: v })}
            tipo="tel"
            placeholder="Opcional"
          />
        </div>
        <CampoTexto
          label="Dirección"
          valor={negocio.direccion}
          alCambiar={(v) => setNegocio({ ...negocio, direccion: v })}
          placeholder="Dirección de tu negocio"
        />
        <div>
          <CampoNumero
            label="Tasa de cambio (Bs por 1 $)"
            valor={tasa}
            alCambiar={setTasa}
            min={0}
            placeholder="Ej.: 36,50"
          />
          <p className="mt-1 text-xs text-gray-400">
            La moneda principal es el dólar ($). Con esta tasa se calcula y muestra el
            equivalente en bolívares en los montos y en el recibo. Déjala en 0 si solo
            quieres ver dólares.
          </p>
        </div>

        {/* Logo oficial */}
        <div>
          <label className="label">Logo oficial del sistema y los recibos</label>
          <div className="flex items-center gap-3 rounded-xl border border-slate-700 bg-black p-3">
            <img
              src="/marca-elspot.png"
              alt="Logo oficial de EL SPOT"
              className="h-20 w-20 rounded-lg object-contain"
            />
            <p className="text-sm text-gray-300">
              Este logo se usa en toda la aplicación y queda centrado en el header oscuro de cada PDF.
            </p>
          </div>
          {errorOperacion && <p className="mt-2 text-sm text-red-300" role="alert">{errorOperacion}</p>}
          {confirmacion && <p className="mt-2 text-sm text-cyan-200" role="status">{confirmacion}</p>}
        </div>

        <div className="flex items-center gap-3">
          <button className="btn-primario" onClick={guardar}>
            Guardar cambios
          </button>
          {guardado && (
            <span className="inline-flex items-center gap-1 text-sm font-medium text-cyan-300">
              <Check className="h-4 w-4" />
              Guardado
            </span>
          )}
        </div>
      </section>

      {/* Respaldo */}
      <section className="tarjeta space-y-4">
        <h2 className="font-semibold text-white">Respaldo de datos</h2>
        <p className="text-sm text-gray-400">
          Los datos se guardan solo en este dispositivo. Exporta respaldos con frecuencia
          para no perder información si cambias de teléfono o computadora.
        </p>
        <div className="flex flex-wrap gap-2">
          <button
            className="btn-secundario"
            onClick={() =>
              descargarRespaldo({
                clientes: useAppStore.getState().clientes,
                toldos: useAppStore.getState().toldos,
                alquileres: useAppStore.getState().alquileres,
                recibos: useAppStore.getState().recibos,
                bitacora: useAppStore.getState().bitacora,
                config: useAppStore.getState().config
              })
            }
          >
            <Download className="h-4 w-4" />
            Exportar respaldo
          </button>
          <label className="btn-secundario cursor-pointer">
            <Upload className="h-4 w-4" />
            Importar respaldo
            <input
              ref={inputImportar}
              type="file"
              accept="application/json"
              className="hidden"
              onChange={(e) => importar(e.target.files?.[0])}
            />
          </label>
          <button className="btn-peligro" onClick={restablecer}>
            <Trash2 className="h-4 w-4" />
            Restablecer todo
          </button>
        </div>
      </section>

      <section className="tarjeta text-sm text-gray-400">
        <p>
          Último recibo emitido:{' '}
          <span className="font-medium text-gray-300">
            {config.ultimoFolio > 0 ? `REC-${String(config.ultimoFolio).padStart(4, '0')}` : 'ninguno'}
          </span>
        </p>
      </section>
    </div>
  );
}
