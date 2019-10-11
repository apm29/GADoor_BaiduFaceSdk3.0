package com.spark.zj.comcom.serial;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;
import android.text.TextUtils;
import android.util.Log;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;

public class SerialSignalManager implements SerialHelper.SerialSignalReceiver {
    private String tag = "SerialSignalManager";
    private SerialHelper serialHelper;
    private Disposable receiverDisposable;
    private Disposable senderDisposable;
    private Disposable distanceSenderDisposable;
    private Disposable distanceReceiveDispable;
    private ObservableEmitter<byte[]> distanceReceiverEmitter;

    private SerialSignalManager() {
        String[] allDevicesPath = new SerialPortFinder().getAllDevicesPath();
        for (int i = 0; i < allDevicesPath.length; i++) {
            String port = allDevicesPath[i];
            System.out.println("port = " + port);
            if (port.contains("ttyS4")) {
                //默认9600波特率 N 8 1
                try {
                    serialHelper = new SerialHelper(port);
                    serialHelper.setDataReceiver(this);
                    serialHelper.open();
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(tag, "serialHelper open failed");
                }


            }
        }
    }

    public interface ICheckCallback {
        void errorTip(String error);

        void onDataBytes(byte[] data);

        void learnState(int timeValidated, String signal);

        void onDistanceInRange();
    }

    private ICheckCallback checkCallback;

    public SerialSignalManager setCheckCallback(ICheckCallback checkCallback) {
        this.checkCallback = checkCallback;
        return this;
    }

    private ObservableEmitter<byte[]> learnReceiverEmitter;
    private static SerialSignalManager instance;

    public static SerialSignalManager getInstance() {
        if (instance == null) {
            synchronized (SerialSignalManager.class) {
                if (instance == null) {
                    instance = new SerialSignalManager();
                }
            }
        }
        return instance;
    }

    //FD5AF9A260DF
    public boolean headTailValidate(byte[] data) {
        String hexString = ByteUtils.bytesToHexString(data);
        if (!TextUtils.isEmpty(hexString) && hexString.length() == 12) {
            return hexString.startsWith("FD") && hexString.endsWith("DF");
        }
        return false;
    }

    private static final String SIGNAL_START_LEARN = "02";
    private static final String SIGNAL_END_LEARN = "00";
    private static final String SIGNAL_DISTANCE_DETECT = "05";
    private static final String SIGNAL_LEARN_TYPE_A = "SIGNAL_LEARN_TYPE_A";
    private static final String SIGNAL_LEARN_TYPE_B = "SIGNAL_LEARN_TYPE_B";
    private static final int LEARN_TIMES = 3;

    @Override
    public void onDataByteReceived(byte[] data) {
        if (learnReceiverEmitter != null && inLearnSignalMode) {
            learnReceiverEmitter.onNext(data);
        }
        if (inDistanceDetect && distanceReceiverEmitter != null) {
            distanceReceiverEmitter.onNext(data);
        }
    }

