package com.apm.anxinju.main

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import com.apm.anxinju.main.server.AppServer

/**
 *  author : ciih
 *  date : 2019-09-28 15:14
 *  description :
 */
class App : Application() {

    companion object {
        @SuppressLint("StaticFieldLeak")
        @JvmStatic
        lateinit var contextGlobal: Context
        var initialized = false
    }



    override fun onCreate() {
        super.onCreate()
        contextGlobal = this
        if(!initialized) {
            try {
                AppServer(8089).start()
                initialized = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}