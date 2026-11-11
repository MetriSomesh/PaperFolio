package com.paperfolio.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "portfolio_snapshot",
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
        Index(value = ["date"]),
        Index(value = ["portfolioId", "date"], unique = true)
    ]
)
data class PortfolioSnapshotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val portfolioId: Long,
    val date: String, // YYYY-MM-DD
    val totalValue: Double,
    val investedAmount: Double,
    val cashBalance: Double,
    val timestamp: Long = System.currentTimeMillis()
)
