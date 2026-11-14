---
name: Galiciana de Peto
description: Archivo gallego contemporáneo, centrado en buscar, leer y documentar hallazgos.
colors:
  galiciana-blue: "#8BCCFF"
  galiciana-blue-deep: "#12476D"
  shell-celeste: "#7EC8E3"
  canvas: "#101A23"
  surface: "#15222D"
  surface-muted: "#20313F"
  ink: "#E5EDF3"
  ink-muted: "#B2C3D0"
  selection: "#253D50"
  outline: "#354B5C"
typography:
  headline:
    fontFamily: "Android system sans-serif"
    fontSize: "32sp"
    fontWeight: 700
  title:
    fontFamily: "Android system sans-serif"
    fontSize: "22sp"
    fontWeight: 600
  body:
    fontFamily: "Android system sans-serif"
    fontSize: "16sp"
    fontWeight: 400
  label:
    fontFamily: "Android system sans-serif"
    fontSize: "14sp"
    fontWeight: 500
rounded:
  small: "8dp"
  medium: "12dp"
  large: "16dp"
spacing:
  compact: "8dp"
  group: "16dp"
  gutter: "24dp"
components:
  primary-action:
    backgroundColor: "{colors.galiciana-blue}"
    textColor: "{colors.surface}"
    height: "48dp minimum"
  reference-note:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.ink}"
    rounded: "{rounded.medium}"
---

# Design System: Galiciana de Peto

## Overview

Archivo gallego contemporáneo. La identidad se mantiene en el azul y la vieira; la interfaz nativa da protagonismo al documento y al contexto de investigación. El usuario aprobó una navegación Buscar / Explorar / Guardados y el rediseño del lector y de las notas. El nombre exacto «Nota de referencia» es vinculante.

## Colors

Theme.kt define un único esquema oscuro, fijado por el usuario a partir de sus capturas del Pixel. No se adapta al modo claro del sistema. Usar roles semánticos Material para texto, superficies, selección y errores. La imagen del documento conserva su color original. El azul identifica acción y selección; el ámbar translúcido y el borde azul identifican coincidencias OCR.

Los contenedores de Material (diálogos, menús, hojas y avisos) suben en escalones desde el lienzo azul noche: #0C141B, #15222D, #1A2834, #1E2E3B, #253746 y #2B3D4C como brillo máximo. Ningún rol queda en la paleta violácea por defecto de Material. El Snackbar invierte la superficie (#E5EDF3 sobre #15222D) con acción #12476D.

Colores fijos fuera de los roles, intencionados:

