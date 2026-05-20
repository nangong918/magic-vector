package com.vectordemo.ui.view.wechat

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kmp.shared.generated.resources.Res
import com.vectordemo.ui.view.wechat.components.ContactMessageItem
import com.vectordemo.ui.view.wechat.components.WeChatAvatar
import com.vectordemo.viewModel.wechat.ChatSender
import com.vectordemo.viewModel.wechat.WeChatChatMessage
import com.vectordemo.viewModel.wechat.WeChatContact
import com.vectordemo.viewModel.wechat.WeChatIntent
import com.vectordemo.viewModel.wechat.WeChatMoment
import com.vectordemo.viewModel.wechat.WeChatMomentPhoto
import com.vectordemo.viewModel.wechat.WeChatNavAction
import com.vectordemo.viewModel.wechat.WeChatPage
import com.vectordemo.viewModel.wechat.WeChatTab
import com.vectordemo.viewModel.wechat.WeChatTransitionStyle
import com.vectordemo.viewModel.wechat.WeChatUiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun WeChatDemoScreen(
    state: WeChatUiState,
    processIntent: (WeChatIntent) -> Unit,
    onBackToCatalog: () -> Unit,
) {
    AnimatedContent(
        targetState = state.currentPage,
        transitionSpec = { weChatTransitionSpec(state.navAction, state.transitionStyle) },
        label = "wechat-root-nav",
    ) { page ->
        when (page) {
            WeChatPage.Home -> WeChatHomePage(
                state = state,
                processIntent = processIntent,
                onBackToCatalog = onBackToCatalog,
            )
            is WeChatPage.Chat -> WeChatChatPage(
                state = state,
                userId = page.userId,
                processIntent = processIntent,
            )
            is WeChatPage.Profile -> WeChatProfilePage(
                state = state,
                userId = page.userId,
                processIntent = processIntent,
            )
            is WeChatPage.VoiceCall -> WeChatVoiceCallPage(
                state = state,
                userId = page.userId,
                processIntent = processIntent,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun WeChatHomePage(
    state: WeChatUiState,
    processIntent: (WeChatIntent) -> Unit,
    onBackToCatalog: () -> Unit,
) {
    val pagerState = rememberPagerState(
        initialPage = state.activeTab.ordinal,
        pageCount = { WeChatTab.entries.size },
    )

    LaunchedEffect(state.activeTab) {
        if (pagerState.currentPage != state.activeTab.ordinal) {
            pagerState.animateScrollToPage(state.activeTab.ordinal)
        }
    }
    LaunchedEffect(pagerState.currentPage) {
        val tab = WeChatTab.entries[pagerState.currentPage]
        if (tab != state.activeTab) {
            processIntent(WeChatIntent.SelectTab(tab))
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1F1F1F))
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onBackToCatalog) { Text("返回", color = Color.White) }
                Text(
                    text = "WeChat UI Demo",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.width(60.dp))
            }
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF6F6F6))
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                WeChatTab.entries.forEach { tab ->
                    val selected = tab == state.activeTab
                    Text(
                        text = tab.toTitle(),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { processIntent(WeChatIntent.SelectTab(tab)) }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        color = if (selected) Color(0xFF1AAD19) else Color(0xFF777777),
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        },
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) { page ->
            when (WeChatTab.entries[page]) {
                WeChatTab.MESSAGES -> WeChatMessagesPage(state = state, processIntent = processIntent)
                WeChatTab.CONTACTS -> WeChatContactsPage(state = state, processIntent = processIntent)
                WeChatTab.DISCOVER -> WeChatDiscoverPage()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeChatMessagesPage(
    state: WeChatUiState,
    processIntent: (WeChatIntent) -> Unit,
) {
    PullToRefreshBox(
        isRefreshing = state.refreshingMessages,
        onRefresh = { processIntent(WeChatIntent.RefreshMessages) },
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
        ) {
            items(state.messagePreviews, key = { it.contactId }) { preview ->
                val contact = state.contacts.firstOrNull { it.id == preview.contactId }
                ContactMessageItem(
                    preview = preview,
                    contact = contact,
                    onClick = { processIntent(WeChatIntent.OpenChatFromMessage(preview.contactId)) },
                    onAvatarClick = { processIntent(WeChatIntent.OpenProfileFromAvatar(preview.contactId)) },
                )
                Divider(color = Color(0xFFEDEDED))
            }
        }
    }
}

@Composable
private fun WeChatContactsPage(
    state: WeChatUiState,
    processIntent: (WeChatIntent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        OutlinedTextField(
            value = state.contactQuery,
            onValueChange = { processIntent(WeChatIntent.UpdateContactQuery(it)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("搜索联系人") },
        )
        Spacer(modifier = Modifier.height(10.dp))
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(state.filteredContacts, key = { it.id }) { contact ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { processIntent(WeChatIntent.OpenProfileFromContact(contact.id)) }
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    WeChatAvatar(
                        modifier = Modifier.size(42.dp),
                        contact = contact,
                        paletteIndex = 1,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(contact.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            contact.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = "详情",
                        color = Color(0xFF1AAD19),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Divider(color = Color(0xFFF1F1F1))
            }
        }
    }
}

@Composable
private fun WeChatDiscoverPage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("发现页（占位）", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text("主学习重点在消息、聊天和详情页交互。", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun WeChatChatPage(
    state: WeChatUiState,
    userId: String,
    processIntent: (WeChatIntent) -> Unit,
) {
    val contact = remember(state.contacts, userId) { state.contacts.firstOrNull { it.id == userId } }
    var input by remember(userId) { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val showBackToLatest by remember {
        derivedStateOf {
            val visibleInfo = listState.layoutInfo.visibleItemsInfo
            if (visibleInfo.isEmpty()) return@derivedStateOf false
            val lastVisible = visibleInfo.last().index
            val total = listState.layoutInfo.totalItemsCount
            total > 0 && lastVisible < total - 1
        }
    }

    LaunchedEffect(state.scrollToLatestToken, state.visibleChatMessages.size) {
        if (state.visibleChatMessages.isNotEmpty()) {
            listState.animateScrollToItem(state.visibleChatMessages.lastIndex)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1F1F1F))
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { processIntent(WeChatIntent.NavigateBack) }) { Text("返回", color = Color.White) }
                Text(
                    text = contact?.name ?: "聊天",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
                TextButton(onClick = { processIntent(WeChatIntent.StartVoiceCall(userId)) }) {
                    Text("语音", color = Color.White)
                }
            }
            if (state.loadingHistory) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF2F2F2))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("加载历史消息中...", style = MaterialTheme.typography.bodySmall)
                }
            }
            ChatMessageList(
                modifier = Modifier.weight(1f),
                messages = state.visibleChatMessages,
                listState = listState,
                canLoadMoreHistory = state.canLoadMoreHistory,
                onLoadMoreHistory = { processIntent(WeChatIntent.LoadOlderHistory) },
                onAvatarClick = { processIntent(WeChatIntent.OpenProfileFromAvatar(it)) },
                contact = contact,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF7F7F7))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("输入消息") },
                    singleLine = true,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {
                    val msg = input.trim()
                    if (msg.isNotBlank()) {
                        input = ""
                        processIntent(WeChatIntent.SendMessage(msg))
                    }
                }) {
                    Text("发送")
                }
            }
        }

        AnimatedVisibility(
            visible = showBackToLatest,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 72.dp),
        ) {
            Text(
                text = "回到最新消息",
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xCC1AAD19))
                    .clickable {
                        if (state.visibleChatMessages.isNotEmpty()) {
                            scope.launch { listState.animateScrollToItem(state.visibleChatMessages.lastIndex) }
                        }
                    }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                color = Color.White,
            )
        }
    }
}

