package com.vectordemo.viewModel.wechat

import androidx.lifecycle.viewModelScope
import com.vectordemo.domain.platform.currentTimeMillis
import com.vectordemo.viewModel.BaseVm
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.random.Random

private const val CURRENT_USER_ID = "me"
private const val INITIAL_VISIBLE_HISTORY = 12
private const val HISTORY_PAGE_SIZE = 8

enum class WeChatTab {
    MESSAGES,
    CONTACTS,
    DISCOVER,
}

enum class WeChatNavAction {
    PUSH,
    POP,
}

enum class WeChatTransitionStyle {
    NORMAL,
    MESSAGE_ZOOM,
    AVATAR_ZOOM,
}

sealed interface WeChatPage {
    val key: String

    data object Home : WeChatPage {
        override val key: String = "home"
    }

    data class Chat(val userId: String) : WeChatPage {
        override val key: String = "chat:$userId"
    }

    data class Profile(val userId: String) : WeChatPage {
        override val key: String = "profile:$userId"
    }

    data class VoiceCall(val userId: String) : WeChatPage {
        override val key: String = "voice:$userId"
    }
}

data class WeChatContact(
    val id: String,
    val name: String,
    val subtitle: String,
    val avatarPalette: List<ULong>,
)

data class WeChatMessagePreview(
    val contactId: String,
    val preview: String,
    val timeLabel: String,
    val unreadCount: Int,
)

enum class ChatSender {
    ME,
    CONTACT,
}

data class WeChatChatMessage(
    val id: String,
    val sender: ChatSender,
    val text: String,
    val timeLabel: String,
)

data class WeChatMomentPhoto(
    val label: String,
    val resourcePath: String,
)

data class WeChatMomentComment(
    val id: String,
    val authorName: String,
    val mine: Boolean,
    val content: String,
)

data class WeChatMoment(
    val id: String,
    val text: String,
    val dateLabel: String,
    val photos: List<WeChatMomentPhoto>,
    val liked: Boolean,
    val likeCount: Int,
    val comments: List<WeChatMomentComment>,
)

data class WeChatUiState(
    val activeTab: WeChatTab = WeChatTab.MESSAGES,
    val currentPage: WeChatPage = WeChatPage.Home,
    val canGoBackInDemo: Boolean = false,
    val navAction: WeChatNavAction = WeChatNavAction.PUSH,
    val transitionStyle: WeChatTransitionStyle = WeChatTransitionStyle.NORMAL,
    val contacts: List<WeChatContact> = sampleContacts(),
    val messagePreviews: List<WeChatMessagePreview> = sampleMessagePreviews(),
    val contactQuery: String = "",
    val refreshingMessages: Boolean = false,
    val activeChatUserId: String? = null,
    val visibleChatMessages: List<WeChatChatMessage> = emptyList(),
    val loadingHistory: Boolean = false,
    val canLoadMoreHistory: Boolean = false,
    val scrollToLatestToken: Int = 0,
    val activeProfileUserId: String? = null,
    val momentsByUser: Map<String, List<WeChatMoment>> = sampleMoments(),
    val activeCallUserId: String? = null,
    val callDurationSeconds: Int = 0,
    val callMuted: Boolean = false,
) {
    val filteredContacts: List<WeChatContact>
        get() = if (contactQuery.isBlank()) {
            contacts
        } else {
            contacts.filter {
                it.name.contains(contactQuery, ignoreCase = true) ||
                    it.subtitle.contains(contactQuery, ignoreCase = true)
            }
        }
}

sealed class WeChatIntent {
    data object Initialize : WeChatIntent()
    data class SelectTab(val tab: WeChatTab) : WeChatIntent()
    data object RefreshMessages : WeChatIntent()
    data class OpenChatFromMessage(val userId: String) : WeChatIntent()
    data class OpenProfileFromAvatar(val userId: String) : WeChatIntent()
    data class OpenProfileFromContact(val userId: String) : WeChatIntent()
    data class OpenChatFromProfile(val userId: String) : WeChatIntent()
    data class UpdateContactQuery(val query: String) : WeChatIntent()
    data class SendMessage(val text: String) : WeChatIntent()
    data object LoadOlderHistory : WeChatIntent()
    data class ToggleMomentLike(val userId: String, val momentId: String) : WeChatIntent()
    data class SubmitMomentComment(val userId: String, val momentId: String, val content: String) : WeChatIntent()
    data class StartVoiceCall(val userId: String) : WeChatIntent()
    data object ToggleMute : WeChatIntent()
    data object EndCall : WeChatIntent()
    data object NavigateBack : WeChatIntent()
}

