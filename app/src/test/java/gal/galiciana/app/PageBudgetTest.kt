package gal.galiciana.app

import gal.galiciana.app.ui.viewer.pageBudget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PageBudgetTest {
    private val gb = 1_000_000_000L

    @Test fun capablePhonesKeepTheFullPage() {
        val budget = pageBudget(12 * gb, lowRam = false)
        assertEquals(7000, budget.longSide)
        assertEquals(22_000_000.0, budget.pixels, 0.0)
    }

    @Test fun smallerPhonesGetLighterPages() {
        val mid = pageBudget(4 * gb, lowRam = false)
        val small = pageBudget(2 * gb, lowRam = false)
        assertTrue(mid.pixels < 22_000_000.0 && small.pixels < mid.pixels)
        assertTrue(small.longSide < mid.longSide)
    }

    @Test fun lowRamFlagWinsOverReportedMemory() {
        assertEquals(pageBudget(2 * gb, lowRam = false), pageBudget(12 * gb, lowRam = true))
    }
}
