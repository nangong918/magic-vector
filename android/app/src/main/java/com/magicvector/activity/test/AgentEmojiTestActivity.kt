package com.magicvector.activity.test

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import com.core.baseutil.fragmentActivity.BaseAppCompatActivity
import com.core.baseutil.permissions.GainPermissionCallback
import com.core.baseutil.permissions.PermissionUtil
import com.data.domain.constant.BaseConstant
import com.detection.yolov8.BoundingBox
import com.detection.yolov8.Detector
import com.detection.yolov8.YOLOv8Constants
import com.detection.yolov8.targetPoint.TargetPoint
import com.detection.yolov8.targetPoint.YOLOv8TargetPointGenerator
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.magicvector.databinding.ActivityAgentEmojiTestBinding
import com.magicvector.manager.yolo.EyesMoveManager
import com.magicvector.manager.yolo.OnResetCallback
import com.magicvector.manager.yolo.TargetActivityDetectionManager
import okio.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class AgentEmojiTestActivity : BaseAppCompatActivity<ActivityAgentEmojiTestBinding>(
    AgentEmojiTestActivity::class
) , Detector.DetectorListener{

    companion object {
        val GSON: Gson = GsonBuilder().setPrettyPrinting().create()
    }
    
    override fun initBinding(): ActivityAgentEmojiTestBinding {
        return ActivityAgentEmojiTestBinding.inflate(layoutInflater)
    }

    private var isFrontCamera = false

    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var detector: Detector? = null

    private var isShowVideo = true

    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cameraExecutor = Executors.newSingleThreadExecutor()
        cameraExecutor.execute {
            detector = Detector(baseContext, YOLOv8Constants.MODEL_PATH, YOLOv8Constants.LABELS_PATH, this) {
                toast(it)
            }
        }

        startCamera()

    }

    private var count = 0

    override fun setListener() {
        super.setListener()

        // 照相测试
        binding.btnTakePhoto.setOnClickListener {
            takePhoto()
        }

        binding.btnSwitchCamera.setOnClickListener {
            switchCamera()
        }

        binding.btnShowVideo.setOnClickListener {
            isShowVideo = !isShowVideo
            if (isShowVideo) {
                binding.lyVideo.visibility = View.VISIBLE
                binding.viewFinder.visibility = View.VISIBLE
                binding.overlay.visibility = View.VISIBLE

                binding.btnShowVideo.text = "隐藏识别"
            }
            else {
                binding.lyVideo.visibility = View.GONE
                binding.viewFinder.visibility = View.GONE
                binding.overlay.visibility = View.GONE

                binding.btnShowVideo.text = "显示识别"
            }
        }

        binding.btnMoveTest.setOnClickListener {
            val screenWidth = resources.displayMetrics.widthPixels
            val screenHeight = resources.displayMetrics.heightPixels
            when (count % 5){
                0 -> {
                    val targetPoint = TargetPoint(0.5f, 0.5f, null, null)
                    EyesMoveManager.moveLayoutToTargetPoint(targetPoint, screenWidth, screenHeight, binding.lyEmoji, false, null)
                }
                1 -> {
                    val targetPoint = TargetPoint(0f, 0f, null, null)
                    EyesMoveManager.moveLayoutToTargetPoint(targetPoint, screenWidth, screenHeight, binding.lyEmoji, false, null)
                }
                2 -> {
                    val targetPoint = TargetPoint(0f, 1f, null, null)
                    EyesMoveManager.moveLayoutToTargetPoint(targetPoint, screenWidth, screenHeight, binding.lyEmoji, false, null)
                }
                3 -> {
                    val targetPoint = TargetPoint(1f, 0f, null, null)
                    EyesMoveManager.moveLayoutToTargetPoint(targetPoint, screenWidth, screenHeight, binding.lyEmoji, false, null)
                }
                4 -> {
                    val targetPoint = TargetPoint(1f, 1f, null, null)
                    EyesMoveManager.moveLayoutToTargetPoint(targetPoint, screenWidth, screenHeight, binding.lyEmoji, false, null)
                }
            }
            println("count % 5:: ${count % 5}")
            count++
        }
    }

    override fun initWindow() {
        // 隐藏标题导航栏
        supportActionBar?.hide()

        // 隐藏状态栏
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        // 保持屏幕常亮
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // 隐藏导航栏
        val decorView = window.decorView
        decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
    }

    private fun toast(message: String) {
        runOnUiThread {
            Toast.makeText(baseContext, message, Toast.LENGTH_LONG).show()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider  = cameraProviderFuture.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(this))
    }

    private val isInitializingCamera: AtomicBoolean = AtomicBoolean(false)
    private fun bindCameraUseCases() {

        if (isInitializingCamera.getAndSet(true)) {
            toast("相机正在初始化")
            return
        }

        try {
            val cameraProvider = cameraProvider ?: throw IllegalStateException("Camera initialization failed.")

            val rotation = binding.viewFinder.display.rotation

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
                .setTargetRotation(binding.viewFinder.display.rotation)
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

                // 缓存一帧
                currentFrameBitmap = rotatedBitmap

                // 识别
                detector?.detect(rotatedBitmap)
            }

            cameraProvider.unbindAll()

            try {
                camera = cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageAnalyzer
                )

                preview?.surfaceProvider = binding.viewFinder.surfaceProvider
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
    fun switchCamera() {
        isFrontCamera = !isFrontCamera
        bindCameraUseCases()
    }
    

    override fun onEmptyDetect() {
        runOnUiThread {
            binding.overlay.clear()
        }
    }

    override fun onDetect(
        boundingBoxes: List<BoundingBox>,
        inferenceTime: Long
    ) {
        runOnUiThread {
            val inferenceTimeStr = "${inferenceTime}ms"
            binding.inferenceTime.text = inferenceTimeStr
            binding.overlay.apply {
                setResults(boundingBoxes)
                invalidate()
            }
        }

//        val maxTargetPoint = YOLOv8TargetPointGenerator.generateMaxTargetPoint(boundingBoxes)
        val maxTarget = YOLOv8TargetPointGenerator.generateTargetPoint(
            boundingBoxes,
            BaseConstant.YOLO.FILTER_SIZE
        )

        // 活动检测
        val result = TargetActivityDetectionManager.detect(
            boundingBoxes,
            targetPoint = maxTarget
        )

        result.let {
            if (it.result){
                if (it.detectionType != null) {
                    when (it.detectionType) {
                        0 -> {
                            binding.vMoveDetect.setBackgroundResource(com.view.appview.R.color.gold)
                        }
                        1 -> {
                            binding.vMoveDetect.setBackgroundResource(com.view.appview.R.color.red)
                        }
                        else -> {
                            binding.vMoveDetect.setBackgroundResource(com.view.appview.R.color.light_blue_600)
                        }
                    }
                }
                else {
                    binding.vMoveDetect.setBackgroundResource(com.view.appview.R.color.light_blue_600)
                }
            }
            else {
                binding.vMoveDetect.setBackgroundResource(com.view.appview.R.color.light_blue_600)
            }
        }

        // 获取屏幕宽高
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels

        // 移动布局
        EyesMoveManager.moveLayoutToTargetPoint(
            targetPoint = maxTarget,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            layout = binding.lyEmoji,
            isThisReset = false,
            onResetCallback = onResetCallback
        )
    }


    // 复位回调
    val onResetCallback = object : OnResetCallback {
        override fun onStartReset() {
            Log.i(TAG, "开始复位")
            binding.vMoveDetect.setBackgroundResource(com.view.appview.R.color.a1_100)
        }

        override fun onFinishReset() {
            Log.i(TAG, "复位完成")
            binding.vMoveDetect.setBackgroundResource(com.view.appview.R.color.light_blue_600)
        }
    }

    // 添加一个变量来保存当前帧的Bitmap
    private var currentFrameBitmap: Bitmap? = null
    private val bitmapLock = Any()
    fun takePhoto(){
        PermissionUtil.requestPermissionSelectX(
            this,
            arrayOf(Manifest.permission.CAMERA),
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            object : GainPermissionCallback {
                override fun allGranted() {
                    // 保存当前帧用于拍照
                    savePhoto()
                }

                override fun notGranted(notGrantedPermissions: Array<String?>?) {
                    println("$TAG 没有权限: ${notGrantedPermissions?.toList()}")
                }

                override fun always() {
                }
            }
        )
    }

    // 实现拍照方法
    private fun savePhoto() {
        synchronized(bitmapLock) {
            currentFrameBitmap?.let { bitmap ->
                try {
                    // 这里可以保存图片到相册或进行其他处理
                    saveBitmapToGallery(bitmap)
                    toast("照片已保存")
                } catch (e: Exception) {
                    Log.e(TAG, "保存照片失败", e)
                    toast("保存照片失败")
                }
            } ?: run {
                toast("当前没有可用的图像帧")
            }
        }
    }

    private fun saveBitmapToGallery(bitmap: Bitmap) {
        val fileName = "AgentEmoji_${System.currentTimeMillis()}.jpg"
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            }
        }

        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let { imageUri ->
            contentResolver.openOutputStream(imageUri)?.use { outputStream ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)) {
                    throw IOException("图片压缩失败")
                }
            }

            // 通知相册更新
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                intent.data = imageUri
                sendBroadcast(intent)
            }
        } ?: throw IOException("创建文件失败")
    }


    override fun onDestroy() {
        super.onDestroy()
        detector?.close()
        cameraExecutor.shutdown()

        // 清除标志位
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    override fun onResume() {
        super.onResume()
        startCamera()
    }
}