package com.magicvector.ui.view.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview


/**
 * Agent 名称输入框 - 对应 GeneralEditText
 */
@Composable
fun AgentNameInput(
    value: String,
    onValueChange: (String) -> Unit,
    isValid: Boolean,
    errorMessage: String?,
    maxLength: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = stringResource(id = com.view.appview.R.string.please_input_agent_name),
                    fontSize = 16.sp,
                    color = Color.Gray
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            isError = value.isNotBlank() && !isValid,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colorResource(id = com.view.appview.R.color.s1_800),
                unfocusedBorderColor = Color.Gray,
                focusedLabelColor = colorResource(id = com.view.appview.R.color.s1_800),
                cursorColor = colorResource(id = com.view.appview.R.color.s1_800)
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
        )

        // 显示错误信息或字符计数
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.End
        ) {
            if (value.isNotBlank() && !isValid && errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = Color.Red,
                    fontSize = 12.sp
                )
            } else {
                Text(
                    text = "${value.length}/$maxLength",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
    }
}


/**
 * Agent 描述输入框 - 对应 GeneralEditText (多行)
 */
@Composable
fun AgentDescriptionInput(
    value: String,
    onValueChange: (String) -> Unit,
    isValid: Boolean,
    errorMessage: String?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = stringResource(id = com.view.appview.R.string.please_input_agent_description),
                    fontSize = 16.sp,
                    color = Color.Gray
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 100.dp),
            minLines = 3,
            maxLines = 5,
            shape = RoundedCornerShape(8.dp),
            isError = value.isNotBlank() && !isValid,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colorResource(id = com.view.appview.R.color.s1_800),
                unfocusedBorderColor = Color.Gray,
                focusedLabelColor = colorResource(id = com.view.appview.R.color.s1_800),
                cursorColor = colorResource(id = com.view.appview.R.color.s1_800)
            )
        )

        // 显示错误信息
        if (value.isNotBlank() && !isValid && errorMessage != null) {
            Text(
                text = errorMessage,
                color = Color.Red,
                fontSize = 12.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )
        }
    }
}

/**
 * 提交按钮 - 对应 AppCompatButton
 */
@Composable
fun SubmitButton(
    enabled: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .padding(horizontal = 30.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = colorResource(id = com.view.appview.R.color.s1_800),
            disabledContainerColor = Color.Gray,
            contentColor = Color.White
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(30.dp),
                strokeWidth = 3.dp
            )
        } else {
            Text(
                text = stringResource(id = com.view.appview.R.string.create_agent),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}



@Preview
@Composable
fun CreateAgentViewPreview() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        AgentNameInput(
            value = "Agent Name",
            onValueChange = {},
            isValid = true,
            errorMessage = null,
            maxLength = 20
        )
        AgentDescriptionInput(
            value = "Agent Description",
            onValueChange = {},
            isValid = true,
            errorMessage = null
        )
    }
}









