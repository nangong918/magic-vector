package com.magicvector.activity.test

import android.Manifest
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import android.view.View
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
import com.detection.yolov8.BoundingBox
import com.detection.yolov8.Detector
import com.detection.yolov8.YOLOv8Constants
import com.detection.yolov8.targetPoint.TargetPoint
import com.detection.yolov8.targetPoint.YOLOv8TargetPointGenerator
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.magicvector.databinding.ActivityAgentEmojiTestBinding
import com.magicvector.manager.yolo.EyesMoveManager
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class AgentEmojiTestActivity : BaseAppCompatActivity<ActivityAgentEmojiTestBinding>(
    AgentEmojiTestActivity::class
) , Detector.DetectorListener{

    companion object {
        val GSON: Gson = GsonBuilder().setPrettyPrinting().create()
    }
    
    override fun initBinding(): ActivityAgentEmojiTestBinding {
        return ActivityAgentEmojiTestBinding.inflate(layoutInflater)
    }

    private val isFrontCamera = false

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

    override fun setListener() {
        super.setListener()

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
    }

    private fun moveLayoutToTargetPoint(targetPoint: TargetPoint) {
        // 获取屏幕宽高
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels

        // 获取布局的宽高
        binding.lyEmoji.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val layoutWidth = binding.lyEmoji.measuredWidth
        val layoutHeight = binding.lyEmoji.measuredHeight

        // 计算左上角坐标
        var left = (targetPoint.x * screenWidth) - (layoutWidth / 2)
        var top = (targetPoint.y * screenHeight) - (layoutHeight / 2)

        /*
          边界处理：
          left < 0 -> left = 0; left + layoutWidth > screenWidth -> left = screenWidth - layoutWidth
          top < 0 -> top = 0; top + layoutHeight > screenHeight -> top = screenHeight - layoutHeight
         */
        if (left < 0) {
            left = 0f
        } else if (left + layoutWidth > screenWidth) {
            left = (screenWidth - layoutWidth).toFloat()
        }

        if (top < 0) {
            top = 0f
        } else if (top + layoutHeight > screenHeight) {
            top = (screenHeight - layoutHeight).toFloat()
        }

        // 设置布局的位置
        binding.lyEmoji.x = left
        binding.lyEmoji.y = top
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

    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider ?: throw IllegalStateException("Camera initialization failed.")

        val rotation = binding.viewFinder.display.rotation

        val cameraSelector = CameraSelector
            .Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

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
        val maxTargetPoint = YOLOv8TargetPointGenerator.generateMaxTargetPoint(boundingBoxes)
        Log.d(TAG, "生成最大目标中心点: ${GSON.toJson(maxTargetPoint)}")

        // 获取屏幕宽高
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels
        // 移动布局
        EyesMoveManager.moveLayoutToTargetPoint(
            targetPoint = maxTargetPoint,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            layout = binding.lyEmoji,
            isThisReset = false
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        detector?.close()
        cameraExecutor.shutdown()
    }

    override fun onResume() {
        super.onResume()
        startCamera()
    }
}