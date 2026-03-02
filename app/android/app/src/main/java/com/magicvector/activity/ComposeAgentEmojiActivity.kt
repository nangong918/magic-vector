package com.magicvector.activity

import android.Manifest
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.core.baseutil.permissions.GainPermissionCallback
import com.core.baseutil.ui.ToastUtils
import com.data.domain.constant.BaseConstant
import com.data.domain.constant.VadChatState
import com.detection.yolov8.BoundingBox
import com.detection.yolov8.Detector
import com.detection.yolov8.OverlayView
import com.detection.yolov8.targetPoint.YOLOv8TargetPointGenerator
import com.magicvector.MainApplication
import com.magicvector.manager.mcp.HandleSystemResponse
import com.magicvector.manager.yolo.TargetActivityDetectionManager
import com.magicvector.manager.yolo.VisionCallback
import com.magicvector.ui.theme.MagicVectorTheme
import com.magicvector.utils.permissions.ComposePermissionUtils
import com.magicvector.viewModel.activity.AgentEmojiEffect
import com.magicvector.viewModel.activity.AgentEmojiIntent
import com.magicvector.viewModel.activity.AgentEmojiState
import com.magicvector.viewModel.activity.ComposeAgentEmojiVm
import com.magicvector.viewModel.activity.DetectionColorType
import kotlinx.coroutines.launch

class ComposeAgentEmojiActivity : ComponentActivity(), HandleSystemResponse, VisionCallback {

    private val vm: ComposeAgentEmojiVm by viewModels()
    private var previewViewRef: PreviewView? = null
    private var overlayViewRef: OverlayView? = null
    private val visionManager = MainApplication.getVisionManager()
    private val recordPermissionUtils = ComposePermissionUtils()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        recordPermissionUtils.registerPermissionLauncher(
            activity = this,
            mustPermissions = arrayOf(Manifest.permission.RECORD_AUDIO)
        )
        initWindow()

        visionManager.setVisionCallback(this)
        vm.processIntent(
            AgentEmojiIntent.Initialize(
                agentId = intent.getStringExtra("agentId"),
                agentName = intent.getStringExtra("agentName")
            )
        )
        vm.processIntent(
            AgentEmojiIntent.OnServiceBound(
                Runnable {
                    vm.processIntent(AgentEmojiIntent.StartVision)
                    initUdpVision()
                }
            )
        )

        observeEffects()

        setContent {
            MagicVectorTheme {
                AgentEmojiRoute(
                    vm = vm,
                    onPreviewReady = { preview, overlay ->
                        previewViewRef = preview
                        overlayViewRef = overlay
                    }
                )
            }
        }
    }

    private fun initUdpVision() {
        val state = vm.uiState.value
        if (state.agentId.isNotEmpty()) {
            vm.realtimeChatController?.getInitUdpVisionManager()?.initialize(
                userId = MainApplication.getUserId(),
                agentId = state.agentId
            )
        }
    }

    private fun observeEffects() {
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                vm.effect.collect { effect ->
                    when (effect) {
                        AgentEmojiEffect.RequestRecordPermission -> {
                            recordPermissionUtils.requestPermissions(this@ComposeAgentEmojiActivity, object : GainPermissionCallback {
                                override fun allGranted() {
                                    vm.processIntent(AgentEmojiIntent.OnRecordPermissionGranted(this@ComposeAgentEmojiActivity))
                                    bindRealtimeCallbacksOnce()
                                }

                                override fun notGranted(notGrantedPermissions: Array<String?>?) {
                                    vm.processIntent(AgentEmojiIntent.OnRecordPermissionDenied)
                                }

                                override fun always() {
                                }
                            })
                        }
                        AgentEmojiEffect.SwitchCamera -> {
                            val preview = previewViewRef ?: return@collect
                            visionManager.switchCamera(preview, this@ComposeAgentEmojiActivity)
                        }
                        AgentEmojiEffect.StartVision -> {
                            val preview = previewViewRef ?: return@collect
                            visionManager.initStart(
                                context = this@ComposeAgentEmojiActivity,
                                previewView = preview,
                                listener = getDetectListener(),
                                lifecycleOwner = this@ComposeAgentEmojiActivity
                            )
                        }
                        AgentEmojiEffect.ShowLoading -> {
                        }
                        AgentEmojiEffect.HideLoading -> {
                        }
                        is AgentEmojiEffect.ShowToast -> {
                            ToastUtils.showToastActivity(this@ComposeAgentEmojiActivity, effect.message)
                        }
                        is AgentEmojiEffect.ShowToastRes -> {
                            ToastUtils.showToastActivity(this@ComposeAgentEmojiActivity, getString(effect.messageRes))
                        }
                    }
                }
            }
        }
    }

    private fun bindRealtimeCallbacksOnce() {
        val current = vm.uiState.value
        if (current.emojiCallbacksBound) return
        val controller = vm.realtimeChatController ?: return
        controller.setCurrentVADStateChange(object : com.magicvector.callback.OnVadChatStateChange {
            override fun onChange(state: VadChatState) {
                vm.processIntent(AgentEmojiIntent.OnVadStateChanged(state))
            }
        })
        controller.setHandleSystemResponse(this@ComposeAgentEmojiActivity)
        vm.processIntent(AgentEmojiIntent.SetEmojiCallbacksBound(true))
    }

    override fun onResume() {
        super.onResume()
        visionManager.onResume(window)
        vm.processIntent(AgentEmojiIntent.OnResume)
    }

    override fun onPause() {
        super.onPause()
        visionManager.onPause()
        vm.processIntent(AgentEmojiIntent.OnPause)
    }

    override fun onDestroy() {
        super.onDestroy()
        visionManager.onDestroy(window)
        vm.processIntent(AgentEmojiIntent.OnDestroy)
    }

    private fun getDetectListener(): Detector.DetectorListener {
        return object : Detector.DetectorListener {
            override fun onEmptyDetect() {
                overlayViewRef?.clear()
            }

            override fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
                overlayViewRef?.setResults(boundingBoxes)
                vm.processIntent(AgentEmojiIntent.OnInferenceChanged(inferenceTime))

                val maxTarget = YOLOv8TargetPointGenerator.generateTargetPoint(
                    boundingBoxes,
                    BaseConstant.YOLO.FILTER_SIZE
                )
                vm.processIntent(
                    AgentEmojiIntent.OnTargetChanged(
                        xFraction = maxTarget.x,
                        yFraction = maxTarget.y
                    )
                )

                val result = TargetActivityDetectionManager.detect(
                    boundingBoxes = boundingBoxes,
                    targetPoint = maxTarget
                )
                vm.processIntent(AgentEmojiIntent.OnDetectionChanged(result.detectionType))
            }
        }
    }

    override fun onReceiveCurrentFrameBitmap(bitmap: Bitmap) {
        vm.processIntent(AgentEmojiIntent.OnCurrentFrame(bitmap))
        if (vm.uiState.value.vadChatState == VadChatState.Speaking) {
            vm.realtimeChatController?.getInitUdpVisionManager()?.sendVideoFrame(bitmap)
        }
    }

    override fun handleSystemResponse(map: Map<String, String>) {
        vm.processIntent(AgentEmojiIntent.HandleSystemResponse(map, this))
    }

    private fun initWindow() {
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
    }
}

