package com.paperfolio.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "trade",
    foreignKeys = [
        ForeignKey(
            entity = PortfolioEntity::class,
            parentColumns = ["id"],
            childColumns = ["portfolioId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["portfolioId"]),
        Index(value = ["yahooSymbol"]),
        Index(value = ["timestamp"])
    ]
)
data class TradeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val portfolioId: Long,
    val yahooSymbol: String,
    val type: String, // "BUY" or "SELL"
    val quantity: Int,
    val price: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val realizedPnl: Double? = null,
    val notes: String? = null
)
