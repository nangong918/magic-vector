package com.magicvector.manager.event

import android.util.Log
import com.magicvector.domain.event.EventSource
import com.magicvector.utils.sort.PageDirection
import com.magicvector.utils.sort.SortItem
import com.magicvector.utils.sort.SortMode
import com.magicvector.utils.sort.SortUtil
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 抽象事件管理器 - 统一管理列表状态和事件分发
 *
 * 数据源处理策略：
 * 1. 用户主动操作 (UI层) → prependTop：实时操作，添加/更新到列表最上层
 * 2. Http全量请求 (RemoteApi) → replaceAll：最新数据，直接替换整个列表
 * 3. Http分页请求 (RemoteApi) → insertOrdered：历史数据，按序插入
 * 4. Ws长连接消息 (RealtimeChatController) → prependTop：实时最新消息，添加最上层
 * 5. 无网络+有内存缓存 (LocalSource分页) → insertOrdered：基于缓存时间戳分页查询，按序插入
 * 6. 无网络+无内存缓存 (LocalSource全量) → replaceAll：Room全量查询，直接替换
 * 7. 名称排序场景 → insertOrdered：按名称升序/降序插入
 *
 * @param TItem 列表项类型，必须实现 SortItem 接口以支持统一排序
 * @param TEvent 事件类型
 * @param defaultSortMode 默认排序模式（默认时间降序：新到旧）
 */
