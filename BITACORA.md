# Bitácora interna del sistema — EL SPOT

> Este documento es interno y no se muestra en la interfaz de la aplicación. La auditoría también se conserva en la store web, Room en Android y los respaldos.
>
> **Regla del proyecto:** cada cambio, modificación o reparación del sistema debe
> quedar registrada aquí. Entradas ordenadas de la más reciente a la más antigua.

**Formato de cada entrada:**

| Campo | Descripción |
| --- | --- |
| **Fecha** | Día del cambio (AAAA-MM-DD) |
| **Versión** | Versión del sistema tras el cambio |
| **Tipo** | `Nuevo` · `Cambio` · `Corrección` |
| **Descripción** | Qué se hizo y por qué |

---

## Auditoría autoupdate — 2026-09-02 — `Nuevo`

### Verificación de fallos elegantes: 404 del manifiesto y SHA-256 incorrecto

- Se verificó el manejo de los dos escenarios de falla del autoupdate: **manifiesto
  inaccesible o corrupto** (404/500, HTML, JSON truncado) termina en
  `UpdateCheckResult.Failed` con diagnóstico del código HTTP, sin crash ni versiones
  aceptadas de datos corruptos; **descarga con SHA-256 distinto al del manifiesto**
  elimina el archivo parcial y propaga un error claro — nunca se notifica la
  instalación de un binario no verificado.
- Verificación empírica contra GitHub real (sonda ejecutada una vez): un manifiesto
  inexistente produce `Failed` con mensaje "El manifiesto respondió 404." y el
  manifiesto de producción se parsea correctamente (`UpToDate`).
- Nuevas pruebas deterministas sin red (`AppUpdateFailureTest`, 8 casos) usando una
  conexión HTTP falsa inyectada por el nuevo punto de extensión de
  `GithubUpdateClient` (parámetro `abrirConexion`; en producción el comportamiento
  es idéntico). Sonda de red real `AppUpdateLiveProbe` marcada `@Ignore` para
  ejecución manual.
- Cobertura previa: `check()` ya envolvía todo en `runCatching` y `download()` ya
  borraba el archivo parcial en `catch`; el trabajador devuelve `Result.success()`
  en fallos (WorkManager no reintentar en bucle) y notifica solo en chequeos
  manuales. El hallazgo es que faltaba cobertura, no corrección.
- Suite completa: 17 pruebas ejecutadas (25 declaradas, 2 sonda omitidas), 0 fallos.

## CI — 2026-09-02 — `Nuevo`

### Release automática de la APK por GitHub Actions

- Nuevo `.github/workflows/android-release.yml`: cuando un push a `main` modifica
  `android-app/app/build.gradle.kts` (es decir, sube el `versionCode`/`versionName`),
  el flujo decide si corresponde publicar, compila la APK release firmada, corre las
  pruebas unitarias (`assembleRelease` + `testDebugUnitTest`), verifica la firma con
  `apksigner` y publica la GitHub Release `v<versión>` con el asset
  `el-spot-toldos-<versión>.apk`; también admite ejecución manual.
- Puerta de versión determinista (`scripts/version_utils.mjs debe-publicar`): compara
  el `versionName` del build contra el tag `vX.Y.Z` más alto publicado; evita releases
  duplicadas y termina en "omitir" sin error cuando no hay versión nueva.
- Se generó el **Gradle wrapper** (`android-app/gradlew`, Gradle 9.3.1), el único
  componente que faltaba para compilar fuera de esta máquina: CI y máquinas locales
  usan ahora la misma versión exacta.
- Firma en CI: el keystore llega decodificado del secret `RELEASE_KEYSTORE_BASE64` a
  `~/.android/debug.keystore` (mismo certificado que la app instalada, actualización
  en sitio que conserva datos). El bloque `signingConfigs` admite sobrescribir
  credenciales por entorno (`RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`,
  `RELEASE_KEY_PASSWORD`) con los mismos valores por defecto en local.
  **Pendiente del propietario:** configurar una vez el secret con
  `base64 -w0 ~/.android/debug.keystore | gh secret set RELEASE_KEYSTORE_BASE64
  --repo luiggiberaldi/el-spot-toldos`; sin él el flujo falla a propósito con
  instrucciones en el log (nunca publica una APK sin la firma correcta).
- `update.json` lo actualiza el propio CI tras publicar (commit firmado como
  `github-actions[bot]`), con el SHA-256 **del asset publicado**: el empaquetado APK
  no es byte-reproducible entre entornos (para el mismo versionName, el build local
  dio `70a619dc…` y el del CI `3c90ebb1…`), así que el hash válido para el autoupdate
  solo se conoce después de publicar. El commit del bump solo redacta las notas;
  el CI preserva `notes` y `mandatory`.
- Validación real en GitHub Actions: el run del push verificó la puerta en un runner
  (`1.0.2 → omitir`, success); una ejecución manual con `forzar` compiló, firmó y
  verificó la APK completa en el runner (mismo certificado `a6fd6451…`, por lo que la
  actualización en sitio conserva datos), subió el artefacto y no publicó nada. Un
  primer intento falló porque el wrapper quedó sin bit ejecutable al generarse en
  Windows (`./gradlew: Permission denied`); corregido con `git update-index
  --chmod=+x` (commit `b594813`).
