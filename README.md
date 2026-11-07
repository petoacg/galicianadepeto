# Galiciana de Peto para Android

Aplicación independiente para buscar y consultar la hemeroteca de Galiciana, Biblioteca Dixital de Galicia. Esta primera beta incluye búsqueda OCR, visor con resaltado, favoritos, carpetas, notas de referencia y copia de los datos de investigación.

## Compilar

Abre la carpeta del repositorio en Android Studio con JDK 17 y Android SDK 35. La aplicación requiere Android 8.0 (API 26) o posterior. Desde la terminal:

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

La primera compilación necesita acceso a los repositorios de dependencias. El APK de desarrollo queda en `app/build/outputs/apk/debug/`.

## Alcance de la beta

La consulta de documentos requiere conexión y depende del servicio de Galiciana. Las notas y los guardados se almacenan en el dispositivo. La copia ZIP de la app conserva los datos de investigación. Queda pendiente ampliar las pruebas de accesibilidad, pantallas grandes y versiones antiguas de Android.

## Licencia y contacto

Código original © 2026 **petocg**, bajo [CC BY-NC 4.0](LICENSE). Contacto: **petoacg@gmail.com**. Los componentes de terceros conservan sus licencias. Los documentos y datos de Galiciana se rigen por los derechos y condiciones de cada recurso. Consulta el aviso legal dentro de la app.
