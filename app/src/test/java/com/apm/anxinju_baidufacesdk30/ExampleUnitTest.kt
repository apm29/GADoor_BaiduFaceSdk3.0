package com.apm.anxinju_baidufacesdk30

import android.util.Log
import com.apm.data.api.ApiKt
import com.apm.data.model.FaceModel
import com.apm.data.model.RetrofitManager
import com.apm.data.persistence.PropertiesUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody
import org.junit.Test
import java.io.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {

    companion object{
        const val TAG = "ExampleUnitTest"
    }

    @Test
    fun addition_isCorrect() {
        val retrofitManager = RetrofitManager.getInstance()
        val apiKt = retrofitManager.retrofit.create(ApiKt::class.java)
        runBlocking {
            while (true){
                //三十秒等待
                delay(30_000)
                val lastSyncTime = 0L
                val baseFileUrl = "http://gadoor.ciih.net/"
                val notSync = apiKt.getNotSync(
                    lastSyncTime.toString(),
                    System.currentTimeMillis().toString(),
                    "1538E38BB2ADAE6E2426032E79252FED43"
                )
                if(notSync.success()){
                    notSync.data.forEach {
                        faceModel:FaceModel->
                        if(faceModel.delete()){
                            Log.d(TAG,"删除：${faceModel.id}")
                        }else{
                            Log.d(TAG,"注册：${faceModel.id}")
                            val picUrl = faceModel.absolutePicUrl(baseFileUrl)
                            val responseBody = apiKt.downloadFile(picUrl)
                        }
                    }
                }else{
                    Log.e(TAG,notSync.text)
                }
            }
        }
    }

    private fun writeResponseBodyToDisk(body: ResponseBody, path: File) {
        try {
            var inputStream: InputStream? = null
            var outputStream: OutputStream? = null

            try {
                val fileReader = ByteArray(4096)


                inputStream = body.byteStream()
                outputStream = FileOutputStream(path)

                while (true) {
                    val read = inputStream!!.read(fileReader)

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
        }

    }

}
