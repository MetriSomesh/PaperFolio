package com.paperfolio.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.paperfolio.data.local.dao.PortfolioDao
import com.paperfolio.data.local.dao.PortfolioSnapshotDao
import com.paperfolio.data.local.dao.StockMasterDao
import com.paperfolio.data.local.dao.TradeDao
import com.paperfolio.data.local.entity.PortfolioEntity
import com.paperfolio.data.local.entity.PortfolioSnapshotEntity
import com.paperfolio.data.local.entity.StockMasterEntity
import com.paperfolio.data.local.entity.TradeEntity

@Database(
    entities = [
        PortfolioEntity::class,
        StockMasterEntity::class,
        TradeEntity::class,
        PortfolioSnapshotEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun portfolioDao(): PortfolioDao
    abstract fun stockMasterDao(): StockMasterDao
    abstract fun tradeDao(): TradeDao
    abstract fun portfolioSnapshotDao(): PortfolioSnapshotDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "paperfolio_v2.db"
                )
                    .createFromAsset("stock_master.db")
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
