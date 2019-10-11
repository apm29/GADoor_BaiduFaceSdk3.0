package com.apm.anxinju.main.service

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.apm.anxinju.main.api.FaceApi
import com.apm.anxinju.main.service.FaceDataSyncService.*
import com.apm.anxinju.main.utils.FileUtils
import com.apm.data.api.ApiKt
import com.apm.data.model.FaceModel
import com.apm.data.model.RetrofitManager
import com.apm.data.persistence.PropertiesUtils
import com.baidu.idl.main.facesdk.model.BDFaceSDKCommon
import kotlinx.coroutines.*
import okhttp3.ResponseBody
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.Future
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.coroutines.CoroutineContext

/**
 *  author : ciih
 *  date : 2019-10-09 10:22
 *  description :
 */
object SyncHelper : CoroutineScope {


    private val coroutineDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
    private var loopAsSync = true
    @Volatile
    private var inLoop = false
    private const val SYNC_TIME_KEY = "lastSyncTime"
    private const val TAG = "SyncHelper"
    private const val GROUP_NAME = "0"
    private val failedIds = HashSet<Int>()
    private val failedIdWithCount = HashMap<Int, Int>()

    override val coroutineContext: CoroutineContext
        get() = coroutineDispatcher

    fun startSync(context: Context, deviceId: String, onFinished: () -> Unit) =
        launch {
            loopAsSync = true
            Log.d(TAG, "开始同步")
            val retrofitManager = RetrofitManager.getInstance()
            val apiKt = retrofitManager.retrofit.create(ApiKt::class.java)
            val prop = PropertiesUtils.getInstance()
            val sharedPreferences =
                context.getSharedPreferences(SP_KEY, Context.MODE_PRIVATE)
            if (inLoop) {
                Log.e(TAG, "已在同步")
                return@launch
            }
            while (loopAsSync) {
                Log.d(TAG, "等待：${simpleDateFormat.format(Date())}")
                Log.d(TAG, "获取人脸库大小。。。")
                val userList = FaceApi.getInstance().getUserList(GROUP_NAME)
                Log.d(TAG, "人脸库大小：${userList?.size}")
                try {
                    inLoop = true
                    delay(30000)
                    prop.init()
                    prop.open()
                    val lastSyncTime = sharedPreferences.getLong(SYNC_TIME_KEY, 0L)
                    val doorCurrentTime = System.currentTimeMillis()
                    Log.d(
                        TAG,
                        "同步开始：${simpleDateFormat.format(Date())}  last sync time:${simpleDateFormat.format(
                            Date(lastSyncTime)
                        )}"
                    )
                    val syncResp =
                        apiKt.getNotSync(
                            lastSyncTime = simpleDateFormat.format(Date(lastSyncTime)),
                            doorCurrentTime = simpleDateFormat.format(Date(doorCurrentTime)),
                            deviceId = deviceId
                        )

                    if (syncResp.success()) {
                        Log.d(TAG, "数据获取：${syncResp.data.size}")
                        syncResp.data.forEach { faceModel: FaceModel ->
                            registerFace(faceModel, prop, apiKt, context)
                        }
                        sharedPreferences.edit()
                            .putLong(SYNC_TIME_KEY, System.currentTimeMillis() - 30_000)
                            .apply()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "同步异常")
                    e.printStackTrace()
                } finally {
                    try {
                        if (failedIds.isNotEmpty()) {
                            val copy = HashSet(failedIds)
                            val unregisterIds = copy.joinToString(",")
                            apiKt.addUnRegisterIds(
                                unregisterIds,
                                deviceId
                            )
                            Log.d(TAG, "上报未完成id：$unregisterIds")
                        }
                    } finally {
                        failedIds.clear()
                    }
                }
            }
            inLoop = false
            Log.d(TAG, "同步循环退出")
            onFinished()
        }

    suspend fun registerFace(
        faceModel: FaceModel,
        prop: PropertiesUtils,
        apiKt: ApiKt,
        context: Context
    ) {
        var success = false
        try {
            Log.d(TAG, "人脸信息:$faceModel")
            if (faceModel.delete()) {
                //删除用户
                val user2Delete = FaceApi.getInstance().getUserListByUserId(
                    faceModel.id.toString() + ""
                )
                if (user2Delete != null) {
                    FaceApi.getInstance()
                        .userDelete(user2Delete.userId, GROUP_NAME)
                    Log.d(TAG, "删除user:${user2Delete.userId}")
                    success = true
                } else {
                    Log.d(TAG, "无需删除user:${faceModel.id}")
                    success = true
                }
            } else {
                Log.d(TAG, "新增用户:${faceModel.id} ${faceModel.personPic}")
                //新增用户
                val picUrl = faceModel.absolutePicUrl(
                    prop.readString(
                        "fileBaseUrl",
                        "http://gadoor.ciih.net/"
                    )
                )
                val batchImageDir = FileUtils.getBatchImportDirectory()
                val imageName = "origin_${faceModel.id}_id.jpg"
                val imageFile = File(batchImageDir, imageName)
                val responseBody = withContext(coroutineDispatcher) {
                    apiKt.downloadFile(picUrl)
                }
                writeResponseBodyToDisk(responseBody, imageFile)

                val faceBitmap = BitmapFactory.decodeFile(imageFile.path)
                if (faceBitmap != null) {
                    val bytes = ByteArray(512)
                    // 走人脸SDK接口，通过人脸检测、特征提取拿到人脸特征值
                    val ret = FaceApi.getInstance().getFeature(
                        faceBitmap, bytes,
                        BDFaceSDKCommon.FeatureType.BDFACE_FEATURE_TYPE_LIVE_PHOTO
                    )
                    if (ret == -1.0F) {
                        Log.e(
                            TAG,
                            faceModel.id.toString() + "未检测到人脸，可能原因：人脸太小或角度不正确"
                        )
                    } else if (ret == 128F) {
                        val user = FaceApi.getInstance()
                            .getUserListByUserId(faceModel.id.toString())
                        val importDBSuccess: Boolean
                        if (user != null) {
                            importDBSuccess =
                                FaceApi.getInstance().userUpdateWithUserId(
                                    GROUP_NAME,
                                    faceModel.id.toString(),
                                    faceModel.personPic,
                                    bytes
                                )
                        } else {
                            // 将用户信息和用户组信息保存到数据库
                            importDBSuccess =
                                FaceApi.getInstance()
                                    .registerUserIntoDBmanager(
                                        GROUP_NAME,
                                        faceModel.id.toString(),
                                        faceModel.personPic,
                                        null,
                                        bytes
                                    )
                        }

                        // 保存数据库成功
                        if (importDBSuccess) {
                            success = true
                            // 保存图片到新目录中
                            val facePicDir =
                                FileUtils.getBatchImportSuccessDirectory()
                            if (facePicDir != null) {
                                val savePicPath =
                                    File(facePicDir, imageName)
                                if (FileUtils.saveBitmap(
                                        savePicPath,
                                        faceBitmap
                                    )
                                ) {
                                    Log.i(TAG, "图片保存成功")
                                } else {
                                    Log.i(TAG, "图片保存失败")
                                }
                            }

                        } else {
                            Log.e(TAG, "$imageName：保存到数据库失败")
                        }
                    }

                } else {
                    Log.e(
                        TAG,
                        "解析图片为空：${faceModel.id}  ${faceModel.personPic}"
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "注册异常")
            e.printStackTrace()
        } finally {
            if (!success) {
                failedIdWithCount[faceModel.id] =
                    (failedIdWithCount.getOrElse(faceModel.id) { 0 } + 1)
                if (failedIdWithCount[faceModel.id] ?: 0 <= 3) {
                    failedIds.add(faceModel.id)
                }
            }
            sendRegisterSuccess(context, faceModel, success)
        }
    }

    private fun sendRegisterSuccess(context: Context, faceModel: FaceModel, success: Boolean) {
        val intent = Intent(ACTION_NOTIFY_REGISTER)
        intent.putExtra(KEY_NOTIFY_REGISTER_MODEL, faceModel)
        intent.putExtra(KEY_NOTIFY_REGISTER_SUCCESS, success)
        //context.sendBroadcast(intent)
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

    private fun writeResponseBodyToDisk(body: ResponseBody, file: File) {
        try {
            var inputStream: InputStream? = null
            var outputStream: OutputStream? = null
            try {
                val fileReader = ByteArray(4096)
                inputStream = body.byteStream()
                outputStream = FileOutputStream(file)
                while (true) {
                    val read = inputStream.read(fileReader)
                    if (read == -1) {
                        break
                    }
                    outputStream.write(fileReader, 0, read)
                }
                outputStream.flush()
            } catch (e: IOException) {
            } finally {
                inputStream?.close()
                outputStream?.close()
            }
        } catch (e: IOException) {
            Log.e(TAG, "response 写入磁盘失败")
            e.printStackTrace()
        }

    }

    fun stopSync() {
        loopAsSync = false
    }
}