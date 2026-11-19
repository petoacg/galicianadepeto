# Galiciana de Peto para Android

Galiciana de Peto es una aplicación independiente para consultar la hemeroteca digital de Galiciana, la Biblioteca Dixital de Galicia, desde Android. Permite buscar en el texto OCR de las publicaciones, abrir los facsímiles con las coincidencias resaltadas y organizar los hallazgos en páginas guardadas, carpetas y notas de referencia.

- **Buscar:** en el texto de las páginas o en el catálogo, con filtros de publicación, autor, lugar, período y tema. Los resultados se agrupan por obra y separan coincidencias exactas, variantes de OCR y coincidencias más amplias. Las búsquedas se pueden guardar y retomar, y se pueden guardar varias páginas a la vez.
- **Leer:** visor con zoom, paso de página con el dedo, pantalla completa, coincidencias resaltadas y un halo que señala la activa, lista de páginas del documento donde aparece la búsqueda, texto OCR para leer y copiar, y descarga en JPEG o PDF.
- **Explorar:** hemeroteca con índice A–Z, años y números; colecciones por tipo de documento; materias, personas y lugares del catálogo.
- **Guardados:** páginas guardadas con la vieira, carpetas, notas de referencia, búsqueda en tus hallazgos y exportación de referencias.
- **Tus datos:** se guardan en el teléfono, sin cuentas. Se pueden exportar e importar con una copia ZIP.
- **Idioma:** gallego, con español a elección desde Acerca de.

