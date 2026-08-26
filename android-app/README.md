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
4. Actualizar `update.json` en la rama `main` con `versionCode`, `versionName`, URL HTTPS del asset, SHA-256, notas y si la actualización es obligatoria.
5. La APK consulta el manifiesto, compara `versionCode`, descarga y verifica la APK, y muestra una notificación.
6. El usuario confirma la instalación en el instalador de Android.

La APK consulta `update.json` cada 12 horas y permite buscar manualmente desde Configuración. Si encuentra un `versionCode` mayor, descarga por HTTPS, valida SHA-256, notifica al usuario y abre el instalador con `FileProvider`. Para preservar datos, toda versión debe conservar `applicationId = com.elspot.toldos`, la misma firma, un `versionCode` mayor y migraciones Room válidas.

## WhatsApp

La APK utiliza Sharesheet y FileProvider para compartir el PDF sin exponer rutas privadas. El usuario confirma el contacto y el envío. El envío automático sin interacción requiere WhatsApp Business Cloud API, backend, autenticación, plantillas y webhooks.

## Referencia UI

`../references/awesome-android-ui` se conserva como catálogo de patrones. No se incorporan automáticamente sus dependencias antiguas; la APK usa componentes modernos de Jetpack Compose y Material 3.
