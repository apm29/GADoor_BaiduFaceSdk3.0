package com.apm.data.persistence

import android.content.Context
import android.graphics.Bitmap
import android.renderscript.*

class BitmapUtils private constructor(context: Context) {

    companion object {

        var instance: BitmapUtils? = null

        @JvmStatic
        fun getInstance(context: Context) {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        instance = BitmapUtils(context)
                    }
                }
            }
        }
    }

    private var rs: RenderScript? = RenderScript.create(context)
    private var yuvToRgbIntrinsic: ScriptIntrinsicYuvToRGB = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs))
    private var yuvType: Type.Builder? = null
    private var rgbaType: Type.Builder? = null
    private var `in`: Allocation? = null
    private var out: Allocation? = null


    fun createBitmapByByteArray(data: ByteArray,width:Int,height:Int): Bitmap {
        yuvType = Type.Builder(rs, Element.U8(rs)).setX(data.size)
        `in` = Allocation.createTyped(rs, yuvType!!.create(), Allocation.USAGE_SCRIPT)

        rgbaType = Type.Builder(rs, Element.RGBA_8888(rs))
                .setX(height)
                .setY(width)
        out = Allocation.createTyped(rs, rgbaType!!.create(), Allocation.USAGE_SCRIPT)

        `in`!!.copyFrom(data)

        yuvToRgbIntrinsic.setInput(`in`)
        yuvToRgbIntrinsic.forEach(out)

        val bmpout = Bitmap.createBitmap(height, width, Bitmap.Config.ARGB_8888)
        out!!.copyTo(bmpout)
        return bmpout
    }


}