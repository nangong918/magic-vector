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

    /**
     * CameraDevice 状态回调
     * 作用：监听相机硬件的 打开成功 / 断开连接 / 打开失败 三种核心状态
     */
    private final CameraDevice.StateCallback mDeviceStateCallback = new CameraDevice.StateCallback() {

        /**
         * 相机硬件【打开成功】
         * 系统已成功获取相机硬件权限，准备就绪
         */
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            // 释放打开锁，允许后续操作
            mCameraOpenCloseLock.release();
            // 保存相机设备实例（后续创建会话、预览都靠它）
            mCameraDevice = cameraDevice;
            // 【关键】相机已打开，立即创建预览会话，启动画面预览
            createCameraPreviewSession();
            // 回调上层：相机已成功打开，通知外部业务层
            if (camera2Listener != null) {
                camera2Listener.onCameraOpened(mPreviewSize, getCameraOrientation(rotation, mCameraId));
            }
        }

        /**
         * 相机【断开连接】
         * 正常关闭、被其他App抢占、系统回收时触发
         */
        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            // 释放锁
            mCameraOpenCloseLock.release();
            // 关闭相机设备，释放硬件资源
            cameraDevice.close();
            // 置空实例，避免内存泄漏
            mCameraDevice = null;
            // 回调上层：相机已关闭
            if (camera2Listener != null) {
                camera2Listener.onCameraClosed();
            }
        }

        /**
         * 相机【打开失败/发生错误】
         * 权限不足、硬件异常、被占用、不支持等情况触发
         */
        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            // 释放锁
            mCameraOpenCloseLock.release();
            // 关闭并释放相机
            cameraDevice.close();
            mCameraDevice = null;
            // 回调上层：相机发生错误，抛出异常信息
            if (camera2Listener != null) {
                camera2Listener.onCameraError(new Exception("error occurred, code is " + error));
            }
        }
    };

    /**
     * 相机捕获会话状态回调
     * 作用：监听相机预览会话是否创建成功 / 失败
     * 成功后：开始循环发送预览请求，启动实时预览
     */
    private final CameraCaptureSession.StateCallback mCaptureStateCallback = new CameraCaptureSession.StateCallback() {

        /**
         * 会话配置成功（相机已经准备好，可以开始预览）
         */
        @Override
        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
            // 相机设备已为空，直接返回
            if (mCameraDevice == null) {
                return;
            }

            // 保存当前会话实例
            mCaptureSession = cameraCaptureSession;

            try {
                /**
                 * 开始【循环发送预览请求】
                 * 作用：让相机不断输出画面 → 形成连续预览
                 * 这是 Camera2 预览真正“启动”的地方
                 */
                mCaptureSession.setRepeatingRequest(
                        mPreviewRequestBuilder.build(),  // 构建预览请求（包含显示Surface+数据Surface）
                        new CameraCaptureSession.CaptureCallback() {}, // 帧捕获回调（这里不需要处理）
                        mBackgroundHandler               // 在后台线程执行，不卡UI
                );
            } catch (CameraAccessException e) {
                Log.e(TAG, "setRepeatingRequest failed", e);
            }
        }

        /**
         * 会话配置失败（相机不支持、参数错误）
         */
        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
            // 回调上层：相机打开失败
            if (camera2Listener != null) {
                camera2Listener.onCameraError(new Exception("相机会话配置失败"));
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
        // 创建 ImageReader 实例：相机原始数据获取器（推流/编码专用）
        mImageReader = ImageReader.newInstance(
                mPreviewSize.getWidth(),    // 参数1：图像宽度（和预览分辨率一致）
                mPreviewSize.getHeight(),   // 参数2：图像高度（和预览分辨率一致）
                ImageFormat.YUV_420_888,    // 参数3：图像数据格式（安卓标准YUV）
                2                           // 参数4：缓冲区最大帧数（2~3帧最稳定）
        );
        // 创建 ImageReader 帧可用监听器
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
            // 1. 从 TextureView 获取 SurfaceTexture（GPU纹理载体）
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            if (texture == null) {
                return;
            }

            // 2. 设置纹理缓冲区大小 = 相机预览分辨率（必须匹配，否则画面变形）
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

            // 3. 通过 SurfaceTexture 创建 Surface（相机输出的渲染目标）
            Surface surface = new Surface(texture);

            // 4. 创建【预览请求构造器】，类型为预览模式 TEMPLATE_PREVIEW
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

            // 5. 设置自动对焦模式：连续图片对焦（相机预览最常用）
            mPreviewRequestBuilder.set(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

            // 6. 添加第一个输出目标：Surface → 渲染到 TextureView 给人看
            mPreviewRequestBuilder.addTarget(surface);

            // 7. 添加第二个输出目标：ImageReader → 获取YUV原始数据给推流/编码用
            mPreviewRequestBuilder.addTarget(mImageReader.getSurface());

            // 8. 创建相机捕获会话（Camera2 真正开始预览的关键）
            // 传入两个输出目标：预览显示 + 数据采集
            // mCaptureStateCallback：会话状态回调
            // mBackgroundHandler：在后台线程执行，不卡UI
            mCameraDevice.createCaptureSession(
                    Arrays.asList(surface, mImageReader.getSurface()),
                    mCaptureStateCallback,
                    mBackgroundHandler);

        } catch (CameraAccessException e) {
            // 相机权限/硬件异常捕获
            Log.e(TAG, "createCameraPreviewSession failed", e);
        }
    }

    /**
     * 计算并设置 TextureView 的显示变换矩阵。
     *
     * Matrix 矩阵变换，完全不吃 CPU！几乎零消耗；它不是在 CPU 上处理 YUV 数据，而是交给 GPU 做的硬件渲染变换！
     * 相机预览天生有 3 个问题
     * 相机输出图像是横的（宽 > 高）
     * 手机 / 开发板屏幕是竖的（高 > 宽）
     * 相机图像方向和屏幕方向不一致（旋转 90/270 度）
     * 对相机预览画面做 旋转、缩放、居中、全屏填充，解决相机预览方向错误、拉伸变形、画面偏移问题。
     */
    /**
     * 配置 TextureView 矩阵变换
     * 作用：将相机横向输出的图像，通过矩阵校正为屏幕正确方向 + 全屏居中 + 不变形（GPU 硬件渲染）
     * 会自动处理：旋转、缩放、居中、裁剪（竖屏时裁左右，横屏几乎不裁）
     */
    private void configureTransform(int viewWidth, int viewHeight) {
        // 判空：预览控件或相机尺寸未初始化，直接返回
        if (mTextureView == null || mPreviewSize == null) {
            return;
        }

        // 创建一个空矩阵，用于存储 平移、缩放、旋转 变换指令
        Matrix matrix = new Matrix();

        // 屏幕显示区域：TextureView 自身的坐标范围 (0,0) 到 (宽,高)
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);

        // 相机图像区域：宽高互换！因为相机硬件输出是横向，屏幕是竖向
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());

        // 计算屏幕中心点坐标（所有变换都围绕中心点执行，保证居中）
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();

        // ==============================================
        // 情况1：屏幕旋转 90° / 270°（横屏状态）
        // 需要做：居中 + 填充缩放 + 旋转校正，把相机画面转正
        // ==============================================
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            // 1. 平移：将相机图像区域 居中到屏幕中心
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());

            // 2. 矩形映射：将屏幕矩形 映射到 相机矩形，FILL 模式 = 填满屏幕、不变形
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);

            // 3. 计算等比缩放比例：取最大比例，保证全屏无黑边
            // 竖屏时会裁掉左右边缘，横屏时几乎不裁剪
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());

            // 4. 执行缩放：围绕中心点等比放大
            matrix.postScale(scale, scale, centerX, centerY);

            // 5. 执行旋转：将相机横向画面旋转转正，适配屏幕方向
            matrix.postRotate((90 * (rotation - 2)) % 360, centerX, centerY);
        }

        // ==============================================
        // 情况2：屏幕旋转 180°（倒置）
        // 只需要旋转180°，不需要缩放
        // ==============================================
        else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }

        // 将矩阵设置给 TextureView
        // 关键：变换由 GPU 硬件执行，不处理 YUV 数据，几乎不占 CPU
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

    /**
     * 相机帧数据回调
     * 作用：接收 ImageReader 输出的原始帧 → 格式转换 → 旋转 → 送给推流
     */
    private class OnImageAvailableListenerImpl implements ImageReader.OnImageAvailableListener {
        private byte[] temp;          // 临时缓冲数组
        private byte[] yuvData;      // 存放转换后的 I420 数据
        private byte[] dstData;      // 存放旋转后的最终数据
        private final ReentrantLock lock = new ReentrantLock(); // 线程锁（防止多帧同时处理错乱）

        /**
         * 相机帧回调：YUV_420_888 -> I420，并按 rotateDegree 转换成推流/编码能用的 I420(YUV420P) 数据 旋转后输出。
         */
        @Override
        public void onImageAvailable(ImageReader reader) {
            // 获取一帧图像
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
                    // Y 通道：宽度×高度 个字节
                    planes[0].getBuffer().get(yuvData, offset, len);
                    offset += len;
                    for (int i = 1; i < planes.length; i++) {
                        int srcIndex = 0;
                        int dstIndex = 0;
                        // 读取 U/V 数据（最复杂，必须处理行间隔、像素间隔）
                        int rowStride = planes[i].getRowStride();      // 一行有多少字节
                        int pixelsStride = planes[i].getPixelStride(); // 两个色度点之间的间隔
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
