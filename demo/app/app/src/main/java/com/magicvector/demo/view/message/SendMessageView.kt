package com.magicvector.demo.view.message

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.magicvector.demo.activity.ui.theme.*
import com.magicvector.demo.R




@Composable
fun rememberSendMessageState(): SendMessageState {
    return remember { SendMessageState() }
}

class SendMessageState {
    var isKeyboardOpen by mutableStateOf(true)
    var messageText by mutableStateOf("")
    var isEnableSend by mutableStateOf(true)
    var audioButtonText by mutableStateOf("按住录音")
    var isAudioPressed by mutableStateOf(false)
}

@Composable
fun SendMessageView(
    state: SendMessageState = rememberSendMessageState(),
    onSendClick: (String) -> Unit = { },
    onImageClick: () -> Unit = { },
    onCallClick: () -> Unit = { },
    onVideoClick: () -> Unit = { },
    onAudioTouch: (isStart: Boolean) -> Unit = { _ -> }
) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .background(A1_200)) {
        if (state.isKeyboardOpen) {
            KeyboardLayout(
                messageText = state.messageText,
                onMessageChange = { state.messageText = it },
                onSendClick = {
                    if (state.messageText.isNotBlank()) {
                        onSendClick(state.messageText)
                        state.messageText = ""
                    }
                },
                onImageClick = onImageClick,
                onAudioClick = { state.isKeyboardOpen = false },
                isEnableSend = state.isEnableSend
            )
        } else {
            AudioLayout(
                audioButtonText = state.audioButtonText,
                isAudioPressed = state.isAudioPressed, // 传递按压状态
                onAudioTouch = { isStart ->
                    state.isAudioPressed = isStart // 更新按压状态
                    state.audioButtonText = if (isStart) "松开取消" else "按住录音"
                    onAudioTouch(isStart)
                },
                onCallClick = onCallClick,
                onVideoClick = onVideoClick,
                onKeyboardClick = { state.isKeyboardOpen = true },
                isEnableSend = state.isEnableSend
            )
        }
    }
}

