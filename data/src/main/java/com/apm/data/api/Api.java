package com.apm.data.api;

import com.apm.data.model.BaseResponse;
import com.apm.data.model.FaceModel;
import com.apm.data.model.FileDetail;
import com.apm.data.model.ImageDetail;
import com.apm.data.model.RFIDModel;

import java.util.List;

import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Url;

public interface Api {
    @FormUrlEncoded
    @POST("/business/visitor/passCodeVerify")
    Single<BaseResponse> passByKeyCode(
            @Field("gateId") String gateId,
            @Field("passCode") String passCode
    );

    //人脸通行
    @FormUrlEncoded
    @POST("/business/inOutLog/addLog")
    Observable<BaseResponse> passByFaceId(
            @Field("recogId") String userId, @Field("doorNo") String gateId, @Field("imageUrl") String imageUrl
    );

    //获取未同步face数据
    @FormUrlEncoded
    @POST("/business/recognition/getNotSync")
    Observable<BaseResponse<List<FaceModel>>> getNotSync(
            @Field("lastSyncTime") String lastSyncTime,
            @Field("doorCurrentTime") String doorCurrentTime,
            @Field("doorNo") String deviceId
    );

//    //获取未同步face数据
//    @FormUrlEncoded
//    @POST("/business/recognition/getNotSync")
//    Flowable<BaseResponse<List<FaceModel>>> getNotSyncFlowable(
//            @Field("lastSyncTime") String lastSyncTime
//    );

    //获取全部人脸数据
    @FormUrlEncoded
    @POST("/business/")
    Single<BaseResponse<List<FaceModel>>> fullSync();

    //文件下载
    @GET
    Single<ResponseBody> downloadFile(
            @Url String fileUrl
    );

    /**
     * pic - image
     */
    @POST("/business/upload/uploadPic")
    Observable<BaseResponse<ImageDetail>> uploadImage(
            @Body MultipartBody image
    );

    /**
     * pic - image
     */
    @POST("/business/upload/uploadFile")
    Observable<BaseResponse<FileDetail>> uploadFile(
            @Body MultipartBody image
    );


    /**
     * pic - image
     */
    @POST("/business/upload/uploadPic")
    Call<BaseResponse<ImageDetail>> uploadImageSync(
            @Body MultipartBody image
    );


    @FormUrlEncoded
    @POST("/business/visitor/addVisitorGate")
    Observable<BaseResponse> addKeyPassRecord(
            @Field("gateId") String gateId,
            @Field("passCode") String passCode,
            @Field("visitorAvatar") String imageUrl
    );

    //上传未注册成功的id,逗号分隔
    @FormUrlEncoded
    @POST("/business/recognition/feedbackNoSyncData")
    Maybe<BaseResponse> addUnRegisterIds(
            @Field("ids") String unregisterIds,
            @Field("doorNo") String deviceId

    );

    //临时访客记录
    @POST("/business/gateImage/uploadGateImage")
    Observable<BaseResponse> addTempVisitorRecord(
            @Body MultipartBody data
    );


    //获取未同步射频id数据
    @FormUrlEncoded
    @POST("/business/ebikeRecognition/getNotSync")
    Observable<BaseResponse<List<RFIDModel>>> getNotSyncRFID(
            @Field("lastSyncTime") String lastSyncTime,
            @Field("doorCurrentTime") String doorCurrentTime,
            @Field("doorNo") String deviceId
    );


    //上传未注册成功的id,逗号分隔
    @FormUrlEncoded
    @POST("/business/ebikeRecognition/feedbackNoSyncData")
    Maybe<BaseResponse> addUnRegisterRFID(
            @Field("ids") String unregisterIds,
            @Field("doorNo") String deviceId
    );


    //电动车通行日志 TODO
    @FormUrlEncoded
    @POST("/business/ebikeLog/addLog")
    Observable<BaseResponse> addEBikePassLog(
            @Field("recogId")String id,
            @Field("doorNo")String deviceId,
            @Field("imageUrl") String image
    );

}


