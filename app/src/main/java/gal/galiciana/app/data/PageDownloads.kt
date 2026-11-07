package gal.galiciana.app.data

import android.content.ContentValues
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import okhttp3.Request

enum class PageFormat { JPEG, PDF }

object PageDownloads {
    class CombinedPdf(context: Context) : AutoCloseable {
        private val file = File.createTempFile("galiciana-pages-", ".pdf", context.cacheDir)
        private val stream = FileOutputStream(file)
        private val offsets = mutableListOf<Long>(0)
        private val pageIds = mutableListOf<Int>()
        private var count = 0L
        private var finished = false

        init { write("%PDF-1.4\n%Galiciana\n") }

        private fun write(s: String) {
            val bytes = s.toByteArray(Charsets.US_ASCII)
            stream.write(bytes)
            count += bytes.size
        }

        private fun objectStart(id: Int) {
            while (offsets.size <= id) offsets.add(0)
            offsets[id] = count
            write("$id 0 obj\n")
        }

        private fun jpegColorSpace(jpeg: ByteArray): String {
            var i = 2
            while (i + 10 < jpeg.size) {
                if ((jpeg[i].toInt() and 255) != 255) break
                val marker = jpeg[i + 1].toInt() and 255
                if (marker == 0xDA || marker == 0xD9) break
                val length = ((jpeg[i + 2].toInt() and 255) shl 8) or (jpeg[i + 3].toInt() and 255)
                if (length < 2 || i + 2 + length > jpeg.size) break
                if (marker in 0xC0..0xCF && marker !in listOf(0xC4, 0xC8, 0xCC)) {
                    return when (jpeg[i + 9].toInt() and 255) {
                        1 -> "/DeviceGray"
                        4 -> "/DeviceCMYK"
                        else -> "/DeviceRGB"
                    }
                }
                i += 2 + length
            }
            return "/DeviceRGB"
        }

