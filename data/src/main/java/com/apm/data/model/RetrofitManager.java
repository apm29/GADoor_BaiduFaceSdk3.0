package com.apm.data.model;

import com.ihsanbal.logging.Level;
import com.ihsanbal.logging.LoggingInterceptor;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.FormBody;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.internal.platform.Platform;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitManager {

    private static volatile RetrofitManager instance;

    public static RetrofitManager getInstance() {
        if (instance == null) {
            synchronized (RetrofitManager.class) {
                if (instance == null) {
                    instance = new RetrofitManager();
                }
            }
        }
        return instance;
    }

    private Retrofit retrofit;

    private RetrofitManager() {
        retrofit = new Retrofit.Builder()
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl("http://gadoor.ciih.net")
                .client(
                        new OkHttpClient.Builder()
                                .connectTimeout(25, TimeUnit.SECONDS)
                                .readTimeout(25, TimeUnit.SECONDS)
                                .callTimeout(35,TimeUnit.SECONDS)
                                .addInterceptor(
                                        new LoggingInterceptor.Builder()
                                                .setLevel(Level.BASIC)
                                                .loggable(true)
                                                .log(Platform.WARN)
                                                .request("REQUEST:")
                                                .response("RESPONSE:")
                                                .build()
                                )
                                .addInterceptor(new Interceptor() {
                                    @Override
                                    public Response intercept(Chain chain) throws IOException {
                                        Request oldReq = chain.request();
                                        if ("POST".equalsIgnoreCase(oldReq.method())){
                                            RequestBody body = oldReq.body();
                                            if (body instanceof FormBody){
                                                FormBody formBody = (FormBody) body;
                                                for (int i = 0; i < formBody.size(); i++) {
                                                    System.out.println(formBody.name(i)+":"+formBody.value(i));
                                                }
                                            }
                                        }
                                        return chain.proceed(oldReq);
                                    }
                                })
                                .build()
                )
                .build();
    }

    public Retrofit getRetrofit() {
        return retrofit;
    }
}
