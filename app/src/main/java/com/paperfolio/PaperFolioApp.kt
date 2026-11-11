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

    val database: AppDatabase by lazy {
        try {
            AppDatabase.getInstance(this)
        } catch (e: Throwable) {
            Log.e("PaperFolioApp", "Error getting database, recreating...", e)
            deleteDatabase("paperfolio_v2.db")
            AppDatabase.getInstance(this)
        }
    }

    val yahooApi: YahooFinanceApi by lazy {
        YahooFinanceApi.create()
    }

    val stockRepository: StockRepository by lazy {
        StockRepository(database.stockMasterDao(), yahooApi)
    }

    val tradeRepository: TradeRepository by lazy {
        TradeRepository(database.tradeDao(), database.portfolioDao(), database.stockMasterDao())
    }

    val portfolioRepository: PortfolioRepository by lazy {
        PortfolioRepository(
            database.portfolioDao(),
            database.portfolioSnapshotDao(),
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
