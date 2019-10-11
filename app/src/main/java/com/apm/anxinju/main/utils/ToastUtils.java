package com.apm.anxinju.main.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.apm.anxinju.main.App;

import me.drakeet.support.toast.ToastCompat;

public class ToastUtils {

    private static Handler handler = new Handler(Looper.getMainLooper());

    public static void toast(final Context context, final String text) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                ToastCompat.makeText(App.getContextGlobal(), text, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static void toast(final Context context, final int resId) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                ToastCompat.makeText(context, resId, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
