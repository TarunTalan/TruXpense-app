package com.example.truxpense.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a single savings entry / contribution towards a goal.
 * [goalName]  — e.g. "Emergency Fund", "Vacation", "New Phone"
 * [amount]    — amount saved in this entry
 * [notes]     — optional description
 * [timestamp] — epoch-ms of when the saving was recorded
 */
@Entity(tableName = "savings")
data class SavingsEntity(
    @PrimaryKey val id: String,
    val goalName: String,
    val amount: Double,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis(),
)

