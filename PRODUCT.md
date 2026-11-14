# Galiciana de Peto

<!-- impeccable:product-schema 1 -->

## Platform

android

## Stack

Aplicación existente Kotlin / Jetpack Compose, Material 3, Navigation Compose, OkHttp, Coil y Jsoup.

## Product Purpose

Consultar los documentos de Galiciana, investigar en su hemeroteca, leer páginas digitalizadas con búsqueda OCR y organizar hallazgos mediante páginas guardadas, carpetas y notas de referencia.

## Operating Context

El usuario investiga nombres, publicaciones y noticias históricas. La búsqueda y la lectura deben conservar el contexto entre pantallas. La comprobación física de esta sesión se realiza en un Pixel 10 conectado.

Uso real: teléfono en vertical y con el tamaño de letra normal. La orientación horizontal y la letra ampliada al 200 % quedan fuera de las revisiones de diseño por decisión del usuario (lista en `.impeccable/critique/ignore.md`); la app no se bloquea en vertical.

## Capabilities and Constraints

Los documentos, metadatos e índices proceden del servicio de Galiciana. El texto OCR y las coordenadas ALTO pueden diferir; los fallos de conexión no deben presentarse como cero resultados. Favoritos, carpetas y notas existentes deben conservarse. Los documentos se muestran con sus colores originales.

## Brand Commitments

Nombre confirmado: «Galiciana de Peto». Autor y contacto del aviso legal se dejan vacíos hasta la fase final por instrucción del usuario. El aviso remite a los derechos indicados en cada documento, sin presumir dominio público de todos los fondos.

El diseño oscuro de las capturas aportadas es permanente, independientemente del modo del sistema. No hay variante clara.

El usuario mantiene los colores principales de identidad gallega y aprueba un diseño moderno, rápido e intuitivo. Se conserva la vieira. El término exacto «Nota de referencia» es una decisión explícita del usuario y un guiño al mundo archivístico e investigador.

## Product Principles

- Buscar, leer y guardar con pocos pasos y controles reconocibles.
- Dar protagonismo a los documentos y a los hallazgos.
- Mostrar los límites del OCR y del servicio sin perder el contexto.
- Proteger las anotaciones y los cambios sin guardar.

## Evidence on Hand

Página real aportada: El Pueblo Gallego, 01/07/1975, página 15, grupo 1264535. Siete consultas comprobadas en servicio, texto y coordenadas; evidencia en verification/REFERENCE_PAGE.md. La aplicación contiene favoritos y notas reales del usuario; las pruebas de almacenamiento de esta revisión usan preferencias aisladas.

## Navigation and scope

Hemeroteca ofrece el índice alfabético de periódicos y revistas y sus años/números. Colecciones agrupa otros tipos de documentos, con una ficha antes de abrir la copia. Materias y datos del catálogo separa índices y herramientas técnicas de la búsqueda OCR. «Sucesos y sociedad» es una opción temática orientativa dentro de los filtros generales, no un repositorio ni una clasificación editorial del archivo.

## Datos de investigación y portabilidad

La app se utiliza online para consultar Galiciana. No incorpora un archivo offline de imágenes, documentos ni OCR. Se mantienen las descargas existentes de JPEG y PDF. Sin cuentas ni inicio de sesión.

Los datos personales de investigación se almacenan en el teléfono: páginas marcadas, carpetas y asociaciones, Notas de referencia (incluidas las de páginas ya desmarcadas), consultas recientes, búsquedas guardadas, borrador con filtros, contexto de lectura y preferencias. Una copia ZIP ligera y versionada permite exportarlos/importarlos sin incluir documentos descargados. La importación se previsualiza; Combinar conserva diferencias por defecto y Reemplazar exige confirmación explícita.

La etiqueta abreviada del icono de escritorio es «GdPeto», por indicación del usuario. No sustituye el nombre «Galiciana de Peto» dentro de la app o en su aviso legal.

## Extensión PWA

La implementación web en `pwa/` extiende Android 0.4.1 con React/TypeScript y una pasarela JVM de mismo origen. Conserva el nombre «Galiciana de Peto», la abreviatura instalable «GdPeto», la vieira, la paleta oscura fija, los destinos Buscar / Explorar / Guardados y el término «Nota de referencia». No sustituye la aplicación Android ni su archivo local.

La investigación personal reside en IndexedDB, separada por navegador, perfil y origen. No hay cuentas ni sincronización automática; la copia ZIP compatible con Android permite trasladarla. La interfaz puede abrir los datos locales sin conexión; imágenes, documentos, metadatos y OCR se consultan online. La pasarela mantiene sesiones de consulta temporales y aisladas, sin almacenar la investigación personal.

El alojamiento preparado reúne interfaz y pasarela bajo un mismo origen; la publicación HTTPS y la instalación pública siguen pendientes. El contenedor Docker está preparado, pero no ejecutado en este entorno. Evidencia y límites concretos en `docs/PWA_IMPLEMENTATION.md`.

## Idioma

La interfaz está en gallego por defecto y en español a elección del usuario, desde Acerca de, por indicación del usuario. El contenido de los documentos se muestra como lo ofrece Galiciana.

