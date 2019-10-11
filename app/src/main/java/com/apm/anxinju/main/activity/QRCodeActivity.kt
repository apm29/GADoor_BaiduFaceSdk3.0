package com.apm.anxinju.main.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.apm.anxinju_baidufacesdk30.R
//import com.apm.qrcode.QRScanView
import kotlinx.android.synthetic.main.activity_qrcode.*

class QRCodeActivity : BaseActivity() {


    companion object {
        const val QR_TEXT = "QR_TEXT"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qrcode)
//        val qrScanView = findViewById<QRScanView>(R.id.layout_qrcode)
//        qrScanView.setIqr { qrText, _ ->
//            setResult(Activity.RESULT_OK, Intent().apply {
//                putExtra(QR_TEXT, qrText)
//            })
//            layout_qrcode.onDestroy()
//            finish()
//        }
//        qrScanView.postDelayed({
//            layout_qrcode.onDestroy()
//            finish()
//        }, 30000)
    }

    override fun onResume() {
        super.onResume()
//        layout_qrcode.onResume()
    }

    override fun onPause() {
        super.onPause()
//        layout_qrcode.onPause()
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onBackPressed() {
        super.onBackPressed()
//        layout_qrcode.onDestroy()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
