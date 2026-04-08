package com.magicvector.ui.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter

/**
 * Agent 头像组件 - 对应 CircleImageView
 */
@Composable
fun AgentAvatar(
    imageFile: java.io.File? = null,
    defaultResId: Int = com.view.appview.R.mipmap.vector,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(120.dp)
            .clip(CircleShape)
            .background(
                color = Color.White,
                shape = CircleShape
            )
            .border(
                width = 1.dp,
                color = Color.LightGray,
                shape = CircleShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (imageFile != null && imageFile.exists()) {
            // 显示选择的图片
            Image(
                painter = rememberAsyncImagePainter(imageFile),
                contentDescription = "Agent Avatar",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        } else {
            // 显示默认图标
            Icon(
                painter = painterResource(id = defaultResId),
                contentDescription = "Default Avatar",
                modifier = Modifier.size(60.dp),
                tint = colorResource(id = com.view.appview.R.color.s1_800)
            )
        }
    }
}


@Preview
@Composable
fun AgentAvatarPreview() {
    AgentAvatar()
}