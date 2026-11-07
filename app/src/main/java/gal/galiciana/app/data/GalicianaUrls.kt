package gal.galiciana.app.data

object GalicianaUrls {
    const val BASE = "https://biblioteca.galiciana.gal"
    const val OAI = "$BASE/es/oai/oai.do"
    const val SPARQL = "https://datos-abertos.galiciana.gal/sparql"
    const val SEARCH_OCR = "$BASE/es/consulta/resultados_ocr.do"
    const val SEARCH_CATALOG = "$BASE/es/consulta/resultados.do"
    const val SEARCH_HISTORY = "$BASE/es/query/history.do"

    const val TEST_PATH = "1472957"
    const val TEST_PUBLICATION = "10262"

    fun record(id: String) = "$BASE/es/consulta/registro.do?id=$id"
    fun recordControl(control: String) = "$BASE/es/consulta/registro.do?control=$control"
    fun issues(publicationId: String, year: String? = null) =
        "$BASE/es/publicaciones/numeros_por_mes.do?idPublicacion=$publicationId" +
            (year?.let { "&anyo=$it" } ?: "")

    fun mets(path: String) = "$BASE/es/media/group/export-mets.do?path=$path"
    fun viewer(path: String, position: Int = 1) =
        "$BASE/es/catalogo_imagenes/grupo.do?path=$path&presentacion=pagina&posicion=$position"
    fun jpeg(imageId: String) =
        "$BASE/es/catalogo_imagenes/imagen_id.do?idImagen=$imageId"
    fun jpegHi(imageId: String) =
        "$BASE/es/catalogo_imagenes/imagen_id.do?idImagen=$imageId&formato=jpg&ancho=7000"
    fun thumb(imageId: String) =
        "$BASE/es/media/object-miniature.do?id=$imageId"
    fun ocr(path: String, position: Int) =
        "$BASE/es/catalogo_imagenes/descargarTextoOCR.do?path=$path&posicion=$position"
    fun alto(path: String, position: Int) =
        "$BASE/es/catalogo_imagenes/descargarAlto.do?path=$path&posicion=$position"
    fun pdfRange(path: String, from: Int, to: Int) =
        "$BASE/es/catalogo_imagenes/download_path.do?groupId=$path&initialPage=$from&finalPage=$to"
    fun jpegDownload(path: String, position: Int, imageId: String) =
        "$BASE/es/catalogo_imagenes/download_path.do?path=$path&posicion=$position&idImagen=$imageId&formato=jpg&groupId=$path&initialPage=$position&finalPage=$position"
}
