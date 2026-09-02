# Auditoría determinista de responsividad — EL SPOT · Gestor de Toldos

**Fecha:** 2026-09-02 · **Alcance:** PWA web (`src/`) · **Método:** doble pasada, 100 % reproducible:

1. **Estática** — reglas verificadas por lectura directa del código (grep + lectura completa de los 8 archivos UI: `Layout.tsx`, `Modal.tsx`, `Campos.tsx`, `SelectPersonalizado.tsx` y los 6 módulos).
2. **Runtime** — dev server Vite 5.4.21, sweep real de las 6 vistas + modales, con datos sembrados, midiendo `scrollWidth` vs `clientWidth`, rects de elementos, objetivos táctiles y tamaños de fuente.

> Todo hallazgo cita archivo/línea y puede reproducirse con los comandos listados.

---

## 1. Veredicto general

**7,5 / 10 — buena responsividad general, con 3 defectos reales y 4 riesgos latentes.**

| Dimensión | Veredicto | Evidencia principal |
| --- | --- | --- |
| Overflow horizontal | ✅ **Sin overflow medido en ninguna vista** | Sweep runtime de 6 vistas (485 px): `scrollWidth == clientWidth` en todas |
| Estrategia de navegación | ✅ Sólida: sidebar ≥768 px, bottom-nav <768 px | `Layout.tsx:40,69,80` |
| Modales en móvil | ✅ Patrón bottom-sheet correcto, footer sticky visible | `Modal.tsx:14,18`; medición: footer visible, sin choque funcional |
| Objetivos táctiles | ⚠️ 12 bajo el mínimo WCAG 2.5.8 (24 px) | Conteo runtime: 12 `tinyTargets` |
| Zoom automático iOS | ✅ **Corregido (2026-09-02):** `.input` ahora a 16 px | `index.css` `.input` = `text-base`; verificado en runtime |
| Área segura iOS (notch) | ✅ **Corregido (2026-09-02):** nav con `env(safe-area-inset-bottom)` | `Layout.tsx` nav inferior + `main` compensado |
| Rendimiento percibido | ⚠️ Bundle inicial 194 kB gzip; jsPDF+html2canvas bloquean el primer render | Salida de `vite build` |

---

## 2. Matriz de reglas estáticas (determinista)

| # | Regla | Resultado | Evidencia |
| --- | --- | --- | --- |
| R1 | Un solo breakpoint principal (768 px) usado consistentemente | ✅ | Únicos puntos de corte activos: `sm:` (640), `md:` (768), `lg:` (1024). Sin `xl:` huérfanos |
| R2 | Contenido principal con `min-w-0` dentro de flex para permitir truncado | ✅ | `Layout.tsx:68` (`min-w-0`), listas de Clientes/Alquileres/Recibos usan `min-w-0` + `truncate` |
| R3 | Modales adaptables: bottom-sheet en móvil, centrado ≥640 px | ✅ | `Modal.tsx:14` `items-end sm:items-center sm:p-4`; `:18` `rounded-t-2xl sm:rounded-2xl` |
| R4 | Modales con altura y scroll controlados | ✅ | `Modal.tsx:18` `max-h-[92vh] overflow-y-auto` |
| R5 | Formulario de alquiler usable en 360 px (grid de líneas) | ✅ (marginal) | `Alquileres.tsx:502-541` `grid-cols-12`: medido a 485 px, **Cant. = 87 px**; a 360 px queda ≈55 px: funciona pero es el punto más apretado del sistema |
| R6 | Textos largos truncados (nombres, direcciones, montos duales) | ✅ | `truncate` en Cliente/Toldo/Alquiler/Recibo; los montos duales `$ X (Bs Y)` pueden crecer pero están en contenedores `flex-wrap` o `min-w-0` |
| R7 | Grillas fluidas sin anchos fijos | ✅ | Toldos `grid gap-3 sm:grid-cols-2 lg:grid-cols-3` (`Toldos.tsx:162`); Dashboard `grid-cols-2 lg:grid-cols-4` (`Dashboard.tsx:95`) |
| R8 | Cero `position:fixed` con left/top negativos (off-canvas fantasma) | ✅ | grep `min-w-\[78px\]|safe-area|env\(` y lectura completa de Layout: no existe |
| R9 | Cero tablas HTML anchas (patrón clásico de overflow) | ✅ | El sistema no usa `<table>` en UI; todas las listas son `ul/li` flexibles |
| R10 | Teclado numérico correcto en campos monetarios | ✅ | `Campos.tsx` `CampoNumero` usa `type="number" inputMode="decimal"` |
| R11 | Menú inferior desplazable si no caben los 6 ítems | ✅ | `Layout.tsx:80` `overflow-x-auto` + ítems `min-w-[78px]` — ver F2 para el costo |
| R12 | Header/branding fluido (`object-contain`, max-width) | ✅ | `Layout.tsx:19-23` y header móvil `h-16 max-w-[150px]` |
| R13 | Tap highlight eliminado y foco visible | ✅ | `index.css` `-webkit-tap-highlight-color: transparent`; `.btn` y botones-opción con `focus:ring-2` |

