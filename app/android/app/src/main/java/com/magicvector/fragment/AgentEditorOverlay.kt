package com.magicvector.fragment

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.magicvector.viewModel.activity.AgentEditorMode
import com.magicvector.viewModel.activity.AgentEditorState

@Composable
fun AgentEditorOverlay(
    state: AgentEditorState,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit,
) {
    AnimatedVisibility(
        visible = state.isVisible,
        enter = fadeIn() + scaleIn(initialScale = 0.9f),
        exit = fadeOut() + scaleOut(targetScale = 0.9f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (state.mode == AgentEditorMode.CREATE) "创建Agent" else "编辑Agent",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "关闭",
                    modifier = Modifier.clickable(onClick = onClose),
                    color = Color.Gray
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = state.name,
                onValueChange = onNameChange,
                label = { Text("名称") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = state.description,
                onValueChange = onDescriptionChange,
                label = { Text("设定（提示词）") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "头像上传待接入 MinIO，当前支持不传头像创建/更新。",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onSubmit,
                enabled = !state.isSubmitting && state.name.isNotBlank() && state.description.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (state.mode == AgentEditorMode.CREATE) "创建" else "保存")
            }
            if (state.mode == AgentEditorMode.EDIT) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onDelete,
                    enabled = !state.isSubmitting,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("删除")
                }
            }
            if (state.isLoading || state.isSubmitting) {
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator()
            }
        }
    }
}




@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewAgentEditorOverlayCreateMode() {
    // 创建创建模式的示例状态
    val sampleState = AgentEditorState(
        isVisible = true,
        mode = AgentEditorMode.CREATE,
        name = "客服助手",
        description = "你是一个友好的客服助手，负责解答用户关于我们产品的咨询。",
        isLoading = false,
        isSubmitting = false
    )

    // 使用 MaterialTheme 包装以确保正确样式
    MaterialTheme {
        AgentEditorOverlay(
            state = sampleState,
            onNameChange = {},
            onDescriptionChange = {},
            onSubmit = {},
            onDelete = {},
            onClose = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewAgentEditorOverlayEditMode() {
    // 创建编辑模式的示例状态
    val sampleState = AgentEditorState(
        isVisible = true,
        mode = AgentEditorMode.EDIT,
        name = "技术支持助手",
        description = "你是一个专业的技术支持专家，帮助用户解决技术问题。",
        isLoading = false,
        isSubmitting = false
    )

    MaterialTheme {
        AgentEditorOverlay(
            state = sampleState,
            onNameChange = {},
            onDescriptionChange = {},
            onSubmit = {},
            onDelete = {},
            onClose = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewAgentEditorOverlayLoadingState() {
    // 创建加载中的示例状态
    val sampleState = AgentEditorState(
        isVisible = true,
        mode = AgentEditorMode.CREATE,
        name = "正在加载的Agent",
        description = "这是一个正在加载的示例描述",
        isLoading = true,
        isSubmitting = false
    )

    MaterialTheme {
        AgentEditorOverlay(
            state = sampleState,
            onNameChange = {},
            onDescriptionChange = {},
            onSubmit = {},
            onDelete = {},
            onClose = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewAgentEditorOverlaySubmittingState() {
    // 创建提交中的示例状态
    val sampleState = AgentEditorState(
        isVisible = true,
        mode = AgentEditorMode.CREATE,
        name = "提交中的Agent",
        description = "这是一个正在提交的示例描述",
        isLoading = false,
        isSubmitting = true
    )

    MaterialTheme {
        AgentEditorOverlay(
            state = sampleState,
            onNameChange = {},
            onDescriptionChange = {},
            onSubmit = {},
            onDelete = {},
            onClose = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewAgentEditorOverlayHiddenState() {
    // 创建隐藏状态的示例
    val sampleState = AgentEditorState(
        isVisible = false,
        mode = AgentEditorMode.CREATE,
        name = "",
        description = "",
        isLoading = false,
        isSubmitting = false
    )

    MaterialTheme {
        AgentEditorOverlay(
            state = sampleState,
            onNameChange = {},
            onDescriptionChange = {},
            onSubmit = {},
            onDelete = {},
            onClose = {}
        )
    }
}