abstract class AbstractEventManager<TItem : SortItem>(
    replay: Int = 1,
    extraBufferCapacity: Int = 64
) {

    companion object {
        private val TAG = AbstractEventManager::class.simpleName
    }

    private val reducerLock = ReentrantLock()
    private val _items = MutableStateFlow<List<TItem>>(emptyList())
    val items: StateFlow<List<TItem>> = _items.asStateFlow()
    private val _defaultSortMode: MutableStateFlow<SortMode> = MutableStateFlow(SortMode.TimestampSort.desc())
    val defaultSortMode: StateFlow<SortMode> = _defaultSortMode.asStateFlow()

    private val _events = MutableSharedFlow<EventSource>(
        replay = replay,
        extraBufferCapacity = extraBufferCapacity
    )
    val events: SharedFlow<EventSource> = _events.asSharedFlow()

    // ========== 基础工具方法 ==========

    /**
     * 获取当前列表快照（线程安全）
     */
    protected fun snapshotItems(): List<TItem> = reducerLock.withLock {
        _items.value.toList()
    }

    /**
     * 发送事件（内部使用）
     */
    private fun emitEvent(event: EventSource) {
        _events.tryEmit(event)
    }

    // ========== 核心操作方法 ==========

    /**
     * 全量替换列表（用于Http全量请求、本地全量查询）
     * 对应需求2、6
     *
     * @param newItems 新列表
     * @param event 变更事件
     * @return 替换后的列表
     */
    protected fun replaceAll(
        newItems: List<TItem>,
        event: EventSource
    ): List<TItem> = reducerLock.withLock {
        _items.value = newItems.toList()
        emitEvent(event)
        _items.value
    }

    /**
     * 置顶插入（用于用户主动操作、Ws新消息）
     * 对应需求1、4
     * 假设：items本身是有序的（如按时间倒序），直接插入到列表顶部
     *
     * @param items 待插入的项列表（本身有序）
     * @param event 变更事件
     * @return 插入后的列表
     */
    protected fun prependTop(
        items: List<TItem>,
        event: EventSource
    ): List<TItem> = reducerLock.withLock {
        val next = _items.value.toMutableList()
        // 在顶部插入所有项（保持items本身的顺序）
        next.addAll(0, items)
        _items.value = next
        emitEvent(event)
        _items.value
    }

    /**
     * 置底插入 - 用于发送新消息等场景
     * 假设：调用时列表已经是有序的，新消息直接追加到底部
     *
     * @param items 待插入的项列表
     * @param event 变更事件
     * @return 插入后的列表
     */
    protected fun appendBottom(
        items: List<TItem>,
        event: EventSource
    ): List<TItem> = reducerLock.withLock {
        val next = _items.value.toMutableList()
        next.addAll(items)  // 直接追加到底部，不查重，不排序
        _items.value = next
        emitEvent(event)
        _items.value
    }

    /**
     * 有序插入历史数据（用于Http分页请求、本地分页查询）
     *
     * 时间复杂度：根据 SortUtil.insertOrdered 的实现决定
     * - 小批量数据：逐个插入 O(m * n)
     * - 大批量数据：建议使用 insertOrderedBatch
     *
     * @param items 待插入的列表
     * @param mode 排序模式（默认使用管理器的 defaultSortMode）
     * @param conflictResolver UID冲突时的解决策略（默认覆盖）
     * @param event 变更事件
     * @return 插入后的列表
     */
    protected fun insertOrderedBatch(
        items: List<TItem>,
        mode: SortMode = defaultSortMode.value,
        conflictResolver: (existing: TItem, new: TItem) -> TItem = { _, new -> new },
        event: EventSource
    ): List<TItem> = reducerLock.withLock {
        val next = _items.value.toMutableList()

        // 调用 SortUtil 的插入方法
        SortUtil.insertOrderedBatch(
            newItems = items,
            sortedList = next,
            mode = mode,
            conflictResolver = conflictResolver
        )

        _items.value = next
        emitEvent(event)
        _items.value
    }

    /**
     * 批量有序插入（整体排序版）- 适用于大量数据
     *
     * 时间复杂度：O((n+m)log(n+m))
     * 使用场景：分页加载、全量刷新等大批量数据插入
     *
     * @param items 待插入的列表
     * @param mode 排序模式（默认使用管理器的 defaultSortMode）
     * @param event 变更事件
     * @return 插入后的列表
     */
    protected fun insertOrderedBatch(
        items: List<TItem>,
        mode: SortMode = defaultSortMode.value,
        event: EventSource
    ): List<TItem> = reducerLock.withLock {
        val next = _items.value.toMutableList()

        // 调用 SortUtil 的批量插入方法
        SortUtil.insertOrderedBatch(
            newItems = items,
            sortedList = next,
            mode = mode
        )

        _items.value = next
        emitEvent(event)
        _items.value
    }

    /**
     * 移除匹配的项
     *
     * @param matcher 匹配规则
     * @param event 变更事件
     * @return 移除后的列表
     */
    protected fun remove(
        matcher: (TItem) -> Boolean,
        event: EventSource
    ): List<TItem> = reducerLock.withLock {
        _items.value = _items.value.filterNot(matcher)
        emitEvent(event)
        _items.value
    }

    /**
     * 更新指定 uid 的项
     * 因为 uid 是唯一的，所以这是精确更新
     *
     * @param uid 要更新的项的唯一标识
     * @param updater 更新函数
     * @param event 变更事件
     * @param resort 更新后是否重新排序（默认false）
     * @param mode 排序模式（当resort=true时使用）
     * @return true 表示有项被更新
     */
    protected fun updateByUid(
        uid: Long,
        updater: (TItem) -> TItem,
        event: EventSource,
        resort: Boolean = false,
        mode: SortMode = defaultSortMode.value
    ): Boolean = reducerLock.withLock {
        val next = _items.value.toMutableList()
        val index = next.indexOfFirst { it.getUid() == uid }

        if (index >= 0) {
            val updatedItem = updater(next[index])
            next[index] = updatedItem

            if (resort) {
                // 重新排序整个列表
                SortUtil.insertOrderedBatch(
                    newItems = emptyList(),
                    sortedList = next,
                    mode = mode
                )
            }

            _items.value = next
            emitEvent(event)
            true
        } else {
            false
        }
    }

    /**
     * 清空列表
     *
     * @param event 变更事件
     * @return 清空后的列表（空列表）
     */
    protected fun clear(event: EventSource): List<TItem> = reducerLock.withLock {
        _items.value = emptyList()
        emitEvent(event)
        _items.value
    }

    /// ========== 抽象方法（子类必须实现数据加载逻辑） ==========

    /**
     * Http全量查询（有网络时）
     */
    protected abstract suspend fun loadFromRemoteFull(sortMode: SortMode): List<TItem>

    /**
     * Http分页查询（有网络时）
     * @param sortMode 排序模式
     * @param direction 查询方向（UP向上查更早的，DOWN向下查更新的）
     * @param limit 查询条数
     * @param cursor 游标值（向上查传第一个元素的排序值，向下查传最后一个元素的排序值，首次传null）
     */
    protected abstract suspend fun loadFromRemotePage(
        sortMode: SortMode,
        direction: PageDirection,
        limit: Int,
        cursor: Any?
    ): List<TItem>

    /**
     * Room全量查询（无网络时）
     */
    protected abstract suspend fun loadFromLocalFull(sortMode: SortMode): List<TItem>

    /**
     * Room分页查询（无网络时）
     */
    protected abstract suspend fun loadFromLocalPage(
        sortMode: SortMode,
        direction: PageDirection,
        limit: Int,
        cursor: Any?
    ): List<TItem>


    // ========== 公开的业务方法（供UI层调用） ==========

    /**
     * 用户添加一条数据到: 缓存，数据库，服务器
     */
    open suspend fun onUserAddOne(item: TItem) {
        // 1. 获取当前排序模式
        val currentSortMode = defaultSortMode.value

        // 2. 统一判断当前排序是升序还是降序
        val isDesc = when (currentSortMode) {
            is SortMode.UidSort -> currentSortMode.isDesc
            is SortMode.TimestampSort -> currentSortMode.isDesc
            is SortMode.StringSort -> currentSortMode.isDesc
        }

        // 3. 根据升/降序决定调用的方法
        if (isDesc) {
            // 降序：最新item数值最大 → 插入顶部
            prependTop(listOf(item), EventSource.UserAction.Add)
        } else {
            // 升序：最新item数值最大 → 插入底部
            appendBottom(listOf(item), EventSource.UserAction.Add)
        }
    }

    /**
     * 用户添加多条数据到: 缓存，数据库，服务器
     */
    open suspend fun onUserUpsert(items: List<TItem>) {
        if (items.isEmpty()) return
        insertOrderedBatch(items, defaultSortMode.value, EventSource.UserAction.Add)
     }

    /**
     * 用户删除单条数据（根据UID）从: 缓存，数据库，服务器
     * @param uid 要删除元素的唯一UID
     */
    open suspend fun onUserDelete(uid: Long): List<TItem> {
        return remove({ it.getUid() == uid }, EventSource.UserAction.Delete)
    }

    /**
     * 用户删除多条数据（根据UID）从: 缓存，数据库，服务器
     * @param uids 要删除元素的唯一UID列表
     */
    open suspend fun onUserDelete(uids: List<Long>): List<TItem> {
        if (uids.isEmpty()) return _items.value
        return remove({ uids.contains(it.getUid()) }, EventSource.UserAction.Delete)
    }

    /**
     * 用户清空所有数据从: 缓存，数据库
     */
    open suspend fun onUserClearAllFromCache() {
        clear(EventSource.UserAction.DeleteAllCache)
    }

    /**
     * 用户切换排序方式
     */
    open suspend fun onUserResort(newSortMode: SortMode, hasNetwork: Boolean) {
        if (defaultSortMode.value == newSortMode) return
        _defaultSortMode.value = newSortMode
        emitEvent(EventSource.UserAction.ChangeSort)

        if (hasNetwork) {
            try {
                val items = loadFromRemoteFull(newSortMode)
                replaceAll(items, EventSource.Remote.Full)
            } catch (e: Exception) {
                Log.e(TAG, "onUserResort: loadFromRemoteFull failed",e)
            }
        }
        else {
            val items = loadFromLocalFull(newSortMode)
            replaceAll(items, EventSource.Room.Full)
        }

    }

    /**
     * Http全量加载（刷新）
     */
    open suspend fun onHttpFullLoad() {
        try {
            val items = loadFromRemoteFull(defaultSortMode.value)
            replaceAll(items, EventSource.Remote.Full)
        } catch (e: Exception) {
            Log.e(TAG, "onHttpFullLoad: loadFromRemoteFull failed",e)
        }
    }

    /**
     * Http分页加载
     * @param direction UP=向上加载更早的数据，DOWN=向下加载更新的数据
     * @param limit 加载条数
     */
    open suspend fun onHttpPageLoad(
        direction: PageDirection = PageDirection.DOWN,
        limit: Int = 20
    ) {
        // 获取游标
        val cursor = when (direction) {
            // 向上查，取第一个元素的排序值
            PageDirection.UP -> getFirstCursor()
            // 向下查，取最后一个元素的排序值
            PageDirection.DOWN -> getLastCursor()
        }

        try {
            val items = loadFromRemotePage(defaultSortMode.value,
                direction, limit, cursor)
            if (items.isEmpty()) return
            insertOrderedBatch(items, defaultSortMode.value, EventSource.Remote.Page)
        } catch (e: Exception) {
            Log.e(TAG, "onHttpPageLoad: loadFromRemotePage failed",e)
        }
    }

    /**
     * Room全量加载（离线模式）
     */
    open suspend fun onLocalFullLoad() {
        val items = loadFromLocalFull(defaultSortMode.value)
        replaceAll(items, EventSource.Room.Full)
    }

    /**
     * Room分页加载（离线模式）
     */
    open suspend fun onLocalPageLoad(
        direction: PageDirection = PageDirection.DOWN,
        limit: Int = 20
    ) {
        val cursor = when (direction) {
            PageDirection.UP -> getFirstCursor()
            PageDirection.DOWN -> getLastCursor()
        }

        val items = loadFromLocalPage(defaultSortMode.value,
            direction, limit, cursor)
        if (items.isEmpty()) return
        insertOrderedBatch(items, defaultSortMode.value, EventSource.Room.Page)
    }

    /**
     * WebSocket插入一条
     */
    open suspend fun onWsUpsertOne(item: TItem) {
        // 1. 获取当前排序模式
        val currentSortMode = defaultSortMode.value

        // 2. 统一判断当前排序是升序还是降序
        val isDesc = when (currentSortMode) {
            is SortMode.UidSort -> currentSortMode.isDesc
            is SortMode.TimestampSort -> currentSortMode.isDesc
            is SortMode.StringSort -> currentSortMode.isDesc
        }

        // 3. 根据升/降序决定调用的方法
        if (isDesc) {
            // 降序：最新item数值最大 → 插入顶部
            prependTop(listOf(item), EventSource.WebSocket)
        } else {
            // 升序：最新item数值最大 → 插入底部
            appendBottom(listOf(item), EventSource.WebSocket)
        }
    }

    /**
     * WebSocket插入
     */
    open suspend fun onWsUpsert(items: List<TItem>) {
        if (items.isEmpty()) return
        insertOrderedBatch(items, defaultSortMode.value, EventSource.WebSocket)
    }

    // ========== 内部辅助方法 ==========

    /**
     * 获取第一个元素的游标值（用于向上查询）
     */
    private fun getFirstCursor(): Any? {
        val firstItem = _items.value.firstOrNull() ?: return null
        return when (defaultSortMode.value) {
            is SortMode.TimestampSort -> firstItem.getTimestamp()
            is SortMode.UidSort -> firstItem.getUid()
            is SortMode.StringSort -> firstItem.getStringIndex()
        }
    }

    /**
     * 获取最后一个元素的游标值（用于向下查询）
     */
    private fun getLastCursor(): Any? {
        val lastItem = _items.value.lastOrNull() ?: return null
        return when (defaultSortMode.value) {
            is SortMode.TimestampSort -> lastItem.getTimestamp()
            is SortMode.UidSort -> lastItem.getUid()
            is SortMode.StringSort -> lastItem.getStringIndex()
        }
    }
}