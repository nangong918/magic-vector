package com.magicvector.demo.view.jetMessage

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFrom
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Divider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.magicvector.demo.R
import com.magicvector.demo.view.jetMessage.data.exampleUiState
import kotlinx.coroutines.launch

const val ConversationTestTag = "ConversationTestTag"
private val JumpToBottomThreshold = 56.dp

@Preview
@Composable
fun MessagesPreview() {
    val scrollState = rememberLazyListState()
    Messages(
        messages = exampleUiState.messages,
        navigateToProfile = {},
        modifier = Modifier.fillMaxSize(),
        scrollState = scrollState,
    )
}


@Composable
fun Messages(messages: List<Message>, navigateToProfile: (String) -> Unit, scrollState: LazyListState, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    Box(modifier = modifier) {

        val authorMe = "me"
        LazyColumn(
            reverseLayout = true,
            state = scrollState,
            modifier = Modifier
                .testTag(ConversationTestTag)
                .fillMaxSize(),
        ) {
            for (index in messages.indices) {
                val prevAuthor = messages.getOrNull(index - 1)?.author
                val nextAuthor = messages.getOrNull(index + 1)?.author
                val content = messages[index]
                val isFirstMessageByAuthor = prevAuthor != content.author
                val isLastMessageByAuthor = nextAuthor != content.author

                // Hardcode day dividers for simplicity
                if (index == messages.size - 1) {
                    item {
                        DayHeader("20 Aug")
                    }
                } else if (index == 2) {
                    item {
                        DayHeader("Today")
                    }
                }

                item {
                    Message(
                        onAuthorClick = { name -> navigateToProfile(name) },
                        msg = content,
                        isUserMe = content.author == authorMe,
                        isFirstMessageByAuthor = isFirstMessageByAuthor,
                        isLastMessageByAuthor = isLastMessageByAuthor,
                    )
                }
            }
        }
        // Jump to bottom button shows up when user scrolls past a threshold.
        // Convert to pixels:
        val jumpThreshold = with(LocalDensity.current) {
            JumpToBottomThreshold.toPx()
        }

        // Show the button if the first visible item is not the first one or if the offset is
        // greater than the threshold.
        val jumpToBottomButtonEnabled by remember {
            derivedStateOf {
                scrollState.firstVisibleItemIndex != 0 ||
                        scrollState.firstVisibleItemScrollOffset > jumpThreshold
            }
        }

        JumpToBottom(
            // Only show if the scroller is not at the bottom
            enabled = jumpToBottomButtonEnabled,
            onClicked = {
                scope.launch {
                    scrollState.animateScrollToItem(0)
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
fun DayHeader(dayString: String) {
    Row(
        modifier = Modifier
            .padding(vertical = 8.dp, horizontal = 16.dp)
            .height(16.dp),
    ) {
        DayHeaderLine()
        Text(
            text = dayString,
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        DayHeaderLine()
    }
}

@Composable
private fun RowScope.DayHeaderLine() {
    Divider(
        modifier = Modifier
            .weight(1f)
            .align(Alignment.CenterVertically),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
    )
}

private val ChatBubbleShape = RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp)
private val MineChatBubbleShape = RoundedCornerShape(20.dp, 4.dp, 20.dp, 20.dp)

@Composable
fun Message(
    onAuthorClick: (String) -> Unit,
    msg: Message,
    isUserMe: Boolean,
    isFirstMessageByAuthor: Boolean,
    isLastMessageByAuthor: Boolean,
) {
    val borderColor = if (isUserMe) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.tertiary
    }

    val spaceBetweenAuthors = if (isLastMessageByAuthor) Modifier.padding(top = 8.dp) else Modifier

    if (isUserMe) {
        // 自己发送的消息：从右往左展示，右边距8dp
        Row(
            modifier = spaceBetweenAuthors
                .fillMaxWidth()
                .padding(end = 8.dp, start = 16.dp), // 右边距8dp，相当于end_to_end_of_parent, marginEnd=8dp
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.Top,
        ) {
            // 消息气泡在右侧
            AuthorAndTextMessage(
                msg = msg,
                isUserMe = true,
                isFirstMessageByAuthor = isFirstMessageByAuthor,
                isLastMessageByAuthor = isLastMessageByAuthor,
                authorClicked = onAuthorClick,
                modifier = Modifier
                    .wrapContentWidth()
                    .widthIn(max = 280.dp), // 限制最大宽度
                shape = MineChatBubbleShape
            )

            // 自己发送的消息不显示头像，右侧留出空间对齐
            if (!isLastMessageByAuthor) {
                Spacer(modifier = Modifier.width(42.dp/* + 16.dp*/)) // 头像宽度 + padding
            }
        }
    } else {
        // 别人发送的消息：从左往右展示，左边距8dp
        Row(
            modifier = spaceBetweenAuthors
                .fillMaxWidth()
                .padding(start = 8.dp, end = 16.dp), // 左边距8dp，相当于start_to_start_of_parent, marginStart=8dp
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.Top,
        ) {
            if (isLastMessageByAuthor) {
                // 只在最后一条消息显示头像
                Image(
                    modifier = Modifier
                        .clickable(onClick = { onAuthorClick(msg.author) })
                        .padding(horizontal = 16.dp)
                        .size(42.dp)
                        .border(1.5.dp, borderColor, CircleShape)
                        .border(3.dp, MaterialTheme.colorScheme.surface, CircleShape)
                        .clip(CircleShape)
                        .align(Alignment.Top),
                    painter = painterResource(id = msg.authorImage),
                    contentScale = ContentScale.Crop,
                    contentDescription = null,
                )
            } else {
                // 非最后一条消息，留出头像空间
                Spacer(modifier = Modifier.width(42.dp + 16.dp))
            }

            // 消息气泡在左侧
            AuthorAndTextMessage(
                msg = msg,
                isUserMe = false,
                isFirstMessageByAuthor = isFirstMessageByAuthor,
                isLastMessageByAuthor = isLastMessageByAuthor,
                authorClicked = onAuthorClick,
                modifier = Modifier
                    .wrapContentWidth()
                    .widthIn(max = 280.dp), // 限制最大宽度
                shape = ChatBubbleShape
            )
        }
    }
}

// 如果需要更精确的布局，可以使用 ConstraintLayout
@Composable
fun MessageWithConstraintLayout(
    onAuthorClick: (String) -> Unit,
    msg: Message,
    isUserMe: Boolean,
    isFirstMessageByAuthor: Boolean,
    isLastMessageByAuthor: Boolean,
) {
    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (!isUserMe) 8.dp else 0.dp,
                end = if (isUserMe) 8.dp else 0.dp
            )
    ) {
        val (avatarRef, messageRef) = createRefs()

        if (!isUserMe && isLastMessageByAuthor) {
            // 别人发送的消息且是最后一条：显示头像在左侧
            Image(
                modifier = Modifier
                    .constrainAs(avatarRef) {
                        start.linkTo(parent.start, margin = 16.dp)
                        top.linkTo(parent.top)
                    }
                    .size(42.dp)
                    .clickable { onAuthorClick(msg.author) }
                    .border(1.5.dp, MaterialTheme.colorScheme.tertiary, CircleShape)
                    .clip(CircleShape),
                painter = painterResource(id = msg.authorImage),
                contentScale = ContentScale.Crop,
                contentDescription = null,
            )
        }

        // 消息气泡
        AuthorAndTextMessage(
            msg = msg,
            isUserMe = isUserMe,
            isFirstMessageByAuthor = isFirstMessageByAuthor,
            isLastMessageByAuthor = isLastMessageByAuthor,
            authorClicked = onAuthorClick,
            modifier = Modifier
                .constrainAs(messageRef) {
                    if (isUserMe) {
                        // 自己发送：靠右
                        end.linkTo(parent.end)
                        start.linkTo(parent.start)
                        width = Dimension.fillToConstraints
                    } else {
                        // 别人发送：靠左
                        start.linkTo(
                            if (isLastMessageByAuthor) avatarRef.end else parent.start,
                            margin = if (isLastMessageByAuthor) 8.dp else 58.dp // 42dp头像 + 16dp padding
                        )
                        end.linkTo(parent.end)
                        width = Dimension.fillToConstraints
                    }
                    top.linkTo(parent.top)
                }
                .widthIn(max = 280.dp),
            shape = if (isUserMe) MineChatBubbleShape else ChatBubbleShape
        )
    }
}

@Composable
fun AuthorAndTextMessage(
    msg: Message,
    isUserMe: Boolean,
    isFirstMessageByAuthor: Boolean,
    isLastMessageByAuthor: Boolean,
    authorClicked: (String) -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape
) {
    Column(modifier = modifier) {
        if (isLastMessageByAuthor) {
            AuthorNameTimestamp(msg)
        }
        ChatItemBubble(msg, isUserMe, shape = shape, authorClicked = authorClicked)
        if (isFirstMessageByAuthor) {
            // Last bubble before next author
            Spacer(modifier = Modifier.height(8.dp))
        } else {
            // Between bubbles
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun AuthorNameTimestamp(msg: Message) {
    // Combine author and timestamp for a11y.
    Row(modifier = Modifier.semantics(mergeDescendants = true) {}) {
        Text(
            text = msg.author,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .alignBy(LastBaseline)
                .paddingFrom(LastBaseline, after = 8.dp), // Space to 1st bubble
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = msg.timestamp,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.alignBy(LastBaseline),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}


@Composable
fun ChatItemBubble(message: Message, isUserMe: Boolean, shape: Shape, authorClicked: (String) -> Unit) {

    val backgroundBubbleColor = if (isUserMe) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Column {
        Surface(
            color = backgroundBubbleColor,
            shape = shape,
        ) {
            ClickableMessage(
                message = message,
                isUserMe = isUserMe,
                authorClicked = authorClicked,
            )
        }

        message.image?.let {
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                color = backgroundBubbleColor,
                shape = shape,
            ) {
                Image(
                    painter = painterResource(it),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(160.dp),
                    contentDescription = stringResource(id = R.string.attached_image),
                )
            }
        }
    }
}

@Composable
fun ClickableMessage(message: Message, isUserMe: Boolean, authorClicked: (String) -> Unit) {
    val uriHandler = LocalUriHandler.current

    val styledMessage = messageFormatter(
        text = message.content,
        primary = isUserMe,
    )

    ClickableText(
        text = styledMessage,
        style = MaterialTheme.typography.bodyLarge.copy(color = LocalContentColor.current),
        modifier = Modifier.padding(16.dp),
        onClick = {
            styledMessage
                .getStringAnnotations(start = it, end = it)
                .firstOrNull()
                ?.let { annotation ->
                    when (annotation.tag) {
                        SymbolAnnotationType.LINK.name -> uriHandler.openUri(annotation.item)
                        SymbolAnnotationType.PERSON.name -> authorClicked(annotation.item)
                        else -> Unit
                    }
                }
        },
    )
}