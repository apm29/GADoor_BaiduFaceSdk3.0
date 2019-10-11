package com.apm.qrcode;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

public class QRScanView extends FrameLayout implements QRCodeReaderView.OnQRCodeReadListener {

    private int maskColor = Color.parseColor("#44FFFFFF");
    private int mBorderColor = Color.GREEN;

    public QRScanView(Context context) {
        this(context, null);
    }

    public QRScanView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public QRScanView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public QRScanView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    QRCodeReaderView mQrCodeView;
    Paint mPaintBorder;
    Paint mPaintText;

    private void init() {

        mPaintBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaintText = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaintText.setColor(Color.RED);
        mPaintText.setTextSize(33);

        LayoutInflater.from(getContext()).inflate(R.layout.layout_qrcode, this, true);
        mQrCodeView = findViewById(R.id.qrCodeReaderView);
        mQrCodeView.setOnQRCodeReadListener(this);

        // Use this function to enable/disable decoding
        mQrCodeView.setQRDecodingEnabled(true);

        // Use this function to change the autofocus interval (default is 5 secs)
        mQrCodeView.setAutofocusInterval(2000L);
        mQrCodeView.forceAutoFocus();
        // Use this function to enable/disable Torch
        mQrCodeView.setTorchEnabled(true);

        // Use this function to set back camera preview
        mQrCodeView.setFrontCamera();
    }


    public  void onResume(){
        mQrCodeView.startCamera();
    }

    public  void onPause(){
        mQrCodeView.stopCamera();
    }

    public void onDestroy(){
        mQrCodeView.close();
    }

    String scanText;
    PointF[] scanBorder = new PointF[]{};

    @Override
    public void onQRCodeRead(String text, PointF[] points) {
        scanText = text;
        scanBorder = points;
        if(iqr!=null){
            iqr.onQrText(text,points);
        }
    }

    RectF mFramingRect;
    Float scanY = 0F;

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        //canvas.drawCircle(mWidth / 2, mHeight / 2, mWidth / 2, mPaintBorder);

        int width = canvas.getWidth();
        int height = canvas.getHeight();

        if (mFramingRect == null) {
            mFramingRect = new RectF();
            float frameWidth = width * 0.8F;
            float horizontalOffset = width * 0.2F / 2;
            float verticalOffset = (height - frameWidth) / 2;
            mFramingRect.left = horizontalOffset;
            mFramingRect.right = horizontalOffset + frameWidth;
            mFramingRect.top = verticalOffset;
            mFramingRect.bottom = verticalOffset + frameWidth;
            scanY = mFramingRect.top;
        }

        mPaintBorder.setStyle(Paint.Style.FILL);
        mPaintBorder.setColor(maskColor);
        canvas.drawRect(0, 0, width, mFramingRect.top, mPaintBorder);
        canvas.drawRect(0, mFramingRect.top, mFramingRect.left, mFramingRect.bottom + 1, mPaintBorder);
        canvas.drawRect(mFramingRect.right + 1, mFramingRect.top, width, mFramingRect.bottom + 1, mPaintBorder);
        canvas.drawRect(0, mFramingRect.bottom + 1, width, height, mPaintBorder);


        mPaintBorder.setStyle(Paint.Style.STROKE);
        mPaintBorder.setColor(mBorderColor);
        mPaintBorder.setStrokeWidth(3);
        canvas.drawRect(mFramingRect, mPaintBorder);

        mPaintBorder.setStrokeWidth(18);
        canvas.drawLine(mFramingRect.left, mFramingRect.top, mFramingRect.left, mFramingRect.top + 140F, mPaintBorder);
        canvas.drawLine(mFramingRect.left, mFramingRect.top, mFramingRect.left + 140F, mFramingRect.top, mPaintBorder);

        canvas.drawLine(mFramingRect.right, mFramingRect.top, mFramingRect.right, mFramingRect.top + 140F, mPaintBorder);
        canvas.drawLine(mFramingRect.right, mFramingRect.top, mFramingRect.right - 140F, mFramingRect.top, mPaintBorder);

        canvas.drawLine(mFramingRect.left, mFramingRect.bottom, mFramingRect.left, mFramingRect.bottom - 140F, mPaintBorder);
        canvas.drawLine(mFramingRect.left, mFramingRect.bottom, mFramingRect.left + 140F, mFramingRect.bottom, mPaintBorder);

        canvas.drawLine(mFramingRect.right, mFramingRect.bottom, mFramingRect.right, mFramingRect.bottom - 140F, mPaintBorder);
        canvas.drawLine(mFramingRect.right, mFramingRect.bottom, mFramingRect.right - 140F, mFramingRect.bottom, mPaintBorder);


        mPaintBorder.setStrokeWidth(3);
        canvas.drawLine(mFramingRect.left, scanY, mFramingRect.right, scanY, mPaintBorder);
        mPaintBorder.setStrokeWidth(1);
        for (int i = 0; i < 4; i++) {
            int div = 12 * i;
            canvas.drawLine(mFramingRect.left + div, scanY - div, mFramingRect.right - div, scanY - div, mPaintBorder);
        }
        scanY = scanY + 10;
        if (scanY > mFramingRect.bottom) {
            scanY = mFramingRect.top;
        }

        String hint = "请将二维码对准扫描区域";
        if(scanText!=null){
            hint = scanText;
        }
        float textWidth = mPaintText.measureText(hint);
        canvas.drawText(hint,mFramingRect.centerX() - textWidth/2,mFramingRect.centerY(),mPaintText);


        mPaintBorder.setStyle(Paint.Style.FILL);
        canvas.drawCircle(mFramingRect.left, mFramingRect.top,9,mPaintBorder);
        canvas.drawCircle(mFramingRect.right, mFramingRect.top,9,mPaintBorder);
        canvas.drawCircle(mFramingRect.left, mFramingRect.bottom,9,mPaintBorder);
        canvas.drawCircle(mFramingRect.right, mFramingRect.bottom,9,mPaintBorder);

        invalidate();
    }

    IQR iqr;

    public void setIqr(IQR iqr) {
        this.iqr = iqr;
    }

    public interface IQR{
        void onQrText(String qrText, PointF[] points);
    }


}
