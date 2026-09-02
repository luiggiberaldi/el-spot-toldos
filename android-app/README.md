# EL SPOT TOLDOS para Android

`android-app/` contiene la APK nativa de **EL SPOT TOLDOS**. La PWA se encuentra en la raíz del proyecto. Ambas plataformas comparten reglas de negocio y formato de respaldo, pero mantienen los datos localmente por separado.

## Requisitos

- Android Studio reciente o JDK 17.
- Android SDK con plataforma 35.
- Teléfono o emulador para validar GPS, notificaciones, permisos y compartir.
- Gradle 9.3.1 mediante Android Studio o la distribución local configurada.

## Compilar y verificar

Desde Android Studio, abre `android-app/` y ejecuta `app > Tasks > build > assembleDebug`. El APK se genera en `app/build/outputs/apk/debug/app-debug.apk`.

En Windows, ajusta las rutas si es necesario:

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
& "$env:USERPROFILE\.gradle\wrapper\dists\gradle-9.3.1-bin\23ovyewtku6u96viwx3xl3oks\gradle-9.3.1\bin\gradle.bat" :app:assembleDebug
```

Comandos de calidad:

```powershell
& "$env:USERPROFILE\.gradle\wrapper\dists\gradle-9.3.1-bin\23ovyewtku6u96viwx3xl3oks\gradle-9.3.1\bin\gradle.bat" :app:compileDebugKotlin
& "$env:USERPROFILE\.gradle\wrapper\dists\gradle-9.3.1-bin\23ovyewtku6u96viwx3xl3oks\gradle-9.3.1\bin\gradle.bat" :app:testDebugUnitTest
& "$env:USERPROFILE\.gradle\wrapper\dists\gradle-9.3.1-bin\23ovyewtku6u96viwx3xl3oks\gradle-9.3.1\bin\gradle.bat" :app:lintDebug
& "$env:USERPROFILE\.gradle\wrapper\dists\gradle-9.3.1-bin\23ovyewtku6u96viwx3xl3oks\gradle-9.3.1\bin\gradle.bat" :app:compileDebugAndroidTestKotlin
& "$env:USERPROFILE\.gradle\wrapper\dists\gradle-9.3.1-bin\23ovyewtku6u96viwx3xl3oks\gradle-9.3.1\bin\gradle.bat" :app:connectedDebugAndroidTest
```

`connectedDebugAndroidTest` requiere un dispositivo o emulador conectado. Sin él solo puede validarse la compilación de la prueba instrumentada.

## Navegación visible

El drawer de la APK muestra seis destinos:

- Panel
- Clientes
- Toldos
- Alquileres
- Recibos
- Configuración

La Bitácora fue retirada de la navegación y no tiene pantalla visible. Sus registros siguen almacenándose en Room y exportándose en los respaldos para preservar trazabilidad.

## Funciones

- Alquileres de 12 y 24 horas; 12 horas aplica la mitad de la tarifa base.
- Inventario por unidades, con bloqueo de sobreasignación y estados operativos.
- Dirección manual y captura GPS con enlace al mapa.
- Moneda principal `$` y equivalente Bs mediante tasa manual.
- Recibos PDF con logo oficial, header oscuro, snapshot, estado Pagado/Por pagar, sin espacios de firma, compartir y WhatsApp.
- Recordatorios de devolución mediante WorkManager.
- Respaldos JSON compatibles con la PWA.
- Formato venezolano para cédulas/RIF y teléfonos.

## Identidad visual

El nombre instalado es **EL SPOT TOLDOS**. `elspot_logo.png` es el icono del teléfono y splash. `marca_elspot.png` es el logo interno de la app. `logo_pdf.png` es exclusivo de los recibos PDF. La app usa fondo oscuro; los recibos PDF usan una plantilla clara con superficies blancas, azul de marca y texto oscuro.

## Persistencia y migraciones

- Room conserva clientes, toldos, alquileres, líneas, recibos y auditoría interna.
- DataStore conserva configuración, tasa, folios y recordatorios.
- Las migraciones Room deben mantenerse consecutivas para conservar datos.
- No se debe borrar la base de datos ni desinstalar para una actualización normal.
- Los respaldos contienen también registros internos, aunque no se muestran en la UI.

## Canal oficial de actualización

Repositorio de distribución: [github.com/luiggiberaldi/el-spot-toldos](https://github.com/luiggiberaldi/el-spot-toldos).

El autoupdate integrado usa GitHub Releases:

1. Incrementar `versionCode` y `versionName` en `android-app/app/build.gradle.kts`.
2. Generar una APK release firmada con la misma keystore de producción.
3. Crear una GitHub Release con tag semántico, por ejemplo `v1.1.0`, y subir el archivo APK.
4. Actualizar `update.json` en la rama `main` con `versionCode`, `versionName`, URL HTTPS del asset, SHA-256, notas y si la actualización es obligatoria (ahora lo hace el CI automáticamente tras publicar — ver "Release automática (CI)" abajo; las notas de la versión se redactan en el commit del bump).
5. La APK consulta el manifiesto, compara `versionCode`, descarga y verifica la APK, y muestra una notificación.
6. El usuario confirma la instalación en el instalador de Android.

La APK consulta `update.json` cada 12 horas y permite buscar manualmente desde Configuración. Si encuentra un `versionCode` mayor, descarga por HTTPS, valida SHA-256, notifica al usuario y abre el instalador con `FileProvider`. Para preservar datos, toda versión debe conservar `applicationId = com.elspot.toldos`, la misma firma, un `versionCode` mayor y migraciones Room válidas.

### Verificar que una actualización no borre datos

La instalación usa el instalador oficial de Android (misma `applicationId`), así que el proceso es una **actualización en sitio**: Room (`elspot.db`), DataStore, clientes, toldos, alquileres, recibos y configuración se conservan. No hay `uninstallApp`, no se llama `deleteDatabase` durante la actualización y `clearAll` solo existe detrás del botón **Restablecer datos** de Ajustes. La compilación con el mismo `versionCode` decide la copia de seguridad automática (Android conserva los datos previos cuando el `versionCode` sube).

Comprobación manual en un dispositivo (APK sobre APK):

1. Registrar al menos un cliente y un alquiler en la versión instalada.
2. Comprobar `adb shell pm dump com.elspot.toldos | findstr versionCode` o, sin ADB, generar la segunda APK y subir su `versionCode`.
3. Instalar la APK nueva sobre la anterior (no desinstalar).
4. Abrir la app y confirmar que los datos siguen presentes.

**Importante (firma y debug):** una APK `debug` se firma con `~/.android/debug.keystore`, que es estable en una misma máquina; por eso la actualización **debug→debug** conserva datos. En cambio, **cambiar a una keystore distinta (p. ej. publicar una `release` firmada con otra llave) provoca que Android fuerce el desinstalado y borre los datos**, o rechace la instalación (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`). Para migrar de debug a una release de producción con datos reales: exportar el respaldo en la APK actual, desinstalar, instalar la release firmada y restaurar el respaldo. Toda release posterior debe usar la misma keystore de producción.

