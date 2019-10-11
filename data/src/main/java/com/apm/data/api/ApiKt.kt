package com.apm.data.api

import com.apm.data.model.*
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.http.*

interface ApiKt {

    /**
     * pic - image
     */
    @POST("/business/upload/uploadPic")
    suspend fun uploadImageSync(
            @Body image: MultipartBody
    ): BaseResponse<ImageDetail>


    /**
     * pic - image
     */
    @POST("/business/upload/uploadFile")
    suspend fun uploadFile(
        @Body image: MultipartBody
    ): BaseResponse<FileDetail>

    @FormUrlEncoded
    @POST("/business/visitor/addVisitorGate")
    suspend fun addKeyPassRecord(
        @Field("gateId") gateId: String,
        @Field("passCode") passCode: String,
        @Field("visitorAvatar") imageUrl: String
    ): BaseResponse<*>


    //电动车通行日志
    @FormUrlEncoded
    @POST("/business/ebikeLog/addLog")
    suspend fun addEBikePassLog(
            @Field("recogId") id: String,
            @Field("doorNo") deviceId: String,
            @Field("imageUrl") image: String?
    ): BaseResponse<*>




    //注册人脸
    @POST("/business/gateImage/uploadGateImage")
    suspend fun registerFace(
        @Body data: MultipartBody
    ): BaseResponse<RegisterResult>

    //获取未同步face数据
    @FormUrlEncoded
    @POST("/business/recognition/getNotSync")
    suspend fun getNotSync(
        @Field("lastSyncTime") lastSyncTime: String,
        @Field("doorCurrentTime") doorCurrentTime: String,
        @Field("doorNo") deviceId: String
    ): BaseResponse<List<FaceModel>>

    //上传未注册成功的id,逗号分隔
    @FormUrlEncoded
    @POST("/business/recognition/feedbackNoSyncData")
    suspend fun addUnRegisterIds(
        @Field("ids") unregisterIds: String,
        @Field("doorNo") deviceId: String
    ): BaseResponse<*>

    //文件下载
    @GET
    suspend fun downloadFile(
        @Url fileUrl: String
    ): ResponseBody


    //人脸通行
    @FormUrlEncoded
    @POST("/business/inOutLog/addLog")
    suspend fun passByFaceId(
        @Field("recogId") userId: String, @Field("doorNo") gateId: String, @Field("imageUrl") imageUrl: String
    ): BaseResponse<*>


}