- Escenario del documento: fondo #1C1A17, cálido y neutro, para no teñir el facsímil. Sus textos de estado usan blanco y gris claro sobre ese fondo.
- Coincidencias OCR en la imagen: relleno ámbar translúcido (#40FFE36D; activa #66FFC54D) con borde #996700; la coincidencia activa lleva borde azul #075A9C.
- Resaltado en extractos de texto: #FFF59D (activo #FFE082) con tinta #1A1A1A.
- Carpetas: icono ámbar #E6C27A, el color de una carpeta de archivo, distinto del azul de acción.
- Vieira: celeste #7EC8E3 con nervaduras #12384F cuando la página está guardada.

## Typography

Tipografía nativa Android mediante Material 3. Titulares para destinos; títulos de publicación con jerarquía contenida; cuerpo legible para extractos y notas. Los controles respetan el escalado del sistema. Los textos extensos se desplazan o se amplían mediante una acción explícita; no desaparecen por truncado sin acceso al contenido.

## Layout

Tres destinos estables en la navegación inferior. El visor dispone de navegación propia y puede ocultar sus controles para leer la imagen. Inicio y Explorar usan márgenes de 24dp; resultados y archivo, 20dp. El contenido y las cabeceras de Guardados comparten una lista desplazable. Los filtros y las carpetas son progresivos. Los controles interactivos tienen al menos 48dp de objetivo.

## Elevation & Depth

Superficies y divisores sutiles, con elevación nativa para hojas y diálogos. El lector evita barras flotantes sobre el documento durante la lectura.

## Shapes

Radios 8/12/16dp definidos en el tema. Un vocabulario común de campos, botones y chips Material. La vieira conserva su silueta y celeste, con una etiqueta accesible que explica la acción.

## Components

- Buscar: consulta directa, filtros desplegables y continuar leyendo.
- Resultados: cantidad y orden compactos; extractos breves ampliables; acciones de página separadas del texto.
- Guardados: páginas / notas de referencia, búsqueda que incluye las anotaciones, carpetas plegables, orden y selección.
- Visor: cabecera compacta, búsqueda plegable, navegación por coincidencias y hoja de acciones.
- Nota de referencia: editor protegido a pantalla completa, contexto de publicación y página, copia de referencia, hasta 4.000 caracteres y confirmación antes de descartar o eliminar. El guardado actualiza las representaciones de la nota en una única transacción.

## Do's and Don'ts

Preservar los datos existentes, los controles nativos y la distinción entre resultados precisos y aproximados. No presentar la ausencia de metadatos o un error de red como cero resultados. No añadir tipografías descargadas, fondos ornamentales ni movimiento que retrase la tarea. No cambiar «Nota de referencia» por un nombre genérico.

Las listas de publicaciones identifican el contexto: título, año, número y página de índice no son equivalentes. La paginación del índice usa Anterior / Siguiente con estado explícito. Los años se eligen en un selector buscable. Las fichas no abren el visor automáticamente al cargarse. El contexto bibliográfico disponible acompaña al lector y a la Nota de referencia.

## Investigación y copias · 0.4.0

Inicio identifica ejemplares con cabecera breve, fecha y número cuando la fuente los ofrece; conserva el título original. El formulario de búsqueda agrupa publicación/persona/lugar y periodo, mantiene Buscar accesible fuera del desplazamiento y permite guardar/reutilizar consultas.

Las fichas muestran etiqueta y valor, derechos y enlace al registro original; los campos largos y el texto íntegro se despliegan sin eliminar contenido. Las galerías reservan una superficie consistente para miniaturas ausentes.

Copia de tus datos se encuentra en Guardados y Acerca de. Exportar y abrir una copia son acciones distintas; la vista previa muestra cantidades y diferencias antes de importar. Combinar es la opción predeterminada. La interfaz no utiliza jerga de checksum, preferencias tipadas o manifiestos para explicar la tarea al usuario.

## Icono del lanzador

Vieira celeste #7EC8E3 sobre azul noche #15222D, con siete nervaduras #12384F y silueta suavizada. Adaptativo en capas, compatible con máscara circular y capa monocroma; variantes rasterizadas para densidades heredadas. Etiqueta del lanzador: GdPeto. Nombre interno de la app y título de Recientes: Galiciana de Peto. Fuente vectorial en design/launcher-icon.svg. Comprobado en el lanzador del Pixel.

## Crítica del 13 de noviembre de 2026 · decisiones aplicadas

Vocabulario único: el lugar se llama «Guardados» y su contenido «páginas guardadas»; no se usa «favoritos» ni «páginas marcadas» en la interfaz. «Quitar» retira una página de Guardados; «Eliminar» destruye una carpeta o una Nota de referencia; «Borrar» queda para vaciar texto o el historial. Todo diálogo de retirada dice qué ocurre con la Nota de referencia.

Visor: la vieira con estado vive en la barra superior, y la línea de página añade «Guardada» o «Guardada con nota». Quitar una página desde el visor es inmediato y reversible con «Deshacer». Abrir «Organizar en carpetas» no guarda nada hasta pulsar «Listo». La hoja de acciones se conserva y no invade la barra de estado.

Guardados: sin carpeta elegida se muestran y se buscan todas las páginas. «Archivador general» es una carpeta más.

Resultados: mientras Galiciana no responde se muestra «Buscando en Galiciana…», nunca un recuento a cero. Un fallo ofrece «Reintentar» y conserva consulta y filtros. La primera vez que hay resultados, una pista explica la vieira y el icono de nota.

Barras de altura fija (etiquetas de navegación, títulos del visor y del editor de notas): el texto sigue el tamaño del sistema hasta el 130 %. El contenido escala sin límite. La cabecera de Inicio queda fija y el contenido no se desliza bajo la barra de estado. La pestaña activa sigue el destino desde el que se llegó a Acerca de, Aviso legal, Copia de tus datos y las fichas.

## Idiomas · galego por defecto

A interface ofrécese en galego (por defecto) e en español, elixible en Acerca de. Cambiar o idioma reinicia a aplicación. Os textos viven no código como pares `tr("galego", "español")` (`i18n/Lang.kt`); todo texto novo engádese nos dous idiomas. Vocabulario galego: Gardados, páxina gardada, cartafol, busca, Nota de referencia (sen cambios), Arquivador xeral, Quitar / Eliminar / Borrar co mesmo reparto que en español. Os termos seguen a versión galega de Galiciana.

Non se traducen: os títulos, extractos e campos que chegan de Galiciana; os temas orientativos da busca (son palabras que se engaden á consulta sobre prensa en español); o nome almacenado do cartafol «Archivador general», do que só se traduce a etiqueta, para non alterar datos gardados nin copias. O aviso legal ten versión galega propia (`legal_notice_gl.json`).

## Detalles finales · 13 de noviembre de 2026

Visor: deslizar horizontalmente con un dedo sobre la página entera pasa de página; con la página ampliada el gesto desplaza la imagen. Las fuentes externas se abren desde una fila de acciones más, con icono. Cerrar la Nota de referencia devuelve al visor.

Navegación: una busca escrita en Inicio abre directamente los resultados y Atrás vuelve a Inicio; el formulario solo es un paso cuando se ha abierto. Las pestañas Explorar y Gardados comparten barra sin flecha; Inicio conserva la cabecera de marca con una vieira decorativa. Inicio ya no repite el enlace a Acerca de (está en su cabecera y en Explorar).

Resultados: las notas de alcance viven en Detalles; solo se avisa arriba cuando el orden por fecha se aplica a resultados parciales. En Gardados la Nota de referencia se lee completa bajo demanda y precede a un único extracto en tono secundario. Copia de datos y Exportar referencias están en el menú de la barra de Gardados.

Coleccións: los tipos de fondo se muestran como mosaicos iguales con un símbolo propio, no con portadas de muestra. Los títulos recortados por la fuente se muestran con una sola elipsis.

Márgenes: un único margen lateral de contenido de 20 dp en todas las pantallas.

## Pulido final · 13 de noviembre de 2026

Inicio: el campo es la búsqueda. Se lanza con la tecla del teclado o con el botón de buscar dentro del campo, que aparece al escribir; el botón ancho «Buscar» solo se muestra cuando hay texto.

Carpetas: el icono de cartafol es siempre ámbar (`FolderAmber` en Theme.kt), en resultados, Gardados y la hoja de cartafoles. Crear cartafol usa el icono CreateNewFolder en azul de acción.

Formulario de búsqueda: un solo nombre, «Buscar con filtros», en Inicio y en el formulario, con un interruptor.

Visor: el botón flotante que devuelve los controles usa FullscreenExit. Al pasar de página, la nueva entra desde el lado del gesto en 200 ms cuando su imagen está lista; con «Quitar animaciones» activado aparece sin movimiento. La página anterior no sale animada: su imagen se libera al cambiar.

Resultados: los tres niveles se nombran y se explican en una línea («Coincidencias exactas», variantes de OCR, coincidencias más amplias). El primero solo lleva encabezado cuando hay otros niveles.

Copias: los tipos de diferencia (`ConflictKind`) son valores fijos dentro de las copias; solo su etiqueta se traduce. Las cadenas gallegas se revisan en docs/GALEGO_REVISION.md.

Pasar página: la página anterior sale hacia el lado del gesto mientras la nueva entra por el contrario (200 ms). Para la salida se guarda una copia ajustada a la pantalla, nunca la imagen completa, y se libera al terminar.

Letra grande: las etiquetas de chips y botones de cabecera no se parten; se acortan («Notas», con el nombre completo para TalkBack) o pasan a otra línea. Los botones de texto de cabeceras y filas de acciones miden al menos 48 dp.

Coleccións: una obra sin portada muestra el icono de su tipo de fondo y su año, en el espacio de la portada.

Termos galegos confirmados polo usuario: «Gardados» (pestana), «cartafol» (carpeta) e o lema «Galiciana, de peto.».

Pasar página también funciona con la página ampliada cuando ya está en el borde de ese lado, como en cualquier lector. Cambiar de idioma recrea la pantalla sin reiniciar la app. Tras fallar la búsqueda ampliada, «Reintentar» repite esa búsqueda, no la normal.

