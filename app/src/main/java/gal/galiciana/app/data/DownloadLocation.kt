package gal.galiciana.app.data

/** Must describe the destination used by PageDownloads, including its legacy fallback. */
fun downloadLocation(format: PageFormat, sdk: Int): String =
    if (sdk < 29) "la carpeta privada Galiciana de la app (se elimina al desinstalar)"
    else if (format == PageFormat.PDF) "Descargas/Galiciana"
    else "Fotos/Galiciana"
