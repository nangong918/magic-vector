package com.magicvector.ui.view.activity




import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension

@Composable
fun ChatToolbar(
    title: String = "",
    onBackClick: () -> Unit = {}
) {
    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(colorResource(id = com.view.appview.R.color.a1_200))
    ) {
        val (imgvBack, tvTitle) = createRefs()

        // 返回按钮
        Image(
            painter = painterResource(id = com.view.appview.R.drawable.chevron_left_24px),
            contentDescription = "Back",
            modifier = Modifier
                .constrainAs(imgvBack) {
                    start.linkTo(parent.start)
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                }
                .padding(start = 20.dp)
                .clickable( onClick = { onBackClick() }),
            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(colorResource(id = com.view.appview.R.color.s1_800))
        )

        // 标题文本
        Text(
            text = title,
            fontSize = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .constrainAs(tvTitle) {
                    start.linkTo(imgvBack.end, margin = 20.dp)
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    width = Dimension.value(250.dp)
                }
                .padding(4.dp),
            color = colorResource(id = com.view.appview.R.color.s1_800)
        )
    }
}






@Preview
@Composable
private fun ChatToolbarPreview() {
    ChatToolbar(title = "鸦羽天下第一!")
}