sealed class WeChatEffect {
    data class ShowToast(val message: String) : WeChatEffect()
}

class WeChatDemoVm : BaseVm() {
    private val _uiState = MutableStateFlow(WeChatUiState())
    val uiState: StateFlow<WeChatUiState> = _uiState.asStateFlow()

    private val _effect = Channel<WeChatEffect>(Channel.BUFFERED)
    val effect: Flow<WeChatEffect> = _effect.receiveAsFlow()

    private val pageStack = mutableListOf<WeChatPage>(WeChatPage.Home)
    private val chatHistoryByUser = mutableMapOf<String, MutableList<WeChatChatMessage>>()
    private val loadedHistoryCountByUser = mutableMapOf<String, Int>()
    private var callTimerJob: Job? = null
    private var messageIdSeed: Long = currentTimeMillis()

    init {
        seedFakeChatData()
    }

    fun processIntent(intent: WeChatIntent) {
        when (intent) {
            WeChatIntent.Initialize -> initialize()
            is WeChatIntent.SelectTab -> _uiState.update { it.copy(activeTab = intent.tab) }
            WeChatIntent.RefreshMessages -> refreshMessages()
            is WeChatIntent.OpenChatFromMessage -> openChat(intent.userId, WeChatTransitionStyle.MESSAGE_ZOOM)
            is WeChatIntent.OpenProfileFromAvatar -> openProfile(intent.userId, WeChatTransitionStyle.AVATAR_ZOOM)
            is WeChatIntent.OpenProfileFromContact -> openProfile(intent.userId, WeChatTransitionStyle.NORMAL)
            is WeChatIntent.OpenChatFromProfile -> openChat(intent.userId, WeChatTransitionStyle.MESSAGE_ZOOM)
            is WeChatIntent.UpdateContactQuery -> _uiState.update { it.copy(contactQuery = intent.query) }
            is WeChatIntent.SendMessage -> sendMessage(intent.text)
            WeChatIntent.LoadOlderHistory -> loadOlderHistory()
            is WeChatIntent.ToggleMomentLike -> toggleMomentLike(intent.userId, intent.momentId)
            is WeChatIntent.SubmitMomentComment -> submitMomentComment(intent.userId, intent.momentId, intent.content)
            is WeChatIntent.StartVoiceCall -> startVoiceCall(intent.userId)
            WeChatIntent.ToggleMute -> _uiState.update { it.copy(callMuted = !it.callMuted) }
            WeChatIntent.EndCall -> endVoiceCall()
            WeChatIntent.NavigateBack -> navigateBack()
        }
    }

    private fun initialize() {
        if (pageStack.size != 1 || pageStack.first() !is WeChatPage.Home) {
            pageStack.clear()
            pageStack += WeChatPage.Home
        }
        stopCallTimer()
        publishNavState(WeChatNavAction.POP, WeChatTransitionStyle.NORMAL)
        _uiState.update {
            it.copy(
                activeChatUserId = null,
                activeProfileUserId = null,
                activeCallUserId = null,
                callDurationSeconds = 0,
                callMuted = false,
            )
        }
    }

    private fun refreshMessages() {
        if (_uiState.value.refreshingMessages) return
        viewModelScope.launch {
            _uiState.update { it.copy(refreshingMessages = true) }
            delay(2000)
            _uiState.update { it.copy(refreshingMessages = false) }
            emitEffect(WeChatEffect.ShowToast("刷新成功"))
        }
    }

    private fun openChat(userId: String, transitionStyle: WeChatTransitionStyle) {
        pushOrReuse(WeChatPage.Chat(userId), transitionStyle)
        markContactRead(userId)
        refreshVisibleHistory(userId = userId, shouldScrollToLatest = true)
    }

    private fun openProfile(userId: String, transitionStyle: WeChatTransitionStyle) {
        pushOrReuse(WeChatPage.Profile(userId), transitionStyle)
        _uiState.update { it.copy(activeProfileUserId = userId) }
    }

    private fun startVoiceCall(userId: String) {
        pushOrReuse(WeChatPage.VoiceCall(userId), WeChatTransitionStyle.NORMAL)
        _uiState.update {
            it.copy(
                activeCallUserId = userId,
                callDurationSeconds = 0,
                callMuted = false,
            )
        }
        startCallTimer()
    }

    private fun endVoiceCall() {
        stopCallTimer()
        navigateBack()
    }