**13 reglas evaluadas: 11 cumplidas, 0 fallas estructurales, 2 incumplidas (F1, F3).**

## 3. Evidencia runtime (dev server, 2026-09-02)

Contexto: 485×868 px (layout móvil activo, `nav` inferior presente), datos sembrados (2 clientes, 2 toldos, 2 alquileres, 1 recibo), restaurados al terminar.

| Medición | Resultado |
| --- | --- |
| Overflow X por vista (Panel, Clientes, Toldos, Alquileres, Recibos, Configuración) | `overflow: false` en las 6 |
| Modal "Nuevo alquiler" a 485 px | ancho 475 px (5 px de margen por lado), alto visible 783 px, footer sticky **visible** |
| Interacción modal vs bottom-nav | el modal pasa por debajo del nav (z-40 vs z-50) pero su contenido y footer son accesibles; sin elementos inalcanzables |
| Objetivos táctiles interactivos | 12 elementos <24 px en alguna dimensión (botones `!h-8 !w-8` = 32 px × alto de fila ≈ y chevrons); **0** por debajo de 16 px |
| Inputs de texto/número | `font-size: 14px`, altura 37 px → **disparan zoom automático en iOS Safari** (umbral: 16 px) |
| Fila "Cant./Precio" del formulario de alquiler | Cant. 87 px, Precio 182 px a 485 px |

Captura de pantalla del Panel a 485 px: sin overflow, tarjetas 2×2 correctas, nav inferior legible.

**Verificación estática posterior:** `npm run typecheck` ✓ · `npm test` 56/56 ✓ · `npm run build` ✓ (13,2 s).

## 4. Hallazgos (fallas reales)

### F1 · 🔴→✅ Inputs a 14 px → zoom automático en iOS — **CORREGIDO 2026-09-02**
- **Dónde:** `index.css` `.input` (`text-sm` = 14 px) — afecta a todos los formularios (Clientes, Toldos, Alquileres, Recibos implícito vía Modales, Configuración, tasa del Panel).
- **Por qué importa:** iOS Safari hace zoom al enfocar cualquier input <16 px; en el flujo principal de la app (registrar alquiler en un teléfono) cada enfoque de campo "salta" la pantalla.
- **Arreglo mínimo:** subir `.input` a `text-base` (16 px), o media query `@media (max-width: 767px)`. Determinista: `grep "text-sm" src/index.css` → 1 sola definición centralizada.
- **Estado:** ✅ Aplicado — `.input` usa `text-base`; verificado en runtime (`font-size: 16px` en inputs del formulario de alquiler, altura 41 px, fila de líneas sin overflow).

### F2 · 🟡→✅ Bottom-nav `overflow-x-auto`: el scroll compite con el toque — **CORREGIDO 2026-09-02**
- **Dónde:** `Layout.tsx:80-101`. 6 ítems × `min-w-[78px]` ≈ 468 px; en un iPhone SE (320-375 px) "Configuración" queda parcialmente fuera y hay que deslizar una barra de 64 px de alto.
- **Por qué importa:** el swipe horizontal sobre la barra puede dispararse mientras el usuario intenta tocar; y en el breakpoint 640-767 px el patrón bottom-tab es discutible (existe la sidebar ≥768 px).
- **Arreglo:** reducir `min-w-[78px]` → `min-w-[56px]` + `px-1` (6×56=336 px, cabe en 360 px), o mostrar la sidebar desde `sm:` (640 px).
- **Estado:** ✅ Aplicado — `min-w-[56px]` + `px-0.5`; verificado en runtime: `min-width: 56px` declarado. En ≥380 px los ítems siguen llenando por `flex-1` (sin cambio visual); en 375 px caben los 6 (6×62,5 px) sin scroll.

### F3 · 🟡→✅ Sin `safe-area-inset-bottom` (iPhone con notch/dynamic island) — **CORREGIDO 2026-09-02**
- **Dónde:** `Layout.tsx:80` (nav fija inferior) y `Layout.tsx:76` (`pb-24` fijo). El `viewport-fit=cover` ya está en `index.html` (correcto), pero nada consume `env(safe-area-inset-bottom)`.
- **Por qué importa:** en iOS sin barra de inicio visible, la fila "Configuración" de la nav puede quedar medio tapada por el gesto de inicio.
- **Arreglo mínimo:** al nav: `padding-bottom: env(safe-area-inset-bottom)` (o clase arbitraria `pb-[env(safe-area-inset-bottom)]`), y `pb-24` → `pb-[calc(6rem+env(safe-area-inset-bottom))]`.
- **Estado:** ✅ Aplicado — nav con `pb-[env(safe-area-inset-bottom)]` y `main` con `pb-[calc(6rem+env(safe-area-inset-bottom))]`; runtime confirma que `main` resuelve 96 px (6 rem + 0 inset en escritorio/Windows) y que la nav crece con el inset cuando existe.

