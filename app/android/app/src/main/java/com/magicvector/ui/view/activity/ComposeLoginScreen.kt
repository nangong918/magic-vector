package com.magicvector.ui.view.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.magicvector.ui.theme.MagicVectorTheme
import com.magicvector.viewModel.activity.LoginState
import com.magicvector.domain.model.UserSessionModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeLoginScreen(
    state: LoginState,
    savedAccounts: List<UserSessionModel>,
    onAccountChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSelectSavedAccount: (String) -> Unit,
    onSubmit: () -> Unit,
    onGoRegister: () -> Unit
) {
    var accountMenuExpanded by remember { mutableStateOf(false) }
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "登录")
            Spacer(modifier = Modifier.height(16.dp))

            ExposedDropdownMenuBox(
                expanded = accountMenuExpanded && savedAccounts.isNotEmpty(),
                onExpandedChange = {
                    if (savedAccounts.isNotEmpty()) {
                        accountMenuExpanded = !accountMenuExpanded
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = state.account,
                    onValueChange = {
                        onAccountChange(it)
                        accountMenuExpanded = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    label = { Text("账号") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(
                            expanded = accountMenuExpanded && savedAccounts.isNotEmpty()
                        )
                    },
                    singleLine = true
                )

                ExposedDropdownMenu(
                    expanded = accountMenuExpanded && savedAccounts.isNotEmpty(),
                    onDismissRequest = { accountMenuExpanded = false }
                ) {
                    savedAccounts.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (option.password.isNotBlank()) {
                                        "${option.account} (已存密码)"
                                    } else {
                                        option.account
                                    }
                                )
                            },
                            onClick = {
                                onSelectSavedAccount(option.account)
                                accountMenuExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = state.password,
                onValueChange = onPasswordChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("密码") },
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
                    Text("登录")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            TextButton(onClick = onGoRegister) {
                Text("没有账号？去注册")
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun ComposeLoginScreenPreview() {
    MagicVectorTheme {
        ComposeLoginScreen(
            state = LoginState(),
            savedAccounts = emptyList(),
            onAccountChange = {},
            onPasswordChange = {},
            onSelectSavedAccount = {},
            onSubmit = {},
            onGoRegister = {}
        )
    }
}
