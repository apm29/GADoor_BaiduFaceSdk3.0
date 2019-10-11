package com.spark.zj.comcom.serial

import java.lang.StringBuilder


object ByteCRC16 {

    private fun getCRC16(bytes: ByteArray, INIT_CRC: Int = 0xFFFF, POLYNOMIAL: Int = 0x8408): String {

        val uBytes = UByteArray(bytes.size) { i ->
            return@UByteArray bytes[i].toUByte()
        }
        var crcValue = INIT_CRC
        for (uByte in uBytes) {
            crcValue = crcValue xor uByte.toInt()
            for (j in 0..7) {
                crcValue = if (crcValue and 0x0001 > 0) {
                    crcValue.shr(1) xor POLYNOMIAL
                } else {
                    crcValue.shr(1)
                }
            }
        }
        val crcString = StringBuilder(Integer.toHexString(crcValue))
        while (crcString.length < 4) {
            crcString.insert(0, "0")
        }
        //交换高位低位
        return crcString.substring(2, 4) + crcString.substring(0, 2)
    }

    fun verifyCRC16Data(bytes: ByteArray?): Boolean {
        if (bytes == null || bytes.size < 4) {
            return false
        }
        val dataArr = bytes.copyOfRange(0, bytes.size - 2)
        val crcRes = getCRC16(dataArr).toUpperCase()
        val crcCompare = bytes.copyOfRange(bytes.size - 2, bytes.size).toHexString().toUpperCase()
        return crcRes.equals(crcCompare, true)
    }


    fun getData(bytes: ByteArray?): ByteArray {
        return if (bytes == null || bytes.size < 6) {
            byteArrayOf()
        } else {
            bytes.copyOfRange(5, bytes.size - 2)
        }
    }

}

fun ByteArray.toHexString(): String {
    return this.joinToString("") {
        Integer.toHexString(it.toUByte().toInt())
    }.toUpperCase()
}

fun String.lastSixHex(): String {
    if (this.length < 6) {
        return this
    }
    return this.substring(this.length - 6)
}