package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [ClientEntity::class, ClientNoteEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(CrmConverters::class)
abstract class CrmDatabase : RoomDatabase() {
    abstract fun crmDao(): CrmDao

    companion object {
        @Volatile
        private var INSTANCE: CrmDatabase? = null

        fun getInstance(context: Context): CrmDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CrmDatabase::class.java,
                    "auto_agenda_crm.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
