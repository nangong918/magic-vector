package com.magicvector.activity

import android.os.Bundle
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import com.data.domain.constant.BaseConstant
import com.detection.yolov8.BoundingBox
import com.detection.yolov8.Detector
import com.detection.yolov8.targetPoint.YOLOv8TargetPointGenerator
import com.magicvector.MainApplication
import com.magicvector.databinding.ActivityAgentEmojiBinding
import com.magicvector.manager.yolo.EyesMoveManager
import com.magicvector.manager.yolo.OnResetCallback
import com.magicvector.manager.yolo.TargetActivityDetectionManager
import com.magicvector.utils.BaseAppCompatVmActivity
import com.magicvector.viewModel.activity.AgentEmojiVm

class AgentEmojiActivity : BaseAppCompatVmActivity<ActivityAgentEmojiBinding, AgentEmojiVm>(
    AgentEmojiActivity::class,
    AgentEmojiVm::class
) {

    companion object {
        val GSON = MainApplication.GSON
        val visionManager = MainApplication.getVisionManager()
    }

    override fun initBinding(): ActivityAgentEmojiBinding {
        return ActivityAgentEmojiBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun initViewModel() {
        super.initViewModel()

        val agentId = intent.getStringExtra("agentId")
        val agentName = intent.getStringExtra("agentName")

        visionManager.initStart(
            context = this,
            previewView = binding.viewFinder,
            listener = getDetectListener(),
            lifecycleOwner = this as LifecycleOwner
        )
    }

    override fun initView() {
        super.initView()
    }

    override fun setListener() {
        super.setListener()
    }

    private fun getDetectListener(): Detector.DetectorListener {
        return object : Detector.DetectorListener {
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
                        runOnUiThread {
                            val gold = com.view.appview.R.color.gold
                            val red = com.view.appview.R.color.red
                            val light_blue_600 = com.view.appview.R.color.light_blue_600
                            var displayColor = light_blue_600
                            if (it.result) {
                                if (it.detectionType != null) {
                                    when (it.detectionType) {
                                        0 -> {
                                            displayColor = gold
                                        }
                                        1 -> {
                                            displayColor = red
                                        }
                                        else -> {
                                            displayColor = light_blue_600
                                        }
                                    }
                                }
                            }

                            binding.vMoveDetect.setBackgroundResource(displayColor)
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
            }
        }
    }

    // 复位回调
    val onResetCallback = object : OnResetCallback {
        override fun onStartReset() {
            Log.i(TAG, "开始复位")
            runOnUiThread {
                binding.vMoveDetect.setBackgroundResource(com.view.appview.R.color.a1_100)
            }
        }

        override fun onFinishReset() {
            Log.i(TAG, "复位完成")
            runOnUiThread {
                binding.vMoveDetect.setBackgroundResource(com.view.appview.R.color.light_blue_600)
            }
        }
    }

}