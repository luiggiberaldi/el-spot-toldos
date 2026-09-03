import { describe, expect, it } from 'vitest';
import {
  capitalizarPalabras,
  formatearCedulaVenezolana,
  formatearTelefonoVenezolano,
  numeroWhatsAppVenezolano
} from './venezuela';

describe('formatos venezolanos', () => {
  it('formatea cédulas y RIF sin alterar el prefijo', () => {
    expect(formatearCedulaVenezolana('12345678')).toBe('V-12.345.678');
    expect(formatearCedulaVenezolana('v-12.345.678')).toBe('V-12.345.678');
    expect(formatearCedulaVenezolana('j123456789')).toBe('J-123456789');
  });

  it('formatea teléfonos nacionales y números con código de país', () => {
    expect(formatearTelefonoVenezolano('04121234567')).toBe('0412-1234567');
    expect(formatearTelefonoVenezolano('+584121234567')).toBe('0412-1234567');
    expect(formatearTelefonoVenezolano('02121234567')).toBe('0212-1234567');
    expect(numeroWhatsAppVenezolano('0412-1234567')).toBe('584121234567');
  });

  it('capitaliza nombres de clientes, toldos y negocios', () => {
    expect(capitalizarPalabras('toldo negro')).toBe('Toldo Negro');
    expect(capitalizarPalabras('luigi beraldi')).toBe('Luigi Beraldi');
    expect(capitalizarPalabras('EL SPOT TOLDOS')).toBe('El Spot Toldos');
    expect(capitalizarPalabras('toldo 3x3 blanco')).toBe('Toldo 3x3 Blanco');
    expect(capitalizarPalabras('maría josé pérez')).toBe('María José Pérez');
    expect(capitalizarPalabras('toldo (negro)')).toBe('Toldo (Negro)');
  });

  it('parsea coordenadas desde texto y enlaces de Google Maps', async () => {
    const { parsearCoordenadas } = await import('./geolocalizacion');
    const direct = parsearCoordenadas('10.142918, -68.016897');
    expect(direct).not.toBeNull();
    expect(direct?.lat).toBeCloseTo(10.142918);
    expect(direct?.lng).toBeCloseTo(-68.016897);

    const url = parsearCoordenadas('https://maps.google.com/?q=10.142918,-68.016897');
    expect(url).not.toBeNull();
    expect(url?.lat).toBeCloseTo(10.142918);
    expect(url?.lng).toBeCloseTo(-68.016897);
  });
});
