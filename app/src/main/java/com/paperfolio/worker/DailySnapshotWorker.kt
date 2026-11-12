package com.paperfolio.worker

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.paperfolio.PaperFolioApp
import com.paperfolio.data.local.AppDatabase
import com.paperfolio.data.local.entity.PortfolioSnapshotEntity
import com.paperfolio.data.remote.YahooFinanceApi
import com.paperfolio.data.repository.PortfolioRepository
import com.paperfolio.data.repository.StockRepository
import com.paperfolio.data.repository.TradeRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class DailySnapshotWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val app = applicationContext as PaperFolioApp
            val database = AppDatabase.getInstance(app)
            val api = YahooFinanceApi.create()
            val stockRepo = StockRepository(database.stockMasterDao(), api)
            val tradeRepo = TradeRepository(database)
            val portfolioRepo = PortfolioRepository(database, tradeRepo, stockRepo)

            val portfolios = database.portfolioDao().getAllPortfoliosList()
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            for (p in portfolios) {
                try {
                    val (enriched, _) = portfolioRepo.getPortfolioDetailsWithLivePrices(p.id)
                    database.portfolioSnapshotDao().insertSnapshot(
                        PortfolioSnapshotEntity(
                            portfolioId = p.id,
                            date = todayStr,
                            totalValue = enriched.totalNetWorth,
                            investedAmount = enriched.investedAmount,
                            cashBalance = enriched.cashBalance
                        )
                    )
                } catch (e: Exception) {
                    // Ignore single portfolio failure
                }
            }

            Result.success()
        } catch (e: Exception) {
            // Was Result.retry(): any failure (including a database
            // error) retried with backoff forever. A missed daily
            // snapshot is not worth that — the next periodic run in
            // 24h tries again anyway.
            Log.e("DailySnapshotWorker", "Snapshot run failed", e)
            Result.failure()
        }
    }

    companion object {
        private const val WORK_NAME = "DailyPortfolioSnapshotWork"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<DailySnapshotWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(1, TimeUnit.HOURS)
                // Snapshots need live prices; don't wake up offline
                // just to fail and (previously) retry forever.
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
