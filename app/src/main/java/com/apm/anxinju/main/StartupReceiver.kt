package com.apm.anxinju.main

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 *  author : ciih
 *  date : 2019-09-28 11:25
 *  description :
 */
class StartupReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.apply {
            if(action == Intent.ACTION_BOOT_COMPLETED){
               context?.apply {
                   val newIntent
                   //1.如果自启动APP，参数为需要自动启动的应用包名
                           = context.packageManager.getLaunchIntentForPackage("com.apm.anxinju")?.apply {
                       addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                   }
                   context.startActivity(newIntent)
               }
            }
        }
    }
}