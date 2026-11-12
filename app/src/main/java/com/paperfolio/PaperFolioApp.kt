package com.paperfolio

import android.app.Application
import android.util.Log
import com.paperfolio.data.local.AppDatabase
import com.paperfolio.data.remote.YahooFinanceApi
import com.paperfolio.data.repository.PortfolioRepository
import com.paperfolio.data.repository.StockRepository
import com.paperfolio.data.repository.TradeRepository
import com.paperfolio.worker.DailySnapshotWorker

class PaperFolioApp : Application() {

    // NOTE: this used to catch ANY database error and call
    // deleteDatabase("paperfolio_v2.db") — i.e. silently erase every
    // portfolio and trade the moment opening the DB hiccuped once.
    // That catch-all is gone. A database failure must now surface as an
    // error, never as silent data loss.
    val database: AppDatabase by lazy {
        AppDatabase.getInstance(this)
    }

    val yahooApi: YahooFinanceApi by lazy {
        YahooFinanceApi.create()
    }

    val stockRepository: StockRepository by lazy {
        StockRepository(database.stockMasterDao(), yahooApi)
    }

    val tradeRepository: TradeRepository by lazy {
        TradeRepository(database)
    }

    val portfolioRepository: PortfolioRepository by lazy {
        PortfolioRepository(
            database,
            tradeRepository,
            stockRepository
        )
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        try {
            DailySnapshotWorker.schedule(this)
        } catch (e: Throwable) {
            Log.e("PaperFolioApp", "Error scheduling DailySnapshotWorker", e)
        }
    }

    companion object {
        lateinit var instance: PaperFolioApp
            private set
    }
}
