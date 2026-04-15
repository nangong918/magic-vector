package com.vectordemo.ui.view.activity

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.vectordemo.ui.theme.VectorDemoTheme
import com.vectordemo.viewModel.activity.RegisterIntent
import com.vectordemo.viewModel.activity.RegisterState

@Composable
fun ComposeRegisterScreen(
    state: RegisterState,
    processIntent: (RegisterIntent) -> Unit
) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "注册", fontSize = 20.sp)
            Spacer(modifier = Modifier.height(16.dp))
            AvatarPicker(state.avatarUri) { processIntent(RegisterIntent.SelectAvatar) }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                state.account,
                { processIntent(RegisterIntent.UpdateAccount(it)) },
                Modifier.fillMaxWidth(),
                label = { Text("账号") },
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                state.password,
                { processIntent(RegisterIntent.UpdatePassword(it)) },
                Modifier.fillMaxWidth(),
                label = { Text("密码") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                state.confirmPassword,
                { processIntent(RegisterIntent.UpdateConfirmPassword(it)) },
                Modifier.fillMaxWidth(),
                label = { Text("确认密码") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = { processIntent(RegisterIntent.SubmitRegister) },
                enabled = state.canSubmit,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isLoading) CircularProgressIndicator(modifier = Modifier.height(20.dp)) else Text("注册")
            }
            Spacer(modifier = Modifier.height(10.dp))
            TextButton(onClick = { processIntent(RegisterIntent.NavigateToLogin) }) { Text("已有账号？去登录") }
        }
    }
}

@Composable
private fun AvatarPicker(avatarUri: Uri?, onSelectAvatar: () -> Unit) {
    if (avatarUri == null) {
        Image(
            painter = painterResource(android.R.drawable.ic_menu_gallery),
            contentDescription = "avatar",
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Color(0xFFEFEFEF))
                .clickable(onClick = onSelectAvatar)
                .padding(20.dp),
            contentScale = ContentScale.Fit
        )
    } else {
        AsyncImage(
            model = avatarUri,
            contentDescription = "avatar",
            modifier = Modifier.size(80.dp).clip(CircleShape).clickable(onClick = onSelectAvatar),
            contentScale = ContentScale.Crop
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ComposeRegisterScreenPreview() {
    VectorDemoTheme {
        ComposeRegisterScreen(RegisterState(), {})
    }
}
