<p align="center">
  <img src="fastlane/metadata/android/en-US/images/icon.png" width="128" height="128" alt="Logotipo de ¿Por qué estoy aquí?">
</p>

# ¿Por qué estoy aquí? — Android

[English](README.md) · [Español](README.es.md) · [Русский](README.ru.md)

Un pequeño acompañante sin conexión para usar tus aplicaciones con intención.
Antes de abrir una aplicación que hayas elegido, escribe qué quieres hacer y
cuánto tiempo necesitas. Un widget mantiene visible tu objetivo; cuando se acaba
el tiempo, vuelves a la pantalla de inicio.

**[Descargar el último APK](https://github.com/hlophlopgaming/whyiamhereAndroid/releases/latest)** · [Licencia MIT](LICENSE)


## Capturas de pantalla

| Ajustes | Objetivo para YouTube | Recordatorio flotante |
| --- | --- | --- |
| <a href="fastlane/metadata/android/en-US/images/phoneScreenshots/1.jpg"><img src="fastlane/metadata/android/en-US/images/phoneScreenshots/1.jpg" width="220" alt="Ajustes"></a> | <a href="fastlane/metadata/android/en-US/images/phoneScreenshots/2.jpg"><img src="fastlane/metadata/android/en-US/images/phoneScreenshots/2.jpg" width="220" alt="Objetivo para YouTube"></a> | <a href="fastlane/metadata/android/en-US/images/phoneScreenshots/3.jpg"><img src="fastlane/metadata/android/en-US/images/phoneScreenshots/3.jpg" width="220" alt="Recordatorio flotante"></a> |

## Funciones

- Selección de aplicaciones instaladas con búsqueda e iconos.
- Objetivo y duración de 5, 15, 30 minutos o un valor personalizado.
- Widget móvil que puedes contraer; opción de terminar antes la sesión.
- Temporizadores independientes, incluso mientras usas otra aplicación o apagas la pantalla.
- Aviso configurable antes de terminar; aviso visual en modo silencio.
- Objetivos recientes, historial local y recuperación de sesiones tras reiniciar.
- Inglés, español y ruso según el idioma de Android. Para otros idiomas se utiliza inglés. Tus propios objetivos no se traducen.

**Android 8.0 o posterior · Sin permiso de internet · Sin root · Sin cuenta · Sin permiso “Mostrar sobre otras aplicaciones”.**

## Instalación

1. Descarga el `.apk` desde [Releases](https://github.com/hlophlopgaming/whyiamhereAndroid/releases).
2. Ábrelo en el teléfono y permite la instalación desde ese origen si Android lo solicita.
3. Abre **¿Por qué estoy aquí?**, elige aplicaciones y pulsa **Abrir ajustes del servicio**.
4. Activa el servicio de accesibilidad y comprueba el estado **En funcionamiento**.

En Android 13 o posterior, puede ser necesario abrir **Información de la aplicación → ⋮ → Permitir ajustes restringidos** antes de activar el servicio. Los nombres de los menús varían según el fabricante. Si el servicio se detiene, permite el inicio automático y elimina las restricciones de batería.

**Para pasar del APK de prueba 0.2.0 a 0.3.0:** desinstala primero la versión
anterior. Se borrarán sus datos locales. La versión 0.3.0 introduce la firma
permanente; las siguientes versiones usarán la misma clave y se podrán actualizar normalmente.

## Privacidad y limitaciones

Los ajustes, objetivos y sesiones se quedan en el almacenamiento de la aplicación
en tu teléfono. Las copias de seguridad y la transferencia entre dispositivos
están desactivadas. La desinstalación elimina los datos. El servicio consulta el
paquete de la ventana activa; no lee textos, contraseñas ni mensajes de pantalla.
No solicita permiso de internet.

Es un recordatorio voluntario: puedes desactivar el servicio, y la aplicación
puede aparecer brevemente antes del formulario. Volver al inicio no detiene
el audio ni el vídeo en segundo plano. Cambiar el reloj del teléfono cambia el
temporizador. Los avisos y el widget se actualizan mientras la aplicación
seleccionada está visible; los vencimientos en segundo plano se gestionan al
volver. Reconectar el servicio puede repetir un aviso sonoro. En pantalla dividida
y PiP se sigue la ventana enfocada. Las restricciones de batería de cada
fabricante necesitan comprobaciones en tu dispositivo.

## Compilación y pruebas

Kotlin, Jetpack Compose Material 3, AccessibilityService, Room y Preferences
DataStore. minSdk 26, compile/target SDK 37.

Instala JDK 17, Android SDK Platform 37.0 y Build Tools 36.0.0. Configura
`JAVA_HOME` y `ANDROID_HOME`, o utiliza Android Studio y un `sdk.dir` local.

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug
python3 scripts/check-locales.py
```

APK de prueba: `app/build/outputs/apk/debug/app-debug.apk`.
Las pruebas unitarias cubren sesiones, vencimiento, temporizadores independientes,
restauración, filtro de paquetes, avisos y formato del tiempo. Las pruebas para
dispositivos cubren el historial de Room y la selección de idiomas, incluidas
las variantes regionales del español y el inglés como idioma de respaldo:

```sh
./gradlew connectedDebugAndroidTest
adb logcat -s WhyService
```

El servicio utiliza `canRetrieveWindowContent=true` para obtener el paquete de
la ventana activa mediante la API de Android. No recorre nodos de texto ni toma
capturas. [Documentación de Android](https://developer.android.com/reference/android/accessibilityservice/AccessibilityServiceInfo).

## Publicación

Consulta las [instrucciones de publicación](docs/RELEASING.md). El script comprueba
las traducciones, ejecuta las pruebas y lint, compila un APK optimizado y firmado
y genera su suma SHA-256. Al publicar, crea una etiqueta de versión y comprueba
el APK subido antes de hacer pública la versión en GitHub.

```sh
scripts/release.sh 0.3.0 --prepare
scripts/release.sh 0.3.0 --publish
```

Guarda la clave permanente y sus contraseñas fuera de Git y haz una copia de
seguridad. Las compilaciones de lanzamiento utilizan R8 y reducción de recursos.

## Comprobaciones manuales

Comprueba la apertura desde el lanzador, aplicaciones recientes y notificaciones;
la cancelación del formulario; mover y contraer el widget; temporizadores
independientes; vencimiento en pantalla y en segundo plano; finalización anticipada;
modo silencio; bloqueo de pantalla; reinicio; cambio del idioma de Android;
limpieza del historial; y desactivación y reconexión del servicio. Comprueba varias
versiones de firmware antes de asumir un comportamiento idéntico.
Las gráficas, horarios, exportación de datos y Google Play quedan fuera de este MVP.
