package com.apm.rs485reader.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.apm.rs485reader.db.dao.GateDao
import com.apm.rs485reader.db.entity.User

@Database(version = 2,entities=[User::class])
abstract  class GateDataBase :RoomDatabase() {
    companion object{
        private const val DATA_BASE_NAME = "db_485.db"
        fun getDB(context: Context): GateDataBase {
            return Room.databaseBuilder(context, GateDataBase::class.java, DATA_BASE_NAME)
                    .allowMainThreadQueries()
                    .build()
        }
    }
    abstract fun getGateDao(): GateDao
}