package com.example.verifyformscore.widget;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;

import com.example.verifyformscore.activity.VerifyScoreActivity;
import com.example.verifyformscore.utils.FileUtils;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * MyCameraView
 *
 * @author yuepengfei
 * @date 2019/6/24
 * @description 自定义Camera2相机
 */
public class MyCameraView extends TextureView {

    /**
     * 学生信息，试卷详情，分数 的矩形选区位置
     */
    private Rect mStuInfoRect;
    private Rect mTestDetailRect;
    private Rect mScoreRect;
    /**
     * 识别类别
     */
    private String mType;

    private static final String TAG = "CameraPreview";
    /**
     * 从屏幕旋转转换为JPEG方向
     */
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    /**
     * Camera2 API 保证的最大预览宽高
     */
    private static final int MAX_PREVIEW_WIDTH = 2560;
    private static final int MAX_PREVIEW_HEIGHT = 1440;
    /**
     * 显示相机预览
     */
    private static final int STATE_PREVIEW = 0;
    /**
     * 焦点锁定中
     */
    private static final int STATE_WAITING_LOCK = 1;
    /**
     * 拍照中
     */
    private static final int STATE_WAITING_PRE_CAPTURE = 2;
    /**
     * 其它状态
     */
    private static final int STATE_WAITING_NON_PRE_CAPTURE = 3;
    /**
     * 拍照完毕
     */
    private static final int STATE_PICTURE_TAKEN = 4;
    private int mState = STATE_PREVIEW;
    private int mRatioWidth = 0, mRatioHeight = 0;
    private int mSensorOrientation;
    /**
     * 是否支持闪光
     */
    private boolean mFlashSupported;

