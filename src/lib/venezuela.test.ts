import { describe, expect, it } from 'vitest';
import { formatearCedulaVenezolana, formatearTelefonoVenezolano, numeroWhatsAppVenezolano } from './venezuela';

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
});
