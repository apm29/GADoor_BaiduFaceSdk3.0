package com.apm.rs485reader.db.dao

import androidx.room.*
import com.apm.rs485reader.db.entity.User
import com.spark.zj.comcom.serial.lastSixHex
import com.spark.zj.comcom.serial.toHexString

@Dao
abstract class GateDao {
    @Query("SELECT * FROM t_user")
    abstract fun getAll(): List<User>

    @Update
    abstract fun updateUser(user: User)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun addUser(user: User):Long

    @Query("SELECT * FROM t_user WHERE t_user.remote_id = :remoteId")
    abstract fun getUserByRemoteId(remoteId:Long?): User?

    @Query("DELETE FROM t_user WHERE t_user.remote_id = :remoteId")
    abstract fun deleteUser(remoteId:Long):Int

    @Query("SELECT * FROM t_user WHERE t_user.hex = :hex")
    abstract fun getUserByHex(hex: String): User?

    fun exist(it: ByteArray?): Boolean {
        if (it == null) {
            return false
        }
        //E200196B06848027B6
        val user = getUserByHex(it.toHexString().lastSixHex())
        return user != null
    }


}