- Verificación local del cambio: `assembleRelease` con el wrapper y variables de
  entorno estilo CI regeneró una APK **byte-idéntica** (SHA-256 `70a619dc…367da`,
  igual al asset v1.0.2 ya publicado) con el mismo certificado; typecheck y las 56
  pruebas de la PWA siguen en verde.
- `android-app/README.md` documenta la nueva sección "Release automática (CI)".

## v1.0.2 — 2026-09-02 — `Nuevo`

### APK release 1.0.2 (versionCode 3) para el autoupdate

- Se compiló `app-release.apk` (versionCode 3, versionName 1.0.2) con Gradle 9.3.1
  y JDK 17, incluyendo todas las mejoras de responsividad, la anulación de recibos,
  las confirmaciones en app y el formulario rediseñado.
- La release se firma con el `debug.keystore` de este equipo (misma firma que las
  versiones instaladas), de modo que la instalación es una actualización en sitio
  que conserva Room, DataStore y todos los datos, según la verificación documentada
  en `android-app/README.md`. `isMinifyEnabled` se mantiene en false.
- Verificación de la APK: `aapt` confirma `com.elspot.toldos` 1.0.2 (3), etiqueta
  EL SPOT TOLDOS; `apksigner verify` confirma firma válida (SHA-256 de certificado
  a6fd6451…); SHA-256 del archivo: 70a619dc986446c8f0da9ffce7df030bfdd8dd351e3591790a99a405f5e367da;
  tamaño 13.112.946 bytes.
- `testDebugUnitTest` correcto: 17 pruebas (2+3+7+5) sin fallos.
- `update.json` actualizado a la versión 3 / 1.0.2 con URL y SHA-256 de la release;
  requiere publicar el tag `v1.0.2` en GitHub Releases con el asset
  `el-spot-toldos-1.0.2.apk` para que el autoupdate encuentre el archivo.
- No se ejecutaron pruebas instrumentadas (sin dispositivo/emulador conectado).

---

## Registro de trabajo pendiente — 2026-09-02 — `Cambio`

### Anulación de recibos, confirmaciones en app, formulario rediseñado y APK 1.0.1

- **Anular recibo:** nueva acción sobre recibos pagados que revierte el abono del
  alquiler, conserva el documento como comprobante y lo deja `por_pagar`
  (`anularRecibo` en la store web y modal de confirmación en Recibos).
- **Confirmaciones en app:** los `window.confirm()` de Clientes, Toldos,
  Alquileres y Configuración (restablecer/importar) se reemplazan por modales
  propios, consistentes con el diseño y sin diálogos nativos.
- **Formulario de alquiler rediseñado:** tarjeta de entrega y ubicación GPS,
  resumen financiero en tarjeta con desglose Total/Abono/Pendiente y pie de
  acciones sticky; paridad visual en la APK Android (secciones con etiqueta
  y divisores).
- **WhatsApp:** `abrirWhatsApp` ya no usa `alert()` bloqueante y abre con
  `noopener,noreferrer`.
- **APK 1.0.1:** `versionCode 2`, `update.json` apuntando a la release v1.0.1
  y documentación en `android-app/README.md` que verifica que la actualización
  en sitio no borra datos (Room/DataStore) y los riesgos de cambio de firma.
- Verificación: typecheck ✓, 56 pruebas node ✓, suite de responsividad 4/4 ✓,
  build de producción ✓.

---

## v1.4.21 — 2026-09-02 — `Nuevo`

### Auditoría de responsividad automatizada como pruebas de CI

- Nueva suite `src/responsive.audit.test.tsx`: renderiza la PWA real en Chromium
  (Edge del sistema vía Vitest browser mode + playwright con `channel: msedge`,
  sin descargas de navegadores) y hace cumplir tres reglas deterministas:
  R-OV sin overflow horizontal en las 6 vistas a 320/360/375/768/1280 px,
  R-TT objetivos táctiles ≥24×24 px (WCAG 2.5.8, con excepción de enlaces inline),
  y R-IN campos de texto ≥16 px contra el zoom automático de iOS.
  Incluye el modal "Nuevo alquiler" (formulario más complejo) a 320 y 375 px.
- Se ejecuta con `npm run test:responsive` (config `vitest.config.responsive.ts`,
  aislada del `npm test` normal). Al fallar deja captura en `src/__screenshots__/`.
- La primera ejecución detectó 3 violaciones reales que la auditoría manual había
  calificado solo como riesgo: botón "Ver todos" del Panel (20 px de alto) y los
  enlaces-botón "Crear un cliente/toldo aquí" del formulario de alquileres
  (16-20 px). Corregidos con `min-h-6` (+ padding), conservando el diseño.
- El detector R-OV distingue el desbordamiento real de la página del scroll
  intencional de la barra inferior documentado para <380 px (regla R11).
- Verificación: suite 4/4 en dos ejecuciones consecutivas ✓, typecheck ✓,
  56 pruebas node ✓, build de producción ✓. No se compiló la APK.

---

