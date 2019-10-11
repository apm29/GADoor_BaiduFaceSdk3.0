package com.apm.anxinju.main.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.util.Log
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import androidx.core.app.NotificationCompat
import com.apm.anxinju_baidufacesdk30.R
import com.apm.anxinju.main.activity.MainActivity
import com.apm.rs485reader.service.DataSyncService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStreamReader
import kotlin.concurrent.thread
import kotlin.coroutines.CoroutineContext


/**
 *  author : ciih
 *  date : 2019-08-14 15:27
 *  description :
 */
class KeepAliveService : Service(),CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    override fun onBind(intent: Intent?): IBinder? {
        throw UnsupportedOperationException()
    }

    private val handler: Handler = Handler()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        println("KeepAliveService.onStartCommand")
        println(intent)
        if (intent?.getBooleanExtra(KEY_SHOW_OVERLAY, false) == true) {
            showOverlay()
            return super.onStartCommand(intent, flags, startId)
        }
        restartAppIfNotInFront()
        startSelfWithNotification()
        return super.onStartCommand(intent, flags, startId)
    }

    private fun restartAppIfNotInFront() {
        println("KeepAliveService.restartAppIfNotInFront")
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed(object : Runnable {
            override fun run() {
                //printForegroundTask()
                //checkActivityInFront()
                checkForegroundActivity()
                handler.postDelayed(this, 3000)
            }
        }, 3_000)
    }

    private fun cancelRestart() {
        println("KeepAliveService.cancelRestart")
        handler.removeCallbacksAndMessages(null)
    }


    companion object {
        const val KEY_SHOW_OVERLAY = "SHOW_NOTIFICATION_DIALOG"
        const val TAG = "KeepAliveService"
        const val CHANNEL_ID = "KEEPALIVE-1"
    }





    private fun startSelfWithNotification() {
        println("KeepAliveService.startSelfWithNotification")
        val intent = Intent(this, KeepAliveService::class.java)
        intent.putExtra(KEY_SHOW_OVERLAY, true)
        val builder = Notification.Builder(this)
                .setContentIntent(PendingIntent.getService(this, 1231, intent, PendingIntent.FLAG_UPDATE_CURRENT))
                .setContentTitle("通行保活服务") // 设置下拉列表里的标题
                .setContentText("通行保活服务") // 设置上
                .setLargeIcon(BitmapFactory.decodeResource(this.resources, R.mipmap.ic_tracking_img)) // 设置下拉列表中的图标(大图标)
                .setSmallIcon(R.mipmap.ic_tracking_img) // 设// 下文内容
                .setWhen(System.currentTimeMillis()) // 设置该通知发生的时间

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                CHANNEL_ID,
                "通行保活服务",
                NotificationManager.IMPORTANCE_HIGH
            )
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(notificationChannel)
            val builderCompat = NotificationCompat.Builder(this, "KEEPALIVE-1")
                .setLargeIcon(
                    BitmapFactory.decodeResource(
                        this.resources,
                        R.mipmap.ic_image_attrs
                    )
                ) // 设置下拉列表中的图标(大图标)
                .setContentTitle("通行保活服务") // 设置下拉列表里的标题
                .setSmallIcon(R.mipmap.ic_check) // 设置状态栏内的小图标
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentText("通行保活服务") // 设置上下文内容
                .setWhen(System.currentTimeMillis()) // 设置该通知发生的时间
            val notification = builderCompat.build() // 获取构建好的Notification
            notification.defaults = Notification.DEFAULT_SOUND //设置为默认的声音
            startForeground(110, notification)
            return
        }

        val notification = builder.build()
        notification.defaults = Notification.DEFAULT_SOUND

        startForeground(1233, notification)
    }

    private fun showOverlay() {
        println("KeepAliveService.showOverlay")
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.layout_lock_dialog)
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        dialog.window?.setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT)

        val editText = dialog.findViewById<EditText>(R.id.edit_code)
        val btnConfirm = dialog.findViewById<Button>(R.id.btn_confirm)
        btnConfirm.setOnClickListener {
            if (editText.text.trim().toString() == "12306") {
                cancelRestart()
            } else if (editText.text.trim().toString() == "96315") {
                restartAppIfNotInFront()
            }
            dialog.dismiss()
        }

        dialog.show()
        handler.postDelayed({
            try {
                dialog.dismiss()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, 25_000)
    }

    private fun checkActivityInFront() {
        val foregroundActivityName = ForegroundUtils.getForegroundActivityName(this)
        if (foregroundActivityName != "com.baidu.idl.face.demo" || foregroundActivityName.isEmpty()) {
            println("foregroundActivityName = $foregroundActivityName")
            bringUpMainActivity()
        }
    }

    private fun bringUpMainActivity() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT)
        })
    }

    private val process by lazy {
        Runtime.getRuntime().exec("su")
    }
    private val outputStream by lazy {
        process.outputStream
    }
    private val bufferedReader by lazy {
        BufferedReader(InputStreamReader(process.inputStream))
    }
    private val bufferedErrorReader by lazy {
        BufferedReader(InputStreamReader(process.errorStream))
    }
    private val dataOutputStream by lazy {
        DataOutputStream(outputStream)
    }
    var initialized:Boolean = false

    private fun initErrorReader() {
        if(initialized){
            return
        }
        initialized = true
        launch {
            try {
                var line: String? = bufferedErrorReader.readLine()
                while (line != null) {
                    Log.e(TAG, line)
                    line = bufferedErrorReader.readLine()
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        launch {
            try {
                var line: String? = bufferedReader.readLine()
                while (line != null) {
                    Log.d(TAG, line?:"")
                    line = bufferedReader.readLine()
                    when {
                        line?.contains("PreviewActivity")  == true -> {
                            Log.d(TAG,"Preview Running")
                        }
                        line?.contains("QRCodeActivity")  == true -> {
                            Log.d(TAG,"QRCode Running")
                        }
                        else -> {
                            Log.e(TAG, line?:"")
                            bringUpMainActivity()
                        }
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun checkForegroundActivity() {
        initErrorReader()
        try {
            dataOutputStream.writeBytes("dumpsys activity activities | grep mFocusedActivity\n")
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

}