package com.apm.anxinju.main

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.apm.anxinju.main.service.FaceDataSyncService
import com.apm.data.model.FaceModel

/**
 *  author : ciih
 *  date : 2019-09-28 10:01
 *  description :
 */
class RegisterReceiver : BroadcastReceiver() {

    companion object{
        var onReceiveRegister: ((FaceModel?, Boolean) -> Unit)? = null
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.apply {
            val faceModel =
                getParcelableExtra<FaceModel>(FaceDataSyncService.KEY_NOTIFY_REGISTER_MODEL)
            val success =
                getBooleanExtra(FaceDataSyncService.KEY_NOTIFY_REGISTER_SUCCESS, false)
            onReceiveRegister?.invoke(
                faceModel, success
            )
        }
    }
}