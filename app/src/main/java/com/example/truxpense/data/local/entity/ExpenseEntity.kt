package com.example.truxpense.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val amount: Double,
    val category: String,
    val paymentMethod: String,
    val merchant: String,
    val timestamp: Long,
    val notes: String = "",
    val source: String = "manual",   // "manual" | "sms"
)

