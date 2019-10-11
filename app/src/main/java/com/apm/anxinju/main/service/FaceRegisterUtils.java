package com.apm.anxinju.main.service;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.util.SparseIntArray;

import com.apm.anxinju.main.api.FaceApi;
import com.apm.anxinju.main.model.User;
import com.apm.anxinju.main.utils.FileUtils;
import com.apm.data.api.Api;
import com.apm.data.model.BaseResponse;
import com.apm.data.model.FaceModel;
import com.apm.data.model.RetrofitManager;
import com.apm.data.persistence.PropertiesUtils;
import com.baidu.idl.main.facesdk.model.BDFaceSDKCommon;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;

/**
 * author : ciih
 * date : 2019-09-24 18:17
 * description :
 */
class FaceRegisterUtils {

    private static final String TAG = "FaceRegisterUtils";
    public static final String GROUP_NAME = "0";
    private static volatile FaceRegisterUtils instance;
    private FileWriter fileWriter;

    public static FaceRegisterUtils getInstance() {
        if (instance == null) {
            synchronized (FaceRegisterUtils.class) {
                if (instance == null) {
                    instance = new FaceRegisterUtils();
                }
            }
        }
        return instance;
    }

    private FaceRegisterUtils() {
        int processors = Runtime.getRuntime().availableProcessors();
        Log.d(TAG, "可用核心: " + processors);
        this.executorService = Executors.newSingleThreadScheduledExecutor();
    }


    private ScheduledExecutorService executorService;


