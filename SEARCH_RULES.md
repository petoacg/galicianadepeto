# Reglas de búsqueda de Galiciana2

Revisión del motor: 5 de noviembre de 2026. Este documento describe el código actual; no garantiza la cobertura ni la interpretación interna del servicio de Galiciana.

## Consulta enviada

- Se normalizan espacios, saltos de línea, espacios Unicode y separadores invisibles; las comillas tipográficas se convierten en comillas dobles. Si queda un número impar de comillas, se envía la consulta sin comillas.
- El formulario envía consulta general, título, autor, lugar y rango de fechas al servicio. Los filtros de metadatos sólo se aplican con búsqueda avanzada activa. Los años deben tener cuatro cifras, estar entre 1000 y 2100 y formar un intervalo válido.
- Con «Buscar en el texto completo» activo y consulta no vacía, se solicita búsqueda OCR. En búsqueda de catálogo no se traslada la consulta al buscador dentro de la página.
- Las palabras sin comillas se envían tal cual al servicio: la aplicación comunica combinación con Y. No se utiliza el ranking local de favoritos para eliminar coincidencias recibidas del servidor.
- `?` y `*` se conservan en la consulta al servidor. El resaltador y los filtros locales de favoritos tokenizan palabras; no son un intérprete completo de los comodines del servicio.
- La sección es una palabra añadida a la consulta, no un filtro bibliográfico. «Sucesos y Sociedad» consulta ambas palabras por separado y une los resultados. La repesca conserva su comportamiento amplio existente y no añade la palabra de sección.

## Resultados precisos, variantes y repesca

1. **Resultado principal:** primera página del servicio, solicitada en el orden seleccionado cuando éste ofrece enlaces de ordenación. Las páginas siguientes se incorporan bajo demanda. Las consultas con comillas verifican que todas las expresiones entrecomilladas estén presentes en extractos o títulos disponibles. Un extracto truncado que no permita verificar la expresión se reserva para la repesca; no demuestra que falte en el documento completo.
2. **Variantes OCR:** para una única expresión entre comillas, sin términos adicionales ni sección, se consultan candidatos sin comillas y omitiendo un término por vez. Se exige después verificar la expresión en un extracto. Se limita a 200 páginas; las consultas de una palabra leen hasta cuatro páginas del servidor y las de varias palabras hasta dos por candidato.
3. **Repesca:** acción explícita que conserva los resultados principales. Una palabra de cinco o más letras genera sustituciones con `?`; varias palabras generan la consulta completa, consultas que omiten un término y parejas de términos, con un máximo de 12 consultas candidatas. Esto permite intentar recuperar páginas donde el OCR ha perdido más de una palabra sin convertir la búsqueda principal en una búsqueda con O. Se mantienen los filtros de metadatos de la búsqueda enviada, aunque el formulario se edite después. Se excluyen páginas ya presentes en resultados principales o variantes y se identifican como candidatos flexibles, no como coincidencias demostradas. El límite es 50 o hasta el doble del total conocido cuando hay al menos 25, con máximo de 200.

La verificación local de una palabra entre comillas exige la palabra completa, tolerando acentos y cortes por guion. Las expresiones de varias palabras mantienen el orden y la adyacencia, toleran cortes OCR y admiten en toda la expresión un error de letra en una palabra suficientemente larga. No se han ampliado estas tolerancias en esta revisión.

## Identidad, orden y continuidad

- La identidad de una coincidencia se compone de `path`, `imageId` y `position`. Las obras se agrupan por `path`, con título como alternativa cuando falta.
- En Relevancia se ordenan localmente las obras y páginas cargadas por proximidad en sus extractos. Los órdenes Título, Autor y Fecha dependen de los enlaces ofrecidos por Galiciana. No se presenta una ordenación local del subconjunto cargado como orden global.
- Favoritos sí utiliza `SearchRanking.kt`: relevancia por proximidad y cobertura, título sin artículos iniciales y fecha inferida del texto. La fecha inferida y el campo alternativo utilizado como autor no sustituyen metadatos bibliográficos estructurados.
- Se deduplican coincidencias y URLs pendientes. Se registran URLs ya cargadas para detener ciclos de paginación. Una búsqueda nueva o un cambio de orden reinicia ese registro.
- Una búsqueda nueva cancela los trabajos anteriores de variantes y repesca, además de usar su generación para rechazar resultados antiguos. La cancelación no se transforma en un error de conexión.

## Correcciones realizadas

- Se separan los filtros enviados de los campos editables para impedir que la repesca mezcle una consulta anterior con metadatos nuevos.
- Se cancelan las exploraciones auxiliares al iniciar otra búsqueda, evitando que terminen de publicar agrupaciones antiguas después de una suspensión.
- Se detienen ciclos de URLs tanto en la carga por lotes del repositorio como al pulsar «cargar más».
- La carga de páginas adicionales propaga correctamente la cancelación.
- Una respuesta HTML inesperada, aunque llegue con HTTP 200, se distingue de una búsqueda vacía: se exige una estructura de resultados reconocida o un mensaje explícito de ausencia de coincidencias. Si no existe esa evidencia, se muestra un error de respuesta en lugar de «sin resultados».