La consulta de documentos necesita conexión. Los derechos de cada documento figuran en su ficha original. También hay una [versión web](https://petoacg.github.io/galicianadepeto-web/) que funciona en iPhone, Android y ordenador.

## Instalar en Android, paso a paso

La app no está en Google Play: se instala con un archivo **APK** que se descarga de esta página. Hace falta **Android 8.0 o posterior**. Los nombres de los menús son los que aparecen con el teléfono en español y pueden cambiar un poco según la marca (Samsung, Xiaomi, Motorola…).

### 1. Descargar el archivo correcto

1. Abre la [última versión](https://github.com/petoacg/galicianadepeto/releases/latest) en el navegador del móvil.
2. Baja hasta el apartado **Assets** (al final de la publicación). Si está cerrado, tócalo para abrirlo.
3. Toca el archivo **`galiciana-de-peto-X.X.X.apk`** (el que termina en **.apk**). No descargues los que se llaman **Source code**: son el código, no la app.
4. Si el navegador avisa de que el archivo «puede ser dañino», toca **Descargar de todos modos**. Es el aviso normal para cualquier APK descargado fuera de Google Play.

### 2. Abrir el archivo descargado

Elige la forma que te resulte más cómoda:

- **Desde el navegador:** cuando termine la descarga, toca **Abrir** en el aviso que aparece abajo. Si ya se cerró, en Chrome toca el menú **⋮** (arriba a la derecha) → **Descargas** y toca el archivo.
- **Desde la notificación:** desliza la barra de notificaciones hacia abajo y toca **Descarga completada**.
- **Desde el explorador de archivos:** abre la app **Archivos** (en Samsung, **Mis archivos**; en Xiaomi, **Gestor de archivos**), entra en **Descargas** (o **Download**) y toca **galiciana-de-peto-X.X.X.apk**.

### 3. Permitir la instalación

La primera vez, Android pide permiso a la app desde la que abres el APK (el navegador o el explorador de archivos):

1. Aparece el mensaje **«Por tu seguridad, tu teléfono no puede instalar aplicaciones desconocidas de esta fuente»**. Toca **Ajustes**.
2. Activa **Permitir de esta fuente**.
3. Vuelve atrás con el botón o el gesto de Atrás y toca **Instalar**.

Si no aparece el mensaje, ve a **Ajustes → Aplicaciones → Acceso especial de aplicaciones → Instalar aplicaciones desconocidas**, elige el navegador o el explorador de archivos y activa **Permitir de esta fuente**.

### 4. Si Play Protect avisa

Google Play Protect puede mostrar **«App no segura bloqueada»** o **«Desarrollador desconocido»** porque la app no viene de Google Play:

1. Toca **Más detalles** (o la flecha pequeña hacia abajo).
2. Toca **Instalar de todos modos**.

No hace falta desactivar Play Protect. Si te pregunta si quieres **enviar la app para analizarla**, puedes elegir lo que prefieras: no cambia nada para instalarla.

### 5. Si el teléfono tiene la Protección avanzada activada

En Android 16 o posterior, la **Protección avanzada** bloquea por completo instalar aplicaciones de fuera de Google Play: la opción **Permitir de esta fuente** aparece gris con el mensaje **«Inhabilitado por Protección avanzada»**, y tampoco deja actualizar apps instaladas así.

1. Ve a **Ajustes → Seguridad y privacidad → Otros ajustes** (en algunos teléfonos está directamente en Seguridad y privacidad) **→ Protección avanzada**.
2. Desactívala.
3. Instala la app siguiendo los pasos 2 a 4.
4. Si quieres, vuelve a activar la Protección avanzada. Ojo: mientras esté activada, Android no dejará instalar las actualizaciones de la app. Para actualizarla tendrás que desactivarla de nuevo.

En teléfonos **Samsung** hay una protección parecida, el **Bloqueador automático**: **Ajustes → Seguridad y privacidad → Bloqueador automático**. Desactívalo para instalar y vuelve a activarlo después.

### 6. Si Android pide verificar al desarrollador

Google está introduciendo la verificación de desarrolladores para las apps instaladas fuera de Google Play. En noviembre de 2026 solo se aplica en algunos países (Brasil, Indonesia, Singapur y Tailandia), pero puede llegar a España y al resto del mundo a lo largo de 2027. Si tu teléfono te avisa de que la app es de un **desarrollador no verificado** y no te deja continuar, sigue la [ayuda oficial de Google](https://support.google.com/android/answer/17588095), que explica cómo permitirlo desde las **Opciones para desarrolladores**. La activación tarda 24 horas la primera vez.

### 7. Abrir la app

Busca el icono **GdPeto** en la pantalla de inicio o en el cajón de aplicaciones. Ya puedes borrar el archivo APK de **Descargas**: no hace falta para usar la app.

### Actualizar a una versión nueva

Descarga el APK nuevo y ábrelo igual: Android pregunta si quieres **actualizar** la app y **conserva tus páginas guardadas, carpetas y notas**, porque todas las versiones están firmadas con el mismo certificado. Por precaución, antes de actualizar haz una copia: **Acerca de → Copia dos teus datos → Gardar copia de datos**.

### Problemas frecuentes

- **«No se ha instalado la aplicación»:** suele pasar si la descarga quedó a medias. Borra el archivo, descárgalo otra vez y vuelve a abrirlo. También pasa si tienes una versión instalada desde otra fuente con otra firma: en ese caso, guarda una copia de tus datos, desinstala la anterior e instala esta.
- **«Error de análisis» o «Problema al analizar el paquete»:** el archivo está dañado o tu Android es anterior a la versión 8.0.
- **No encuentro el archivo:** abre el explorador de archivos y busca «galiciana» en la lupa.

## Compilar

Abre la carpeta del repositorio en Android Studio con JDK 17 y Android SDK 35. La aplicación requiere Android 8.0 (API 26) o posterior. Desde la terminal:

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

La primera compilación necesita acceso a los repositorios de dependencias. El APK de desarrollo queda en `app/build/outputs/apk/debug/`.

## Licencia y contacto

Código original de Android © 2026 **petocg**, bajo [GPL-3.0-only](LICENSE). Contacto: **petoacg@gmail.com**. Los componentes de terceros conservan sus licencias. Los documentos y datos de Galiciana se rigen por los derechos y condiciones de cada recurso. Consulta el aviso legal dentro de la app.
