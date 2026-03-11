package com.magicvector.fragment

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.magicvector.ui.theme.MagicVectorTheme
import com.magicvector.viewModel.activity.AgentVoiceOrbPhase
import com.magicvector.viewModel.fragment.AgentEmojiFragmentEffect
import com.magicvector.viewModel.fragment.AgentEmojiFragmentIntent
import com.magicvector.viewModel.fragment.AgentEmojiFragmentState
import com.magicvector.viewModel.fragment.AgentEmojiFragmentVm

@Composable
fun AgentEmojiFragment(
    vm: AgentEmojiFragmentVm,
    onToggleMic: () -> Unit,
    onRequestWakeUp: () -> Unit,
    onEndVoiceMode: () -> Unit,
) {
    val state by vm.uiState.collectAsState()

    LaunchedEffect(vm) {
        vm.effect.collect { effect ->
            when (effect) {
                AgentEmojiFragmentEffect.ForwardToggleMic -> onToggleMic()
                AgentEmojiFragmentEffect.ForwardRequestWakeUp -> onRequestWakeUp()
                AgentEmojiFragmentEffect.ForwardEndVoiceMode -> onEndVoiceMode()
            }
        }
    }

    AgentEmojiFragmentScreen(
        state = state,
        onToggleMic = { vm.processIntent(AgentEmojiFragmentIntent.ToggleMic) },
        onWakeUp = { vm.processIntent(AgentEmojiFragmentIntent.RequestWakeUp) },
        onEnd = { vm.processIntent(AgentEmojiFragmentIntent.EndVoiceMode) }
    )
}

@Composable
private fun AgentEmojiFragmentScreen(
    state: AgentEmojiFragmentState,
    onToggleMic: () -> Unit,
    onWakeUp: () -> Unit,
    onEnd: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (state.isOrbExpanded) 1.45f else 1f,
        animationSpec = spring(
            dampingRatio = 0.45f,
            stiffness = 220f
        ),
        label = "voiceOrbScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(28.dp))
        EyesView()
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = state.statusText,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .size(28.dp)
                .scale(scale)
                .background(color = getOrbColor(state.phase), shape = CircleShape)
        )

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(onClick = onToggleMic) {
                Icon(
                    painter = painterResource(com.view.appview.R.drawable.mic_24px),
                    contentDescription = "mic",
                    tint = Color.White
                )
            }
            IconButton(onClick = onWakeUp) {
                Icon(
                    painter = painterResource(com.view.appview.R.drawable.call_24px),
                    contentDescription = "wake",
                    tint = Color.White
                )
            }
            IconButton(onClick = onEnd) {
                Icon(
                    painter = painterResource(com.view.appview.R.drawable.call_end_24px),
                    contentDescription = "end",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun EyesView() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(36.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Eye()
        Eye()
    }
}

@Composable
private fun Eye() {
    Box(
        modifier = Modifier
            .size(92.dp)
            .background(Color.White, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(Color.Black, CircleShape)
        )
    }
}

@Composable
private fun getOrbColor(phase: AgentVoiceOrbPhase): Color {
    return when (phase) {
        AgentVoiceOrbPhase.DISCONNECTED -> Color(0xFF9E9E9E)
        AgentVoiceOrbPhase.ERROR -> Color(0xFFE53935)
        AgentVoiceOrbPhase.READY -> Color(0xFF43A047)
        AgentVoiceOrbPhase.USER_SPEAKING -> Color(0xFF1E88E5)
        AgentVoiceOrbPhase.AGENT_REPLYING -> Color(0xFF8E24AA)
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun AgentEmojiFragmentPreview() {
    MagicVectorTheme {
        AgentEmojiFragmentScreen(
            state = AgentEmojiFragmentState(
                phase = AgentVoiceOrbPhase.AGENT_REPLYING,
                isOrbExpanded = true,
                statusText = "Agent 正在回复"
            ),
            onToggleMic = {},
            onWakeUp = {},
            onEnd = {}
        )
    }
}