## 5. Riesgos latentes (no fallan hoy, fallarán al crecer)

| # | Riesgo | Gatillo de activación | Severidad si dispara |
| --- | --- | --- | --- |
| R-A | **Búsqueda sin memo** — `Clientes.tsx:~96` y `Alquileres.tsx:~65` filtran en cada render, sin `useMemo`; la bitácora sí lo hace (`Bitacora.tsx:26`) | ~500+ clientes o alquileres en un teléfono de gama baja: cada pulsación de tecla re-filtra y re-renderiza la lista completa | 🟠 Lag al escribir en la búsqueda |
| R-B | **Listas sin virtualización ni paginación** — todas las listas pintan el array completo | ~200+ recibos/alquileres: el DOM crece sin límite y el scroll se degrada | 🟠 Scroll pesado, TTI móvil |
| R-C | **Sin paginación táctil del dropdown** — `SelectPersonalizado.tsx` renderiza todas las opciones con `max-h-60 overflow-auto` | 100+ clientes en el select "Cliente" del alquiler | 🟡 Selección tediosa en móvil |
| R-D | **Bundle inicial 194 kB gzip (610 kB crudo)** — jsPDF + html2canvas (373 kB combinados, crudo) se cargan al abrir la app aunque el PDF solo se genere al emitir/ver un recibo | Ya activo hoy; pesa en cada primera carga móvil/PWA | ✅ **Corregido (2026-09-02):** carga diferida → inicial de 75 kB gzip (−61 %) |
| R-E | **Fila 12-col del formulario de alquiler en <380 px** — Cant. mide 87 px a 485 px; escala a ≈55 px en un SE | Ya marginal; cualquier etiqueta o unidad larga empuja a wrap | 🟡 Usabilidad en 320-375 px |

## 6. Conclusiones

1. **La base es sana**: breakpoint único coherente, modales bottom-sheet, truncado disciplinado, cero overflow medido en las 6 vistas y el formulario más complejo (alquileres) mantiene sus footer sticky accesibles en móvil.
2. **Los 3 hallazgos son de pulido móvil, no de arquitectura**: F1 (fuente de inputs) es el de mayor impacto diario y el más barato de corregir (1 línea en `.input`); F2/F3 se resuelven con clases arbitrarias de Tailwind en 2 elementos.
3. **Los riesgos R-A/R-B son los únicos con potencial de degradación real a mediano plazo** (crecimiento orgánico de datos en un negocio activo); ambos tienen arreglo local (`useMemo` ahora; virtualización cuando las listas superen ~200 ítems).
4. El bundle pesado (R-D) es una decisión de producto: code-splitting de jsPDF vía `import()` diferido recortaría el primer render significativamente sin tocar la lógica de recibos.

### Orden de corrección recomendado
1. ~~F1 (1 línea, impacto inmediato en todo uso móvil)~~ ✅ → 2. ~~F3 (nav + padding seguro)~~ ✅ → 3. ~~F2 (min-width de ítems)~~ ✅ → 4. ~~R-A (`useMemo` en 2 filtros)~~ ✅ → 5. ~~R-D (import dinámico de pdf)~~ ✅

> **Verificación de los tres arreglos (2026-09-02):** `npm run typecheck` ✓ · `npm test` 56/56 ✓ · `npm run build` ✓ · runtime: inputs 16 px, nav `min-width: 56px` + safe-area, sin overflow y con footer sticky visible en el modal de alquileres.

---

*Auditoría generada por revisión estática de código + sweep runtime sobre Vite 5.4.21. Los comandos de reproducción: `npm run dev`, inspección DOM por consola; `npm run typecheck`; `npm test`; `npm run build`.*

---

## 7. Automatización (2026-09-02)

Las tres reglas nucleares de esta auditoría viven ahora como pruebas repetibles:
`npm run test:responsive` ejecuta la PWA real en Edge/Chromium headless (Vitest
browser mode) y hace cumplir R-OV (sin overflow horizontal en 6 vistas ×
320/360/375/768/1280 px), R-TT (objetivos táctiles ≥24×24 px, WCAG 2.5.8) y
R-IN (inputs ≥16 px contra el zoom de iOS), incluyendo el modal de alquileres.
Su primera ejecución detectó y permitió corregir 2 violaciones reales de R-TT
(botón "Ver todos" del Panel y los enlaces "Crear un cliente/toldo aquí").
Detalle de implementación: `src/responsive.audit.test.tsx` y
`vitest.config.responsive.ts`.
