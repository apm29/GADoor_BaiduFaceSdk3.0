package com.apm.anxinju.main.service;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.apm.anxinju.main.activity.FaceAuthActivity;
import com.apm.anxinju.main.activity.PreviewActivity;
import com.apm.anxinju.main.listener.SdkInitListener;
import com.apm.anxinju.main.manager.FaceSDKManager;
import com.apm.anxinju.main.utils.FileUtils;
import com.apm.anxinju_baidufacesdk30.R;
import com.apm.data.api.Api;
import com.apm.data.model.BaseResponse;
import com.apm.data.model.FaceModel;
import com.apm.data.model.RetrofitManager;
import com.baidu.idl.main.facesdk.FaceAuth;

import org.reactivestreams.Subscription;

import java.io.File;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import io.reactivex.BackpressureStrategy;
import io.reactivex.FlowableSubscriber;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlinx.coroutines.Job;


public class FaceDataSyncService extends Service {

    private static final String TAG = "FaceDataSyncService";
    public static final String LAST_SYNC_TIME = "last_sync_time";
    public static final String SP_KEY = "sync_data";
    public static final String CHANNEL_ID = "FACE_DATA_SYNC-1";
    private Subscription intervalDisposable;
    private Disposable fixDisposable;
    private Handler handler = new Handler();
    private String deviceId;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "service created");
        initLicense();
        new KeepInAppHandler(this).sendEmptyMessage(0);
        deviceId = new FaceAuth().getDeviceId(this);
        //deviceId = "19BC95DC053A2A4D130FC17C9B4E6EED43";
        startSyncLoop();
    }


    private void initLicense() {
        if (FaceSDKManager.initStatus != FaceSDKManager.SDK_MODEL_LOAD_SUCCESS) {
            FaceSDKManager.getInstance().init(this, new SdkInitListener() {
                @Override
                public void initStart() {

                }

                public void initLicenseSuccess() {
                }

                public void initLicenseFail(int errorCode, String msg) {
                    // 如果授权失败，跳转授权页面
                    startActivity(new Intent(FaceDataSyncService.this, FaceAuthActivity.class));
                }

                public void initModelSuccess() {
                }

                public void initModelFail(int errorCode, String msg) {

                }
            });
        }
    }


    final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
    final HashSet<Integer> failedIds = new HashSet<>();

    Job job;
    /**
     * 30s间隔,更新数据
     */
    @SuppressLint("CheckResult")
    private void startSyncLoop() {
        if(job==null||job.isCompleted()){
            job = SyncHelper.INSTANCE.startSync(this, deviceId, new Function0<Unit>() {
                @Override
                public Unit invoke() {
                    stopForeground(true);
                    return null;
                }
            });
        }else {
            Log.e(TAG,"同步进行中，无需更新JOB");
        }
        //loop();
    }

    private void loop() {
        if (intervalDisposable != null) {
            intervalDisposable.cancel();
        }
        //每30s同步一次face model
        Observable.interval(15, 30, TimeUnit.SECONDS)
                .flatMap(new Function<Long, Observable<BaseResponse<List<FaceModel>>>>() {
                    @Override
                    public Observable<BaseResponse<List<FaceModel>>> apply(Long aLong) throws Exception {

                        long lastSyncTimeMills = getSharedPreferences().getLong(LAST_SYNC_TIME, 0);

                        boolean jobDone = failedIds.isEmpty();

                        Log.d(TAG, "failedIds:");
                        StringBuilder stringBuilder = new StringBuilder();
                        for (Integer integerBooleanPair : failedIds) {
                            stringBuilder.append("[");
                            stringBuilder.append(integerBooleanPair);
                            stringBuilder.append("],");
                        }
                        Log.d(TAG, stringBuilder.toString());
                        System.out.println("jobDone = " + jobDone);
                        if (!jobDone) {
                            Log.e(TAG, "有注册任务未完成 ==---> SKIP TO NEXT SYNC");
                            BaseResponse<List<FaceModel>> item = BaseResponse.emptyList();
                            if (!failedIds.isEmpty()) {
                                reportFailedFace();
                            }
                            return Observable.just(item);
                        } else {
                            System.out.println(" lastSyncTimeMills = " + lastSyncTimeMills);
                            getSharedPreferences().edit()
                                    .putLong(LAST_SYNC_TIME, System.currentTimeMillis())
                                    .apply();
                        }

                        Date now = new Date();
                        now.setTime(now.getTime() - 60 * 1000 * 5);//调后五分钟
                        return RetrofitManager.getInstance().getRetrofit()
                                .create(Api.class)
                                .getNotSync(
                                        simpleDateFormat.format(new Date(lastSyncTimeMills)),
                                        simpleDateFormat.format(now),
                                        deviceId
                                )
                                .subscribeOn(Schedulers.io());

                    }
                })
                .flatMap(new Function<BaseResponse<List<FaceModel>>, ObservableSource<FaceModel>>() {
                    @Override
                    public ObservableSource<FaceModel> apply(BaseResponse<List<FaceModel>> listBaseResponse) throws Exception {
                        if (listBaseResponse.success()) {
                            Log.d(TAG, "同步数据获取:" + listBaseResponse.data.size());
                            return Observable.fromIterable(listBaseResponse.data);
                        } else {
                            Log.d(TAG, "同步数据获取:" + listBaseResponse.text);
                            return Observable.fromIterable(new ArrayList<FaceModel>());
                        }
                    }
                })
                .subscribeOn(Schedulers.io())
                .onErrorReturn(new Function<Throwable, FaceModel>() {
                    @Override
                    public FaceModel apply(Throwable throwable) throws Exception {
                        Log.e(TAG, "同步数据出错");
                        throwable.printStackTrace();

                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                startSyncLoop();
                            }
                        }, 30_000);
                        return new FaceModel();
                    }
                })
                .toFlowable(BackpressureStrategy.BUFFER)
                .subscribe(new FlowableSubscriber<FaceModel>() {

                    @Override
                    public void onSubscribe(Subscription d) {
                        intervalDisposable = d;
                        d.request(Long.MAX_VALUE);
                    }

                    @Override
                    public void onNext(FaceModel model) {
                        if (model.id == 0) {
                            return;
                        }
                        //记录同步时间
                        Log.e(TAG, "注册:" + model.toString());
                        Pair<Integer, Boolean> result
                                = FaceRegisterUtils.getInstance().registerFaceSync(model);

                        sendMessage(model, result.second);

                        if (!result.second) {
                            failedIds.add(result.first);
                        }

                        if (!failedIds.isEmpty()) {
                            reportFailedFace();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, "同步数据出错");
                        e.printStackTrace();
                    }

                    @Override
                    public void onComplete() {
                        Log.e(TAG, "同步数据完成");
                    }
                });
    }


    public static String ACTION_NOTIFY_REGISTER = "ACTION_NOTIFY_REGISTER";
    public static String KEY_NOTIFY_REGISTER_MODEL = "KEY_NOTIFY_REGISTER_MODEL";
    public static String KEY_NOTIFY_REGISTER_SUCCESS = "KEY_NOTIFY_REGISTER_SUCCESS";

    public void sendMessage(FaceModel faceModel, boolean success) {
        Intent intent = new Intent(ACTION_NOTIFY_REGISTER);
        intent.putExtra(KEY_NOTIFY_REGISTER_MODEL, faceModel);
        intent.putExtra(KEY_NOTIFY_REGISTER_SUCCESS, success);
        sendBroadcast(intent);
    }

    private void reportFailedFace() {
        //上报未完成任务
        Log.d(TAG, "上报未完成任务:" + failedIds.size() + "[" + TextUtils.join(".", failedIds) + "]");
        HashSet<Integer> copy = new HashSet<>(failedIds);
        try {
            FaceRegisterUtils.getInstance().addFailedIdSync(copy, deviceId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        failedIds.removeAll(copy);
    }

    private SharedPreferences getSharedPreferences() {
        return getSharedPreferences(SP_KEY, Context.MODE_PRIVATE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "service onStartCommand");
        getFailedIds();
        startSyncLoop();
        // 在API11之后构建Notification的方式
        Notification.Builder builder = new Notification.Builder(this.getApplicationContext()); //获取一个Notification构造器
        Intent nfIntent = new Intent(this, PreviewActivity.class);

        builder.setContentIntent(PendingIntent.getActivity(this, 0, nfIntent, 0))
                .setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.mipmap.ic_image_attrs)) // 设置下拉列表中的图标(大图标)
                .setContentTitle("数据同步服务") // 设置下拉列表里的标题
                .setSmallIcon(R.mipmap.ic_check) // 设置状态栏内的小图标
                .setContentText("人脸库数据同步服务") // 设置上下文内容
                .setWhen(System.currentTimeMillis()); // 设置该通知发生的时间

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, "人脸库数据同步服务", NotificationManager.IMPORTANCE_HIGH);
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(notificationChannel);
            NotificationCompat.Builder builderCompat
                    = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.mipmap.ic_image_attrs)) // 设置下拉列表中的图标(大图标)
                    .setContentTitle("数据同步服务") // 设置下拉列表里的标题
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setSmallIcon(R.mipmap.ic_check) // 设置状态栏内的小图标
                    .setContentText("人脸库数据同步服务") // 设置上下文内容
                    .setWhen(System.currentTimeMillis()); // 设置该通知发生的时间
            Notification notification = builderCompat.build(); // 获取构建好的Notification
            notification.defaults = Notification.DEFAULT_SOUND; //设置为默认的声音
            startForeground(110, notification);
            return super.onStartCommand(intent, flags, startId);
        }

        Notification notification = builder.build(); // 获取构建好的Notification
        notification.defaults = Notification.DEFAULT_SOUND; //设置为默认的声音
        startForeground(110, notification);
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("on bind not implemented");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "service onDestroy");
        if (fixDisposable != null && !fixDisposable.isDisposed()) {
            fixDisposable.dispose();
        }
        if (intervalDisposable != null) {
            intervalDisposable.cancel();
        }
        saveFailedIds();
        FaceRegisterUtils.getInstance().release();
        SyncHelper.INSTANCE.stopSync();
        stopForeground(true);// 停止前台服务--参数：表示是否移除之前的通知
    }

    private void saveFailedIds() {
        String ids = TextUtils.join(",", failedIds);
        Log.d(TAG, "saveFailedIds: " + ids);
        File dir = FileUtils.getBatchImportDirectory();
        FileUtils.writeTxtFile(ids, dir.getAbsolutePath() + File.pathSeparator + "failedId.txt");
    }

    private void getFailedIds() {
        File dir = FileUtils.getBatchImportDirectory();
        File textFile = new File(dir.getAbsolutePath(), "failedId.txt");
        if (textFile.exists()) {
            String ids = FileUtils.txt2String(textFile.getPath());
            Log.d(TAG, "getFailedIds: " + ids);
            if (ids == null || ids.isEmpty()) {
                return;
            }
            String[] split = ids.split(",");
            for (String id : split) {
                try {
                    failedIds.add(Integer.valueOf(id));
                    Log.d(TAG, "add failedId: " + id);
                } catch (NumberFormatException e) {
                    Log.d(TAG, "add failedId: " + id + "failed");
                    e.printStackTrace();
                }
            }
        }
    }


    private static class KeepInAppHandler extends Handler {
        private final WeakReference<FaceDataSyncService> mService;
        private Context context;
        private boolean sendHandler = false;

        public KeepInAppHandler(FaceDataSyncService mService) {
            this.mService = new WeakReference<FaceDataSyncService>(mService);
            this.context = mService;
            this.sendHandler = true;

        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            ActivityManager activityManager = (ActivityManager) context.getSystemService(Service.ACTIVITY_SERVICE);

            if (activityManager.getRecentTasks(2, 0).size() > 0) {
                if (activityManager.getRecentTasks(2, 0).get(0).id != GlobalVars.taskID) {
                    activityManager.moveTaskToFront(GlobalVars.taskID, 0);
                }

                if (sendHandler) {
                    sendEmptyMessageDelayed(0, 1000);
                }
            }


        }

    }

    public static class GlobalVars {
        public static int taskID;
        public static Intent keepInApp;
    }

}
