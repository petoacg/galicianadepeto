package gal.galiciana.app

import gal.galiciana.app.data.OcrRepository.AltoHit
import gal.galiciana.app.ui.viewer.constrainPageTransform
import gal.galiciana.app.ui.viewer.focusPageMatch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ViewerGeometryTest {
    @Test fun fittedPageCannotBePannedIntoEmptyMargins() {
        val transform = constrainPageTransform(400f, 600f, 1000f, 2000f, 1f, 500f, -500f)
        assertEquals(0f, transform.x, 0.001f)
        assertEquals(0f, transform.y, 0.001f)
    }

    @Test fun selectingAnOffscreenMatchMovesTowardItsActualImageCoordinates() {
        val transform = focusPageMatch(400f, 600f, 1000f, 2000f, 1000f, 2000f,
            AltoHit(700f, 1500f, 100f, 30f), 1f)
        assertTrue(transform.scale >= 2f)
        val renderedX = 200f + transform.x + (750f - 500f) * 0.3f * transform.scale
        val renderedY = 300f + transform.y + (1515f - 1000f) * 0.3f * transform.scale
        assertEquals(200f, renderedX, 0.01f)
        assertEquals(300f, renderedY, 0.01f)
    }

    @Test fun panningLimitsFollowImageAspectRatioRatherThanViewportAspectRatio() {
        val transform = constrainPageTransform(400f, 600f, 2000f, 1000f, 2f, 999f, 999f)
        assertEquals(200f, transform.x, 0.001f)
        assertEquals(0f, transform.y, 0.001f)
    }
}
