package gal.galiciana.app.ui.viewer

import gal.galiciana.app.data.OcrRepository.AltoHit
import kotlin.math.min

internal data class PageTransform(val scale: Float, val x: Float, val y: Float)

/** Both drawing and navigation use the same image-fit transform. */
internal fun constrainPageTransform(
    viewportWidth: Float, viewportHeight: Float, imageWidth: Float, imageHeight: Float,
    scale: Float, x: Float, y: Float,
): PageTransform {
    val zoom = scale.coerceIn(1f, 8f)
    val fit = min(viewportWidth / imageWidth.coerceAtLeast(1f),
        viewportHeight / imageHeight.coerceAtLeast(1f))
    val limitX = ((imageWidth * fit * zoom - viewportWidth) / 2f).coerceAtLeast(0f)
    val limitY = ((imageHeight * fit * zoom - viewportHeight) / 2f).coerceAtLeast(0f)
    return PageTransform(zoom, x.coerceIn(-limitX, limitX), y.coerceIn(-limitY, limitY))
}

internal fun focusPageMatch(
    viewportWidth: Float, viewportHeight: Float, imageWidth: Float, imageHeight: Float,
    pageWidth: Float, pageHeight: Float, hit: AltoHit, currentScale: Float,
): PageTransform {
    val fit = min(viewportWidth / imageWidth.coerceAtLeast(1f),
        viewportHeight / imageHeight.coerceAtLeast(1f))
    val widthAtFit = hit.width * imageWidth * fit / pageWidth.coerceAtLeast(1f)
    val zoom = maxOf(currentScale, (viewportWidth * 0.65f / widthAtFit.coerceAtLeast(1f))
        .coerceIn(2f, 5f)).coerceAtMost(8f)
    val x = (pageWidth / 2f - hit.left - hit.width / 2f) * imageWidth * fit * zoom / pageWidth
    val y = (pageHeight / 2f - hit.top - hit.height / 2f) * imageHeight * fit * zoom / pageHeight
    return constrainPageTransform(viewportWidth, viewportHeight, imageWidth, imageHeight, zoom, x, y)
}