## Release automática (CI)

`.github/workflows/android-release.yml` compila y publica la APK release automáticamente: **se dispara cuando un push a `main` modifica `android-app/app/build.gradle.kts`** (es decir, cuando sube el `versionCode`/`versionName`) y también admite ejecución manual (`workflow_dispatch`).

Comportamiento:

- **Puerta de versión**: `scripts/version_utils.mjs debe-publicar` compara el `versionName` del build contra el tag `vX.Y.Z` más alto publicado; solo continúa si es mayor. Pushes que tocan el build sin subir versión (o commits anteriores ya publicados) terminan en "omitir" sin error.
- **Compilación reproducible**: usa el **Gradle wrapper** (`android-app/gradlew`, Gradle 9.3.1, JDK 17), así el CI y las máquinas locales compilan con la misma versión exacta.
- **Firma**: el CI decodifica el secret `RELEASE_KEYSTORE_BASE64` (keystore completa en Base64) a `~/.android/debug.keystore` y compila con el mismo certificado que la app instalada — **la actualización en sitio conserva los datos**. `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS` y `RELEASE_KEY_PASSWORD` son opcionales y por defecto usan `android` / `androiddebugkey` / `android` (el keystore de depuración de este equipo).
- **Verificación**: el flujo corre `assembleRelease` + `testDebugUnitTest`, valida la firma con `apksigner verify`, sube la APK como artefacto (30 días) y publica la GitHub Release `v<versión>` con el asset `el-spot-toldos-<versión>.apk`.
- **`update.json` lo actualiza el CI**: tras publicar, el flujo reescribe `versionCode`, `versionName`, `apkUrl` y `sha256` del manifiesto con el hash **del asset que acaba de publicar** y lo sube a `main` como `github-actions[bot]`. Conserva `notes` y `mandatory` tal como estén en el commit del bump — solo hay que redactar las notas de la versión ahí. (El empaquetado APK no es byte-reproducible entre entornos, así que el hash válido para el autoupdate solo se conoce después de publicar; nunca se anuncia el hash de un build local.)

Configuración única del secret (una sola vez, con la cuenta propietaria):

```bash
base64 -w0 ~/.android/debug.keystore | gh secret set RELEASE_KEYSTORE_BASE64 \
  --repo luiggiberaldi/el-spot-toldos
```

En Windows (Git Bash) la ruta del keystore es `%USERPROFILE%\.android\debug.keystore`. Sin ese secret, el flujo falla a propósito con instrucciones en el log (nunca publica una APK sin firmar con el certificado correcto).

## WhatsApp

La APK utiliza Sharesheet y FileProvider para compartir el PDF sin exponer rutas privadas. El usuario confirma el contacto y el envío. El envío automático sin interacción requiere WhatsApp Business Cloud API, backend, autenticación, plantillas y webhooks.

## Referencia UI

`../references/awesome-android-ui` se conserva como catálogo de patrones. No se incorporan automáticamente sus dependencias antiguas; la APK usa componentes modernos de Jetpack Compose y Material 3.
