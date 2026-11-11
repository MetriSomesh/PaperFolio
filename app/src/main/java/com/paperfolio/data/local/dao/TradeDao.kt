package com.paperfolio.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.paperfolio.data.local.entity.TradeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TradeDao {

    @Query("SELECT * FROM trade WHERE portfolioId = :portfolioId ORDER BY timestamp DESC")
    fun getTradesForPortfolio(portfolioId: Long): Flow<List<TradeEntity>>

    @Query("SELECT * FROM trade WHERE portfolioId = :portfolioId ORDER BY timestamp ASC")
    suspend fun getTradesForPortfolioList(portfolioId: Long): List<TradeEntity>

    @Query("SELECT * FROM trade ORDER BY timestamp DESC")
    fun getAllTrades(): Flow<List<TradeEntity>>

    @Query("SELECT * FROM trade ORDER BY timestamp ASC")
    suspend fun getAllTradesList(): List<TradeEntity>

    @Query("SELECT * FROM trade WHERE portfolioId = :portfolioId AND yahooSymbol = :yahooSymbol ORDER BY timestamp ASC")
    suspend fun getTradesForStock(portfolioId: Long, yahooSymbol: String): List<TradeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrade(trade: TradeEntity): Long

    @Delete
    suspend fun deleteTrade(trade: TradeEntity)

    @Query("SELECT * FROM trade WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    suspend fun getTradesBetweenDates(startTime: Long, endTime: Long): List<TradeEntity>
}
