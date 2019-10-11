package com.apm.rs485reader.service

import android.annotation.SuppressLint
import android.app.*
import android.content.*
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PixelFormat
import android.hardware.Camera
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.apm.data.api.Api
import com.apm.data.api.ApiKt
import com.apm.data.model.BaseResponse
import com.apm.data.model.ImageDetail
import com.apm.data.model.RetrofitManager
import com.apm.rs485reader.R
import com.apm.rs485reader.db.GateDataBase
import com.apm.rs485reader.db.entity.User
import com.common.pos.api.util.PosUtil
import com.spark.zj.comcom.serial.*
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.*
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class DataSyncService : Service() {

    companion object {
        const val NOTIFICATION_ID = 1219
        const val REQUEST_CODE = 1231
        const val DEVICE_ID = "deviceId"
        const val TTY_NAME = "ttyName"
        const val LAST_SYNC_TIME = "lastSyncTime"
        private val CHANNEL_ID = "RFID_DATA_SYNC-1"
    }


    private val dataBase: GateDataBase by lazy {
        GateDataBase.getDB(this)
    }

    private val api: Api by lazy {
        RetrofitManager.getInstance().retrofit.create(Api::class.java)
    }

    private val apiKt: ApiKt by lazy {
        RetrofitManager.getInstance().retrofit.create(ApiKt::class.java)
    }

    private lateinit var deviceId: String
    private lateinit var ttyName: String
    private var pictureFrame: Bitmap? = null
    private var mBinder: IPictureCaptureInterface.Stub = object : IPictureCaptureInterface.Stub() {
        override fun setPicture(data: Bitmap?) {
            pictureFrame = data
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return mBinder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        return super.onUnbind(intent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val int = intent?.extras?.getInt("show")
        Log.d(TAG, "int = $int")
        if (int == 1) {
            showOverlay()
            return super.onStartCommand(intent, flags, startId)
        }

        deviceId = intent?.extras?.getString(DEVICE_ID) ?: "NO_DEVICE_ID"
        ttyName = intent?.extras?.getString(TTY_NAME) ?: "ttyS1"
        Log.d(TAG, "deviceId = $deviceId")
        startListenOnRs485()

        startSelfWithNotification()

        startSyncLoop()

        return super.onStartCommand(intent, flags, startId)
    }

    private fun showOverlay() {
        val mView = LayoutInflater.from(this).inflate(R.layout.layout_notification_dialog, null)
        val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_ERROR,
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                //WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                //| WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT)
        params.gravity = Gravity.START or Gravity.TOP
        params.title = "Load Average"
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        configViews(mView)
        wm.addView(mView, params)
    }

    private fun configViews(mView: View) {
        val editText = mView.findViewById<TextView>(R.id.editText)
        val listener = View.OnClickListener { v -> editText.append((v as Button).text) }
        mView.findViewById<Button>(R.id.button0)
                .setOnClickListener(listener)
        mView.findViewById<Button>(R.id.button1)
                .setOnClickListener(listener)
        mView.findViewById<Button>(R.id.button2)
                .setOnClickListener(listener)
        mView.findViewById<Button>(R.id.button3)
                .setOnClickListener(listener)
        mView.findViewById<Button>(R.id.button4)
                .setOnClickListener(listener)
        mView.findViewById<Button>(R.id.button5)
                .setOnClickListener(listener)
        mView.findViewById<Button>(R.id.button6)
                .setOnClickListener(listener)
        mView.findViewById<Button>(R.id.button7)
                .setOnClickListener(listener)
        mView.findViewById<Button>(R.id.button8)
                .setOnClickListener(listener)
        mView.findViewById<Button>(R.id.button9)
                .setOnClickListener(listener)
        mView.findViewById<Button>(R.id.confirm)
                .setOnClickListener {
                    if (editText.text.toString() == "121591") {
                        try {

                            startActivity(Intent().apply {
                                component = ComponentName(
                                        "com.baidu.idl.face.demo", "com.baidu.idl.sample.ui.MainActivity"
                                )
                                putExtra("finish", true)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT
                            })

                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
        mView.findViewById<Button>(R.id.clear)
                .setOnClickListener {
                    editText.text = null
                    val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
                    configViews(mView)
                    wm.removeViewImmediate(mView)
                }
        mView.findViewById<Button>(R.id.button8)
    }

    private val sharedPreferences: SharedPreferences by lazy {
        getSharedPreferences("SyncData", Context.MODE_PRIVATE)
    }
    private val TAG: String = "RFID_DATA_SYNC ------> "

    private var loopDisposable: Disposable? = null
    private val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)


    private fun startSyncLoop() {
        dispose(loopDisposable)


        loopDisposable = Observable.interval(
                30_000, 30_000, TimeUnit.MILLISECONDS, Schedulers.io()
        )
                .flatMap {
                    val lastSyncTime = simpleDateFormat.format(Date(sharedPreferences.getLong(LAST_SYNC_TIME, 0)))
                    Log.d(TAG, "last sync time: $lastSyncTime")
                    api.getNotSyncRFID(
                            lastSyncTime, simpleDateFormat.format(Date()), deviceId
                    )
                }
                .map {
                    if (it.success()) {
                        Log.d(TAG, "获取远端数据成功 size: ${it.data.size} \r\n[${it.data.joinToString()}]")
                        it.data.forEach { model ->
                            try {
                                if (!model.delete) {
                                    val user = dataBase.getGateDao().getUserByRemoteId(model.id)
                                    if (user != null) {
                                        Log.d(TAG, "user 已存在 remoteId ${model.id}")
                                    }
                                    val id = dataBase.getGateDao().addUser(
                                            User(remote_id = model.id
                                                    ?: 0, hex = model.hexString)
                                    )
                                    Log.d(TAG, "插入成功 remoteId ${model.id} - 数据库ROW ID $id")
                                } else {
                                    val deleteUser = dataBase.getGateDao().deleteUser(model.id ?: 0)
                                    Log.d(TAG, "删除记录 remoteId ${model.id} - 删除条数 $deleteUser")
                                }
                            } catch (e: Exception) {
                                //返回未完成id
                                api.addUnRegisterRFID(
                                        model.id.toString(),
                                        deviceId
                                ).subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                                        .subscribe({}, {
                                            it.printStackTrace()
                                        })

                            }
                        }
                    } else {
                        Log.e(TAG, "获取远端数据失败 $it")
                    }
                    Unit
                }
                .subscribe(
                        {
                            Log.d(TAG, "数据库条数: ${dataBase.getGateDao().getAll().size}")
                            sharedPreferences.edit()
                                    .putLong(LAST_SYNC_TIME, Date().time)
                                    .apply()
                        }
                        ,
                        {
                            startSyncLoop()
                            it.printStackTrace()
                        }
                )
    }

    private var rs485disposable: Disposable? = null
    private var serialSignalEmitter: ObservableEmitter<ByteArray>? = null
    private val handler: Handler = Handler()
    private val serialHelper by lazy {
        //打开485接收
        val status = PosUtil.setRs485Status(0)
        Log.d(TAG, "485 接收状态开启 status = $status")
        val path = SerialPortFinder().allDevicesPath.first {
            it.contains(ttyName, true)
        }
        val serialHelper = SerialHelper(57600, path, 1, 8, 0)
        serialHelper.setDataReceiver {
            val verified = ByteCRC16.verifyCRC16Data(it)
            if (verified && dataBase.getGateDao().exist(ByteCRC16.getData(it))) {
                //有效信号
                serialSignalEmitter?.onNext(ByteCRC16.getData(it))
                Log.d(TAG, "校验成功 ${it?.toHexString()}")
            } else {
                if (!verified)//crc校验失败
                    Log.e(TAG, "校验失败 ${it?.toHexString()}")
                else//库里没有
                    Log.e(TAG, "无数据 ${it?.toHexString()}")
            }
        }
        serialHelper
    }

    private fun startListenOnRs485() {
        serialHelper.open()
        dispose(rs485disposable)
        rs485disposable = Observable.create<ByteArray> {
            serialSignalEmitter = it
        }
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .doOnError {
                    it.printStackTrace()
                }
                .buffer(500, TimeUnit.MILLISECONDS)
                .subscribe {
                    if (it != null && it.size > 0)
                        letGo(it.first())
                }
    }

    private fun dispose(disposable: Disposable?) {
        if (disposable != null && !disposable.isDisposed) {
            disposable.dispose()
        }
    }

    var firstOpenSignal = true
    private var uploadJob1: Deferred<BaseResponse<ImageDetail>>? = null
    private var uploadJob2: Deferred<BaseResponse<ImageDetail>>? = null

    private fun letGo(data: ByteArray) = runBlocking {
        Log.d(TAG, "GATE OPEN")
        if (firstOpenSignal) GlobalScope.launch {
            //拍照上传
            firstOpenSignal = false
            val image1 = getCurrentFrame(1)
            Log.d(TAG, "照片1拍摄完成")
            uploadJob1 = GlobalScope.async(start = CoroutineStart.LAZY) {
                val response: BaseResponse<ImageDetail>
                try {
                    Log.d(TAG, "上传照片1开始")

                    response = apiKt.uploadImageSync(
                            MultipartBody.Builder()
                                    .addFormDataPart("pic",
                                            image1.name,
                                            RequestBody.create(MediaType.parse("multipart/form-data"),
                                                    image1)
                                    )
                                    .build()
                    )
                    Log.d(TAG, "照片1上传完成")
                    response
                } catch (e: Exception) {
                    e.printStackTrace()
                    BaseResponse.error<ImageDetail>()
                }

            }
            uploadJob1?.start()
            delay(1000)
            val image2 = getCurrentFrame(2)
            Log.d(TAG, "照片2拍摄完成")


            uploadJob2 = GlobalScope.async(start = CoroutineStart.LAZY) {
                val response: BaseResponse<ImageDetail>
                try {
                    Log.d(TAG, "上传照片2开始")

                    response = apiKt.uploadImageSync(
                            MultipartBody.Builder()
                                    .addFormDataPart("pic",
                                            image2.name,
                                            RequestBody.create(MediaType.parse("multipart/form-data"),
                                                    image2)
                                    )
                                    .build()
                    )
                    Log.d(TAG, "照片2上传完成")
                    response
                } catch (e: Exception) {
                    e.printStackTrace()
                    BaseResponse.error<ImageDetail>()
                }

            }
            uploadJob2?.start()

        }
        if (!firstOpenSignal)
            handler.removeCallbacksAndMessages(null)
        val relayPower = PosUtil.setRelayPower(1)
        if (relayPower == 0) {
            handler.postDelayed({
                PosUtil.setRelayPower(0)
                Log.d(TAG, "GATE CLOSE")
                //添加通行记录
                sendEntryLog(data)
            }, 600)
        }
    }

    val camera: Camera by lazy {
        Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT).apply {
            startPreview()
        }
    }


    private suspend fun getCurrentFrame(code: Int) = suspendCoroutine<File> { con ->
        try {
            val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val image = File(directory, "img_captured_bike_$code.jpg")
            val fileOutputStream = FileOutputStream(image)
            pictureFrame?.compress(
                    Bitmap.CompressFormat.JPEG,
                    60,
                    fileOutputStream
            )
            fileOutputStream.flush()
            con.resume(image)
        } catch (e: Exception) {
            con.resumeWithException(e)
        }
    }

    private suspend fun takePicture(code: Int) = suspendCoroutine<File> { con ->
        try {

            camera.takePicture(
                    {}, { _, _ -> },
                    { data, _ ->
                        camera.startPreview()
                        val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                        var fileOutputStream: FileOutputStream? = null
                        val image = File(directory, "img_captured_bike_$code.jpg")
                        try {

                            fileOutputStream = FileOutputStream(image)
                            fileOutputStream.write(data)
                            fileOutputStream.flush()
                        } catch (e: Exception) {
                            e.printStackTrace()
                            con.resumeWithException(e)
                        } finally {
                            if (fileOutputStream != null) {
                                try {
                                    fileOutputStream.close()
                                } catch (e: IOException) {
                                    e.printStackTrace()
                                }

                            }
                        }
                        con.resume(image)
                    }
            )
        } catch (e: Exception) {
            e.printStackTrace()
            con.resumeWithException(e)
        } finally {

        }
    }

    @SuppressLint("CheckResult")
    private fun sendEntryLog(data: ByteArray) {
        GlobalScope.launch {
            val pair = try {
                uploadJob1?.await() to uploadJob2?.await()

            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
            val response1 = pair?.first
            val response2 = pair?.second
            val successful1 = response1?.success()
            val successful2 = response2?.success()
            if (successful1 == true) {
                try {
                    apiKt.addEBikePassLog(
                            dataBase.getGateDao().getUserByHex(data.toHexString().lastSixHex())?.remote_id.toString(),
                            deviceId,
                            response1.data?.orginPicPath
                    )
                    Log.d(TAG, "日志1上传成功")
                } catch (e: Exception) {
                    Log.d(TAG, "日志1上传失败")
                    e.printStackTrace()
                }
            }
            if (successful2 == true) {
                try {
                    apiKt.addEBikePassLog(
                            dataBase.getGateDao().getUserByHex(data.toHexString().lastSixHex())?.remote_id.toString(),
                            deviceId,
                            response2.data?.orginPicPath
                    )
                    Log.d(TAG, "日志2上传成功")
                } catch (e: Exception) {
                    Log.d(TAG, "日志2上传失败")
                    e.printStackTrace()
                }
            }
            permitLogAgain()
        }
    }

    private fun permitLogAgain() = GlobalScope.launch {
        delay(1000)
        if (!firstOpenSignal) {
            Log.d(TAG, "允许再次上传日志")
        }
        uploadJob1 = null
        uploadJob2 = null
        firstOpenSignal = true
    }



    private fun startSelfWithNotification() {
        val intent = Intent(this, DataSyncService::class.java)
        intent.putExtra("show", 2)
        val builder = Notification.Builder(this)
                .setContentIntent(PendingIntent.getService(this, REQUEST_CODE, intent, 0))
                .setLargeIcon(BitmapFactory.decodeResource(this.resources, R.drawable.track_image_angle)) // 设置下拉列表中的图标(大图标)
                .setContentTitle("RFID数据同步服务") // 设置下拉列表里的标题
                .setSmallIcon(R.drawable.track_image_angle) // 设置状态栏内的小图标
                .setContentText("数据同步中") // 设置上下文内容
                .setWhen(System.currentTimeMillis()) // 设置该通知发生的时间

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                CHANNEL_ID,
                "RFID数据同步服务",
                NotificationManager.IMPORTANCE_HIGH
            )
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(notificationChannel)
            val builderCompat = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("RFID数据同步服务") // 设置下拉列表里的标题
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setSmallIcon(R.drawable.track_image_angle) // 设置状态栏内的小图标
                .setContentText("数据同步中") // 设置上下文内容
                .setWhen(System.currentTimeMillis()) // 设置该通知发生的时间
            val notification = builderCompat.build() // 获取构建好的Notification
            notification.defaults = Notification.DEFAULT_SOUND //设置为默认的声音
            startForeground(110, notification)
            return
        }

        val notification = builder.build()

        notification.defaults = Notification.DEFAULT_SOUND

        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        dataBase.close()
        dispose(loopDisposable)
        dispose(rs485disposable)
        stopForeground(true)
    }
}