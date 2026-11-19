# Galiciana de Peto para Android

Galiciana de Peto es una aplicación independiente para consultar la hemeroteca digital de Galiciana, la Biblioteca Dixital de Galicia, desde Android. Permite buscar en el texto OCR de las publicaciones, abrir los facsímiles con las coincidencias resaltadas y organizar los hallazgos en páginas guardadas, carpetas y notas de referencia.

- **Buscar:** en el texto de las páginas o en el catálogo, con filtros de publicación, autor, lugar, período y tema. Los resultados se agrupan por obra y separan coincidencias exactas, variantes de OCR y coincidencias más amplias. Las búsquedas se pueden guardar y retomar, y se pueden guardar varias páginas a la vez.
- **Leer:** visor con zoom, paso de página con el dedo, pantalla completa, coincidencias resaltadas y un halo que señala la activa, lista de páginas del documento donde aparece la búsqueda, texto OCR para leer y copiar, y descarga en JPEG o PDF.
- **Explorar:** hemeroteca con índice A–Z, años y números; colecciones por tipo de documento; materias, personas y lugares del catálogo.
- **Guardados:** páginas guardadas con la vieira, carpetas, notas de referencia, búsqueda en tus hallazgos y exportación de referencias.
- **Tus datos:** se guardan en el teléfono, sin cuentas. Se pueden exportar e importar con una copia ZIP.
- **Idioma:** gallego, con español a elección desde Acerca de.

La consulta de documentos necesita conexión. Los derechos de cada documento figuran en su ficha original. También hay una [versión web](https://petoacg.github.io/galicianadepeto-web/) que funciona en iPhone, Android y ordenador.

> **¿Prefieres no instalar el APK?** La [versión web](https://petoacg.github.io/galicianadepeto-web/?instalar) se instala en un toque desde Chrome, sin avisos de seguridad, y funciona también en iPhone. Al abrirla te guía en gallego o español.

## Instalar en Android, paso a paso

La app no está en Google Play: se instala con un archivo **APK** que se descarga de esta página. Hace falta **Android 8.0 o posterior**. Los nombres de los menús son los que aparecen con el teléfono en español y pueden cambiar un poco según la marca (Samsung, Xiaomi, Motorola…).

### Antes de empezar: por qué Android avisa, y por qué puedes estar tranquilo

Durante la instalación, Android te mostrará varios avisos. **Es normal y no significa que la app tenga nada malo.** Android avisa de la misma forma con **cualquier** app que no venga de Google Play, porque no puede comprobarla por su cuenta. No está diciendo que sea peligrosa: dice que no la conoce.

Por qué esta app es de fiar:

- **El código está a la vista.** Todo el código de la app está publicado en este repositorio de GitHub, con licencia libre GPL-3.0. Cualquiera puede leerlo, revisarlo o compilarlo por su cuenta.
- **Se descarga desde la fuente original.** El APK está en las publicaciones oficiales de este repositorio. No lo descargues de otras webs ni de enlaces que te envíen: solo de esta página.
- **Siempre lleva la misma firma.** Todas las versiones están firmadas con el mismo certificado, y Android comprueba que cada actualización viene del mismo autor. Huella SHA-256 del certificado: `e8285607650d9aa85d9920aa8492da63e2c1ceeac12c7bd11ed0dcfdac0c9880`.
- **Pide un solo permiso: Internet,** para consultar Galiciana. No accede a tus contactos, fotos, ubicación, micrófono ni cámara.
- **Sin cuentas, anuncios ni rastreadores.** Tus páginas guardadas, carpetas y notas se quedan en tu teléfono.

Los avisos que vas a ver son estos tres, y en cada paso se explica qué hacer:

- **«Fuentes desconocidas»:** el permiso para instalar apps de fuera de Google Play.
- **Play Protect:** el antivirus de Google, que no conoce las apps de fuera de Google Play.
- **Protección avanzada:** solo aparece si la tienes activada.

Al terminar puedes **volver a dejar todas las protecciones como estaban** (paso 8). La app seguirá funcionando igual.

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

No hace falta desactivar Play Protect, y no conviene hacerlo: déjalo activado para que siga vigilando el resto de tus apps. Si te pregunta si quieres **enviar la app para analizarla**, puedes elegir lo que prefieras: no cambia nada para instalarla.

### 5. Si el teléfono tiene la Protección avanzada activada

En Android 16 o posterior, la **Protección avanzada** bloquea por completo instalar aplicaciones de fuera de Google Play: la opción **Permitir de esta fuente** aparece gris con el mensaje **«Inhabilitado por Protección avanzada»**, y tampoco deja actualizar apps instaladas así.

1. Ve a **Ajustes → Seguridad y privacidad → Otros ajustes** (en algunos teléfonos está directamente en Seguridad y privacidad) **→ Protección avanzada**.
2. Desactívala.
3. Instala la app siguiendo los pasos 2 a 4.
4. Vuelve a activarla en el mismo sitio cuando la app ya esté instalada (lo explica el paso 8). La app seguirá funcionando con ella activada. Ten en cuenta que, mientras esté activada, Android no deja instalar actualizaciones de apps de fuera de Google Play: cuando quieras actualizar Galiciana de Peto, desactívala un momento, actualiza y vuelve a activarla.

En teléfonos **Samsung** hay una protección parecida, el **Bloqueador automático**: **Ajustes → Seguridad y privacidad → Bloqueador automático**. Desactívalo para instalar y vuelve a activarlo después.

### 6. Si Android pide verificar al desarrollador

Google está introduciendo la verificación de desarrolladores para las apps instaladas fuera de Google Play. En noviembre de 2026 solo se aplica en algunos países (Brasil, Indonesia, Singapur y Tailandia), pero puede llegar a España y al resto del mundo a lo largo de 2027. Si tu teléfono te avisa de que la app es de un **desarrollador no verificado** y no te deja continuar, sigue la [ayuda oficial de Google](https://support.google.com/android/answer/17588095), que explica cómo permitirlo desde las **Opciones para desarrolladores**. La activación tarda 24 horas la primera vez.

### 7. Abrir la app

Busca el icono **GdPeto** en la pantalla de inicio o en el cajón de aplicaciones. Ya puedes borrar el archivo APK de **Descargas**: no hace falta para usar la app.

### 8. Dejar las protecciones como estaban

Una vez instalada la app, puedes devolver el teléfono a su estado anterior. La app seguirá funcionando igual:

1. **Fuentes desconocidas:** ve a **Ajustes → Aplicaciones → Acceso especial de aplicaciones → Instalar aplicaciones desconocidas**, toca el navegador o el explorador de archivos que usaste y desactiva **Permitir de esta fuente**. Es lo más recomendable: así ninguna web podrá instalar nada sin que tú lo vuelvas a permitir.
2. **Protección avanzada** (si la desactivaste): **Ajustes → Seguridad y privacidad → (Otros ajustes) → Protección avanzada** y actívala.
3. **Bloqueador automático de Samsung** (si lo desactivaste): **Ajustes → Seguridad y privacidad → Bloqueador automático** y actívalo.
4. **Play Protect** no hace falta tocarlo: sigue activado todo el tiempo.

Para instalar una versión nueva más adelante, repite solo lo necesario de los pasos 3 a 5 y vuelve a dejarlo todo como estaba.

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