## v1.4.20 — 2026-09-02 — `Cambio`

### Carga diferida de jsPDF: −61 % de peso inicial de la PWA (riesgo R-D de la auditoría)

- `src/lib/pdf.ts` ya no importa jsPDF estáticamente: usa `import type` para los tipos y
  un `import('jspdf')` dinámico cacheado que resuelve solo al generar o ver el primer recibo.
- El paquete inicial baja de 610 kB (194 kB gzip) a 252 kB (75 kB gzip): jsPDF,
  html2canvas y dompurify (~118 kB gzip) quedan en un chunk diferido que también
  precachea el service worker, por lo que los recibos siguen funcionando sin conexión.
- El módulo se cachea en memoria (`moduloJsPdf`), así que los recibos sucesivos no
  repiten la carga.
- Verificado en build de producción servido con `vite preview`: recibo REC-0001
  generado y mostrado en el visor a través del chunk diferido. Typecheck ✓,
  56 pruebas ✓. No se compiló la APK.

---

## v1.4.19 — 2026-09-02 — `Cambio`

### Búsquedas memoizadas en Clientes y Alquileres (riesgo R-A de la auditoría)

- El filtrado de la lista de clientes y de alquileres ahora se calcula con `useMemo`
  (dependencias explícitas), igual que la bitácora: dejar de re-filtrar todo el
  listado en cada render elimina el costo por pulsación de tecla al escribir en la
  búsqueda cuando el negocio crezca a cientos de registros.
- Los resultados verificados en runtime: buscar/borrar actualiza la lista al
  instante (3 → 1 → 3 clientes; 2 → 1 alquileres filtrando por dirección y estado).
- Verificación: typecheck ✓, 56 pruebas ✓, build de producción ✓. No se compiló la APK.

---

## v1.4.18 — 2026-09-02 — `Corrección`

### Corrección de los 3 hallazgos de la auditoría de responsividad

- **Inputs a 16 px (F1):** `.input` pasa de `text-sm` a `text-base` en `src/index.css`,
  eliminando el zoom automático de iOS al enfocar cualquier campo del sistema.
- **Safe-area iOS (F3):** la navegación inferior añade `pb-[env(safe-area-inset-bottom)]`
  y `main` compensa con `pb-[calc(6rem+env(safe-area-inset-bottom))]`, de modo que el
  gesto de inicio del iPhone ya no puede tapar la fila de navegación.
- **Navegación inferior en pantallas angostas (F2):** los ítems bajan de
  `min-w-[78px]` a `min-w-[56px]` con `px-0.5`; los 6 ítems caben sin scroll a partir
  de 336 px y el comportamiento en teléfonos anchos no cambia (los ítems crecen con `flex-1`).
- Verificación determinista: inputs miden 16 px en runtime, `min-width: 56px`
  declarado en la nav, `main` resuelve 96 px de colchón inferior, sin overflow
  horizontal en ninguna vista y con el footer sticky del formulario de alquileres visible.
- Typecheck ✓, 56 pruebas ✓, build de producción ✓. No se compiló la APK.

---

## Auditoría — 2026-09-02 — `Auditoría`

### Auditoría determinista de responsividad (solo documentación, sin cambios de código)

- Auditoría en dos pasadas sobre la PWA: revisión estática de 13 reglas (breakpoints, overflow,
  truncado, modales, teclado numérico, tablas, off-canvas) y sweep runtime de las 6 vistas y
  modales a 485×868 px con datos sembrados (restaurados al terminar).
- Resultado: 7,5/10 — sin overflow horizontal medido en ninguna vista; 3 hallazgos de pulido
  móvil (inputs a 14 px → zoom iOS, bottom-nav con scroll a <380 px, sin safe-area iOS) y
  5 riesgos latentes (filtros sin memo, listas sin virtualización, bundle de 194 kB gzip,
  fila 12-col del formulario en pantallas pequeñas).
- Informe completo con matriz, mediciones y orden de corrección: `docs/AUDITORIA_RESPONSIVIDAD.md`.
- Verificación: typecheck ✓, 56 pruebas ✓, build de producción ✓. No se compiló la APK.

---

## v1.4.17 — 2026-08-26 — `Cambio`

### Logo exclusivo y colores claros en PDF

- Se añadió el nuevo recurso exclusivo para PDF `logo-pdf.png` / `logo_pdf.png`.
- La interfaz y el icono de instalación permanecen sin cambios.
- Los PDF web y Android ahora usan fondo blanco, tarjetas claras, texto oscuro y acentos azules.
- Se mantuvieron los datos del cliente, montos, pendiente, estado de pago, GPS y moneda dual.

---

## v1.4.16 — 2026-08-26 — `Cambio`

### Logo exclusivo y plantilla clara para PDF

- Se agregó `public/logo-pdf.png` y `android-app/app/src/main/res/drawable-nodpi/logo_pdf.png` usando el recurso suministrado.
- El logo de la interfaz y el icono de instalación permanecen intactos.
- Los PDF web y Android ahora usan fondo claro, tarjetas blancas, texto oscuro y acentos de marca.
- Se conservó el estado de pago, pendiente, GPS, moneda dual y datos del cliente.