@Composable
private fun KeyboardLayout(
    messageText: String,
    onMessageChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onImageClick: () -> Unit,
    onAudioClick: () -> Unit,
    isEnableSend: Boolean,
    hintText: String = "输入消息..."
) {
    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
    ) {
        val (editText, pictureBtn, audioBtn, sendBtn) = createRefs()

        // 输入框
        BasicTextField(
            value = messageText,
            onValueChange = onMessageChange,
            modifier = Modifier
                .constrainAs(editText) {
                    start.linkTo(parent.start, margin = 10.dp)
                    end.linkTo(pictureBtn.start, margin = 10.dp)
                    top.linkTo(parent.top, margin = 15.dp)
                    bottom.linkTo(parent.bottom, margin = 15.dp)
                    width = Dimension.fillToConstraints
                }
                .height(45.dp)
                .background(
                    color = if (isEnableSend) A1_10 else A1_50,
                    shape = RoundedCornerShape(10.dp)
                )
                .padding(horizontal = 15.dp),
            textStyle = LocalTextStyle.current.copy(
                color = if (isEnableSend) S1_800 else S1_800.copy(alpha = 0.5f),
                fontSize = 14.sp
            ),
            cursorBrush = SolidColor(S1_800),
            singleLine = true,
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (messageText.isEmpty()) {
                        Text(
                            text = hintText, // 使用传入的 hintText
                            color = S1_800.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                    }
                    innerTextField()
                }
            }
        )

        // 图片按钮
        Box(
            modifier = Modifier
                .size(37.dp)
                .constrainAs(pictureBtn) {
                    end.linkTo(audioBtn.start, margin = 5.dp)
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                }
                .background(
                    color = if (isEnableSend) A1_10 else A1_50,
                    shape = RoundedCornerShape(20.dp)
                )
                .clickable(
                    enabled = isEnableSend,
                    onClick = onImageClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.image_24px),
                contentDescription = "图片",
                tint = if (isEnableSend) S1_800 else S1_800.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }

        // 音频按钮
        Box(
            modifier = Modifier
                .size(37.dp)
                .constrainAs(audioBtn) {
                    end.linkTo(sendBtn.start, margin = 5.dp)
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                }
                .background(
                    color = if (isEnableSend) A1_10 else A1_50,
                    shape = RoundedCornerShape(20.dp)
                )
                .clickable(
                    enabled = isEnableSend,
                    onClick = onAudioClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.mic_24px),
                contentDescription = "语音",
                tint = if (isEnableSend) S1_800 else S1_800.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }

        // 发送按钮
        Box(
            modifier = Modifier
                .size(37.dp)
                .constrainAs(sendBtn) {
                    end.linkTo(parent.end, margin = 10.dp)
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                }
                .background(
                    color = if (isEnableSend) A1_10 else A1_50,
                    shape = RoundedCornerShape(20.dp)
                )
                .clickable(
                    enabled = isEnableSend && messageText.isNotBlank(),
                    onClick = onSendClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.send_24px),
                contentDescription = "发送",
                tint = if (isEnableSend && messageText.isNotBlank()) S1_800 else S1_800.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun AudioLayout(
    audioButtonText: String,
    isAudioPressed: Boolean, // 新增：录音按钮按压状态
    onAudioTouch: (Boolean) -> Unit,
    onCallClick: () -> Unit,
    onVideoClick: () -> Unit,
    onKeyboardClick: () -> Unit,
    isEnableSend: Boolean
) {
    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
    ) {
        val (audioBtn, callBtn, videoBtn, keyboardBtn) = createRefs()

        // 录音按钮
        Box(
            modifier = Modifier
                .constrainAs(audioBtn) {
                    start.linkTo(parent.start, margin = 10.dp)
                    end.linkTo(callBtn.start, margin = 10.dp)
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    width = Dimension.fillToConstraints
                }
                .height(50.dp)
                .background(
                    color = if (isEnableSend) {
                        if (isAudioPressed) A1_200 else A1_10 // 按压时改变颜色
                    } else {
                        A1_50
                    },
                    shape = RoundedCornerShape(25.dp)
                )
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            onAudioTouch(true)  // 按下时通知外部
                            tryAwaitRelease()
                            onAudioTouch(false) // 释放时通知外部
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = audioButtonText,
                color = if (isAudioPressed) White else S1_800,
                fontSize = 18.sp
            )
        }


        Box(
            modifier = Modifier
                .size(37.dp) // 这里设置的大小会生效
                .constrainAs(callBtn) {
                    end.linkTo(videoBtn.start, margin = 10.dp)
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                }
                .background(
                    color = if (isEnableSend) A1_10 else A1_50,
                    shape = RoundedCornerShape(20.dp)
                )
                .clickable(
                    enabled = isEnableSend,
                    onClick = onCallClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.call_24px),
                contentDescription = "通话",
                tint = if (isEnableSend) S1_800 else S1_800.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }

        // 视频通话按钮
        Box(
            modifier = Modifier
                .size(37.dp)
                .constrainAs(videoBtn) {
                    end.linkTo(keyboardBtn.start, margin = 10.dp)
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                }
                .background(
                    color = if (isEnableSend) A1_10 else A1_50,
                    shape = RoundedCornerShape(20.dp)
                )
                .clickable(
                    enabled = isEnableSend,
                    onClick = onVideoClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.videocam_24px),
                contentDescription = "视频通话",
                tint = if (isEnableSend) S1_800 else S1_800.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }

        // 键盘按钮
        Box(
            modifier = Modifier
                .size(37.dp)
                .constrainAs(keyboardBtn) {
                    end.linkTo(parent.end, margin = 10.dp)
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                }
                .background(
                    color = if (isEnableSend) A1_10 else A1_50,
                    shape = RoundedCornerShape(20.dp)
                )
                .clickable(
                    enabled = isEnableSend,
                    onClick = onKeyboardClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.keyboard_24px),
                contentDescription = "键盘",
                tint = if (isEnableSend) S1_800 else S1_800.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 80)
@Composable
fun KeyboardLayoutPreview() {
    AppDemoTheme {
        KeyboardLayout(
            messageText = "测试消息",
            onMessageChange = { },
            onSendClick = { },
            onImageClick = { },
            onAudioClick = { },
            isEnableSend = true
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 80)
@Composable
fun AudioLayoutPreview() {
    AppDemoTheme {
        AudioLayout(
            audioButtonText = "按住录音",
            isAudioPressed = false,
            onAudioTouch = { },
            onCallClick = { },
            onVideoClick = { },
            onKeyboardClick = { },
            isEnableSend = true
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 80)
@Composable
fun AudioLayoutPressedPreview() {
    AppDemoTheme {
        AudioLayout(
            audioButtonText = "松开取消",
            isAudioPressed = true,
            onAudioTouch = { },
            onCallClick = { },
            onVideoClick = { },
            onKeyboardClick = { },
            isEnableSend = true
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 150)
@Composable
fun SendMessageViewPreview() {
    AppDemoTheme {
        Column {
            // 键盘模式预览
            SendMessageView(
                state = remember {
                    SendMessageState().apply {
                        isKeyboardOpen = true
                        messageText = "测试消息内容"
                    }
                },
                onSendClick = { },
                onImageClick = { },
                onCallClick = { },
                onVideoClick = { },
                onAudioTouch = { }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 音频模式预览
            SendMessageView(
                state = remember {
                    SendMessageState().apply {
                        isKeyboardOpen = false
                        audioButtonText = "按住录音"
                    }
                },
                onSendClick = { },
                onImageClick = { },
                onCallClick = { },
                onVideoClick = { },
                onAudioTouch = { }
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 80)
@Composable
fun SendMessageViewDisabledPreview() {
    AppDemoTheme {
        SendMessageView(
            state = remember {
                SendMessageState().apply {
                    isEnableSend = false
                    messageText = "禁用状态"
                }
            },
            onSendClick = { },
            onImageClick = { },
            onCallClick = { },
            onVideoClick = { },
            onAudioTouch = { }
        )
    }
}