    private fun navigateBack() {
        if (pageStack.size <= 1) return
        val popped = pageStack.removeAt(pageStack.lastIndex)
        if (popped is WeChatPage.VoiceCall) {
            stopCallTimer()
        }
        val style = when (popped) {
            is WeChatPage.Chat -> WeChatTransitionStyle.MESSAGE_ZOOM
            is WeChatPage.Profile -> WeChatTransitionStyle.AVATAR_ZOOM
            else -> WeChatTransitionStyle.NORMAL
        }
        publishNavState(WeChatNavAction.POP, style)
        when (val current = pageStack.last()) {
            WeChatPage.Home -> _uiState.update {
                it.copy(
                    activeChatUserId = null,
                    activeProfileUserId = null,
                    activeCallUserId = null,
                )
            }
            is WeChatPage.Chat -> {
                markContactRead(current.userId)
                refreshVisibleHistory(current.userId, shouldScrollToLatest = false)
            }
            is WeChatPage.Profile -> _uiState.update {
                it.copy(
                    activeProfileUserId = current.userId,
                    activeCallUserId = null,
                )
            }
            is WeChatPage.VoiceCall -> {
                _uiState.update { it.copy(activeCallUserId = current.userId) }
                startCallTimer()
            }
        }
    }

    private fun pushOrReuse(nextPage: WeChatPage, transitionStyle: WeChatTransitionStyle) {
        val existingIndex = pageStack.indexOfFirst { it.key == nextPage.key }
        if (existingIndex == pageStack.lastIndex) return
        if (existingIndex >= 0) {
            while (pageStack.size > existingIndex + 1) {
                val removed = pageStack.removeAt(pageStack.lastIndex)
                if (removed is WeChatPage.VoiceCall) {
                    stopCallTimer()
                }
            }
            publishNavState(WeChatNavAction.POP, transitionStyle)
        } else {
            pageStack += nextPage
            publishNavState(WeChatNavAction.PUSH, transitionStyle)
        }
    }

    private fun sendMessage(rawText: String) {
        val userId = _uiState.value.activeChatUserId ?: return
        val text = rawText.trim()
        if (text.isBlank()) return

        val history = chatHistoryByUser.getOrPut(userId) { mutableListOf() }
        val loadedBefore = loadedHistoryCountByUser[userId] ?: INITIAL_VISIBLE_HISTORY
        history += createChatMessage(ChatSender.ME, text)
        loadedHistoryCountByUser[userId] = min(history.size, loadedBefore + 1)
        updateMessagePreview(userId = userId, latestText = text, incoming = false)
        refreshVisibleHistory(userId = userId, shouldScrollToLatest = true)

        viewModelScope.launch {
            delay(650)
            val autoReply = "收到：$text"
            history += createChatMessage(ChatSender.CONTACT, autoReply)
            val loadedNow = loadedHistoryCountByUser[userId] ?: INITIAL_VISIBLE_HISTORY
            loadedHistoryCountByUser[userId] = min(history.size, loadedNow + 1)
            updateMessagePreview(userId = userId, latestText = autoReply, incoming = true)
            refreshVisibleHistory(userId = userId, shouldScrollToLatest = true)
        }
    }

    private fun loadOlderHistory() {
        val userId = _uiState.value.activeChatUserId ?: return
        val all = chatHistoryByUser[userId].orEmpty()
        val loaded = loadedHistoryCountByUser[userId] ?: INITIAL_VISIBLE_HISTORY
        if (loaded >= all.size || _uiState.value.loadingHistory) return

        viewModelScope.launch {
            _uiState.update { it.copy(loadingHistory = true) }
            delay(900)
            loadedHistoryCountByUser[userId] = min(all.size, loaded + HISTORY_PAGE_SIZE)
            refreshVisibleHistory(userId = userId, shouldScrollToLatest = false)
            _uiState.update { it.copy(loadingHistory = false) }
            if (loadedHistoryCountByUser[userId] == all.size) {
                emitEffect(WeChatEffect.ShowToast("历史消息已全部加载"))
            }
        }
    }

    private fun refreshVisibleHistory(userId: String, shouldScrollToLatest: Boolean) {
        val all = chatHistoryByUser[userId].orEmpty()
        val loaded = loadedHistoryCountByUser.getOrPut(userId) {
            min(INITIAL_VISIBLE_HISTORY, all.size)
        }.coerceAtLeast(min(INITIAL_VISIBLE_HISTORY, all.size))
        val visible = if (loaded >= all.size) all else all.takeLast(loaded)
        _uiState.update {
            it.copy(
                activeChatUserId = userId,
                visibleChatMessages = visible,
                canLoadMoreHistory = loaded < all.size,
                scrollToLatestToken = if (shouldScrollToLatest) it.scrollToLatestToken + 1 else it.scrollToLatestToken,
            )
        }
    }