@Composable
private fun AgentEmojiRoute(
    vm: ComposeAgentEmojiVm,
    onPreviewReady: (PreviewView, OverlayView) -> Unit
) {
    val state by vm.uiState.collectAsState()
    val context = LocalContext.current

    AgentEmojiScreen(
        state = state,
        onToggleMic = { vm.processIntent(AgentEmojiIntent.ToggleMic) },
        onSwitchCamera = { vm.processIntent(AgentEmojiIntent.SwitchCamera) },
        onToggleVideo = { vm.processIntent(AgentEmojiIntent.ToggleVideoVisible) },
        onVisionTest = { vm.processIntent(AgentEmojiIntent.VisionTest) },
        onPreviewReady = onPreviewReady,
        onBack = { (context as? ComponentActivity)?.finish() }
    )
}

@Composable
private fun AgentEmojiScreen(
    state: AgentEmojiState,
    onToggleMic: () -> Unit,
    onSwitchCamera: () -> Unit,
    onToggleVideo: () -> Unit,
    onVisionTest: () -> Unit,
    onPreviewReady: (PreviewView, OverlayView) -> Unit,
    onBack: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (state.isVideoVisible) {
                CameraPreviewOverlay(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(250.dp),
                    onPreviewReady = onPreviewReady
                )
            }

            MovingEmojiEyes(
                modifier = Modifier.fillMaxSize(),
                targetXFraction = state.eyeTargetX,
                targetYFraction = state.eyeTargetY
            )

            TopInfoBar(
                state = state,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                onBack = onBack
            )

            BottomActions(
                isMicClosed = state.isMicClosed,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(12.dp),
                onToggleMic = onToggleMic,
                onSwitchCamera = onSwitchCamera,
                onToggleVideo = onToggleVideo,
                onVisionTest = onVisionTest
            )
        }
    }
}

@Composable
private fun CameraPreviewOverlay(
    modifier: Modifier,
    onPreviewReady: (PreviewView, OverlayView) -> Unit
) {
    AndroidView(
        modifier = modifier.clip(RoundedCornerShape(8.dp)),
        factory = { context ->
            androidx.constraintlayout.widget.ConstraintLayout(context).apply {
                val preview = PreviewView(context).apply { id = View.generateViewId() }
                val overlay = OverlayView(context, null).apply { id = View.generateViewId() }
                addView(
                    preview,
                    androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
                        androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.MATCH_PARENT,
                        androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.MATCH_PARENT
                    )
                )
                addView(
                    overlay,
                    androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
                        androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.MATCH_PARENT,
                        androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.MATCH_PARENT
                    )
                )
                onPreviewReady(preview, overlay)
            }
        }
    )
}

