package com.example.flutteraar.live.camera;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;

import androidx.annotation.NonNull;

import com.example.flutteraar.live.util.YuvUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Camera2 采集辅助类：
 * - 负责相机生命周期管理；
 * - 输出预览到 TextureView；
 * - 将 YUV_420_888 转换成 I420 并回调给上层推流模块。
 */
public class Camera2Helper {
    private static final String TAG = Camera2Helper.class.getSimpleName();

    public static final String CAMERA_ID_FRONT = "1";
    public static final String CAMERA_ID_BACK = "0";

    private Context context;
    private String mCameraId;
    private String specificCameraId;
    private TextureView mTextureView;
    private final int rotation;
    private final Point previewViewSize;
    private Camera2Listener camera2Listener;
    private CameraCaptureSession mCaptureSession;
    private CameraDevice mCameraDevice;
    private Size mPreviewSize;
    private int rotateDegree = 0;
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    private ImageReader mImageReader;
    private CaptureRequest.Builder mPreviewRequestBuilder;
    private final Semaphore mCameraOpenCloseLock = new Semaphore(1);
    private int mSensorOrientation;

    /**
     * 私有构造，使用 Builder 创建。
     */
    private Camera2Helper(Builder builder) {
        mTextureView = builder.previewDisplayView;
        specificCameraId = builder.specificCameraId;
        camera2Listener = builder.camera2Listener;
        rotation = builder.rotation;
        rotateDegree = builder.rotateDegree;
        previewViewSize = builder.previewViewSize;
        context = builder.context;
    }

    /**
     * 切换前后摄像头并重启预览。
     */
    public void switchCamera() {
        if (CAMERA_ID_BACK.equals(mCameraId)) {
            specificCameraId = CAMERA_ID_FRONT;
        } else if (CAMERA_ID_FRONT.equals(mCameraId)) {
            specificCameraId = CAMERA_ID_BACK;
        }
        stop();
        start();
        Log.i(TAG, "switchCamera, specificCameraId=" + specificCameraId);
    }

    /**
     * 计算 camera sensor 与屏幕方向综合后的预览旋转角。
     */
    private int getCameraOrientation(int rotation, String cameraId) {
        int degree = rotation * 90;
        switch (rotation) {
            case Surface.ROTATION_0:
                degree = 0;
                break;
            case Surface.ROTATION_90:
                degree = 90;
                break;
            case Surface.ROTATION_180:
                degree = 180;
                break;
            case Surface.ROTATION_270:
                degree = 270;
                break;
            default:
                break;
        }
        int result;
        if (CAMERA_ID_FRONT.equals(cameraId)) {
            result = (mSensorOrientation + degree) % 360;
            result = (360 - result) % 360;
        } else {
            result = (mSensorOrientation - degree + 360) % 360;
        }
        Log.i(TAG, "getCameraOrientation, result=" + result);
        return result;
    }

    private final TextureView.SurfaceTextureListener mSurfaceTextureListener =
            new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
                    openCamera();
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