        fun add(image: ByteArray, ocr: String = "") {
            check(!finished)
            var jpeg = image
            if (image.size < 2 || image[0] != 0xFF.toByte() || image[1] != 0xD8.toByte()) {
                val bitmap = BitmapFactory.decodeByteArray(image, 0, image.size)
                    ?: error("Imagen no válida para el PDF")
                val encoded = ByteArrayOutputStream()
                try { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, encoded) }
                finally { bitmap.recycle() }
                jpeg = encoded.toByteArray()
            }
            val dimensions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size, dimensions)
            val width = dimensions.outWidth
            val height = dimensions.outHeight
            require(width > 0 && height > 0) { "Dimensiones de imagen no válidas" }
            // Validate and lay out OCR before adding any page objects to the PDF.
            val hiddenText = pdfOcrCommands(ocr, width, height)
            val pageId = 4 + pageIds.size * 3
            val imageId = pageId + 1
            val contentId = pageId + 2
            pageIds.add(pageId)
            objectStart(pageId)
            write("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 $width $height] " +
                "/Resources << /XObject << /Im0 $imageId 0 R >> /Font << /F1 3 0 R >> >> " +
                "/Contents $contentId 0 R >>\nendobj\n")
            objectStart(imageId)
            write("<< /Type /XObject /Subtype /Image /Width $width /Height $height " +
                "/ColorSpace ${jpegColorSpace(jpeg)} /BitsPerComponent 8 /Filter /DCTDecode /Length ${jpeg.size} >>\nstream\n")
            stream.write(jpeg)
            count += jpeg.size
            write("\nendstream\nendobj\n")
            val draw = "q $width 0 0 $height 0 0 cm /Im0 Do Q\n$hiddenText"
            objectStart(contentId)
            write("<< /Length ${draw.toByteArray(Charsets.US_ASCII).size} >>\nstream\n$draw" +
                "endstream\nendobj\n")
        }

        private fun finish() {
            if (finished) return
            check(pageIds.isNotEmpty())
            objectStart(1)
            write("<< /Type /Catalog /Pages 2 0 R >>\nendobj\n")
            objectStart(2)
            write("<< /Type /Pages /Kids [${pageIds.joinToString(" ") { "$it 0 R" }}] " +
                "/Count ${pageIds.size} >>\nendobj\n")
            objectStart(3)
            write("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>\nendobj\n")
            val xref = count
            write("xref\n0 ${offsets.size}\n0000000000 65535 f \n")
            for (id in 1 until offsets.size) {
                write(String.format(Locale.US, "%010d 00000 n \n", offsets[id]))
            }
            write("trailer\n<< /Size ${offsets.size} /Root 1 0 R >>\nstartxref\n$xref\n%%EOF\n")
            stream.flush()
            stream.close()
            finished = true
        }

        fun bytes(): ByteArray {
            finish()
            return file.readBytes()
        }

        fun save(context: Context): Int {
            finish()
            val name = "galiciana-seleccion-${System.currentTimeMillis()}.pdf"
            savePdfFile(context, name, file)
            return pageIds.size
        }

        override fun close() {
            if (!finished) stream.close()
            file.delete()
        }
    }

    suspend fun ocr(path: String, position: Int): String = try {
        require(path.matches(Regex("\\d+")) && position > 0)
        withContext(Dispatchers.IO) {
            Http.client.newCall(Request.Builder().url(GalicianaUrls.ocr(path, position)).build())
                .execute().use { response ->
                    if (!response.isSuccessful) return@use ""
                    val text = response.body?.string().orEmpty()
                    text.takeUnless {
                        it.isBlank() || it.trimStart().let { start ->
                            start.startsWith("<", true) || start.startsWith("{", true)
                        }
                    }.orEmpty()
                }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        ""
    }

    private fun savePdfFile(context: Context, name: String, file: File) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val folder = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: context.filesDir, "Galiciana")
            if (!folder.exists() && !folder.mkdirs()) error("No se pudo crear la carpeta")
            file.copyTo(File(folder, name), overwrite = false)
            return
        }
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Galiciana")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: error("No se pudo crear el PDF")
        try {
            resolver.openOutputStream(uri)?.use { target -> file.inputStream().use { it.copyTo(target) } }
                ?: error("No se pudo escribir el PDF")
            resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
    }

    suspend fun pdf(context: Context, path: String, position: Int, knownImageId: String? = null): ByteArray = withContext(Dispatchers.IO) {
        require(path.matches(Regex("\\d+")) && position > 0)
        val imageId = knownImageId ?: MetsRepository().load(path).pages.firstOrNull { it.position == position }?.imageId
            ?: error("Imagen no identificada")
        val image = gal.galiciana.app.ui.viewer.fetchFullGalicianaPage(path, position, imageId)
        CombinedPdf(context).use { document ->
            document.add(image, ocr(path, position))
            document.bytes()
        }
    }

    fun save(context: Context, path: String, position: Int, bytes: ByteArray, format: PageFormat) {
        val isPdf = format == PageFormat.PDF
        val jpeg = bytes.size > 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte()
        val png = bytes.size > 8 && bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte()
        if (isPdf) {
            if (bytes.size < 5 || !bytes.copyOfRange(0, 5).contentEquals("%PDF-".toByteArray()))
                error("La respuesta no es un PDF")
        } else if (!jpeg && !png) error("La respuesta no es una imagen")
        // The image fallback can be PNG. A JPEG choice must produce a JPEG file.
        val output = if (!isPdf && png) {
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: error("No se pudo convertir la imagen a JPEG")
            try {
                ByteArrayOutputStream().use { target ->
                    check(bitmap.compress(Bitmap.CompressFormat.JPEG, 95, target))
                    target.toByteArray()
                }
            } finally { bitmap.recycle() }
        } else bytes
        val extension = if (isPdf) "pdf" else "jpg"
        val name = "galiciana-$path-$position.$extension"
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val folder = File(context.getExternalFilesDir(
                if (isPdf) Environment.DIRECTORY_DOWNLOADS else Environment.DIRECTORY_PICTURES,
            ) ?: context.filesDir, "Galiciana")
            if (!folder.exists() && !folder.mkdirs()) error("No se pudo crear la carpeta")
            File(folder, name).writeBytes(output)
            return
        }
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE,
                if (isPdf) "application/pdf" else "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH,
                (if (isPdf) Environment.DIRECTORY_DOWNLOADS else Environment.DIRECTORY_PICTURES) + "/Galiciana")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val collection = if (isPdf) MediaStore.Downloads.EXTERNAL_CONTENT_URI
            else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val uri = resolver.insert(collection, values) ?: error("No se pudo crear el archivo")
        try {
            resolver.openOutputStream(uri)?.use { it.write(output) } ?: error("No se pudo escribir el archivo")
            resolver.update(uri, ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }, null, null)
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
    }
}
