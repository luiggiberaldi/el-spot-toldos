/**
 * Captura de coordenadas GPS usando la geolocalización del navegador.
 * El permiso lo pide el navegador en el momento de capturar.
 */

export interface Ubicacion {
  lat: number;
  lng: number;
}

/** Obtiene la ubicación actual del dispositivo. */
export function obtenerUbicacion(): Promise<Ubicacion> {
  return new Promise((resolve, reject) => {
    if (!('geolocation' in navigator)) {
      reject(new Error('Este dispositivo no soporta geolocalización.'));
      return;
    }
    navigator.geolocation.getCurrentPosition(
      (posicion) => {
        resolve({
          lat: posicion.coords.latitude,
          lng: posicion.coords.longitude
        });
      },
      (error) => {
        reject(new Error(mensajeDeError(error)));
      },
      { enableHighAccuracy: true, timeout: 15_000, maximumAge: 0 }
    );
  });
}

/** Traduce los códigos de error de geolocalización a mensajes en español. */
function mensajeDeError(error: GeolocationPositionError): string {
  switch (error.code) {
    case error.PERMISSION_DENIED:
      return 'Permiso de ubicación denegado. Actívalo en los ajustes del navegador e intenta de nuevo.';
    case error.POSITION_UNAVAILABLE:
      return 'No se pudo obtener la ubicación. Revisa que el GPS esté activo.';
    case error.TIMEOUT:
      return 'Se agotó el tiempo esperando la ubicación. Intenta de nuevo.';
    default:
      return 'Ocurrió un error al obtener la ubicación.';
  }
}

/** Enlace para ver las coordenadas en Google Maps. */
export function enlaceMapa(lat: number, lng: number): string {
  return `https://www.google.com/maps?q=${lat},${lng}`;
}

/** Formatea las coordenadas para mostrarlas, p. ej. "10.4805937, -66.9036063". */
export function formatearCoordenadas(lat: number, lng: number): string {
  return `${lat.toFixed(7)}, ${lng.toFixed(7)}`;
}

/** Extrae latitud y longitud desde texto o enlace de Google Maps. */
export function parsearCoordenadas(input: string): { lat: number; lng: number } | null {
  const texto = input.trim();
  if (!texto) return null;

  const matchAt = texto.match(/@(-?\d+(?:\.\d+)?),(-?\d+(?:\.\d+)?)/);
  if (matchAt) {
    const lat = parseFloat(matchAt[1]);
    const lng = parseFloat(matchAt[2]);
    if (lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180) return { lat, lng };
  }

  const matchQuery = texto.match(/[?&](?:q|ll)=(-?\d+(?:\.\d+)?),(-?\d+(?:\.\d+)?)/);
  if (matchQuery) {
    const lat = parseFloat(matchQuery[1]);
    const lng = parseFloat(matchQuery[2]);
    if (lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180) return { lat, lng };
  }

  const matchDirect = texto.match(/(-?\d+(?:\.\d+)?)\s*[,;\s]\s*(-?\d+(?:\.\d+)?)/);
  if (matchDirect) {
    const lat = parseFloat(matchDirect[1]);
    const lng = parseFloat(matchDirect[2]);
    if (lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180) return { lat, lng };
  }

  return null;
}

