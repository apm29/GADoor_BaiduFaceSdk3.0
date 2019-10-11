package com.apm.rs485reader.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = "t_user",primaryKeys = ["uid","remote_id"])
data class User (
        @ColumnInfo
        val uid:Long,

        @ColumnInfo
        val remote_id:Long,

        @ColumnInfo
        val hex:String?,

        @ColumnInfo
        val validateTime:Long?,

        @ColumnInfo
        val updateTime:Long?

){


        constructor(remote_id: Long,hex: String?) : this(
                uid = 0,remote_id = remote_id,hex = hex,validateTime = null,updateTime = null
        )
}