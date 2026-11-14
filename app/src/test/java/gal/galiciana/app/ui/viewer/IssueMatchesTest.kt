package gal.galiciana.app.ui.viewer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IssueMatchesTest {
    @Test fun readsNearestPagesFirst() {
        assertEquals(listOf(3, 4, 2, 5, 1, 6), issueScanOrder(3, 6))
    }

    @Test fun coversEveryPageFromTheEdges() {
        assertEquals(listOf(1, 2, 3, 4), issueScanOrder(1, 4))
        assertEquals(listOf(4, 3, 2, 1), issueScanOrder(4, 4))
    }

    @Test fun longDocumentsStopAtTheCapAroundTheOpenPage() {
        val order = issueScanOrder(200, 400, cap = 5)
        assertEquals(listOf(200, 201, 199, 202, 198), order)
    }

    @Test fun emptyDocumentHasNothingToRead() {
        assertEquals(emptyList<Int>(), issueScanOrder(1, 0))
    }

    @Test fun nextMatchPageMovesForwardAndWraps() {
        val pages = listOf(3, 5, 9)
        assertEquals(5, adjacentMatchPage(pages, 3, forward = true))
        assertEquals(5, adjacentMatchPage(pages, 4, forward = true))
        assertEquals(3, adjacentMatchPage(pages, 9, forward = true))
    }

    @Test fun previousMatchPageMovesBackAndWraps() {
        val pages = listOf(3, 5, 9)
        assertEquals(3, adjacentMatchPage(pages, 5, forward = false))
        assertEquals(9, adjacentMatchPage(pages, 3, forward = false))
    }

    @Test fun noOtherPageMeansNoJump() {
        assertNull(adjacentMatchPage(listOf(4), 4, forward = true))
        assertNull(adjacentMatchPage(emptyList(), 4, forward = false))
    }
}
