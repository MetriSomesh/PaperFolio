package com.paperfolio

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.paperfolio.data.local.AppDatabase
import com.paperfolio.data.local.entity.PortfolioEntity
import com.paperfolio.data.local.entity.TradeEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Regression test for the v1.0.1 data-loss bug (Nov 2026):
 *
 * creating a portfolio, adding stocks, then swiping the app away from
 * Recents and reopening it wiped every portfolio and trade.
 *
 * Root cause: the old database was built with
 * `.createFromAsset("stock_master.db")` + `.fallbackToDestructiveMigration()`.
 * The bundled asset shipped with `PRAGMA user_version = 0` while Room
 * expected version 1, so on every cold (re)open Room's copy-helper saw a
 * version mismatch with no migration path and deleted the live database
 * file and re-copied the asset — user tables included.
 *
 * The fix seeds the catalog with INSERTs into an ordinary Room database
 * (CatalogSeedCallback) and never deletes the file. This test simulates
 * the exact user flow — create, insert, close (process death), reopen —
 * twice, and asserts the user data and the catalog both survive.
 *
 * Runs on the JVM under Robolectric against the REAL on-disk database
 * file ("paperfolio_v2.db"), not an in-memory database, so it exercises
 * the same open/seed/close path the app uses.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PersistenceTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // Start each test from a clean slate, like a fresh install.
        context.deleteDatabase("paperfolio_v2.db")
    }

    @Test
    fun portfoliosAndTradesSurviveColdRestart() = runBlocking {
        // --- First "process": create a portfolio and a trade -----------
        val db1 = AppDatabase.create(context)
        val portfolioId = db1.portfolioDao().insertPortfolio(
            PortfolioEntity(
                name = "Regression Portfolio",
                initialBalance = 100_000.0,
                cashBalance = 95_000.0
            )
        )
        db1.tradeDao().insertTrade(
            TradeEntity(
                portfolioId = portfolioId,
                yahooSymbol = "RELIANCE.NS",
                type = "BUY",
                quantity = 2,
                price = 2500.0
            )
        )
        assertEquals(1, db1.portfolioDao().getPortfolioCount())
        assertEquals(1, db1.tradeDao().getAllTradesList().size)
        // Catalog must be seeded on first open.
        assertTrue(
            "catalog should be seeded on first open",
            db1.stockMasterDao().getStockCount() >= 8000
        )
        db1.close() // <-- simulates the app being swiped away / process death

        // --- Second "process": reopen and check everything is there ----
        val db2 = AppDatabase.create(context)
        val portfolios = db2.portfolioDao().getAllPortfoliosList()
        assertEquals(1, portfolios.size)
        assertEquals("Regression Portfolio", portfolios[0].name)
        assertEquals(95_000.0, portfolios[0].cashBalance, 0.001)

        val trades = db2.tradeDao().getTradesForPortfolioList(portfolioId)
        assertEquals(1, trades.size)
        assertEquals("RELIANCE.NS", trades[0].yahooSymbol)
        assertEquals(2, trades[0].quantity)

        // Catalog must still be intact (and not re-seeded over user data).
        assertTrue(db2.stockMasterDao().getStockCount() >= 8000)
        assertTrue(db2.stockMasterDao().searchStocks("RELIANCE").isNotEmpty())
        db2.close()

        // --- Third "process": reopen once more — still there ------------
        val db3 = AppDatabase.create(context)
        assertEquals(1, db3.portfolioDao().getPortfolioCount())
        assertEquals(1, db3.tradeDao().getAllTradesList().size)
        assertTrue(db3.stockMasterDao().getStockCount() >= 8000)
        db3.close()
    }

    @Test
    fun emptyCatalogSelfHealsWithoutTouchingUserData() = runBlocking {
        // If the catalog is somehow empty (e.g. an install damaged by the
        // old createFromAsset bug), the next open re-seeds it — and must
        // not delete the user's portfolios to do so.
        val db1 = AppDatabase.create(context)
        val portfolioId = db1.portfolioDao().insertPortfolio(
            PortfolioEntity(name = "Survivor", initialBalance = 50_000.0, cashBalance = 50_000.0)
        )
        // Simulate a wiped catalog: delete only the catalog tables' rows.
        // (The FTS table only exists once the seed has run — it is not a
        // Room entity — so delete from it only if it is there.)
        db1.openHelper.writableDatabase.execSQL("DELETE FROM stock_master")
        val ftsExists = db1.openHelper.readableDatabase.query(
            "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='stock_master_fts'"
        ).use { c -> if (c.moveToFirst()) c.getLong(0) > 0 else false }
        if (ftsExists) {
            db1.openHelper.writableDatabase.execSQL("DELETE FROM stock_master_fts")
        }
        assertEquals(0, db1.stockMasterDao().getStockCount())
        db1.close()

        val db2 = AppDatabase.create(context)
        assertTrue(
            "catalog should self-heal on reopen",
            db2.stockMasterDao().getStockCount() >= 8000
        )
        assertEquals(1, db2.portfolioDao().getPortfolioCount())
        assertEquals("Survivor", db2.portfolioDao().getPortfolioByIdOnce(portfolioId)?.name)
        db2.close()
    }
}
