package com.example.truxpense.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "income")
data class IncomeEntity(
    @PrimaryKey val id: String,
    val amount: Double,
    val source: String,          // e.g. "Salary", "Freelance", "Business"
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val paymentMethod: String = "",   // e.g. "Bank Transfer", "Cash", "UPI"
)