    class SignalHostLifecycleObserver implements LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        public void onDestroy() {
            close();
        }
    }

    public void close() {
        if (serialHelper != null) {
            try {
                serialHelper.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (receiverDisposable != null && !receiverDisposable.isDisposed()) {
            receiverDisposable.dispose();
        }
    }

    public SerialSignalManager bindLifecycleOwner(LifecycleOwner owner) {
        owner.getLifecycle().addObserver(new SignalHostLifecycleObserver());
        return this;
    }

    private boolean inLearnSignalMode = false;
    private int currentLearnButtonType = -1;

    /**
     * 开始学习信号或者停止学习信号
     *
     * @param type 按键类型
     * @return 当前的状态:学习中->true 已停止->false
     * @throws IllegalAccessException
     */
    public boolean startStopLearnSignal(int type) throws IllegalAccessException {
        if (serialHelper != null) {
            if (!inLearnSignalMode) {
                receiverDisposable = Observable.create(new ObservableOnSubscribe<byte[]>() {
                    @Override
                    public void subscribe(ObservableEmitter<byte[]> emitter) throws Exception {
                        learnReceiverEmitter = emitter;
                        Log.d(tag, "receiver thread is running");
                    }
                }).subscribeOn(AndroidSchedulers.mainThread())
                        .observeOn(Schedulers.computation())
                        .filter(new Predicate<byte[]>() {
                            @Override
                            public boolean test(byte[] bytes) throws Exception {
                                boolean check = headTailValidate(bytes);
                                if (!check && checkCallback != null) {
                                    checkCallback.errorTip("数据校验失败,请重试");
                                }
                                return check;
                            }
                        })
                        .map(new Function<byte[], byte[]>() {
                            @Override
                            public byte[] apply(byte[] bytes) throws Exception {
                                if (checkCallback != null) {
                                    checkCallback.errorTip("单次学习成功");
                                    checkCallback.onDataBytes(bytes);
                                }
                                return bytes;
                            }
                        })
                        .buffer(LEARN_TIMES)
                        .subscribe(new Consumer<List<byte[]>>() {
                            @Override
                            public void accept(List<byte[]> data) throws Exception {
                                boolean dataConsistent = checkDataByteList(data);
                                if (dataConsistent && checkCallback != null) {
                                    checkCallback.learnState(data.size(), ByteUtils.bytesToHexString(data.get(0)));
                                }
                            }
                        }, new Consumer<Throwable>() {
                            @Override
                            public void accept(Throwable throwable) throws Exception {
                                checkCallback.errorTip(throwable.getMessage());
                            }
                        });
                senderDisposable = Observable.interval(200, TimeUnit.MILLISECONDS)
                        .subscribe(new Consumer<Long>() {
                            @Override
                            public void accept(Long aLong) throws Exception {
                                serialHelper.sendHexString(SIGNAL_START_LEARN);
                            }
                        });

            } else {
                if (receiverDisposable != null && !receiverDisposable.isDisposed()) {
                    receiverDisposable.dispose();
                }
                if (senderDisposable != null && !senderDisposable.isDisposed()) {
                    senderDisposable.dispose();
                }
                serialHelper.sendHexString(SIGNAL_END_LEARN);
            }
            inLearnSignalMode = !inLearnSignalMode;
            currentLearnButtonType = type;
            return inLearnSignalMode;
        } else {
            throw new IllegalAccessException("serial help 未实例化");
        }
    }

    //FD5AF9A262DF
    private boolean checkDataByteList(List<byte[]> data) {
        String last = null;
        for (int i = 0; i < data.size(); i++) {
            byte[] bytes = data.get(i);
            String hexString = ByteUtils.bytesToHexString(bytes);
            if (last == null) {
                last = hexString.substring(2, 8);
            } else {
                if (!last.equals(hexString.substring(2, 8))) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 06控制码
     * @param savedSignal
     */
    public void sendSimulateOpenSignal(String savedSignal) {
        if (serialHelper != null) {
            serialHelper.sendHexString("06" + savedSignal);
        }
    }

    public boolean isInDistanceDetect() {
        return inDistanceDetect;
    }

    private boolean inDistanceDetect = false;
    private long last = System.currentTimeMillis();
    public boolean startStopDistanceReceive() throws IllegalAccessException {
        if (serialHelper != null) {
            last = 0;
            if (!inDistanceDetect) {
                distanceReceiveDispable = Observable.create(new ObservableOnSubscribe<byte[]>() {
                    @Override
                    public void subscribe(ObservableEmitter<byte[]> emitter) throws Exception {
                        distanceReceiverEmitter = emitter;
                        Log.d(tag, "receiver thread is running");
                    }
                }).subscribeOn(AndroidSchedulers.mainThread())
                        .observeOn(Schedulers.computation())
                        .map(new Function<byte[], Integer>() {
                            @Override
                            public Integer apply(byte[] bytes) throws Exception {
                                Integer integer = null;
                                try {
                                    integer = Integer.valueOf(ByteUtils.bytesToAscii(bytes));
                                } catch (NumberFormatException e) {
                                    e.printStackTrace();
                                    integer = 0;
                                }
                                System.out.println(integer);
                                return integer;
                            }
                        })
                        .filter(new Predicate<Integer>() {
                            @Override
                            public boolean test(Integer integer) throws Exception {
                                return integer > 0 && integer < 50;
                            }
                        })
//                        .buffer(220, TimeUnit.MILLISECONDS)
                        .subscribe(new Consumer<Integer>() {
                            @Override
                            public void accept(final Integer range) throws Exception {
                                long interval = System.currentTimeMillis() - last;
                                if (checkCallback != null && interval > 3000) {
                                    checkCallback.onDistanceInRange();
                                    last = System.currentTimeMillis();
                                }
                            }
                        });

                distanceSenderDisposable = Observable.interval(200, TimeUnit.MILLISECONDS)
                        .subscribeOn(AndroidSchedulers.mainThread())
                        .observeOn(Schedulers.computation())
                        .doOnNext(new Consumer<Long>() {
                            @Override
                            public void accept(Long aLong) throws Exception {
                                serialHelper.sendHexString(SIGNAL_DISTANCE_DETECT);
                            }
                        })
                        .subscribe();
            } else {
                stopDistanceDetect();
            }
            inDistanceDetect = !inDistanceDetect;
            return inDistanceDetect;
        } else {
            throw new IllegalAccessException("serial-port 未初始化");
        }

    }

    private void stopDistanceDetect() {
        if (distanceSenderDisposable != null && !distanceSenderDisposable.isDisposed()) {
            distanceSenderDisposable.dispose();
        }
        if (distanceReceiveDispable != null && !distanceReceiveDispable.isDisposed()) {
            distanceReceiveDispable.dispose();
        }
    }

}
