package com.paperfolio.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.paperfolio.data.local.entity.StockMasterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StockMasterDao {

    @Query("""
        SELECT * FROM stock_master 
        WHERE tradingSymbol LIKE :query || '%' 
           OR companyName LIKE '%' || :query || '%' 
        ORDER BY 
            CASE 
                WHEN tradingSymbol = :query THEN 1
                WHEN tradingSymbol LIKE :query || '%' THEN 2
                ELSE 3
            END,
            tradingSymbol ASC
        LIMIT :limit
    """)
    suspend fun searchStocks(query: String, limit: Int = 50): List<StockMasterEntity>

    @Query("SELECT * FROM stock_master WHERE yahooSymbol = :yahooSymbol LIMIT 1")
    suspend fun getStockByYahooSymbol(yahooSymbol: String): StockMasterEntity?

    @Query("SELECT * FROM stock_master WHERE tradingSymbol = :tradingSymbol LIMIT 1")
    suspend fun getStockByTradingSymbol(tradingSymbol: String): StockMasterEntity?

    @Query("SELECT * FROM stock_master WHERE yahooSymbol IN (:symbols)")
    suspend fun getStocksBySymbols(symbols: List<String>): List<StockMasterEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStock(stock: StockMasterEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllStocks(stocks: List<StockMasterEntity>)

    @Query("SELECT COUNT(*) FROM stock_master")
    suspend fun getStockCount(): Int
}