@Composable
private fun ChatMessageList(
    modifier: Modifier = Modifier,
    messages: List<WeChatChatMessage>,
    listState: LazyListState,
    canLoadMoreHistory: Boolean,
    onLoadMoreHistory: () -> Unit,
    onAvatarClick: (String) -> Unit,
    contact: WeChatContact?,
) {
    var topDragOffset by remember { mutableStateOf(0f) }

    LaunchedEffect(listState.firstVisibleItemIndex, topDragOffset, canLoadMoreHistory) {
        if (listState.firstVisibleItemIndex == 0 && topDragOffset > 80f && canLoadMoreHistory) {
            onLoadMoreHistory()
            topDragOffset = 0f
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFEFEFEF))
            .pointerInput(canLoadMoreHistory) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (listState.firstVisibleItemIndex == 0 && dragAmount > 0 && canLoadMoreHistory) {
                        topDragOffset += dragAmount
                    } else {
                        topDragOffset = 0f
                    }
                }
            },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            if (canLoadMoreHistory) {
                Text(
                    text = "下拉加载更早消息",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF808080),
                )
            }
        }
        itemsIndexed(
            items = messages,
            key = { index, message -> "${message.id}-$index" },
        ) { _, message ->
            val isMe = message.sender == ChatSender.ME
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp),
                horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
                verticalAlignment = Alignment.Top,
            ) {
                if (!isMe) {
                    WeChatAvatar(
                        modifier = Modifier.size(36.dp),
                        contact = contact,
                        paletteIndex = 0,
                        onClick = { onAvatarClick(contact?.id.orEmpty()) },
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Column(horizontalAlignment = if (isMe) Alignment.End else Alignment.Start) {
                    Text(
                        message.timeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF8A8A8A),
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isMe) Color(0xFF95EC69) else Color.White)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                    ) {
                        Text(message.text, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (isMe) {
                    Spacer(modifier = Modifier.width(8.dp))
                    WeChatAvatar(
                        modifier = Modifier.size(36.dp),
                        contact = WeChatContact("me", "我", "自己", listOf(0xFF66BB6AuL)),
                        paletteIndex = 0,
                        onClick = { onAvatarClick("me") },
                    )
                }
            }
        }
        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WeChatProfilePage(
    state: WeChatUiState,
    userId: String,
    processIntent: (WeChatIntent) -> Unit,
) {
    val contact = remember(state.contacts, userId) {
        state.contacts.firstOrNull { it.id == userId } ?: WeChatContact("me", "我", "自己", listOf(0xFF66BB6AuL))
    }
    val moments = state.momentsByUser[userId].orEmpty()
    val avatarPagerState = rememberPagerState(pageCount = { contact.avatarPalette.size.coerceAtLeast(1) })
    var selectedPhoto by remember { mutableStateOf<WeChatMomentPhoto?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .safeContentPadding(),
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF222222))
                    .padding(horizontal = 10.dp, vertical = 10.dp),
            ) {
                TextButton(onClick = { processIntent(WeChatIntent.NavigateBack) }) {
                    Text("返回", color = Color.White)
                }
                Text(
                    text = "用户详情",
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
        item {
            HorizontalPager(
                state = avatarPagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
            ) { page ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(contact.avatarPalette[page].toLong())),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = contact.name,
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }
            }
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(contact.name, style = MaterialTheme.typography.titleLarge)
                    Text(contact.subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(onClick = { processIntent(WeChatIntent.OpenChatFromProfile(userId)) }) { Text("发消息") }
            }
        }
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "朋友圈",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        items(moments, key = { it.id }) { moment ->
            MomentCard(
                moment = moment,
                onLike = { processIntent(WeChatIntent.ToggleMomentLike(userId, moment.id)) },
                onCommentSubmit = { text ->
                    processIntent(WeChatIntent.SubmitMomentComment(userId, moment.id, text))
                },
                onPhotoClick = { selectedPhoto = it },
            )
        }
        item { Spacer(modifier = Modifier.height(14.dp)) }
    }

    if (selectedPhoto != null) {
        MomentPhotoPreviewDialog(photo = selectedPhoto!!, onDismiss = { selectedPhoto = null })
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MomentCard(
    moment: WeChatMoment,
    onLike: () -> Unit,
    onCommentSubmit: (String) -> Unit,
    onPhotoClick: (WeChatMomentPhoto) -> Unit,
) {
    var commentInput by remember(moment.id) { mutableStateOf("") }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(moment.text, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            MomentPhotoGrid(photos = moment.photos, onPhotoClick = onPhotoClick)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(moment.dateLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (moment.liked) "取消赞(${moment.likeCount})" else "点赞(${moment.likeCount})",
                        color = if (moment.liked) Color(0xFF1AAD19) else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.clickable(onClick = onLike),
                    )
                    Text(text = "评论(${moment.comments.size})")
                }
            }
            if (moment.comments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF2F2F2))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    moment.comments.forEach { comment ->
                        Text(
                            text = "${comment.authorName}: ${comment.content}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (comment.mine) Color(0xFF1AAD19) else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = commentInput,
                    onValueChange = { commentInput = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("写评论...") },
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {
                    val text = commentInput.trim()
                    if (text.isNotBlank()) {
                        commentInput = ""
                        onCommentSubmit(text)
                    }
                }) {
                    Text("发送")
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MomentPhotoGrid(
    photos: List<WeChatMomentPhoto>,
    onPhotoClick: (WeChatMomentPhoto) -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val cellSize = (maxWidth - 12.dp) / 3
        val rows = (photos.size + 2) / 3
        val gridHeight = (cellSize * rows) + (6.dp * (rows - 1).coerceAtLeast(0))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            userScrollEnabled = false,
            modifier = Modifier
                .fillMaxWidth()
                .height(gridHeight),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(photos, key = { it.label }) { photo ->
                AsyncImage(
                    model = Res.getUri(photo.resourcePath),
                    contentDescription = photo.label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(cellSize)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onPhotoClick(photo) },
                )
            }
        }
    }
}