    private fun toggleMomentLike(userId: String, momentId: String) {
        val updated = _uiState.value.momentsByUser.toMutableMap()
        val rows = updated[userId].orEmpty().map { moment ->
            if (moment.id != momentId) {
                moment
            } else if (moment.liked) {
                moment.copy(liked = false, likeCount = (moment.likeCount - 1).coerceAtLeast(0))
            } else {
                moment.copy(liked = true, likeCount = moment.likeCount + 1)
            }
        }
        updated[userId] = rows
        _uiState.update { it.copy(momentsByUser = updated) }
    }

    private fun submitMomentComment(userId: String, momentId: String, rawContent: String) {
        val content = rawContent.trim()
        if (content.isBlank()) {
            emitEffect(WeChatEffect.ShowToast("评论不能为空"))
            return
        }

        val myComment = WeChatMomentComment(
            id = "mc-${nextMessageId()}",
            authorName = "我",
            mine = true,
            content = content,
        )
        updateMomentComments(userId, momentId) { it + myComment }

        viewModelScope.launch {
            delay(700)
            val replier = _uiState.value.contacts.firstOrNull { it.id == userId }?.name ?: "对方"
            val autoReply = WeChatMomentComment(
                id = "mc-${nextMessageId()}",
                authorName = replier,
                mine = false,
                content = "收到你的评论：$content",
            )
            updateMomentComments(userId, momentId) { it + autoReply }
        }
    }

    private fun updateMomentComments(
        userId: String,
        momentId: String,
        update: (List<WeChatMomentComment>) -> List<WeChatMomentComment>,
    ) {
        val updated = _uiState.value.momentsByUser.toMutableMap()
        val rows = updated[userId].orEmpty().map { moment ->
            if (moment.id == momentId) {
                moment.copy(comments = update(moment.comments))
            } else {
                moment
            }
        }
        updated[userId] = rows
        _uiState.update { it.copy(momentsByUser = updated) }
    }

    private fun updateMessagePreview(userId: String, latestText: String, incoming: Boolean) {
        val old = _uiState.value.messagePreviews
        val target = old.firstOrNull { it.contactId == userId } ?: return
        val updatedTarget = target.copy(
            preview = latestText,
            timeLabel = nowTimeLabel(),
            unreadCount = if (incoming) target.unreadCount + 1 else 0,
        )
        val newRows = listOf(updatedTarget) + old.filterNot { it.contactId == userId }
        _uiState.update { it.copy(messagePreviews = newRows) }
    }

    private fun markContactRead(userId: String) {
        _uiState.update { state ->
            state.copy(
                messagePreviews = state.messagePreviews.map {
                    if (it.contactId == userId) it.copy(unreadCount = 0) else it
                },
            )
        }
    }

    private fun startCallTimer() {
        stopCallTimer()
        callTimerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                _uiState.update { it.copy(callDurationSeconds = it.callDurationSeconds + 1) }
            }
        }
    }

    private fun stopCallTimer() {
        callTimerJob?.cancel()
        callTimerJob = null
    }

    private fun publishNavState(action: WeChatNavAction, style: WeChatTransitionStyle) {
        val currentPage = pageStack.lastOrNull() ?: WeChatPage.Home
        _uiState.update {
            it.copy(
                currentPage = currentPage,
                canGoBackInDemo = pageStack.size > 1,
                navAction = action,
                transitionStyle = style,
            )
        }
    }

    private fun seedFakeChatData() {
        val contacts = _uiState.value.contacts
        contacts.forEach { contact ->
            val seed = Random(contact.id.hashCode())
            val allMessages = buildList {
                repeat(24) { index ->
                    val fromContact = index % 2 == 0
                    val sender = if (fromContact) ChatSender.CONTACT else ChatSender.ME
                    add(
                        createChatMessage(
                            sender = sender,
                            text = if (fromContact) {
                                listOf("今晚有空吗", "这个需求我看过了", "明天一起吃午饭？", "你看下这个截图").random(seed)
                            } else {
                                listOf("收到", "我来处理", "OK，稍后回复你", "没问题").random(seed)
                            },
                            hour = 9 + (index / 4),
                            minute = (index * 7) % 60,
                        ),
                    )
                }
            }.toMutableList()
            chatHistoryByUser[contact.id] = allMessages
            loadedHistoryCountByUser[contact.id] = min(INITIAL_VISIBLE_HISTORY, allMessages.size)
        }
    }

    private fun createChatMessage(
        sender: ChatSender,
        text: String,
        hour: Int = 20,
        minute: Int = Random.nextInt(0, 60),
    ): WeChatChatMessage = WeChatChatMessage(
        id = nextMessageId(),
        sender = sender,
        text = text,
        timeLabel = "${toTwoDigits(hour)}:${toTwoDigits(minute)}",
    )

    private fun nowTimeLabel(): String {
        val minute = ((currentTimeMillis() / 60000) % 60).toInt()
        val hour = ((currentTimeMillis() / 3600000) % 24).toInt()
        return "${toTwoDigits(hour)}:${toTwoDigits(minute)}"
    }

    private fun toTwoDigits(value: Int): String = if (value >= 10) "$value" else "0$value"

    private fun nextMessageId(): String {
        messageIdSeed += 1L
        return messageIdSeed.toString()
    }

    private fun emitEffect(effect: WeChatEffect) {
        viewModelScope.launch { _effect.send(effect) }
    }
}