---

## v1.4.15 — 2026-08-26 — `Nuevo`

### Autoupdate Android por GitHub

- Se activó el cliente de actualización contra `update.json` del repositorio `luiggiberaldi/el-spot-toldos`.
- Se añadió comprobación periódica cada 12 horas y búsqueda manual desde Configuración.
- Las descargas requieren HTTPS y validan SHA-256 cuando el manifiesto lo publica.
- La APK queda en almacenamiento privado y se instala mediante `FileProvider` y el instalador oficial.
- Si Android solicita autorización para instalar desde esta fuente, la descarga queda pendiente y el flujo se retoma al regresar.
- Las actualizaciones conservan Room, DataStore y los datos locales si mantienen firma, `applicationId`, `versionCode` creciente y migraciones.
- Se añadió `update.json` inicial con la versión instalada para evitar descargas hasta publicar una release nueva.

---

## v1.4.14 — 2026-08-26 — `Cambio`

### Navegación simplificada, documentación y canal de actualización

- Se ocultó Bitácora de la navegación y de las pantallas visibles de la PWA y la APK.
- Se conservaron los registros internos, la persistencia Room/Zustand y su exportación en respaldos.
- Se actualizó la documentación principal para reflejar los seis módulos visibles y el flujo actual de clientes, toldos, alquileres, recibos, GPS, tasa Bs, logo y eliminación controlada de recibos.
- Se corrigieron referencias históricas a correo electrónico y logo editable.
- Se documentó `https://github.com/luiggiberaldi/el-spot-toldos` como repositorio oficial para futuras releases y autoupdate de la APK.
- Se aclaró que el autoupdate Android requería implementar el verificador, publicar una APK release firmada y configurar el manifiesto de versiones.

---

## v1.4.13 — 2026-08-26 — `Nuevo`

### Eliminación controlada de recibos

- Se añadió una acción de papelera en cada recibo, con etiqueta accesible que incluye el folio.
- Antes de borrar se muestra una confirmación visual con el resumen de la acción y botones claros para cancelar o continuar.
- La eliminación quita únicamente el documento de recibo y registra el cambio en la bitácora.
- Se informa explícitamente que borrar el recibo no revierte automáticamente el abono registrado en el alquiler.
- Se verificó la aparición del botón y el flujo de cancelación en el Preview; el recibo permanece intacto al cancelar.
- Typecheck y 51 pruebas deterministas correctos.

---

## v1.4.12 — 2026-08-26 — `Mejora`

### Campos numéricos sin controles nativos innecesarios

- Se ocultaron las flechas de incremento/decremento del navegador en los campos numéricos.
- La tasa conserva validación numérica, teclado decimal en dispositivos móviles, edición manual y guardado con Enter.
- La presentación queda consistente entre navegadores y evita controles cuadrados que distraen del formulario.
- Se verificó el campo de tasa en el Preview con `type=number` y apariencia de texto, sin spinners visibles.
- Typecheck y 51 pruebas deterministas correctos.
- No se compiló la APK.

---

## v1.4.11 — 2026-08-26 — `Cambio`

### Acceso rápido para actualizar la tasa desde el Panel

- Se añadió una sección de acceso rápido en el inicio para editar la tasa manual de Bs por cada `$ 1`.
- El control reutiliza `actualizarConfig`, por lo que la tasa se conserva en la configuración persistida y se refleja inmediatamente en panel, alquileres y equivalentes.
- Se incorporaron validación de valores, guardado con Enter, confirmación inline y mensaje de error accesible.
- Se corrigió la presentación de la conversión actual para evitar repetir el prefijo `Bs`.
- El campo se sincroniza cuando la tasa cambia desde Configuración.
- Preview verificado con guardado de `36,50` y persistencia en `localStorage`.
- Typecheck y 51 pruebas deterministas correctos.
- No se compiló la APK.

---

## v1.4.10 — 2026-08-26 — `Cambio`

### Ajuste de tamaño y centrado del logo

- Se aumentó el tamaño del logo oficial en la barra lateral de la PWA.
- Se centró horizontal y verticalmente dentro de un bloque de marca más amplio.
- Se mantuvo el logo sin deformación mediante `object-contain`.
- La versión móvil conserva un tamaño compacto y centrado.
- No se modificó el icono de instalación de Android.
- Typecheck verificado y resultado visual confirmado en el Preview.

---

## v1.4.9 — 2026-08-26 — `Cambio`

### Nuevo logo oficial de EL SPOT TOLDOS

- Se reemplazó el logo visual del sistema por el archivo suministrado `tasas al dia (18).png`.
- La PWA, el PDF web, la interfaz Android y el PDF nativo utilizan ahora el nuevo recurso de marca.
- Se mantuvo intacto `elspot_logo.png` como icono de instalación y splash del teléfono.
- Se conservaron los headers oscuros y el logo centrado en los recibos PDF.
- Se verificó el nuevo logo en la interfaz y la generación del recibo desde el Preview.
- No se compiló la APK.

---

## v1.4.8 — 2026-08-26 — `Corrección`

### Corrección de layout y paleta oscura en recibos PDF