@Composable
private fun MomentPhotoPreviewDialog(
    photo: WeChatMomentPhoto,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = Res.getUri(photo.resourcePath),
            contentDescription = photo.label,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp)),
        )
        Text(
            text = "点击空白关闭",
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-24).dp),
        )
    }
}

@Composable
private fun WeChatVoiceCallPage(
    state: WeChatUiState,
    userId: String,
    processIntent: (WeChatIntent) -> Unit,
) {
    val contact = state.contacts.firstOrNull { it.id == userId }
    val duration = formatDuration(state.callDurationSeconds)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212)),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 24.dp)
                .size(48.dp)
                .clip(CircleShape)
                .clickable { processIntent(WeChatIntent.NavigateBack) }
                .background(Color(0x33FFFFFF)),
            contentAlignment = Alignment.Center,
        ) {
            Text("返回", color = Color.White, style = MaterialTheme.typography.labelMedium)
        }

        WeChatAvatar(
            modifier = Modifier
                .align(Alignment.Center)
                .size(180.dp),
            contact = contact,
            paletteIndex = 2,
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 90.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(contact?.name ?: "语音通话", color = Color.White, style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Text(duration, color = Color(0xFFDADADA))
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 34.dp),
        ) {
            Button(
                onClick = { processIntent(WeChatIntent.ToggleMute) },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(120.dp),
            ) {
                Text(if (state.callMuted) "取消静音" else "静音")
            }
            Button(
                onClick = { processIntent(WeChatIntent.EndCall) },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(120.dp),
            ) {
                Text("挂断")
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
private fun weChatTransitionSpec(
    action: WeChatNavAction,
    style: WeChatTransitionStyle,
) = when (style) {
    WeChatTransitionStyle.MESSAGE_ZOOM -> {
        if (action == WeChatNavAction.PUSH) {
            (fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)) +
                scaleIn(initialScale = 0.85f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))) togetherWith
                (fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium)) +
                    scaleOut(targetScale = 1.03f, animationSpec = spring(stiffness = Spring.StiffnessLow)))
        } else {
            (fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)) +
                scaleIn(initialScale = 1.04f, animationSpec = spring(stiffness = Spring.StiffnessLow))) togetherWith
                (fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium)) +
                    scaleOut(targetScale = 0.86f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow)))
        }
    }
    WeChatTransitionStyle.AVATAR_ZOOM -> {
        if (action == WeChatNavAction.PUSH) {
            (fadeIn() + scaleIn(initialScale = 0.7f)) togetherWith (fadeOut() + scaleOut(targetScale = 1.06f))
        } else {
            (fadeIn() + scaleIn(initialScale = 1.05f)) togetherWith (fadeOut() + scaleOut(targetScale = 0.75f))
        }
    }
    WeChatTransitionStyle.NORMAL -> {
        if (action == WeChatNavAction.PUSH) {
            fadeIn() togetherWith fadeOut()
        } else {
            fadeIn() togetherWith fadeOut()
        }
    }
}

private fun WeChatTab.toTitle(): String = when (this) {
    WeChatTab.MESSAGES -> "消息"
    WeChatTab.CONTACTS -> "通讯录"
    WeChatTab.DISCOVER -> "发现"
}

private fun formatDuration(totalSeconds: Int): String {
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    val mm = if (m >= 10) "$m" else "0$m"
    val ss = if (s >= 10) "$s" else "0$s"
    return "$mm:$ss"
}
