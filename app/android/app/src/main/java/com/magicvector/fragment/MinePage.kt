package com.magicvector.fragment

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.magicvector.activity.ComposeMineSettingActivity
import com.magicvector.activity.ComposeMineVideoActivity
import com.magicvector.activity.test.ComposeTestActivity
import com.magicvector.viewModel.fragment.MineEffect
import com.magicvector.viewModel.fragment.MineIntent
import com.magicvector.viewModel.fragment.MineState
import com.magicvector.viewModel.fragment.MineVm
import com.view.appview.R

@Composable
fun MineScreen(
    modifier: Modifier = Modifier,
    viewModel: MineVm
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.processIntent(MineIntent.Initialize)
    }
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                MineEffect.NavigateToSetting -> context.startActivity(Intent(context, ComposeMineSettingActivity::class.java))
                MineEffect.NavigateToVideo -> context.startActivity(Intent(context, ComposeMineVideoActivity::class.java))
                is MineEffect.NavigateToActivity -> {
                    if (effect.activityClassName == ComposeTestActivity::class.java.name) {
                        context.startActivity(Intent(context, ComposeTestActivity::class.java))
                    }
                }
                is MineEffect.ShowToast -> Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    MineEntryContent(
        modifier = modifier,
        state = state,
        onOpenSetting = { viewModel.processIntent(MineIntent.OpenSettingPage) },
        onOpenVideo = { viewModel.processIntent(MineIntent.OpenVideoPage) },
        onOpenTest = { viewModel.processIntent(MineIntent.TestButtonClick) }
    )
}

@Composable
private fun MineEntryContent(
    modifier: Modifier = Modifier,
    state: MineState,
    onOpenSetting: () -> Unit,
    onOpenVideo: () -> Unit,
    onOpenTest: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        HeaderProfile(userName = state.userName)
        Spacer(modifier = Modifier.height(18.dp))
        Button(onClick = onOpenSetting, modifier = Modifier.fillMaxWidth()) { Text("设置") }
        Spacer(modifier = Modifier.height(10.dp))
        Button(onClick = onOpenVideo, modifier = Modifier.fillMaxWidth()) { Text("视频") }
        Spacer(modifier = Modifier.height(10.dp))
        Button(onClick = onOpenTest, modifier = Modifier.fillMaxWidth()) { Text(stringResource(id = R.string.test)) }
    }
}

@Composable
private fun HeaderProfile(userName: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text("U")
            }
            Column {
                Text(text = "UserAccount", style = MaterialTheme.typography.titleLarge)
                Text(text = if (userName.isBlank()) "Guest" else userName)
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun MineEntryPreview() {
    MineEntryContent(
        state = MineState(userName = "Demo"),
        onOpenSetting = {},
        onOpenVideo = {},
        onOpenTest = {}
    )
}