private fun sampleContacts(): List<WeChatContact> = listOf(
    WeChatContact("u_lina", "Lina", "产品经理", listOf(0xFFE57373uL, 0xFFF06292uL, 0xFFBA68C8uL)),
    WeChatContact("u_chen", "Chen", "Android 开发", listOf(0xFF4FC3F7uL, 0xFF4DB6ACuL, 0xFF81C784uL)),
    WeChatContact("u_mia", "Mia", "设计师", listOf(0xFFFFB74DuL, 0xFFA1887FuL, 0xFFFF8A65uL)),
    WeChatContact("u_tao", "Tao", "测试工程师", listOf(0xFF64B5F6uL, 0xFF7986CBuL, 0xFF90A4AEuL)),
    WeChatContact("u_sam", "Sam", "后端开发", listOf(0xFF81C784uL, 0xFFAED581uL, 0xFFDCE775uL)),
    WeChatContact("u_eva", "Eva", "运营", listOf(0xFFFFD54FuL, 0xFFFFB74DuL, 0xFFFF8A65uL)),
)

private fun sampleMessagePreviews(): List<WeChatMessagePreview> = listOf(
    WeChatMessagePreview("u_lina", "明早 10 点评审别忘了", "21:08", 2),
    WeChatMessagePreview("u_chen", "我把 PR 发你了", "20:36", 1),
    WeChatMessagePreview("u_mia", "九宫格素材在群里", "19:42", 0),
    WeChatMessagePreview("u_tao", "回归通过，今晚可提测", "18:20", 3),
    WeChatMessagePreview("u_sam", "接口限流已上线", "17:11", 0),
    WeChatMessagePreview("u_eva", "活动海报今晚发布", "16:45", 0),
)

private fun sampleMoments(): Map<String, List<WeChatMoment>> = mapOf(
    "u_lina" to listOf(
        WeChatMoment(
            id = "m_lina_1",
            text = "今天把需求评审完了，大家辛苦了。",
            dateLabel = "05-19",
            photos = sampleMomentPhotos("评审", 5),
            liked = false,
            likeCount = 16,
            comments = listOf(
                WeChatMomentComment("c_lina_1", "Chen", false, "节奏很好"),
                WeChatMomentComment("c_lina_2", "Mia", false, "赞同这个方案"),
                WeChatMomentComment("c_lina_3", "我", true, "确实很高效"),
            ),
        ),
        WeChatMoment(
            id = "m_lina_2",
            text = "周末爬山，天气不错。",
            dateLabel = "05-17",
            photos = sampleMomentPhotos("爬山", 9),
            liked = true,
            likeCount = 29,
            comments = listOf(
                WeChatMomentComment("c_lina_4", "Tao", false, "风景很棒"),
                WeChatMomentComment("c_lina_5", "Sam", false, "下次一起"),
            ),
        ),
    ),
    "u_chen" to listOf(
        WeChatMoment(
            id = "m_chen_1",
            text = "Compose 动效终于调顺了。",
            dateLabel = "05-18",
            photos = sampleMomentPhotos("动效", 6),
            liked = false,
            likeCount = 9,
            comments = listOf(
                WeChatMomentComment("c_chen_1", "Lina", false, "辛苦啦"),
            ),
        ),
    ),
)

private fun sampleMomentPhotos(prefix: String, count: Int): List<WeChatMomentPhoto> {
    val resources = listOf(
        "drawable/android_studio_logo.png",
        "drawable/vector.png",
    )
    return (1..count).map {
        WeChatMomentPhoto(
            label = "$prefix$it",
            resourcePath = resources[(it - 1) % resources.size],
        )
    }
}
