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
import com.vectordemo.domain.model.UserSessionModel
import com.vectordemo.viewModel.activity.LoginIntent
import com.vectordemo.viewModel.activity.LoginState

@Composable
fun ComposeLoginScreen(
    state: LoginState,
    savedAccounts: List<UserSessionModel>,
    processIntent: (LoginIntent) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("登录")
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = state.account,
            onValueChange = { processIntent(LoginIntent.UpdateAccount(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("账号") },
            singleLine = true,
        )
        if (savedAccounts.isNotEmpty()) {
            TextButton(onClick = { processIntent(LoginIntent.SelectSavedAccount(savedAccounts.first().account)) }) {
                Text("使用最近账号：${savedAccounts.first().account}")
            }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = state.password,
            onValueChange = { processIntent(LoginIntent.UpdatePassword(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("密码") },
            singleLine = true,
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { processIntent(LoginIntent.SubmitLogin) },
            enabled = state.canSubmit,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (state.isLoading) "登录中..." else "登录")
        }
        Spacer(Modifier.height(10.dp))
        TextButton(onClick = { processIntent(LoginIntent.NavigateToRegister) }) { Text("没有账号？去注册") }
        TextButton(onClick = { processIntent(LoginIntent.TouristAccess) }) { Text("游客访问") }
    }
}