- Se corrigió el desbordamiento de direcciones y campos largos en las tarjetas del PDF web mediante alturas calculadas según el contenido real.
- Se evitó que la información del cliente o del servicio invada el título y la tabla de conceptos.
- Se aplicó una paleta oscura completa al PDF web y nativo: fondo profundo, superficies slate, texto claro, bordes azul acero y cian de marca.
- Se conservaron colores diferenciados para estados `PAGADO`, `POR PAGAR`, monto pendiente y equivalentes en Bs.
- La plantilla Android quedó alineada con la web y mantiene margen para datos extensos, tabla y resumen financiero, sin espacios de firma.
- Typecheck y 51 pruebas deterministas verificadas; la APK no fue compilada.

---

## v1.4.7 — 2026-08-26 — `Cambio`

### Mejora UX del flujo de emisión de recibos

- Se reemplazaron los desplegables de monto y estado por opciones visuales seleccionables con iconos.
- El modal ahora muestra el alquiler, el monto pendiente actual y el efecto de la operación antes de confirmar.
- Se incorporaron estados claros: `Se registrará`, `Quedará pendiente` y `Selecciona un monto`.
- Se bloquea la emisión cuando el monto es cero y se muestra una explicación inline.
- El concepto se actualiza automáticamente al elegir el tipo de cobro.
- El pie de acciones queda disponible al desplazarse por el modal, especialmente en pantallas pequeñas.
- Los botones incluyen estados accesibles mediante `aria-pressed`, foco visible y mensajes de alerta/estado.
- Se verificó el flujo en Preview seleccionando total y alternando entre Pagado y Por pagar.
- Typecheck y 51 pruebas deterministas continúan pasando.

---

## v1.4.6 — 2026-08-26 — `Cambio`

### Rediseño profesional de recibos PDF

- Se reorganizó el recibo en una composición comercial A4 con mejor jerarquía visual.
- El header oscuro conserva el logo centrado y los metadatos de folio, fecha y estado.
- Los datos del cliente y del servicio ahora se muestran en tarjetas separadas y fáciles de escanear.
- La tabla de conceptos incorpora encabezado oscuro, columnas alineadas y filas alternas.
- El monto a cancelar queda destacado en un bloque visual independiente junto al resumen de total, abono y pendiente de pago.
- Se mejoraron la separación de secciones, el bloque de GPS, la referencia del cliente y las líneas de firma.
- La plantilla nativa Android fue alineada visualmente con la plantilla web, manteniendo compartir, WhatsApp y el formato de una sola página.
- Se verificó la vista previa real del recibo sin solapamientos, junto con typecheck y 51 pruebas deterministas verdes.

---

## v1.4.5 — 2026-08-26 — `Cambio`

### Logo oficial y header de recibos

- Se incorporó el logo suministrado como marca visual fija del sistema en PWA y Android.
- El PDF ahora usa un header oscuro con el logo centrado y sin repetir el nombre como texto.
- El logo oficial se usa también como respaldo visual de recibos y en Configuración.
- Se mantuvo `elspot_logo.png` sin cambios como icono actual del teléfono y splash.
- Se eliminó el selector de logo personalizado para evitar que una marca anterior reemplace la identidad oficial.
- La nueva APK debug se compiló correctamente: `android-app/app/build/outputs/apk/debug/app-debug.apk` (18,98 MB).
- SHA-256 de la APK: `13AD005E9AC38F66DE867A028DDA7CEEF7C233662C6ECBD37E9D31DE5E4E5589`.
- PWA validada con typecheck, 51 pruebas y build de producción; Android validado con pruebas JVM, compilación instrumentada y lint.

---

## v1.4.4 — 2026-08-26 — `Cambio`

### Recibos, GPS y datos venezolanos

- Los recibos de PWA y APK distinguen explícitamente **Pagado** y **Por pagar**.
- `Pagado` registra el monto como abono y `Por pagar` conserva el monto pendiente.
- El PDF muestra logo, nombre comercial, cliente, estado de pago y usa el nombre del cliente en el archivo.
- La captura GPS intenta completar automáticamente la dirección legible; las coordenadas permanecen como respaldo.
- Cédulas/RIF y teléfonos se normalizan al formato venezolano (`V-12.345.678`, `J-123456789`, `0412-1234567`).
- Se eliminó el campo correo de la pantalla de clientes; los valores históricos se conservan solo para compatibilidad de respaldos.
- Android incorpora migración Room 2 -> 3 para el estado de pago de recibos existentes.
- Para notificar nuevas versiones de la APK se documentó el flujo recomendado: endpoint de versión, notificación persistente, descarga firmada y confirmación manual de instalación. La automatización requiere todavía un backend o servicio de distribución.
- La validación posterior quedó completada: PWA con 51 pruebas deterministas verdes, typecheck y build de producción; Android con pruebas JVM, compilación instrumentada, lint y `assembleDebug` exitosos.
- APK debug generada el 2026-08-26: `android-app/app/build/outputs/apk/debug/app-debug.apk`.
- SHA-256 de esta compilación: `2CB3370B93FD663D184D3E838DBFE22F644421DE9340AF7C8437EC4831762402`.

---

