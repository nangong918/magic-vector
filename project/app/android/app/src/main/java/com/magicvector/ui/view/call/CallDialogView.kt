package com.magicvector.ui.view.call

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.magicvector.domain.constant.VADChatState

@Composable
fun CallDialogView(
    visible: Boolean,
    agentName: String,
    agentAvatar: String?,
    chatState: VADChatState,
    chatMessage: String,
    isMicClosed: Boolean,
    onCloseClick: () -> Unit,
    onMicClick: () -> Unit,
    onCallEndClick: () -> Unit,
) {
    if (!visible) return

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(600.dp)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(Color.White)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = agentName,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 36.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(
                        painter = painterResource(com.view.appview.R.drawable.close_24px),
                        contentDescription = "close",
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(24.dp)
                            .clickable(onClick = onCloseClick),
                        tint = colorResource(id = com.view.appview.R.color.s1_800)
                    )
                }

                Spacer(modifier = Modifier.height(26.dp))

                AsyncImage(
                    model = agentAvatar ?: com.view.appview.R.mipmap.logo,
                    contentDescription = "agent avatar",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape),
                    placeholder = painterResource(com.view.appview.R.mipmap.logo),
                    error = painterResource(com.view.appview.R.mipmap.logo)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = getVadStateText(chatState),
                    color = colorResource(id = com.view.appview.R.color.s1_800)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = chatMessage,
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                    textAlign = TextAlign.Center,
                    color = colorResource(id = com.view.appview.R.color.s1_800)
                )

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircleActionButton(
                        iconRes = if (isMicClosed) com.view.appview.R.drawable.mic_24px
                        else com.view.appview.R.drawable.mic_off_24px,
                        containerColor = colorResource(id = com.view.appview.R.color.s1_400),
                        onClick = onMicClick
                    )
                    Spacer(modifier = Modifier.width(90.dp))
                    CircleActionButton(
                        iconRes = com.view.appview.R.drawable.call_end_24px,
                        containerColor = colorResource(id = com.view.appview.R.color.red),
                        onClick = onCallEndClick
                    )
                }
            }
        }
    }
}

@Composable
private fun CircleActionButton(
    iconRes: Int,
    containerColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(60.dp)
            .clip(CircleShape)
            .background(containerColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun getVadStateText(state: VADChatState): String {
    return when (state) {
        is VADChatState.Muted -> stringResource(com.view.appview.R.string.muted)
        is VADChatState.Silent -> stringResource(com.view.appview.R.string.silent)
        is VADChatState.Speaking -> stringResource(com.view.appview.R.string.user_speaking)
        is VADChatState.Replying -> stringResource(com.view.appview.R.string.agent_replying)
        is VADChatState.Error -> stringResource(com.view.appview.R.string.error)
        else -> stringResource(com.view.appview.R.string.muted)
    }
}


@Preview
@Composable
private fun CallDialogViewPreview() {
    CallDialogView(
        visible = true,
        agentName = "Agent Name",
        agentAvatar = null,
        chatState = VADChatState.Muted,
        chatMessage = "Chat message",
        isMicClosed = true,
        onCloseClick = {},
        onMicClick = {},
        onCallEndClick = {}
    )
}
