package com.paperfolio.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "portfolio")
data class PortfolioEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val description: String = "",
    val colorTag: String = "#00D09C",
    val initialBalance: Double = 100000.0,
    val cashBalance: Double = 100000.0
)
