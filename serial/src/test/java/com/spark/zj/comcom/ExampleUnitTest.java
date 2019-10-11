package com.spark.zj.comcom;

import org.junit.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Predicate;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    long last = 0;
    @Test
    public void addition_isCorrect() {

        Observable.fromArray(200,222,2222,222,11,11,200,200,33,33,33,2000,222,222,222,222,222,222,222)

//        Observable.interval(100,TimeUnit.MILLISECONDS)
//                .map(new Function<Long, Integer>() {
//                    @Override
//                    public Integer apply(Long aLong) throws Exception {
//                        return 200;
//                    }
//                })
                .delay(200,TimeUnit.MILLISECONDS)
                .filter(new Predicate<Integer>() {
                    @Override
                    public boolean test(Integer integer) throws Exception {
                        boolean b = integer > 0 && integer < 100;
                        System.out.println("integer = [" + integer + "] passed: " + b);
                        return b;
                    }
                })
//                .buffer(3,TimeUnit.SECONDS)
                .buffer(450, TimeUnit.MILLISECONDS)
                .subscribe(new Consumer<List<Integer>>() {
                    @Override
                    public void accept(List<Integer> integers) throws Exception {
                        long interval = System.currentTimeMillis() - last;
                        if ( integers.size() >= 3 && interval > 3000) {
                            System.out.println("in range");
                            last = System.currentTimeMillis();
                        }
                    }
                });
        try {
            Thread.sleep(100000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}