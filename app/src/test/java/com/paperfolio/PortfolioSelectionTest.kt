package com.paperfolio

import com.paperfolio.domain.model.Portfolio
import com.paperfolio.ui.components.resolveInitialPortfolioId
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Regression test for the wrong-portfolio buy bug (reported Nov 2026):
 * portfolios A (id 1, created first) and B (id 2) exist; the list the
 * app shows is newest-first, so it arrives as [B, A]. A buy started
 * from A's page used to open the sheet with B pre-selected — the first
 * portfolio in the list — and the stock landed in B.
 *
 * The fix threads the originating portfolio through the navigation
 * (PortfolioDetail -> Search -> StockDetail -> buy sheet) and resolves
 * it here: the requested portfolio wins when it exists, otherwise the
 * first portfolio in the list. Pure JVM test, no database needed.
 */
class PortfolioSelectionTest {

    private fun portfolio(id: Long, name: String) = Portfolio(
        id = id,
        name = name,
        createdAt = id * 1000L,
        description = "",
        colorTag = "#122D28",
        initialBalance = 100000.0,
        cashBalance = 100000.0
    )

    // Newest-first, as PortfolioDao returns it (ORDER BY createdAt DESC).
    private val newestFirst = listOf(portfolio(2, "B"), portfolio(1, "A"))

    @Test
    fun requestedPortfolioWinsOverFirstInList() {
        // Buy started from A's page: A (id 1) must be pre-selected,
        // even though B is first in the newest-first list.
        assertEquals(1L, resolveInitialPortfolioId(newestFirst, 1L))
    }

    @Test
    fun noRequestFallsBackToFirstInList() {
        // Buy from the Home search tab: no originating portfolio, so
        // the first portfolio in the list is the default.
        assertEquals(2L, resolveInitialPortfolioId(newestFirst, null))
        assertEquals(2L, resolveInitialPortfolioId(newestFirst, -1L))
    }

    @Test
    fun staleRequestedIdFallsBackToFirstInList() {
        // Portfolio deleted since the navigation started: never buy
        // into a non-existent portfolio id.
        assertEquals(2L, resolveInitialPortfolioId(newestFirst, 99L))
    }

    @Test
    fun emptyPortfolioListResolvesToZero() {
        assertEquals(0L, resolveInitialPortfolioId(emptyList(), 1L))
        assertEquals(0L, resolveInitialPortfolioId(emptyList(), null))
    }
}
