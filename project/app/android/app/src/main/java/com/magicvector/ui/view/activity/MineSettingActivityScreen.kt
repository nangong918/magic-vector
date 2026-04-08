package com.magicvector.ui.view.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.magicvector.ui.theme.MagicVectorTheme
import com.magicvector.viewModel.activity.MineSettingState

@Composable
fun ComposeMineSettingActivityScreen(
    state: MineSettingState,
    onOldPasswordChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onSubmitPassword: () -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MineProfileCard(userName = state.userName)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Setting", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = state.oldPassword,
                    onValueChange = onOldPasswordChange,
                    label = { Text("旧密码") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.newPassword,
                    onValueChange = onNewPasswordChange,
                    label = { Text("新密码") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onSubmitPassword) { Text("修改密码") }
                    Button(onClick = onLogout) { Text("登出") }
                }
            }
        }
    }
}

@Composable
private fun MineProfileCard(userName: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
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
private fun ComposeMineSettingActivityScreenPreview() {
    MagicVectorTheme {
        ComposeMineSettingActivityScreen(
            state = MineSettingState(userName = "Demo"),
            onOldPasswordChange = {},
            onNewPasswordChange = {},
            onSubmitPassword = {},
            onLogout = {}
        )
    }
}
