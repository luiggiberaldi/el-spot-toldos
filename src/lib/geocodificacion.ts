import type { Ubicacion } from './geolocalizacion';

export async function direccionDesdeUbicacion(ubicacion: Ubicacion): Promise<string | null> {
  try {
    const params = new URLSearchParams({
      format: 'jsonv2',
      lat: String(ubicacion.lat),
      lon: String(ubicacion.lng),
      zoom: '18',
      addressdetails: '1'
    });
    const controller = new AbortController();
    const timeout = globalThis.setTimeout(() => controller.abort(), 5_000);
    try {
      const response = await fetch(`https://nominatim.openstreetmap.org/reverse?${params}`, {
        headers: { Accept: 'application/json' },
        signal: controller.signal
      });
      if (!response.ok) return null;
      const data = (await response.json()) as { display_name?: string };
      return data.display_name?.trim() || null;
    } finally {
      globalThis.clearTimeout(timeout);
    }
  } catch {
    return null;
  }
}
