package gal.galiciana.app.ui.viewer

import gal.galiciana.app.data.OcrRepository.AltoHit
import kotlin.math.min

internal data class PageTransform(val scale: Float, val x: Float, val y: Float)

/**
 * The page rests in a fixed frame, the band of the viewport between [frameTop] and
 * [frameBottom] that the reading controls leave free. It is fitted and centred there
 * whether the controls show or not, so hiding them never moves it; zoomed in, it may
 * be panned across the whole viewport.
 */
internal fun pageFit(viewportWidth: Float, viewportHeight: Float, imageWidth: Float, imageHeight: Float,
                     frameTop: Float = 0f, frameBottom: Float = 0f): Float =
    min(viewportWidth / imageWidth.coerceAtLeast(1f),
        (viewportHeight - frameTop - frameBottom).coerceAtLeast(1f) / imageHeight.coerceAtLeast(1f))

/** Vertical centre of the page frame, the page's resting point. */
internal fun pageFrameCenter(viewportHeight: Float, frameTop: Float = 0f, frameBottom: Float = 0f): Float =
    frameTop + (viewportHeight - frameTop - frameBottom).coerceAtLeast(1f) / 2f

/** Both drawing and navigation use the same image-fit transform. */
internal fun constrainPageTransform(
    viewportWidth: Float, viewportHeight: Float, imageWidth: Float, imageHeight: Float,
    scale: Float, x: Float, y: Float, frameTop: Float = 0f, frameBottom: Float = 0f,
): PageTransform {
    val zoom = scale.coerceIn(1f, 8f)
    val fit = pageFit(viewportWidth, viewportHeight, imageWidth, imageHeight, frameTop, frameBottom)
    val limitX = ((imageWidth * fit * zoom - viewportWidth) / 2f).coerceAtLeast(0f)
    val half = imageHeight * fit * zoom / 2f
    val center = pageFrameCenter(viewportHeight, frameTop, frameBottom)
    // The page's edges may reach the viewport's edges, never leave a gap inside them.
    val minY = min(0f, viewportHeight - center - half)
    val maxY = maxOf(0f, half - center)
    return PageTransform(zoom, x.coerceIn(-limitX, limitX), y.coerceIn(minY, maxY))
}

internal fun focusPageMatch(
    viewportWidth: Float, viewportHeight: Float, imageWidth: Float, imageHeight: Float,
    pageWidth: Float, pageHeight: Float, hit: AltoHit, currentScale: Float,
    frameTop: Float = 0f, frameBottom: Float = 0f,
): PageTransform {
    val fit = pageFit(viewportWidth, viewportHeight, imageWidth, imageHeight, frameTop, frameBottom)
    val widthAtFit = hit.width * imageWidth * fit / pageWidth.coerceAtLeast(1f)
    val zoom = maxOf(currentScale, (viewportWidth * 0.65f / widthAtFit.coerceAtLeast(1f))
        .coerceIn(2f, 5f)).coerceAtMost(8f)
    val x = (pageWidth / 2f - hit.left - hit.width / 2f) * imageWidth * fit * zoom / pageWidth
    val y = (pageHeight / 2f - hit.top - hit.height / 2f) * imageHeight * fit * zoom / pageHeight
    return constrainPageTransform(viewportWidth, viewportHeight, imageWidth, imageHeight, zoom, x, y, frameTop, frameBottom)
}