## v1.4.3 — 2026-08-26 — `Cambio`

### Identidad de la APK: EL SPOT TOLDOS

- Se reemplazó el ícono de fábrica por el logo cuadrado suministrado (`elspot_logo.png`).
- El launcher, el ícono redondo y el splash de Android usan ahora el logo oficial.
- El nombre que aparece instalado en el teléfono cambió a **EL SPOT TOLDOS**.
- Se eliminó el texto **CONCEPT STORE** de la marca visible, el encabezado de navegación y el PDF de recibos.
- La documentación y el respaldo visual de recibos quedan alineados con la nueva identidad.

---

## v1.4.1 — 2026-08-26 — `Corrección`

### Cierre de validación de la APK

- Se corrigieron recursos de tema API 27/31 y cargas asíncronas Compose detectadas por lint.
- `lintDebug` queda exitoso; permanecen únicamente advertencias de dependencias, orientación
  y APIs de iconos deprecadas, sin errores bloqueantes.
- Se confirmó `assembleDebug` con APK en `android-app/app/build/outputs/apk/debug/app-debug.apk`.
- Se confirmó `testDebugUnitTest` con 12 pruebas verdes y la compilación de la prueba Compose
  instrumentada. No se ejecutó en dispositivo porque ADB no reportó emuladores ni teléfonos.
- Se mantuvo el permiso mínimo: ubicación y notificaciones; se retiró `SCHEDULE_EXACT_ALARM`
  porque WorkManager no requiere alarmas exactas.

---

## v1.4.2 — 2026-08-26 — `Corrección`

### Auditoría E2E, integridad de inventario y UX operativa

- La PWA ahora valida alquileres en la store antes de persistirlos: cliente existente,
  líneas completas sin duplicados, GPS válido, total coherente, abono dentro de límites
  y disponibilidad física por unidades.
- Se añadió `unidades` al inventario con migración lógica de respaldos antiguos (1 unidad
  por defecto); el panel, formularios y tarjetas muestran unidades reales disponibles.
- La emisión de recibos y el registro del abono se realizan en una única actualización,
  evitando dobles cargos y congelando en el snapshot el saldo posterior al pago.
- Se añadió bitácora persistente en la PWA, visible como módulo propio y exportable en
  respaldos v2; los respaldos v1 siguen siendo aceptados.
- Se bloquearon transiciones inválidas de alquileres cerrados y se agregaron mensajes
  inline para que errores de inventario no cierren los formularios.
- Se agregaron 13 pruebas deterministas nuevas de validación y respaldos: la PWA queda
  con **49 pruebas verdes**.
- Se restauró y verificó la pantalla nativa de alquileres tras la auditoría; la APK pasa
  **12 pruebas JVM**, compilación de prueba instrumentada, `lintDebug` y `assembleDebug`.
- `build` de producción PWA correcto. La prueba instrumentada requiere emulador/teléfono
  ADB conectado; no se ejecutó físicamente en este entorno.

---

## v1.4.0 — 2026-08-25 — `Nuevo`

### APK nativa funcional de EL SPOT

- Se implemento `android-app/` con Kotlin, Jetpack Compose y Material 3 en tema oscuro.
- Se agrego persistencia local con Room y DataStore: clientes, inventario por unidades,
  alquileres 12h/24h, GPS, pagos, recibos snapshot, bitacora y configuracion.
- Se agregaron reglas de dominio para bloquear doble reserva, toldos en reparacion o
  retirados, abonos superiores al monto pendiente y coordenadas GPS invalidas.
- Se implemento migracion Room 1 -> 2 para unidades de inventario y migracion JSON
  bidireccional con el formato de la PWA, incluyendo fechas ISO locales y recibos legacy.
- Se implementaron PDF nativo con logo configurable, moneda `$` + Bs a tasa congelada,
  enlace GPS, FileProvider/Sharesheet, fallback de WhatsApp y mensaje profesional.
- Se agregaron recordatorios de devolucion con WorkManager, canales de notificacion,
  permiso Android 13+ y cancelacion al devolver, cancelar, importar, restablecer o apagar.
- Se agregaron 12 pruebas JVM de reglas, dinero y snapshots, mas una prueba Compose de
  arranque. La compilacion Kotlin y `testDebugUnitTest` quedan verificadas.
- Documentacion nativa: `android-app/README.md`; el catalogo `awesome-android-ui` se
  conserva como referencia, sin incorporar dependencias antiguas al APK.

---

## v1.2.0 — 2026-08-25 — `Cambio`

### Modalidad 12h/24h, moneda dual ($ + Bs), paleta oscura y rebranding EL SPOT

- **Modalidad de alquiler por 12h o 24h** — el alquiler ya no usa fechas ni tiempo de
  uso: se elige **modalidad 12 horas (mitad de tarifa)** o **24 horas (tarifa completa)**.
  - `ModalidadAlquiler` en el modelo; alquileres antiguos sin modalidad se normalizan a 24h.
  - Nueva función `calcularMontoModalidad`/`tarifaEfectiva` (factor 0,5 para 12h) con pruebas.
  - El formulario muestra selector de modalidad, subtotal base y total recalcular al instante.
  - Lista, detalle, panel y PDF muestran la modalidad y la tarifa efectiva.
