package com.magicvector.manager.yolo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import android.view.Window
import android.view.WindowManager
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.lifecycle.LifecycleOwner
import com.core.baseutil.ui.ToastUtils
import com.detection.yolov8.Detector
import com.detection.yolov8.YOLOv8Constants
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class VisionManager {

    companion object {
        val TAG = VisionManager::class.simpleName
    }

    private var isFrontCamera = false

    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var detector: Detector? = null

    // 是否初始化了相机
    private val isInitializingCamera: AtomicBoolean = AtomicBoolean(false)

    // 当前帧管理
    private var visionCallback: VisionCallback? = null
    fun setVisionCallback(visionCallback: VisionCallback) {
        this.visionCallback = visionCallback
    }

    private lateinit var cameraExecutor: ExecutorService

    fun initStart(context: Context, previewView: PreviewView, listener: Detector.DetectorListener, lifecycleOwner: LifecycleOwner){
        cameraExecutor = Executors.newSingleThreadExecutor()
        cameraExecutor.execute {
            detector = Detector(
                context = context,
                modelPath = YOLOv8Constants.MODEL_PATH,
                labelPath = YOLOv8Constants.LABELS_PATH,
                detectorListener = listener) {
                ToastUtils.showToastActivity(
                    context = context,
                    message = it
                )
            }
        }

        startCamera(context, previewView, lifecycleOwner)
    }

    fun startCamera(context: Context, previewView: PreviewView, lifecycleOwner: LifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            cameraProvider  = cameraProviderFuture.get()
            bindCameraUseCases(previewView, lifecycleOwner)
        }, ContextCompat.getMainExecutor(context))
    }

    private fun bindCameraUseCases(previewView: PreviewView, lifecycleOwner: LifecycleOwner) {

        if (isInitializingCamera.getAndSet(true)) {
            ("相机正在初始化")
            return
        }

        try {
            val cameraProvider = cameraProvider ?: throw IllegalStateException("Camera initialization failed.")

            val rotation = previewView.display.rotation

            // 根据 isFrontCamera 选择摄像头
            val cameraSelector = if (isFrontCamera) {
                CameraSelector
                    .Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                    .build()
            } else {
                CameraSelector
                    .Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build()
            }

            preview =  Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(rotation)
                .build()

            imageAnalyzer = ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetRotation(previewView.display.rotation)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()

            imageAnalyzer?.setAnalyzer(cameraExecutor) { imageProxy ->
                // 获取bitmap
                val bitmapBuffer =
                    createBitmap(imageProxy.width, imageProxy.height)

                imageProxy.use { bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer) }
                imageProxy.close()

                // 构建变换矩阵
                val matrix = Matrix().apply {
                    postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())

                    // 如果是前置摄像头，需要水平翻转
                    if (isFrontCamera) {
                        postScale(
                            -1f,
                            1f,
                            imageProxy.width.toFloat(),
                            imageProxy.height.toFloat()
                        )
                    }
                }

                // 生成旋转后的位图
                val rotatedBitmap = Bitmap.createBitmap(
                    bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height,
                    matrix, true
                )

                // 为外部提供当前的vision
                visionCallback?.onReceiveCurrentFrameBitmap(rotatedBitmap)

                // 识别
                detector?.detect(rotatedBitmap)
            }

            cameraProvider.unbindAll()

            try {
                camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner = lifecycleOwner,
                    cameraSelector = cameraSelector,
                    preview,
                    imageAnalyzer
                )

                preview?.surfaceProvider = previewView.surfaceProvider
            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Camera initialization failed", e)
        } finally {
            isInitializingCamera.set(false)
        }
    }

    // 切换摄像头的方法
    fun switchCamera(previewView: PreviewView, lifecycleOwner: LifecycleOwner) {
        isFrontCamera = !isFrontCamera
        bindCameraUseCases(previewView, lifecycleOwner)
    }

    fun onResume(window: Window){
        // 保持屏幕常亮
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    // 由于surface可能在onStop销毁，所以分析器要在onPause中提前结束
    private val cameraLock = Any()
    fun onPause(){
        // 线程同步，避免在Surface销毁的时候还从Buffer中获取数据
        synchronized(cameraLock){
            // 停止线程池行为
            cameraExecutor.shutdownNow()
            // 停止分析器
            imageAnalyzer?.clearAnalyzer()
            // 停止相机
            cameraProvider?.unbindAll()
        }
    }

    fun onDestroy(window: Window){
        // 清除标志位
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        detector?.close()
        cameraExecutor.shutdownNow()
    }
}