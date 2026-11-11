package com.paperfolio.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.paperfolio.data.local.entity.PortfolioEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PortfolioDao {

    @Query("SELECT * FROM portfolio ORDER BY createdAt DESC")
    fun getAllPortfolios(): Flow<List<PortfolioEntity>>

    @Query("SELECT * FROM portfolio ORDER BY createdAt DESC")
    suspend fun getAllPortfoliosList(): List<PortfolioEntity>

    @Query("SELECT * FROM portfolio WHERE id = :id LIMIT 1")
    fun getPortfolioById(id: Long): Flow<PortfolioEntity?>

    @Query("SELECT * FROM portfolio WHERE id = :id LIMIT 1")
    suspend fun getPortfolioByIdOnce(id: Long): PortfolioEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPortfolio(portfolio: PortfolioEntity): Long

    @Update
    suspend fun updatePortfolio(portfolio: PortfolioEntity)

    @Query("UPDATE portfolio SET cashBalance = :newBalance WHERE id = :portfolioId")
    suspend fun updateCashBalance(portfolioId: Long, newBalance: Double)

    @Delete
    suspend fun deletePortfolio(portfolio: PortfolioEntity)

    @Query("SELECT COUNT(*) FROM portfolio")
    suspend fun getPortfolioCount(): Int
}