@Composable
private fun MovingEmojiEyes(
    modifier: Modifier,
    targetXFraction: Float,
    targetYFraction: Float
) {
    BoxWithConstraints(modifier = modifier) {
        val maxW = maxWidth
        val maxH = maxHeight
        val emojiW = 300.dp
        val emojiH = 100.dp

        val targetX = ((maxW - emojiW) * targetXFraction.coerceIn(0f, 1f))
        val targetY = ((maxH - emojiH) * targetYFraction.coerceIn(0f, 1f))

        val animatedX by androidx.compose.animation.core.animateDpAsState(
            targetValue = targetX,
            animationSpec = spring(dampingRatio = 0.85f),
            label = "emojiX"
        )
        val animatedY by androidx.compose.animation.core.animateDpAsState(
            targetValue = targetY,
            animationSpec = spring(dampingRatio = 0.85f),
            label = "emojiY"
        )

        val pupilOffsetX = (targetXFraction - 0.5f) * 18f
        val pupilOffsetY = (targetYFraction - 0.5f) * 18f
        val pupilX by androidx.compose.animation.core.animateDpAsState(
            targetValue = pupilOffsetX.dp,
            animationSpec = tween(140),
            label = "pupilX"
        )
        val pupilY by androidx.compose.animation.core.animateDpAsState(
            targetValue = pupilOffsetY.dp,
            animationSpec = tween(140),
            label = "pupilY"
        )

        Row(
            modifier = Modifier
                .offset(x = animatedX, y = animatedY)
                .width(emojiW)
                .height(emojiH),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Eye(pupilX = pupilX, pupilY = pupilY)
            Eye(pupilX = pupilX, pupilY = pupilY)
        }
    }
}

@Composable
private fun Eye(pupilX: Dp, pupilY: Dp) {
    Box(
        modifier = Modifier
            .size(100.dp)
            .clip(CircleShape)
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .offset(x = pupilX, y = pupilY)
                .size(34.dp)
                .clip(CircleShape)
                .background(Color.Black)
        )
    }
}

@Composable
private fun TopInfoBar(
    state: AgentEmojiState,
    modifier: Modifier,
    onBack: () -> Unit
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(com.view.appview.R.drawable.chevron_left_24px),
                    contentDescription = "back",
                    tint = Color.White
                )
            }
            Text(
                text = if (state.agentName.isNotEmpty()) state.agentName else "Agent Emoji",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${state.inferenceTimeMs}ms",
                color = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(getDetectionColor(state.detectionColorType))
            )
        }

        Text(
            text = getVadStateText(state.vadChatState),
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun BottomActions(
    isMicClosed: Boolean,
    modifier: Modifier,
    onToggleMic: () -> Unit,
    onSwitchCamera: () -> Unit,
    onToggleVideo: () -> Unit,
    onVisionTest: () -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onToggleMic) {
            Icon(
                painter = painterResource(
                    if (isMicClosed) com.view.appview.R.drawable.mic_24px
                    else com.view.appview.R.drawable.mic_off_24px
                ),
                contentDescription = "mic",
                tint = Color.White
            )
        }

        Row {
            Button(onClick = onSwitchCamera) { Text("切换摄像头") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = onToggleVideo) { Text("显示识别") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = onVisionTest) { Text("Vision Test") }
        }
    }
}

@Composable
private fun getVadStateText(state: VadChatState): String {
    return when (state) {
        is VadChatState.Muted -> stringResource(com.view.appview.R.string.muted)
        is VadChatState.Silent -> stringResource(com.view.appview.R.string.silent)
        is VadChatState.Speaking -> stringResource(com.view.appview.R.string.user_speaking)
        is VadChatState.Replying -> stringResource(com.view.appview.R.string.agent_replying)
        is VadChatState.Error -> stringResource(com.view.appview.R.string.error)
        else -> stringResource(com.view.appview.R.string.muted)
    }
}

@Composable
private fun getDetectionColor(type: DetectionColorType): Color {
    return when (type) {
        DetectionColorType.DEFAULT -> colorResource(com.view.appview.R.color.light_blue_600)
        DetectionColorType.GOLD -> colorResource(com.view.appview.R.color.gold)
        DetectionColorType.RED -> colorResource(com.view.appview.R.color.red)
        DetectionColorType.RESETTING -> colorResource(com.view.appview.R.color.a1_100)
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun AgentEmojiScreenPreview() {
    MagicVectorTheme {
        AgentEmojiScreen(
            state = AgentEmojiState(
                agentName = "鸦羽",
                inferenceTimeMs = 108,
                vadChatState = VadChatState.Replying,
                eyeTargetX = 0.72f,
                eyeTargetY = 0.34f,
                detectionColorType = DetectionColorType.GOLD
            ),
            onToggleMic = {},
            onSwitchCamera = {},
            onToggleVideo = {},
            onVisionTest = {},
            onPreviewReady = { _, _ -> },
            onBack = {}
        )
    }
}