## Validación y límites

Las pruebas unitarias cubren normalización, límites de coincidencias entrecomilladas y aproximadas, ranking, preservación de resultados de catálogo/OCR, filtros enviados y ciclos de paginación. No sustituyen pruebas del servicio remoto ni la ejecución en dispositivo. Los extractos pueden estar truncados, el OCR puede contener errores y el servidor puede devolver más páginas que las examinadas.

## Búsqueda dentro del visor y marcas sobre la imagen

- La consulta se aplica a la página abierta. El texto plano y el ALTO son representaciones diferentes del mismo documento: encontrar texto no garantiza disponer de coordenadas utilizables para dibujarlo.
- El parser ALTO interpreta XML, namespaces, entidades y ambos tipos de comillas de atributos. Exige dimensiones y coordenadas válidas; no inventa la escala de la página a partir de las palabras encontradas.
- Las expresiones entre comillas siguen el mismo criterio que el buscador de texto: una palabra es literal; una frase mantiene adyacencia y admite un único error OCR en toda la expresión, además de cortes de palabra. Se corrigió la duplicación de expresiones completamente entrecomilladas con comillas simples o bajas.
- Una coincidencia puede tener varios rectángulos si cruza líneas o columnas. El contador y los botones recorren coincidencias lógicas en el orden de lectura ALTO, no rectángulos sueltos.
- Las respuestas se consultan en paralelo, pero se eligen por prioridad fija: página y posición; página, posición e imagen; imagen sola. Una respuesta alternativa más rápida no desplaza a la página canónica válida.
- La coincidencia activa se distingue por borde azul más grueso y fondo ámbar. Los botones anterior/siguiente centran y amplían su inicio. El control de marcas permite ocultarlas para leer el original.
- Se distinguen ALTO válido sin coincidencias, página sin palabras posicionadas, descarga no disponible y XML/coordenadas inválidos. Los fallos transitorios no se guardan como búsquedas vacías y permiten reintento; el lector de texto sigue siendo una alternativa.
- «Continuar leyendo» conserva también la consulta enviada al visor, además de la página.

## Casos de nombres incompletos y prensa

La búsqueda normal sigue exigiendo todas las palabras. Para `Pedro Antonio Cárcamo Álvarez`, las páginas que solo contienen `Antonio Cárcamo Álvarez` o `Pedro Cárcamo Álvarez` son candidatas a «Buscar coincidencias más amplias»; se generan ambas combinaciones conservando los filtros enviados. La generación está probada, pero no garantiza que el índice remoto devuelva una publicación concreta.

El visor marca cada término sin comillas presente en la página, aunque falte otro. Las frases entrecomilladas no omiten palabras. El ranking, el recuento y el resaltado local reconstruyen cortes como `Cár-` seguido de salto de línea y `camo`. También se reconocen guiones Unicode y metadatos ALTO `HYP`, `HypPart1`/`HypPart2` y `SUBS_CONTENT`.

En las coordenadas ALTO las continuaciones entre bloques requieren metadatos explícitos de palabra partida o líneas consecutivas con geometría compatible dentro de la misma columna. No se unen artículos ni columnas independientes. El texto plano no permite deducir por sí solo la disposición de las columnas.

## Página real aportada: 1 de julio de 1975

Las siete consultas aportadas se comprobaron sobre el OCR completo y el ALTO de El Pueblo Gallego, página 15, grupo `1264535`. Los recuentos de texto e imagen coinciden tras corregir metadatos de palabras partidas y distinguir guiones de continuación de separadores. Véase [evidencia de la página](verification/REFERENCE_PAGE.md).

### Relevancia por proximidad (noviembre 2026)

El orden Relevancia aplica la puntuación local a las obras y páginas ya cargadas, también tras paginar y dentro de las categorías auxiliares existentes. Se calcula sobre extractos OCR, sin unir extractos separados para simular una frase. Favorece cobertura de términos, ventanas compactas, orden y pares consecutivos de términos consultados. Una coincidencia completa próxima con palabras intercaladas precede a una parcial compacta; esta puede preceder a una coincidencia completa dispersa. La repetición tiene una bonificación limitada.

No cambia la consulta remota, admisión, condiciones entre comillas, filtros ni separación de coincidencias ampliadas. Título/autor/fecha conservan su orden propio. Es relevancia sobre candidatos cargados, no una clasificación global del archivo: cargar más candidatos puede cambiar el orden. Regresiones: SearchProximityRankingTest y SearchSemanticsTest.
