package com.apm.anxinju.main.utils

import android.util.Log
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


/**
 *  author : ciih
 *  date : 2019-09-28 15:01
 *  description :
 */
object QRCodeUtils {

    /**
     * 解析二维码
     *
     * @param binaryBitmap 被解析的图形对象
     * @return 解析的结果
     */
    private fun decode(binaryBitmap: BinaryBitmap): String? {
        try {
            val hints = HashMap<DecodeHintType, Any>()
            hints[DecodeHintType.CHARACTER_SET] = "utf-8"
            hints[DecodeHintType.TRY_HARDER] = java.lang.Boolean.TRUE
            hints[DecodeHintType.POSSIBLE_FORMATS] = BarcodeFormat.QR_CODE

            val result = QRCodeReader().decode(binaryBitmap, hints)

            return result.text
        } catch (e: NotFoundException) {
            return null
        } catch (e: ChecksumException) {
            return null
        } catch (e: FormatException) {
            return null
        }

    }

    fun decodeWithImage(data: ByteArray, width: Int, height: Int): String? {
        val hybridBinarizer = HybridBinarizer(
            PlanarYUVLuminanceSource(
                data, width, height, 0, 0, width, height, false
            )
        )
        Log.e("RESPONSE","QRCODE DETECT")
        return decode(BinaryBitmap(hybridBinarizer))
    }

    suspend fun decodeAsync(data: ByteArray, width: Int, height: Int) = suspendCoroutine<String?> {
        try {
            it.resume(decodeWithImage(data, width, height))
        } catch (e: Exception) {
            it.resumeWithException(e)
        }
    }

}