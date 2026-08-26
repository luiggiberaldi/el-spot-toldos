/** Normaliza datos de contacto al formato de uso habitual en Venezuela. */

export function formatearCedulaVenezolana(valor: string): string {
  const limpio = valor.trim().toUpperCase().replace(/\s+/g, '');
  if (!limpio) return '';
  const prefijo = limpio.match(/^[VEJGPT]/)?.[0] ?? 'V';
  const digitos = limpio.replace(/^[VEJGPT][-:.]?/, '').replace(/\D/g, '');
  if (!digitos) return limpio;
  if (prefijo === 'V' || prefijo === 'E') {
    const cuerpo = digitos.slice(0, 9).replace(/\B(?=(\d{3})+(?!\d))/g, '.');
    return `${prefijo}-${cuerpo}`;
  }
  return `${prefijo}-${digitos.slice(0, 10)}`;
}

export function formatearTelefonoVenezolano(valor: string): string {
  const original = valor.trim();
  if (!original) return '';
  const digitos = original.replace(/\D/g, '');
  if (digitos.length === 11 && digitos.startsWith('0')) {
    return `${digitos.slice(0, 4)}-${digitos.slice(4)}`;
  }
  if (digitos.length === 10 && /^[24]/.test(digitos)) {
    return `0${digitos.slice(0, 3)}-${digitos.slice(3)}`;
  }
  if (digitos.length === 12 && digitos.startsWith('58')) {
    const nacional = `0${digitos.slice(2)}`;
    return `${nacional.slice(0, 4)}-${nacional.slice(4)}`;
  }
  return original;
}

/** Convierte un teléfono venezolano a formato internacional para enlaces wa.me. */
export function numeroWhatsAppVenezolano(valor: string): string {
  const digitos = valor.replace(/\D/g, '');
  if (digitos.startsWith('58') && digitos.length >= 11) return digitos;
  if (digitos.startsWith('0') && digitos.length === 11) return `58${digitos.slice(1)}`;
  if (digitos.length === 10 && /^[24]/.test(digitos)) return `58${digitos}`;
  return digitos;
}

export function nombreArchivoSeguro(valor: string, fallback = 'Cliente'): string {
  return valor.trim()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[^a-zA-Z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '') || fallback;
}
