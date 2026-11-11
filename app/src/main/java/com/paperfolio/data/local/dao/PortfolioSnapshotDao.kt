package com.paperfolio.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.paperfolio.data.local.entity.PortfolioSnapshotEntity

@Dao
interface PortfolioSnapshotDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: PortfolioSnapshotEntity)

    @Query("SELECT * FROM portfolio_snapshot WHERE portfolioId = :portfolioId ORDER BY date ASC")
    suspend fun getSnapshotsForPortfolio(portfolioId: Long): List<PortfolioSnapshotEntity>

    @Query("SELECT * FROM portfolio_snapshot WHERE portfolioId = :portfolioId AND date >= :startDate ORDER BY date ASC")
    suspend fun getSnapshotsSinceDate(portfolioId: Long, startDate: String): List<PortfolioSnapshotEntity>

    @Query("SELECT * FROM portfolio_snapshot WHERE portfolioId = :portfolioId ORDER BY date DESC LIMIT 1")
    suspend fun getLatestSnapshot(portfolioId: Long): PortfolioSnapshotEntity?

    @Query("SELECT * FROM portfolio_snapshot ORDER BY date ASC")
    suspend fun getAllSnapshots(): List<PortfolioSnapshotEntity>
}
