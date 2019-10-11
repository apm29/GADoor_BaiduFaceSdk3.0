package com.apm.anxinju.main.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.apm.anxinju_baidufacesdk30.R
import com.apm.anxinju.main.listener.SdkInitListener
import com.apm.anxinju.main.manager.FaceSDKManager
import com.apm.anxinju.main.utils.ConfigUtils
import com.apm.anxinju.main.utils.ToastUtils
import me.drakeet.support.toast.ToastCompat

class MainActivity : BaseActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        //初始化配置文件
        val isConfigExit = ConfigUtils.isConfigExit()
        val isInitConfig = ConfigUtils.initConfig()
        if (isInitConfig && isConfigExit) {
            ToastCompat.makeText(this@MainActivity, "初始配置加载成功", Toast.LENGTH_SHORT).show()
        } else {
            ToastCompat.makeText(this@MainActivity, "初始配置失败,将重置文件内容为默认配置", Toast.LENGTH_SHORT).show()
            ConfigUtils.modityJson()
        }

        //激活引擎
        initLicense()
    }

    private fun initLicense() {
        if (FaceSDKManager.initStatus != FaceSDKManager.SDK_MODEL_LOAD_SUCCESS) {
            FaceSDKManager.getInstance().init(this, object :
                SdkInitListener {
                override fun initStart() {

                }

                override fun initLicenseSuccess() {
                    runOnUiThread {
                        goPreview()
                    }
                }

                override fun initLicenseFail(errorCode: Int, msg: String) {
                    // 如果授权失败，跳转授权页面
                    ToastUtils.toast(this@MainActivity, errorCode.toString() + msg)
                    startActivity(Intent(this@MainActivity, FaceAuthActivity::class.java))
                }

                override fun initModelSuccess() {}

                override fun initModelFail(errorCode: Int, msg: String) {

                }
            })
        }else{
            goPreview()
        }
    }

    private fun goPreview() {
        startActivity(Intent(this, PreviewActivity::class.java))
    }
}
