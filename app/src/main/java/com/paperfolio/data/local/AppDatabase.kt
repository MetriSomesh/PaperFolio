package com.paperfolio.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.paperfolio.data.local.dao.PortfolioDao
import com.paperfolio.data.local.dao.PortfolioSnapshotDao
import com.paperfolio.data.local.dao.StockMasterDao
import com.paperfolio.data.local.dao.TradeDao
import com.paperfolio.data.local.entity.PortfolioEntity
import com.paperfolio.data.local.entity.PortfolioSnapshotEntity
import com.paperfolio.data.local.entity.StockMasterEntity
import com.paperfolio.data.local.entity.TradeEntity
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

@Database(
    entities = [
        PortfolioEntity::class,
        StockMasterEntity::class,
        TradeEntity::class,
        PortfolioSnapshotEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun portfolioDao(): PortfolioDao
    abstract fun stockMasterDao(): StockMasterDao
    abstract fun tradeDao(): TradeDao
    abstract fun portfolioSnapshotDao(): PortfolioSnapshotDao

    companion object {
        private const val TAG = "AppDatabase"
        private const val DATABASE_NAME = "paperfolio_v2.db"

        // NOTE: the file name is deliberately unchanged from v1.0.1. Existing
        // installs keep their portfolios and trades in this file.

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: create(context.applicationContext).also { INSTANCE = it }
            }
        }

        /**
         * Builds a NEW database instance (no singleton caching). Used by the
         * persistence regression tests to simulate a cold process restart:
         * create -> insert -> close -> create again -> data must survive.
         */
        fun create(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                // Deliberately NO createFromAsset() and NO
                // fallbackToDestructiveMigration() here.
                //
                // The old code combined both, which is what wiped user data:
                // the bundled stock_master.db shipped with PRAGMA user_version
                // = 0 while Room expected version 1, so on (re)open Room's
                // copy-helper saw a version mismatch with no migration path
                // and deleted the live database file and re-copied the asset
                // — portfolios and trades included. The stock catalog is now
                // seeded from the asset into an ordinary Room database
                // (see CatalogSeedCallback), so user tables can never be
                // wiped by a catalog operation again.
                .addCallback(CatalogSeedCallback(context.applicationContext))
                .build()
        }
    }
}

/**
 * Seeds the 8,100+ row stock catalog from the bundled `stock_master.db`
 * asset into the Room database, without ever touching the user tables
 * (`portfolio`, `trade`, `portfolio_snapshot`).
 *
 * - On a fresh install Room calls [onCreate] once: the catalog is copied
 *   from the asset with ATTACH + INSERT SELECT.
 * - On every open, [onOpen] checks whether the catalog is empty and
 *   re-seeds if needed. This self-heals installs whose catalog was wiped
 *   by the old createFromAsset bug, and it can never delete user data:
 *   seeding only ever INSERTs into `stock_master` / `stock_master_fts`.
 * - Seeding never throws: a failure is logged and retried on the next
 *   open, so a bad asset can never break app startup.
 */