    /**
     * 未注册id上传
     *
     * @param results  当前sync所有结果
     * @param deviceId
     */
    @SuppressLint("CheckResult")
    void addUnregisterId(final HashSet<Integer> results, String deviceId, final Function<HashSet<Integer>, Boolean> onResult) {
        RetrofitManager.getInstance().getRetrofit().create(Api.class)
                .addUnRegisterIds(TextUtils.join(",", results), deviceId)
                .delay(1, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .onErrorReturn(new Function<Throwable, BaseResponse>() {
                    @Override
                    public BaseResponse apply(Throwable throwable) throws Exception {
                        return new BaseResponse<Object>(
                                "0", "", "", null
                        );
                    }
                })
                .subscribe(new Consumer<BaseResponse>() {
                    @Override
                    public void accept(BaseResponse baseResponse) throws Exception {
                        if (baseResponse.success()) {
                            onResult.apply(results);
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Log.e(TAG, "accept: " + throwable.getLocalizedMessage());
                        throwable.printStackTrace();
                    }
                });
    }

    protected HashMap<Integer,Integer> failedCountTrackMap = new HashMap<Integer,Integer>();

    BaseResponse addFailedIdSync(HashSet<Integer> failedIds, String deviceId) throws Exception {
        HashSet<Integer> copy = new HashSet<>(failedIds);
        for (Integer failedId : copy) {
            Integer integer = failedCountTrackMap.get(failedId);
            if(integer==null){
                integer = 0;
            }
            int value = integer + 1;
            failedCountTrackMap.put(failedId, value);
            if (value > 3) {
                copy.remove(failedId);
            }
        }
        return RetrofitManager.getInstance().getRetrofit().create(Api.class)
                .addUnRegisterIds(TextUtils.join(",", copy), deviceId)
                .subscribeOn(Schedulers.io())
                .blockingGet(BaseResponse.emptyList());
    }

    public void release() {
        if (fileWriter != null) {
            try {
                fileWriter.flush();
                fileWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    Pair<Integer, Boolean> registerFaceSync(FaceModel faceModel) {

        //删除逻辑
        if (faceModel.delFlag != null && faceModel.delFlag.equals("1")) {
            User user2Delete = FaceApi.getInstance().getUserListByUserId(faceModel.id + "");
            boolean delete = false;
            if (user2Delete != null) {
                delete = FaceApi.getInstance().userDelete(user2Delete.getUserId(), GROUP_NAME);
                Log.e(TAG, "删除:" + (delete ? "成功" : "失败"));
            }
            if (user2Delete == null) {
                Log.e(TAG, "库中无对应user,无需删除");
            }
            return new Pair<>(faceModel.id, delete || user2Delete == null);
        }

        PropertiesUtils instance = PropertiesUtils.Companion.getInstance();
        instance.init();
        instance.open();
        String fileBaseUrl = instance.readString("fileBaseUrl", "http://gadoor.ciih.net/");
        if (faceModel.personPic != null && !faceModel.personPic.startsWith("http")) {
            faceModel.personPic = fileBaseUrl + faceModel.personPic;
        }
        File batchImageDir = FileUtils.getBatchImportDirectory();
        ResponseBody responseBody = null;
        try {
            responseBody = RetrofitManager.getInstance().getRetrofit().create(Api.class)
                    .downloadFile(faceModel.personPic)
                    .blockingGet();
        } catch (Exception e) {
            Log.e(TAG, "图片下载失败");
            return new Pair<>(faceModel.id, false);
        }
        //保存到本地的图片路径
        String imageName = "origin_" + faceModel.id + "_id.jpg";
        //放到批量导入文件夹下
        File faceFile = new File(batchImageDir, imageName);
        //文件写入
        writeResponseBodyToDisk(responseBody, faceFile);
        Bitmap bitmap = BitmapFactory.decodeFile(faceFile.getPath());
        boolean success = false;
        if (bitmap != null) {
            byte[] bytes = new byte[512];
            float ret;
            // 走人脸SDK接口，通过人脸检测、特征提取拿到人脸特征值
            ret = FaceApi.getInstance().getFeature(bitmap, bytes,
                    BDFaceSDKCommon.FeatureType.BDFACE_FEATURE_TYPE_LIVE_PHOTO);

            Log.i(TAG, "live_photo = " + ret);

            if (ret == -1) {
                Log.e(TAG, faceModel.id + "未检测到人脸，可能原因：人脸太小或角度不正确");
            } else if (ret == 128) {

                User user = FaceApi.getInstance().getUserListByUserId(faceModel.id + "");
                boolean importDBSuccess;
                if (user != null) {
                    importDBSuccess = FaceApi.getInstance().userUpdateWithUserId(
                            GROUP_NAME, faceModel.id + "", faceModel.personPic, bytes
                    );
                } else {
                    // 将用户信息和用户组信息保存到数据库
                    importDBSuccess = FaceApi.getInstance().registerUserIntoDBmanager(GROUP_NAME,
                            faceModel.id + "", faceModel.personPic, null, bytes);
                }

                // 保存数据库成功
                if (importDBSuccess) {
                    // 保存图片到新目录中
                    File facePicDir = FileUtils.getBatchImportSuccessDirectory();
                    if (facePicDir != null) {
                        File savePicPath = new File(facePicDir, imageName);
                        if (FileUtils.saveBitmap(savePicPath, bitmap)) {
                            Log.i(TAG, "图片保存成功");
                            success = true;
                        } else {
                            Log.i(TAG, "图片保存失败");
                        }
                    }
                } else {
                    Log.e(TAG, imageName + "：保存到数据库失败");
                }
            } else {
                Log.e(TAG, imageName + "：未检测到人脸");
            }

            // 图片回收
            if (!bitmap.isRecycled()) {
                bitmap.recycle();
            }
        } else {
            Log.e(TAG, imageName + "：该图片转成Bitmap失败");
        }
        Log.e(TAG, "注册:" + (success ? "成功" : "失败") + faceModel.toString());
        return new Pair<>(faceModel.id, success);
    }


    private static void writeResponseBodyToDisk(ResponseBody body, File path) {
        try {
            InputStream inputStream = null;
            OutputStream outputStream = null;

            try {
                byte[] fileReader = new byte[4096];


                inputStream = body.byteStream();
                outputStream = new FileOutputStream(path);

                while (true) {
                    int read = inputStream.read(fileReader);

                    if (read == -1) {
                        break;
                    }

                    outputStream.write(fileReader, 0, read);


                }

                outputStream.flush();

            } catch (IOException e) {
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }

                if (outputStream != null) {
                    outputStream.close();
                }
            }
        } catch (IOException e) {
        }
    }
}
