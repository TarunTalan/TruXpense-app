package com.example.truxpense.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.truxpense.data.local.dao.PendingTransactionDao
import com.example.truxpense.data.local.entity.PendingTransactionEntity

@Database(entities = [PendingTransactionEntity::class], version = 1, exportSchema = false)
abstract class SmsDatabase : RoomDatabase() {
    abstract fun pendingTransactionDao(): PendingTransactionDao

    companion object {
        private const val DB_NAME = "truxpense_sms.db"

        @Volatile
        private var INSTANCE: SmsDatabase? = null

        fun getInstance(context: Context): SmsDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SmsDatabase::class.java,
                    DB_NAME
                )
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}