private class CatalogSeedCallback(
    private val appContext: Context
) : RoomDatabase.Callback() {

    private val healAttempted = AtomicBoolean(false)

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        seedCatalog(db)
    }

    override fun onOpen(db: SupportSQLiteDatabase) {
        super.onOpen(db)
        // Self-heal exactly once per process: if the catalog is empty
        // (e.g. wiped by the old pre-packaged-DB bug), re-seed it.
        if (healAttempted.compareAndSet(false, true)) {
            try {
                val count = db.query("SELECT COUNT(*) FROM stock_master").use { cursor ->
                    if (cursor.moveToFirst()) cursor.getLong(0) else 0L
                }
                if (count == 0L) {
                    Log.i("CatalogSeed", "stock_master is empty, re-seeding catalog from asset")
                    seedCatalog(db)
                }
            } catch (e: Exception) {
                Log.e("CatalogSeed", "Catalog empty-check failed, will retry on next open", e)
            }
        }
    }

    private fun seedCatalog(db: SupportSQLiteDatabase) {
        var seedFile: File? = null
        var assetDb: SQLiteDatabase? = null
        try {
            seedFile = File(appContext.cacheDir, "stock_master_seed.db")
            appContext.assets.open("stock_master.db").use { input ->
                seedFile.outputStream().use { output -> input.copyTo(output) }
            }
            // Open the asset as its own read-only database and copy rows
            // over with plain INSERTs. Do NOT use ATTACH + cross-database
            // INSERT...SELECT here: ATTACH is per-connection, and Room
            // may serve statements from a pooled connection that never
            // saw the ATTACH — verified in test, the SELECT on the
            // attached DB returned 8,100 rows while the very next
            // INSERT...SELECT failed with "no such table: seed.stock_master".
            assetDb = SQLiteDatabase.openDatabase(
                seedFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY
            )
            // onCreate already runs inside a transaction; onOpen (the
            // self-heal path) does not — only manage one when needed.
            val inTx = db.inTransaction()
            if (!inTx) db.beginTransaction()
            try {
                assetDb.rawQuery(
                    "SELECT yahooSymbol, tradingSymbol, companyName, exchange, sector, isCustom FROM stock_master",
                    null
                ).use { cursor ->
                    val yahooIdx = cursor.getColumnIndexOrThrow("yahooSymbol")
                    val tradingIdx = cursor.getColumnIndexOrThrow("tradingSymbol")
                    val companyIdx = cursor.getColumnIndexOrThrow("companyName")
                    val exchangeIdx = cursor.getColumnIndexOrThrow("exchange")
                    val sectorIdx = cursor.getColumnIndexOrThrow("sector")
                    val customIdx = cursor.getColumnIndexOrThrow("isCustom")
                    while (cursor.moveToNext()) {
                        db.execSQL(
                            """
                            INSERT OR IGNORE INTO stock_master
                                (yahooSymbol, tradingSymbol, companyName, exchange, sector, isCustom)
                            VALUES (?,?,?,?,?,?)
                            """.trimIndent(),
                            arrayOf<Any?>(
                                cursor.getString(yahooIdx),
                                cursor.getString(tradingIdx),
                                cursor.getString(companyIdx),
                                cursor.getString(exchangeIdx),
                                if (cursor.isNull(sectorIdx)) null else cursor.getString(sectorIdx),
                                cursor.getInt(customIdx)
                            )
                        )
                    }
                }
                // Rebuild the search index from the catalog (deterministic:
                // a re-seed can never leave duplicate FTS rows behind).
                // NOTE: stock_master_fts is NOT a Room entity, so Room
                // never creates it — the seed must create it itself,
                // using the same definition as the bundled asset, or
                // the DELETE below throws and the whole seed fails.
                db.execSQL(
                    """
                    CREATE VIRTUAL TABLE IF NOT EXISTS stock_master_fts USING fts4(
                        yahooSymbol,
                        tradingSymbol,
                        companyName
                    )
                    """.trimIndent()
                )
                db.execSQL("DELETE FROM stock_master_fts")
                db.execSQL(
                    """
                    INSERT INTO stock_master_fts (yahooSymbol, tradingSymbol, companyName)
                    SELECT yahooSymbol, tradingSymbol, companyName FROM stock_master
                    """.trimIndent()
                )
                val count = db.query("SELECT COUNT(*) FROM stock_master").use { cursor ->
                    if (cursor.moveToFirst()) cursor.getLong(0) else 0L
                }
                Log.i("CatalogSeed", "Seeded $count stocks from asset")
                if (!inTx) db.setTransactionSuccessful()
            } finally {
                if (!inTx) db.endTransaction()
            }
        } catch (e: Exception) {
            // Never fail the database open because seeding failed.
            Log.e("CatalogSeed", "Failed to seed stock catalog from asset", e)
        } finally {
            try {
                assetDb?.close()
            } catch (_: Exception) {
            }
            try {
                seedFile?.delete()
            } catch (_: Exception) {
            }
        }
    }
}
