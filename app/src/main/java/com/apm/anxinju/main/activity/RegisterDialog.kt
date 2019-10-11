package com.apm.anxinju.main.activity

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.apm.anxinju_baidufacesdk30.R
import kotlinx.android.synthetic.main.register_dialog.*

/**
 *  author : ciih
 *  date : 2019-10-10 15:30
 *  description :
 */
class RegisterDialog(val bitmap: Bitmap, val onRegister: () -> Unit,val onCancel: () -> Unit) : DialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.register_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mImageRegisterFace.setImageBitmap(bitmap)
        mBtnCancel.setOnClickListener {
            onCancel()
            dismiss()
        }
        mBtnRegister.setOnClickListener {
            onRegister()
            dismiss()
        }
    }
}