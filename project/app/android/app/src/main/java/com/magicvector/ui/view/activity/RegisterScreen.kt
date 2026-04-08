package com.magicvector.ui.view.activity

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
import androidx.compose.foundation.layout.width
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
import com.magicvector.ui.theme.MagicVectorTheme
import com.magicvector.viewModel.activity.RegisterState

@Composable
fun ComposeRegisterScreen(
    state: RegisterState,
    onAccountChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onSelectAvatar: () -> Unit,
    onSubmit: () -> Unit,
    onGoLogin: () -> Unit
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

            AvatarPicker(
                avatarUri = state.avatarUri,
                onSelectAvatar = onSelectAvatar
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = state.account,
                onValueChange = onAccountChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("账号") },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = state.password,
                onValueChange = onPasswordChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("密码") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = state.confirmPassword,
                onValueChange = onConfirmPasswordChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("确认密码") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onSubmit,
                enabled = state.canSubmit,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp))
                } else {
                    Text("注册")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            TextButton(onClick = onGoLogin) {
                Text("已有账号？去登录")
            }
        }
    }
}

@Composable
private fun AvatarPicker(
    avatarUri: Uri?,
    onSelectAvatar: () -> Unit
) {
    if (avatarUri == null) {
        Image(
            painter = painterResource(id = com.view.appview.R.drawable.person_24px),
            contentDescription = "avatar",
            modifier = Modifier
                .height(80.dp)
                .width(80.dp)
                .clip(CircleShape)
                .background(Color(0xFFEFEFEF))
                .clickable { onSelectAvatar() }
                .padding(20.dp),
            contentScale = ContentScale.Fit
        )
    } else {
        AsyncImage(
            model = avatarUri,
            contentDescription = "avatar",
            modifier = Modifier
                .height(80.dp)
                .width(80.dp)
                .clip(CircleShape)
                .clickable { onSelectAvatar() },
            contentScale = ContentScale.Crop
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun ComposeRegisterScreenPreview() {
    MagicVectorTheme {
        ComposeRegisterScreen(
            state = RegisterState(),
            onAccountChange = {},
            onPasswordChange = {},
            onConfirmPasswordChange = {},
            onSelectAvatar = {},
            onSubmit = {},
            onGoLogin = {}
        )
    }
}
