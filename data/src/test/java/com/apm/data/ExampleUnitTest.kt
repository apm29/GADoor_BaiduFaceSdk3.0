package com.apm.data

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect()  = runBlocking<Unit>{
        GlobalScope.launch {
            GlobalScope.launch {
                delay(2000)
                println("world")
            }
            println("hello ")
            delay(3000)
        }
    }

}