    CompositeDisposable mCompositeDisposable;
    /**
     * 使用信号量 Semaphore 进行多线程任务调度
     */
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);
    private Activity activity;
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    private Size mPreviewSize;
    private String mCameraId;
    private CameraDevice mCameraDevice;
    private CaptureRequest.Builder mPreviewRequestBuilder;
    private CaptureRequest mPreviewRequest;
    private CameraCaptureSession mCaptureSession;
    /**
     * 照片渲染
     */
    private ImageReader mImageReader;

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    public MyCameraView(Context context) {
        this(context, null);
    }

    public MyCameraView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MyCameraView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mCompositeDisposable = new CompositeDisposable();
    }

    public void setType(String type) {
        this.mType = type;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (0 == mRatioWidth || 0 == mRatioHeight) {
            // 重新测量宽高
            setMeasuredDimension(width, height);
        } else {
            if (width < height * mRatioWidth / mRatioHeight) {
                setMeasuredDimension(width, width * mRatioHeight / mRatioWidth);
            } else {
                setMeasuredDimension(height * mRatioWidth / mRatioHeight, height);
            }
        }
    }

    public void onResume(Activity activity) {
        this.activity = activity;
        startBackgroundThread();
        //当Activity或Fragment OnResume()时,可以冲洗打开一个相机并开始预览,否则,这个Surface已经准备就绪
        if (this.isAvailable()) {
            openCamera(this.getWidth(), this.getHeight());
        } else {
            this.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    public void onPause() {
        closeCamera();
        stopBackgroundThread();
        mCompositeDisposable.clear();
    }


    public void setAspectRatio(int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Size can't be negative");
        }
        mRatioWidth = width;
        mRatioHeight = height;
        requestLayout();
    }

    public void setAutoFlash(CaptureRequest.Builder requestBuilder) {
        if (mFlashSupported) {
            requestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
        }
    }

    public void takePicture() {
        lockFocus();
    }

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        // 安全地退出处理程序线程的循环程序
        mBackgroundThread.quitSafely();
        try {
            // 等待线程死亡。
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 处理生命周期内的回调事件
     */
    private final TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {
        }
    };

    /**
     * 相机状态改变回调
     */
    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            // 释放线程
            mCameraOpenCloseLock.release();
            Log.d(TAG, "相机已打开");
            mCameraDevice = cameraDevice;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
            if (null != activity) {
                activity.finish();
            }
        }
    };

    /**
     * 处理与照片捕获相关的事件
     */
    private CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() {

        private void process(CaptureResult result) {
            switch (mState) {
                case STATE_PREVIEW: {
                    break;
                }
                case STATE_WAITING_LOCK: {
                    // 获取自动聚焦状态
                    Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                    if (afState == null) {
                        // 不聚焦则 拍摄静态图片
                        Log.d("yue", "process: 1");
                        captureStillPicture();
                    } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState ||
                            CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {
                        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                        if (aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                            mState = STATE_PICTURE_TAKEN;
                            Log.d("yue", "process: 2");
                            captureStillPicture();
                        } else {
                            Log.d("yue", "process: 3");
                            runPreCaptureSequence();
                        }
                        Log.d("yue", "process: 4");
                    }
                    break;
                }
                case STATE_WAITING_PRE_CAPTURE: {
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null ||
                            aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                            aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                        mState = STATE_WAITING_NON_PRE_CAPTURE;
                        Log.d("yue", "process: 5");
                    }
                    Log.d("yue", "process: 6");
                    break;
                }
                case STATE_WAITING_NON_PRE_CAPTURE: {
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        mState = STATE_PICTURE_TAKEN;
                        Log.d("yue", "process: 7");
                        captureStillPicture();
                    }
                    Log.d("yue", "process: 8");
                    break;
                }
                default:
            }
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                                        @NonNull CaptureRequest request,
                                        @NonNull CaptureResult partialResult) {
            process(partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {
            process(result);
        }

    };

    /**
     * 在确定相机预览大小后应调用此方法
     *
     * @param viewWidth  宽
     * @param viewHeight 高
     */
    private void configureTransform(int viewWidth, int viewHeight) {
        if (null == mPreviewSize || null == activity) {
            return;
        }
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        // 以竖屏向上方向为0°，后置相机采集到的图像是90°，前置相机采集到的图像时270°，需要旋转视图
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            // 旋转前的矩阵转换到旋转后的矩形，缩放大小为填充屏幕
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        this.setTransform(matrix);
    }

    /**
     * 根据mCameraId打开相机
     */
    private void openCamera(int width, int height) {
        setUpCameraOutputs();
        configureTransform(width, height);
        CameraManager manager = (CameraManager) getContext().getSystemService(Context.CAMERA_SERVICE);
        int timeout = 2500;
        try {
            // 如果尝试请求线程超时，表示阻塞，抛出异常
            if (!mCameraOpenCloseLock.tryAcquire(timeout, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            // 配置完后 打开相机 并绑定回调接口
            manager.openCamera(mCameraId, mStateCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
    }

    /**
     * 关闭相机
     */
    private void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            if (null != mCaptureSession) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != mImageReader) {
                mImageReader.close();
                mImageReader = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    /**
     * 设置相机相关的属性或变量
     */
    @SuppressWarnings("SuspiciousNameCombination")
    private void setUpCameraOutputs() {
        CameraManager manager = (CameraManager) getContext().getSystemService(Context.CAMERA_SERVICE);
        try {
            // 遍历所有连接的相机列表
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                // 只需要使用后置摄像头，前置摄像头跳过
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }
                // 获取可用的配置流
                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map == null) {
                    continue;
                }
                // 获取与JPEG兼容最大尺寸 相机支持的预览尺寸
               /* Size largest = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                        new CompareSizesByArea());*/

                int displayRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
                // noinspection ConstantConditions
                mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                boolean swappedDimensions = false;
                switch (displayRotation) {
                    case Surface.ROTATION_0:
                    case Surface.ROTATION_180:
                        if (mSensorOrientation == 90 || mSensorOrientation == 270) {
                            swappedDimensions = true;
                        }
                        break;
                    case Surface.ROTATION_90:
                    case Surface.ROTATION_270:
                        if (mSensorOrientation == 0 || mSensorOrientation == 180) {
                            swappedDimensions = true;
                        }
                        break;
                    default:
                        Log.e(TAG, "Display rotation is invalid: " + displayRotation);
                }

                Point displaySize = new Point();
                activity.getWindowManager().getDefaultDisplay().getSize(displaySize);
                int maxPreviewWidth = displaySize.x;
                int maxPreviewHeight = displaySize.y;

                if (swappedDimensions) {
                    maxPreviewWidth = displaySize.y;
                    maxPreviewHeight = displaySize.x;
                }

                if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
                    maxPreviewWidth = MAX_PREVIEW_WIDTH;
                }

                if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
                    maxPreviewHeight = MAX_PREVIEW_HEIGHT;
                }


                mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                        maxPreviewWidth,
                        maxPreviewHeight);

                // 为照片阅读器设置尺寸，高宽均为屏幕大小，便于 截取选区
                mImageReader = ImageReader.newInstance(maxPreviewWidth, maxPreviewHeight,
                        ImageFormat.JPEG, 2);
                //监听ImageReader的事件，当有图像流数据可用时回调
                mImageReader.setOnImageAvailableListener(
                        mOnImageAvailableListener, mBackgroundHandler);

                int orientation = getResources().getConfiguration().orientation;
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    setAspectRatio(mPreviewSize.getWidth(), mPreviewSize.getHeight());
                } else {
                    setAspectRatio(mPreviewSize.getHeight(), mPreviewSize.getWidth());
                }
                Boolean available = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                mFlashSupported = available == null ? false : available;

                mCameraId = cameraId;
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            Log.e(TAG, "设备不支持Camera2");
        }
    }

    /**
     * 获取一个合适的相机预览尺寸
     *
     * @param choices 支持的预览尺寸列表
     * @param width   本机分辨率宽度
     * @param height  本机分辨率高度
     * @return 最佳预览尺寸
     */
    private static Size chooseOptimalSize(Size[] choices, int width, int height) {
        // 挑选分辨率相近的
        for (Size option : choices) {
            int deviationWidth = Math.abs(option.getWidth() - width);
            int deviationHeight = Math.abs(option.getHeight() - height);
            if ((deviationHeight < 300) && (deviationWidth < 300)) {
                return option;
            }
        }
        float reqRatio = (float) width / height;
        Size retSize = null;
        float curRatio, deltaRatio;
        float deltaRatioMin = Float.MAX_VALUE;
        // 没有分辨率相近的则挑选 宽高比相近的
        for (Size size : choices) {
            curRatio = (float) size.getWidth() / size.getHeight();
            deltaRatio = Math.abs(reqRatio - curRatio);
            if (deltaRatio < deltaRatioMin) {
                deltaRatioMin = deltaRatio;
                retSize = size;
            }
        }
        return retSize;
    }

    /**
     * 为相机预览创建新的CameraCaptureSession
     */
    private void createCameraPreviewSession() {
        try {
            SurfaceTexture texture = this.getSurfaceTexture();
            assert texture != null;
            // 将默认缓冲区的大小配置为想要的相机预览的大小
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            Surface surface = new Surface(texture);
            // camera2通过创建请求会话的方式调用
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);
            // 我们创建一个 CameraCaptureSession 来进行相机预览
            mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            // 摄像头配置完成，处理capture请求
                            if (null == mCameraDevice) {
                                return;
                            }
                            // 会话准备好后，我们开始显示预览
                            mCaptureSession = cameraCaptureSession;
                            try {
                                // 设置连续自动聚焦
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                setAutoFlash(mPreviewRequestBuilder);
                                // 显示相机预览
                                mPreviewRequest = mPreviewRequestBuilder.build();
                                // 请求此会话重复不停捕捉图像
                                mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback, mBackgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                        }
                    }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 从指定的屏幕旋转中检索照片方向
     *
     * @param rotation 屏幕方向
     * @return 照片方向（0,90,270,360）
     */
    private int getOrientation(int rotation) {
        return (ORIENTATIONS.get(rotation) + mSensorOrientation + 270) % 360;
    }

    /**
     * 锁定焦点
     */
    private void lockFocus() {
        try {
            // 如何通知相机锁定焦点
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
            // 通知mCaptureCallback等待锁定
            mState = STATE_WAITING_LOCK;
            // 发送对焦请求 拍照
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback, mBackgroundHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 解锁焦点
     */
    private void unlockFocus() {
        // 初始化状态到预览状态
        mState = STATE_PREVIEW;
    }

    /**
     * 拍摄静态图片
     */
    private void captureStillPicture() {
        try {
            if (null == activity || null == mCameraDevice) {
                return;
            }
            // 停止重复获取图像
            mCaptureSession.stopRepeating();
            // 丢弃所有正在进行捕捉的进程
            mCaptureSession.abortCaptures();
            // 创建一个适合静态图片捕捉的请求
            final CaptureRequest.Builder captureBuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            setAutoFlash(captureBuilder);
            // 方向
            int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getOrientation(rotation));
            CameraCaptureSession.CaptureCallback captureCallback
                    = new CameraCaptureSession.CaptureCallback() {
            };

            // 捕获拍照
            mCaptureSession.capture(captureBuilder.build(), captureCallback, null);
            unlockFocus();

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 运行preCapture序列来捕获静止图像
     */
    private void runPreCaptureSequence() {
        try {
            // 设置拍照参数请求,预捕捉
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            // 当前状态设置为等待预捕捉
            mState = STATE_WAITING_PRE_CAPTURE;
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 比较两者大小
     */
    private static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // o1大返回1，o2大返回-1，相等返回0
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }
    }

    /**
     * ImageReader的回调对象
     */
    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener
            = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
            final Image image = reader.acquireNextImage();
            // 获取图片的像素屏幕数组的数据帧
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            // 创建剩余数据帧大小的数组
            final byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            image.close();

            Observable<Integer> observable = Observable.create(new ObservableOnSubscribe<Integer>() {
                @Override
                public void subscribe(ObservableEmitter<Integer> emitter) {
                    FileUtils.saveImage(bytes);
                    emitter.onNext(0);
                }
            });
            Disposable disposable = observable.subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Consumer<Integer>() {
                        @Override
                        public void accept(Integer integer) {
                            Intent intent = new Intent(activity, VerifyScoreActivity.class);
                            intent.putExtra("type", mType);
                            activity.startActivity(intent);
                        }
                    });
            mCompositeDisposable.add(disposable);
        }
    };

}
