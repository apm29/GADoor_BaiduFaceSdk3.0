package com.apm.data.model

import com.google.gson.annotations.SerializedName


/**

    {
        "recogId": 7,
        "districtId": 2,
        "plateIncode": "789",
        "plateDecode": "ghi",
        "userId": null,
        "updateTime": "2019-06-17T06:09:07.000+0000",
        "delFlag": "0"
    },
 */
data class RFIDModel(
        @SerializedName("recogId")val id: Long?,
        @SerializedName("plateIncode")val hexString: String?,
        val delFlag:String?

){
    val delete: Boolean
        get() {
            return "1" == delFlag
        }
}