    private final CameraDevice.StateCallback mDeviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            mCameraDevice = cameraDevice;
            createCameraPreviewSession();
            if (camera2Listener != null) {
                camera2Listener.onCameraOpened(mPreviewSize, getCameraOrientation(rotation, mCameraId));
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
            if (camera2Listener != null) {
                camera2Listener.onCameraClosed();
            }
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
            if (camera2Listener != null) {
                camera2Listener.onCameraError(new Exception("error occurred, code is " + error));
            }
        }
    };

    private final CameraCaptureSession.StateCallback mCaptureStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
            if (mCameraDevice == null) {
                return;
            }
            mCaptureSession = cameraCaptureSession;
            try {
                mCaptureSession.setRepeatingRequest(
                        mPreviewRequestBuilder.build(),
                        new CameraCaptureSession.CaptureCallback() {
                        },
                        mBackgroundHandler);
            } catch (CameraAccessException e) {
                Log.e(TAG, "setRepeatingRequest failed", e);
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
            if (camera2Listener != null) {
                camera2Listener.onCameraError(new Exception("configureFailed"));
            }
        }
    };

    private Size getBestSupportedSize(List<Size> sizes) {
        Size defaultSize = sizes.get(0);
        int defaultDelta = Math.abs(defaultSize.getWidth() * defaultSize.getHeight()
                - previewViewSize.x * previewViewSize.y);
        for (Size size : sizes) {
            int currentDelta = Math.abs(size.getWidth() * size.getHeight() - previewViewSize.x * previewViewSize.y);
            if (currentDelta < defaultDelta) {
                defaultDelta = currentDelta;
                defaultSize = size;
            }
        }
        return defaultSize;
    }

    /**
     * 启动 Camera2 采集流程。
     */
    public synchronized void start() {
        if (mCameraDevice != null) {
            return;
        }
        startBackgroundThread();
        if (mTextureView.isAvailable()) {
            openCamera();
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
        Log.i(TAG, "start");
    }

    /**
     * 更新预览旋转角，用于帧回调前的 YUV 旋转处理。
     */
    public void updatePreviewDegree(int degree) {
        rotateDegree = degree;
        Log.i(TAG, "updatePreviewDegree=" + degree);
    }

    /**
     * 停止 Camera2 采集流程。
     */
    public synchronized void stop() {
        if (mCameraDevice == null) {
            return;
        }
        closeCamera();
        stopBackgroundThread();
        Log.i(TAG, "stop");
    }

    /**
     * 彻底释放资源与引用。
     */
    public void release() {
        stop();
        mTextureView = null;
        camera2Listener = null;
        context = null;
        Log.i(TAG, "release");
    }

    /**
     * 遍历并选择可用相机输出参数。
     */
    private void setUpCameraOutput(CameraManager cameraManager) {
        try {
            if (configCameraParams(cameraManager, specificCameraId)) {
                return;
            }
            for (String cameraId : cameraManager.getCameraIdList()) {
                if (configCameraParams(cameraManager, cameraId)) {
                    return;
                }
            }
        } catch (CameraAccessException | NullPointerException e) {
            if (camera2Listener != null) {
                camera2Listener.onCameraError(e);
            }
        }
    }

    /**
     * 按 cameraId 读取分辨率、ImageReader、方向等关键参数。
     */
    private boolean configCameraParams(CameraManager manager, String cameraId) throws CameraAccessException {
        CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
        StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        if (map == null) {
            return false;
        }
        mPreviewSize = getBestSupportedSize(new ArrayList<>(Arrays.asList(map.getOutputSizes(SurfaceTexture.class))));
        mImageReader = ImageReader.newInstance(
                mPreviewSize.getWidth(),
                mPreviewSize.getHeight(),
                ImageFormat.YUV_420_888,
                2);
        mImageReader.setOnImageAvailableListener(new OnImageAvailableListenerImpl(), mBackgroundHandler);
        mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        mCameraId = cameraId;
        return true;
    }

    /**
     * 申请并打开 camera 设备。
     */
    private void openCamera() {
        CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        setUpCameraOutput(cameraManager);
        configureTransform(mTextureView.getWidth(), mTextureView.getHeight());
        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            if (context.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            cameraManager.openCamera(mCameraId, mDeviceStateCallback, mBackgroundHandler);
        } catch (CameraAccessException | InterruptedException e) {
            if (camera2Listener != null) {
                camera2Listener.onCameraError(e);
            }
        }
    }

    /**
     * 关闭 camera session / device / image reader。
     */
    private void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            if (mCaptureSession != null) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if (mCameraDevice != null) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (mImageReader != null) {
                mImageReader.close();
                mImageReader = null;
            }
            if (camera2Listener != null) {
                camera2Listener.onCameraClosed();
            }
        } catch (InterruptedException e) {
            if (camera2Listener != null) {
                camera2Listener.onCameraError(e);
            }
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    /**
     * 创建后台线程用于 Camera2 回调处理。
     */
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * 停止并回收后台线程。
     */
    private void stopBackgroundThread() {
        if (mBackgroundThread == null) {
            return;
        }
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            Log.e(TAG, "stopBackgroundThread failed", e);
        }
    }

    /**
     * 创建预览会话并把输出绑定到 TextureView + ImageReader。
     */
    private void createCameraPreviewSession() {
        try {
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            if (texture == null) {
                return;
            }
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            Surface surface = new Surface(texture);
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.set(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            mPreviewRequestBuilder.addTarget(surface);
            mPreviewRequestBuilder.addTarget(mImageReader.getSurface());
            mCameraDevice.createCaptureSession(
                    Arrays.asList(surface, mImageReader.getSurface()),
                    mCaptureStateCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "createCameraPreviewSession failed", e);
        }
    }

    /**
     * 计算并设置 TextureView 的显示变换矩阵。
     */
    private void configureTransform(int viewWidth, int viewHeight) {
        if (mTextureView == null || mPreviewSize == null) {
            return;
        }
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate((90 * (rotation - 2)) % 360, centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        mTextureView.setTransform(matrix);
    }

    /**
     * Builder：用于创建 Camera2Helper 并注入必要参数。
     */
    public static final class Builder {
        private TextureView previewDisplayView;
        private String specificCameraId;
        private Camera2Listener camera2Listener;
        private Point previewViewSize;
        private int rotation;
        private int rotateDegree;
        private Context context;

        /**
         * 设置预览输出控件。
         */
        public Builder previewOn(TextureView val) {
            previewDisplayView = val;
            return this;
        }

        /**
         * 设置期望的预览尺寸。
         */
        public Builder previewViewSize(Point val) {
            previewViewSize = val;
            return this;
        }

        /**
         * 设置显示旋转角。
         */
        public Builder rotation(int val) {
            rotation = val;
            return this;
        }

        /**
         * 设置预览帧旋转角。
         */
        public Builder rotateDegree(int val) {
            rotateDegree = val;
            return this;
        }

        /**
         * 设置初始摄像头 ID。
         */
        public Builder specificCameraId(String val) {
            specificCameraId = val;
            return this;
        }

        /**
         * 设置采集回调监听器。
         */
        public Builder cameraListener(Camera2Listener val) {
            camera2Listener = val;
            return this;
        }

        /**
         * 设置上下文。
         */
        public Builder context(Context val) {
            context = val;
            return this;
        }

        /**
         * 构建 Camera2Helper。
         */
        public Camera2Helper build() {
            if (previewDisplayView == null) {
                throw new NullPointerException("must preview on a textureView");
            }
            return new Camera2Helper(this);
        }
    }

    private class OnImageAvailableListenerImpl implements ImageReader.OnImageAvailableListener {
        private byte[] temp;
        private byte[] yuvData;
        private byte[] dstData;
        private final ReentrantLock lock = new ReentrantLock();

        /**
         * 相机帧回调：YUV_420_888 -> I420，并按 rotateDegree 旋转后输出。
         */
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = reader.acquireNextImage();
            if (image == null) {
                return;
            }
            if (camera2Listener != null && image.getFormat() == ImageFormat.YUV_420_888) {
                Image.Plane[] planes = image.getPlanes();
                lock.lock();
                try {
                    int offset = 0;
                    int width = image.getWidth();
                    int height = image.getHeight();
                    int len = width * height;
                    if (yuvData == null) {
                        yuvData = new byte[len * 3 / 2];
                    }
                    planes[0].getBuffer().get(yuvData, offset, len);
                    offset += len;
                    for (int i = 1; i < planes.length; i++) {
                        int srcIndex = 0;
                        int dstIndex = 0;
                        int rowStride = planes[i].getRowStride();
                        int pixelsStride = planes[i].getPixelStride();
                        ByteBuffer buffer = planes[i].getBuffer();
                        if (temp == null || temp.length != buffer.capacity()) {
                            temp = new byte[buffer.capacity()];
                        }
                        buffer.get(temp);
                        for (int j = 0; j < height / 2; j++) {
                            for (int k = 0; k < width / 2; k++) {
                                yuvData[offset + dstIndex++] = temp[srcIndex];
                                srcIndex += pixelsStride;
                            }
                            if (pixelsStride == 2) {
                                srcIndex += rowStride - width;
                            } else if (pixelsStride == 1) {
                                srcIndex += rowStride - width / 2;
                            }
                        }
                        offset += len / 4;
                    }

                    if (rotateDegree == 90 || rotateDegree == 180) {
                        if (dstData == null) {
                            dstData = new byte[len * 3 / 2];
                        }
                        if (rotateDegree == 90) {
                            YuvUtil.YUV420pRotate90(dstData, yuvData, width, height);
                        } else {
                            YuvUtil.YUV420pRotate180(dstData, yuvData, width, height);
                        }
                        camera2Listener.onPreviewFrame(dstData);
                    } else {
                        camera2Listener.onPreviewFrame(yuvData);
                    }
                } finally {
                    lock.unlock();
                }
            }
            image.close();
        }
    }
}