- **Moneda principal $ y secundaria Bs a tasa manual** —
  - `Config.tasaBs`: tasa de cambio manual (Bs por 1 $) configurable en Configuración
    (reemplaza al selector de moneda). La moneda principal pasa a ser siempre `$`.
  - Nuevas funciones `formatearMontoDual` y `formatearBsEquivalente` (con pruebas).
  - Montos en Panel, Toldos, Alquileres, Recibos y WhatsApp muestran `$ X (Bs Y)`.
  - El PDF del recibo muestra el equivalente en Bs junto al MONTO A CANCELAR con la tasa
    usada (congelada en el snapshot del recibo).
- **Paleta oscura profesional** — fondo `#0b1018` con resplandores teal sutiles, tarjetas
  y modales en superficies oscuras (slate), inputs oscuros, etiquetas de estado con
  variantes translúcidas, barra lateral y navegación móvil en negro con acentos teal.
- **Rebranding "EL SPOT"** — marca del sistema en la barra lateral y el encabezado móvil
  (réplica estilizada del logo: bloque negro + tipografía condensada "EL SPOT" con
  subtítulo "CONCEPT STORE"), título de pestaña, manifest de la PWA, README y docs.
- Se reemplazó el emoji ✅ de confirmación por el icono `CheckCircle2` de lucide.

---

## v1.2.1 — 2026-08-25 — `Cambio`

### Ajuste de paleta: azul acero y cian sobre fondo oscuro

- Se reemplazaron los acentos teal/verde por una paleta azul acero y cian frío,
  más compatible con la identidad oscura de EL SPOT.
- Se actualizaron botones, navegación, dropdowns, modal, tarjetas, estados,
  confirmaciones y equivalentes visuales en los módulos.
- Se corrigieron clases de fondo duplicadas en Alquileres.
- El PDF ahora usa el mismo color de marca azul acero en encabezados y tabla.
- Se conservaron ámbar para alertas y rojo para acciones destructivas.

---

## v1.3.0 — 2026-08-25 — `Cambio`

### Referencia UI para la futura APK de EL SPOT

- Se clono `https://github.com/wasabeef/awesome-android-ui` en
  `references/awesome-android-ui`, fijado al commit
  `312f9be3c50b3ea33ff3ba7eb9aa1c21b52de8b2`.
- Se audito el catalogo y se documentaron patrones utiles para navegacion,
  bottom sheets, listas, formularios, calendarios, graficos y microinteracciones.
- La futura APK usara Kotlin, Jetpack Compose y Material 3; el repositorio se
  conserva como referencia visual y no se incorporan sus librerias antiguas sin
  validar mantenimiento, compatibilidad y licencia.
- Se definio el plan tecnico para notificaciones con WorkManager y envio seguro
  de recibos mediante Sharesheet/FileProvider y WhatsApp.
- Nueva documentacion: `docs/APK_UI_REFERENCIA.md`.

---

## v1.0.0 — 2026-08-25 — `Nuevo`

### Primera versión completa del sistema

Entrega inicial funcional, estructurada por módulos y documentada.

**Cambios incluidos en esta versión (resumen del desarrollo):**

- **v0.1.0 · Base del proyecto** — `Nuevo`
  - Creación del proyecto con Vite + React + TypeScript + Tailwind CSS.
  - Configuración de PWA (instalable, sin conexión) con `vite-plugin-pwa`.
  - Estructura de carpetas por módulos, componentes compartidos (Layout, Modal, Campos, Etiquetas).
  - Iconos de la aplicación generados por script (`scripts/generar-iconos.mjs`).

- **v0.2.0 · Capa de datos** — `Nuevo`
  - Modelos de datos del dominio (`src/types/modelos.ts`): Cliente, Toldo, Alquiler, Recibo, Configuración.
  - Tienda central con Zustand y persistencia automática en `localStorage` (`src/data/store.ts`).
  - Funciones puras de cálculo (montos, pendientes de pago, días), formato (moneda, fechas) y folios correlativos.
  - Exportación e importación de respaldos en JSON (`src/data/respaldo.ts`).
  - Pruebas unitarias con Vitest (19 casos en la primera tanda).

- **v0.3.0 · Configuración y Panel** — `Nuevo`
  - Módulo de configuración del negocio (nombre, RIF, teléfono, dirección, logo, moneda).
  - Panel de control con tarjetas de resumen y lista de alquileres recientes.

- **v0.4.0 · Módulo Clientes** — `Nuevo`
  - Registro, búsqueda, edición y eliminación de clientes con validaciones.

- **v0.5.0 · Módulo Toldos** — `Nuevo`
  - Inventario de toldos con tamaño, tarifa y estados (disponible, alquilado, en reparación, retirado).

- **v0.6.0 · Módulo Alquileres** — `Nuevo`
  - Creación y edición de alquileres con múltiples toldos, fechas, tiempo de uso y montos.
  - Captura de coordenadas GPS con la geolocalización del navegador y enlace al mapa.
  - Folios correlativos de alquiler (ALQ-0001…), estados del ciclo de vida y vista de detalle.

