package com.vectordemo.ui.view.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vectordemo.viewModel.activity.RegisterIntent
import com.vectordemo.viewModel.activity.RegisterState

@Composable
fun ComposeRegisterScreen(
    state: RegisterState,
    processIntent: (RegisterIntent) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("注册")
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = { processIntent(RegisterIntent.SelectAvatar) }) {
            Text(if (state.avatar == null) "选择头像" else "已选择头像（点击更换）")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = state.account,
            onValueChange = { processIntent(RegisterIntent.UpdateAccount(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("账号") },
            singleLine = true,
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = state.password,
            onValueChange = { processIntent(RegisterIntent.UpdatePassword(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("密码") },
            singleLine = true,
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = state.confirmPassword,
            onValueChange = { processIntent(RegisterIntent.UpdateConfirmPassword(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("确认密码") },
            singleLine = true,
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { processIntent(RegisterIntent.SubmitRegister) },
            enabled = state.canSubmit,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (state.isLoading) "注册中..." else "注册") }
        Spacer(Modifier.height(10.dp))
        TextButton(onClick = { processIntent(RegisterIntent.NavigateToLogin) }) { Text("已有账号？去登录") }
    }
}
