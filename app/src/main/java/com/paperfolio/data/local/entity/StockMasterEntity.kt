package com.paperfolio.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stock_master",
    indices = [
        Index(value = ["tradingSymbol"]),
        Index(value = ["exchange"]),
        Index(value = ["companyName"])
    ]
)
data class StockMasterEntity(
    @PrimaryKey
    val yahooSymbol: String,
    val tradingSymbol: String,
    val companyName: String,
    val exchange: String,
    val sector: String? = null,
    val isCustom: Boolean = false
)