- **v0.7.0 · Módulo Recibos** — `Nuevo`
  - Emisión de recibos con folio correlativo automático (REC-0001…).
  - Datos congelados del alquiler (snapshot) para que el PDF no cambie al editar.
  - Generación del recibo en PDF con jsPDF (logo, cliente, tiempo de uso, GPS, montos, firmas).
  - Vista previa del PDF, descarga, envío por WhatsApp y compartir con la API nativa.
  - Registro opcional del pago como abono del alquiler al emitir el recibo.

## v1.1.0 — 2026-08-25 — `Cambio`

### Mejora visual integral (design system + iconos profesionales)

- **Iconos profesionales SVG (lucide-react)**: se eliminaron todos los emojis de la interfaz
  (navegación, tarjetas del panel, botones de recibos, configuración y alquileres) y se
  reemplazaron por iconos de Lucide, consistentes, nítidos y vectoriales.
- **Selects sin el dropdown nativo cuadrado**: se creó `SelectPersonalizado`
  (combobox propio con panel flotante redondeado, sombra, chevron animado, check en la
  opción activa, soporte de teclado y cierre al hacer clic fuera). Se aplicó en
  formularios (`CampoSelect`) y filtros (`SelectFiltro`).
- **Gradientes y acabado profesional**: fondo con degradado suave, sidebar con gradiente
  teal→emerald, botones primarios/peligro con gradiente y sombra, tarjetas con sombras
  suaves y esquinas redondeadas, modales con barra de gradiente superior y animaciones
  de entrada (dropdown, modal, fondo).
- **Detalle**: clase `btn-icono` para botones de acción compactos (Recibos), escala de
  color `marca` completada (200-400) en Tailwind, botón "Nuevo" con icono `Plus` en
  todos los módulos.
- Se eliminó `src/components/Icono.tsx` (re-export innecesario; los componentes importan
  de lucide-react directamente).

## v1.1.1 — 2026-08-25 — `Corrección`

### Auditoría e2e: bugs corregidos y mejoras

- **Bug: abono duplicado al emitir recibo** — al emitir un recibo con tipo "Abono ya
  recibido" y la opción "Registrar como abono" activa, el abono se sumaba dos veces
  (100 + 100 = 200 cuando ya estaba en 100). Ahora, si el recibo es por el abono ya
  registrado, no se suma de nuevo.
- **Bug: logo en PDF** — `pdf.ts` siempre incrustaba el logo como `PNG` aunque fuera
  JPEG o WebP, lo que podía romper o distorsionar el PDF. Ahora se detecta el formato
  real (PNG/JPEG) y los WebP se convierten a PNG vía canvas antes de incrustar.
- **Mejora: sincronización de estados del inventario** — nueva función pura
  `calcularEstadosToldos` (`src/lib/estados.ts`): al crear/editar/eliminar un alquiler,
  los toldos ocupados pasan automáticamente a "Alquilado" y vuelven a "Disponible" al
  devolverse o cancelarse. Respeta los estados manuales "En reparación" y "Retirado".
  Con 5 pruebas unitarias nuevas (total 26).
- **Mejora: validación de fechas** — el formulario de alquiler ahora rechaza guardar
  con fecha de fin anterior a la de inicio.
- **Mejora: autocompletado de tiempo de uso** — si el usuario escribe el tiempo a mano,
  deja de sobrescribirse al cambiar las fechas.
- **Mejora: normalización de respaldos** — al restaurar un respaldo antiguo sin
  `ultimoFolioAlquiler`, se normaliza a 0 (evita folios `ALQ-NaN`) y se resincronizan
  los estados de los toldos.
- **Limpieza**: se eliminó código muerto (interfaz `DimensionesLogo` en pdf.ts, variable
  sin uso en store.ts).

## Correcciones aplicadas durante el desarrollo

| Fecha | Tipo | Descripción |
| --- | --- | --- |
| 2026-08-25 | `Corrección` | Agregado el campo `ultimoFolioAlquiler` al modelo `Config` (faltaba para la correlatividad de alquileres). |
| 2026-08-25 | `Corrección` | Corregido error de tipos en el módulo de alquileres (selectores de tienda sin uso y `moneda` no declarada en `FormularioAlquiler`). |
| 2026-08-25 | `Corrección` | Fechas se mostraban un día antes (28/08 → 27/08): JavaScript interpreta `"YYYY-MM-DD"` como medianoche UTC. Se fuerza la hora local en `parsearFecha` y se agregaron pruebas. |
| 2026-08-25 | `Cambio` | Reemplazado el `alert()` bloqueante tras emitir un recibo por una pantalla de confirmación integrada en el modal, con acceso directo al módulo de Recibos. |

---

## Cómo actualizar esta bitácora

Cada vez que se haga un cambio al sistema:

1. Crear una entrada nueva **arriba** con la fecha y la nueva versión (subir la menor:
   `1.0.0` → `1.1.0` para cambios, `1.0.1` para correcciones).
2. Indicar el **tipo** (`Nuevo`, `Cambio` o `Corrección`) y describir qué se hizo y por qué.
3. Si son varios cambios, agruparlos bajo la misma versión.
