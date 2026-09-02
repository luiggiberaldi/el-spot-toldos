import { useState } from 'react';
import { useAppStore } from '../../data/store';
import { crearBlobPdf, descargarPdfRecibo, generarPdfRecibo, nombreArchivoRecibo } from '../../lib/pdf';
import { formatearFechaHora, formatearMontoDual } from '../../lib/formato';
import { abrirWhatsApp, compartirPdf, textoReciboWhatsApp } from '../../lib/whatsapp';
import type { Recibo } from '../../types/modelos';
import { Modal } from '../../components/Modal';
import { Eye, Download, MessageCircle, Share2, CheckCircle2, Clock3, Trash2, AlertTriangle } from 'lucide-react';

/** Módulo de recibos digitales emitidos. */
export function Recibos() {
  const recibos = useAppStore((s) => s.recibos);
  const moneda = useAppStore((s) => s.config.negocio.moneda);
  const tasaBs = useAppStore((s) => s.config.tasaBs);

  const [ver, setVer] = useState<Recibo | null>(null);
  const [urlPdf, setUrlPdf] = useState<string | null>(null);
  const [cargando, setCargando] = useState(false);
  const [reciboPorBorrar, setReciboPorBorrar] = useState<Recibo | null>(null);
  const [errorBorrado, setErrorBorrado] = useState('');
  const [mensaje, setMensaje] = useState('');
  const [reciboPorAnular, setReciboPorAnular] = useState<Recibo | null>(null);
  const eliminarRecibo = useAppStore((s) => s.eliminarRecibo);
  const anularRecibo = useAppStore((s) => s.anularRecibo);

  const abrirVista = async (recibo: Recibo) => {
    setVer(recibo);
    setCargando(true);
    try {
      const doc = await generarPdfRecibo(recibo.datos);
      setUrlPdf(URL.createObjectURL(doc.output('blob')));
    } finally {
      setCargando(false);
    }
  };

  const cerrarVista = () => {
    if (urlPdf) URL.revokeObjectURL(urlPdf);
    setUrlPdf(null);
    setVer(null);
  };

  const descargar = async (recibo: Recibo) => {
    await descargarPdfRecibo(recibo.datos);
  };

  const enviarWhatsApp = (recibo: Recibo) => {
    abrirWhatsApp(recibo.datos.cliente.telefono, textoReciboWhatsApp(recibo.datos));
  };

  const compartir = async (recibo: Recibo) => {
    const blob = await crearBlobPdf(recibo.datos);
    const nombre = nombreArchivoRecibo(recibo.datos);
    const compartido = await compartirPdf(blob, nombre);
    if (!compartido) {
      await descargar(recibo);
      setMensaje('El dispositivo no permite compartir archivos; el PDF se descargó automáticamente.');
    }
  };

  const confirmarBorrado = () => {
    if (!reciboPorBorrar) return;
    try {
      if (ver?.id === reciboPorBorrar.id) cerrarVista();
      eliminarRecibo(reciboPorBorrar.id);
      setReciboPorBorrar(null);
      setErrorBorrado('');
      setMensaje(`Recibo ${reciboPorBorrar.folio} eliminado.`);
    } catch (error) {
      setErrorBorrado(error instanceof Error ? error.message : 'No se pudo eliminar el recibo.');
    }
  };

  return (
    <div className="space-y-4">
      <div>
        <h1 className="titulo-pagina">Recibos</h1>
        {mensaje && <p className="mt-2 rounded-xl border border-cyan-400/25 bg-cyan-400/10 px-3 py-2 text-sm text-cyan-200" role="status">{mensaje}</p>}
        <p className="text-sm text-gray-500">
          {recibos.length} recibo{recibos.length === 1 ? '' : 's'} emitido
          {recibos.length === 1 ? '' : 's'} con folio correlativo.
        </p>
      </div>

      {recibos.length === 0 ? (
        <div className="tarjeta text-sm text-gray-400">
          Aún no has emitido recibos. Ve al módulo de Alquileres, abre un alquiler y pulsa
          "Emitir recibo".
        </div>
      ) : (
        <ul className="space-y-2">
          {recibos.map((recibo) => (
            <li key={recibo.id} className="tarjeta flex flex-wrap items-center justify-between gap-3">
              <div className="min-w-0">
                <p className="font-medium text-white">
                  {recibo.folio} · {recibo.datos.cliente.nombre}
                </p>
                <div className="flex flex-wrap items-center gap-2 text-xs text-gray-400">
                  <span>{formatearFechaHora(recibo.emitidoEn)} · {recibo.concepto}</span>
                  <span className={recibo.estado === 'pagado' ? 'inline-flex items-center gap-1 font-medium text-cyan-300' : 'inline-flex items-center gap-1 font-medium text-amber-300'}>
                    {recibo.estado === 'pagado' ? <CheckCircle2 className="h-3.5 w-3.5" /> : <Clock3 className="h-3.5 w-3.5" />}
                    {recibo.estado === 'pagado' ? 'Pagado' : 'Por pagar'}
                  </span>
                </div>
              </div>
              <div className="flex shrink-0 flex-wrap items-center gap-2">
                <p className="text-sm font-bold text-marca-400">
                  {formatearMontoDual(recibo.monto, moneda, tasaBs)}
                </p>
                <button
                  className="btn-icono"
                  onClick={() => abrirVista(recibo)}
                  title="Ver recibo"
                >
                  <Eye className="h-4 w-4" />
                </button>
                <button
                  className="btn-icono"
                  onClick={() => descargar(recibo)}
                  title="Descargar PDF"
                >
                  <Download className="h-4 w-4" />
                </button>
                <button
                  className="btn-icono"
                  onClick={() => enviarWhatsApp(recibo)}
                  title="Enviar por WhatsApp"
                >
                  <MessageCircle className="h-4 w-4" />
                </button>
                <button
                  className="btn-icono"
                  onClick={() => compartir(recibo)}
                  title="Compartir"
                >
                  <Share2 className="h-4 w-4" />
                </button>
                {recibo.estado === 'pagado' && (
                  <button
                    className="btn-icono text-amber-300 hover:border-amber-400/50 hover:bg-amber-500/10"
                    onClick={() => setReciboPorAnular(recibo)}
                    title="Anular recibo y revertir abono"
                    aria-label={`Anular recibo ${recibo.folio}`}
                  >
                    <AlertTriangle className="h-4 w-4" />
                  </button>
                )}
                <button
                  className="btn-icono text-red-400 hover:border-red-400/50 hover:bg-red-500/10 hover:text-red-300"
                  onClick={() => {
                    setErrorBorrado('');
                    setReciboPorBorrar(recibo);
                  }}
                  title="Eliminar recibo"
                  aria-label={`Eliminar recibo ${recibo.folio}`}
                >
                  <Trash2 className="h-4 w-4" />
                </button>
              </div>
            </li>
          ))}
        </ul>
      )}

      {reciboPorAnular && (
        <Modal titulo="Anular recibo" alCerrar={() => setReciboPorAnular(null)}>
          <div className="space-y-4">
            <div className="rounded-xl border border-amber-400/25 bg-amber-400/10 p-4 text-sm text-amber-100">
              <p className="font-semibold text-white">¿Anular el recibo {reciboPorAnular.folio}?</p>
              <p className="mt-1">El recibo se conservará como comprobante y se revertirá su abono del alquiler.</p>
            </div>
            <div className="flex justify-end gap-2">
              <button className="btn-secundario" onClick={() => setReciboPorAnular(null)}>Cancelar</button>
              <button className="btn-peligro" onClick={() => {
                try {
                  anularRecibo(reciboPorAnular.id);
                  setMensaje(`Recibo ${reciboPorAnular.folio} anulado y abono revertido.`);
                  setReciboPorAnular(null);
                } catch (error) {
                  setErrorBorrado(error instanceof Error ? error.message : 'No se pudo anular el recibo.');
                }
              }}>
                <AlertTriangle className="h-4 w-4" /> Anular recibo
              </button>
            </div>
          </div>
        </Modal>
      )}

      {reciboPorBorrar && (
        <Modal
          titulo="Eliminar recibo"
          alCerrar={() => {
            setReciboPorBorrar(null);
            setErrorBorrado('');
          }}
        >
          <div className="space-y-4">
            <div className="flex items-start gap-3 rounded-xl border border-red-400/25 bg-red-400/10 p-4">
              <AlertTriangle className="mt-0.5 h-5 w-5 shrink-0 text-red-300" aria-hidden />
              <div className="min-w-0 text-sm">
                <p className="font-semibold text-white">
                  ¿Eliminar el recibo {reciboPorBorrar.folio}?
                </p>
                <p className="mt-1 text-red-100/75">
                  Se quitará este documento de la lista de recibos y no podrás recuperarlo desde la aplicación.
                </p>
              </div>
            </div>
            <div className="rounded-xl border border-slate-700/70 bg-slate-950/45 px-3 py-2.5 text-xs leading-5 text-gray-400">
              Esta acción elimina solo el recibo. El abono registrado en el alquiler no se revertirá automáticamente.
            </div>
            {errorBorrado && (
              <p className="flex items-center gap-2 text-sm text-red-300" role="alert">
                <AlertTriangle className="h-4 w-4 shrink-0" aria-hidden />
                {errorBorrado}
              </p>
            )}
            <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
              <button
                className="btn-secundario"
                onClick={() => {
                  setReciboPorBorrar(null);
                  setErrorBorrado('');
                }}
              >
                Cancelar
              </button>
              <button className="btn-peligro" onClick={confirmarBorrado}>
                <Trash2 className="h-4 w-4" aria-hidden />
                Eliminar recibo
              </button>
            </div>
          </div>
        </Modal>
      )}

      {ver && (
        <Modal
          titulo={`Recibo ${ver.folio}`}
          alCerrar={cerrarVista}
          anchoMaximo="max-w-3xl"
        >
          <div className="mb-3 flex flex-wrap justify-end gap-2">
            <button className="btn-secundario !py-1.5" onClick={() => enviarWhatsApp(ver)}>
              <MessageCircle className="h-4 w-4" />
              WhatsApp
            </button>
            <button className="btn-secundario !py-1.5" onClick={() => descargar(ver)}>
              <Download className="h-4 w-4" />
              Descargar
            </button>
            <button className="btn-secundario !py-1.5" onClick={() => compartir(ver)}>
              <Share2 className="h-4 w-4" />
              Compartir
            </button>
          </div>
          {cargando ? (
            <p className="py-8 text-center text-sm text-gray-400">Generando PDF…</p>
          ) : urlPdf ? (
            <iframe
              src={urlPdf}
              title={`Recibo ${ver.folio}`}
              className="h-[70vh] w-full rounded-lg border border-slate-800 bg-white"
            />
          ) : null}
        </Modal>
      )}
    </div>
  );
}
