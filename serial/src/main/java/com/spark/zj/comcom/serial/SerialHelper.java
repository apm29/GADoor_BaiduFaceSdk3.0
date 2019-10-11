package com.spark.zj.comcom.serial;

import android.os.SystemClock;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class SerialHelper {
    private String tag = this.getClass().getSimpleName();
    private int baudRate = 9600;
    private String port;
    private int stopBits = 1;
    private int dataBits = 8;
    private int parity = 0;
    private SerialPort mSerialPort;

    public interface SerialSignalReceiver {
        void onDataByteReceived(byte[] data);
    }

    public SerialHelper(int baudRate, String port, int stopBits, int dataBits, int parity) {
        this.baudRate = baudRate;
        this.port = port;
        this.stopBits = stopBits;
        this.dataBits = dataBits;
        this.parity = parity;
    }

    public SerialHelper(String port) {
        this.port = port;
    }

    private SerialSignalReceiver dataReceiver;

    public void setDataReceiver(SerialSignalReceiver dataReceiver) {
        this.dataReceiver = dataReceiver;
    }

    private ObservableEmitter<byte[]> mSenderEmitter;
    private Disposable mSenderDisposable;

    private Disposable mReceiverDisposable;
    private boolean isInterrupted = false;
    private boolean mLogData = false;

    public void setLogData(boolean mLogData) {
        this.mLogData = mLogData;
    }

    public void open() {
        if (mSerialPort != null) {
            Log.e(tag, "serial-port has been opened before");
            return;
        }
        try {
            SerialPort.setSuPath("su");
            mSerialPort = new SerialPort(new File(port), baudRate, parity, dataBits, stopBits, 0);
        } catch (IOException e) {
            e.printStackTrace();
        }

        mSenderDisposable = Observable.create(new ObservableOnSubscribe<byte[]>() {
            @Override
            public void subscribe(ObservableEmitter<byte[]> emitter) throws Exception {
                mSenderEmitter = emitter;
            }
        }).doOnNext(new Consumer<byte[]>() {
            @Override
            public void accept(byte[] bytes) throws Exception {
                if (mSerialPort != null) {
                    OutputStream outputStream = mSerialPort.getOutputStream();
                    outputStream.write(bytes);
                    if (mLogData)
                        Log.d(tag, "data sent: " + ByteUtils.bytesToHexString(bytes));
                }
            }
        })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<byte[]>() {
                    @Override
                    public void accept(byte[] bytes) throws Exception {

                    }
                });

        mReceiverDisposable = Flowable.create(new FlowableOnSubscribe<byte[]>() {
            @Override
            public void subscribe(FlowableEmitter<byte[]> emitter) throws Exception {
                InputStream is = mSerialPort.getInputStream();
                int available;
                int first;
                while (!isInterrupted
                        && mSerialPort != null
                        && is != null
                        && (first = is.read()) != -1) {
                    do {
                        available = is.available();
                        SystemClock.sleep(1);
                    } while (available != is.available());
                    available = is.available();
                    byte[] bytes = new byte[available + 1];
                    int read = is.read(bytes, 1, available);
                    bytes[0] = (byte) (first & 0xFF);
                    emitter.onNext(bytes);
                }
                while (!isInterrupted) {
                    int size;
                    try {
                        byte[] buffer = new byte[64];
                        if (is == null) return;
                        size = is.read(buffer);
                        //System.out.println("serial size = " + size);
                        if (size > 0) {
                            byte[] data = new byte[size];
                            for (int i = 0; i < size; i++) {
                                data[i] = buffer[i];
                            }
                            emitter.onNext(data);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        return;
                    }
                }
                System.out.println("main loop break");
                //close();
            }
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.single())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(new Consumer<byte[]>() {
                    @Override
                    public void accept(byte[] bytes) throws Exception {
                        if (mLogData)
                            Log.d(tag, "data received: " + ByteUtils.bytesToHexString(bytes)
                                    + "(" + ByteUtils.bytesToAscii(bytes) + ")");
                        if (dataReceiver != null) {
                            dataReceiver.onDataByteReceived(bytes);
                        }
                    }
                })
                .subscribe();
    }

    private void send(byte[] data) {
        if (mSenderEmitter != null) {
            mSenderEmitter.onNext(data);
        }
    }

    public void sendHexString(String hexString) {
        send(ByteUtils.hexStringToBytes(hexString));
    }

    public void sendText(String text) {
        send(text.getBytes());
    }


    public void close() {
        isInterrupted = true;
        dispose(mReceiverDisposable);
        mSerialPort = null;
    }

    private void dispose(Disposable disposable) {
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
    }
}
