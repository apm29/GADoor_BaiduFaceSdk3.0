package com.apm.rs485reader

import com.spark.zj.comcom.serial.lastSixHex
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.lang.RuntimeException
import java.util.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
class ExampleUnitTest {
    private lateinit var loopEmitter: ObservableEmitter<Unit>

    @Test
    fun addition_isCorrect() = runBlocking{

        //loop()

        //val file = File("1.jpg")
        //if (!file.exists()){
        //    file.createNewFile()
        //}
        //val uploadImageSync = RetrofitManager.getInstance().retrofit
        //        .create(ApiKt::class.java)
        //        .uploadImageSync(
        //                MultipartBody.Builder()
        //                        .addFormDataPart("pic", file.name, RequestBody.create(
        //                                MediaType.parse("multipart/form-data"), file
        //                        ))
        //                        .build()
        //        )
        println("000000001012".lastSixHex())
        //Thread.sleep(30000)
    }

    private fun loop() {
        Observable.create<Unit> {
            loopEmitter = it
            it.onNext(Unit)
        }.subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .flatMap {

                    Observable.just(arrayListOf(1))
                }
                .map {
                    if (it is List<*>){
                        throw RuntimeException()
                    }
                    it
                }
                .subscribe(
                        {
                            it.forEach {
                                model->
                                //insert
                                println("model = ${model}")
                            }
                            Timer().schedule(
                                    object :TimerTask(){
                                        override fun run() {
                                            loopEmitter.onNext(Unit)
                                        }

                                    },3000
                            )
                        },
                        {
                            println("error() = error")
                            Thread.sleep(3000)
                            loopEmitter.onNext(Unit)
                            it.printStackTrace()
                        }
                )
    }
}