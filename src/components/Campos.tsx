import type { ReactNode } from 'react';
import { SelectPersonalizado } from './SelectPersonalizado';

/** Envuelve un campo de formulario con su etiqueta. */
export function GrupoCampo({
  label,
  obligatorio,
  children
}: {
  label: string;
  obligatorio?: boolean;
  children: ReactNode;
}) {
  return (
    <div>
      <label className="label">
        {label}
        {obligatorio && <span className="text-red-600"> *</span>}
      </label>
      {children}
    </div>
  );
}

interface CampoTextoProps {
  label: string;
  valor: string;
  alCambiar: (valor: string) => void;
  obligatorio?: boolean;
  tipo?: string;
  placeholder?: string;
  maxLength?: number;
}

export function CampoTexto({
  label,
  valor,
  alCambiar,
  obligatorio,
  tipo = 'text',
  placeholder,
  maxLength
}: CampoTextoProps) {
  return (
    <GrupoCampo label={label} obligatorio={obligatorio}>
      <input
        className="input"
        type={tipo}
        value={valor}
        placeholder={placeholder}
        maxLength={maxLength}
        onChange={(e) => alCambiar(e.target.value)}
      />
    </GrupoCampo>
  );
}

interface CampoNumeroProps {
  label: string;
  valor: string;
  alCambiar: (valor: string) => void;
  obligatorio?: boolean;
  paso?: string;
  min?: number;
  placeholder?: string;
}

export function CampoNumero({
  label,
  valor,
  alCambiar,
  obligatorio,
  paso = '0.01',
  min,
  placeholder
}: CampoNumeroProps) {
  return (
    <GrupoCampo label={label} obligatorio={obligatorio}>
      <input
        className="input"
        type="number"
        inputMode="decimal"
        step={paso}
        min={min}
        value={valor}
        placeholder={placeholder}
        onChange={(e) => alCambiar(e.target.value)}
      />
    </GrupoCampo>
  );
}

interface CampoFechaProps {
  label: string;
  valor: string;
  alCambiar: (valor: string) => void;
  obligatorio?: boolean;
}

export function CampoFecha({ label, valor, alCambiar, obligatorio }: CampoFechaProps) {
  return (
    <GrupoCampo label={label} obligatorio={obligatorio}>
      <input
        className="input"
        type="date"
        value={valor}
        onChange={(e) => alCambiar(e.target.value)}
      />
    </GrupoCampo>
  );
}

interface CampoSelectProps<T extends string> {
  label: string;
  valor: T;
  alCambiar: (valor: T) => void;
  opciones: Array<{ valor: T; etiqueta: string }>;
  obligatorio?: boolean;
  placeholder?: string;
}

/**
 * Campo de selección con desplegable de diseño propio (sin el select nativo cuadrado).
 */
export function CampoSelect<T extends string>({
  label,
  valor,
  alCambiar,
  opciones,
  obligatorio,
  placeholder
}: CampoSelectProps<T>) {
  return (
    <SelectPersonalizado
      label={label}
      valor={valor}
      alCambiar={alCambiar}
      opciones={opciones}
      obligatorio={obligatorio}
      placeholder={placeholder}
    />
  );
}

interface SelectFiltroProps<T extends string> {
  valor: T;
  alCambiar: (valor: T) => void;
  opciones: Array<{ valor: T; etiqueta: string }>;
  placeholder?: string;
}

/** Select de diseño propio para usar sin etiqueta (p. ej. filtros con ancho fijo). */
export function SelectFiltro<T extends string>({
  valor,
  alCambiar,
  opciones,
  placeholder
}: SelectFiltroProps<T>) {
  return (
    <SelectPersonalizado
      valor={valor}
      alCambiar={alCambiar}
      opciones={opciones}
      placeholder={placeholder}
    />
  );
}

interface CampoTextoAreaProps {
  label: string;
  valor: string;
  alCambiar: (valor: string) => void;
  filas?: number;
  placeholder?: string;
}

export function CampoTextoArea({
  label,
  valor,
  alCambiar,
  filas = 3,
  placeholder
}: CampoTextoAreaProps) {
  return (
    <GrupoCampo label={label}>
      <textarea
        className="input"
        rows={filas}
        value={valor}
        placeholder={placeholder}
        onChange={(e) => alCambiar(e.target.value)}
      />
    </GrupoCampo>
  );
}
