package com.magicvector.manager.event

import com.magicvector.domain.event.EventSource
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
 * 1. 用户主动操作 (UI层) → upsertTop：实时操作，添加/更新到列表最上层
 * 2. Http全量请求 (RemoteApi) → replaceAll：最新数据，直接替换整个列表
 * 3. Http分页请求 (RemoteApi) → insertOrdered：历史数据，按序插入（时间倒序）
 * 4. Ws长连接消息 (RealtimeChatController) → upsertTop：实时最新消息，添加最上层
 * 5. 无网络+有内存缓存 (LocalSource分页) → insertOrdered：基于缓存时间戳分页查询，按序插入
 * 6. 无网络+无内存缓存 (LocalSource全量) → replaceAll：Room全量查询，直接替换
 * 7. 名称排序场景 → insertOrdered：按名称升序/降序插入
 *
 * @param TItem 列表项类型，必须实现 SortItem 接口以支持统一排序
 * @param TEvent 事件类型
 * @param defaultSortMode 默认排序模式（默认时间降序：新到旧）
 */
abstract class AbstractEventManager<TItem : SortItem, TEvent : EventSource>(
    private val defaultSortMode: SortMode = SortMode.LONG_DESC,
    replay: Int = 1,
    extraBufferCapacity: Int = 64
) {
    private val reducerLock = ReentrantLock()
    private val _items = MutableStateFlow<List<TItem>>(emptyList())
    val items: StateFlow<List<TItem>> = _items.asStateFlow()

    private val _events = MutableSharedFlow<TEvent>(
        replay = replay,
        extraBufferCapacity = extraBufferCapacity
    )
    val events: SharedFlow<TEvent> = _events.asSharedFlow()

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
    private fun emitEvent(event: TEvent) {
        _events.tryEmit(event)
    }

    // ========== 核心操作方法 ==========

    /**
     * 全量替换列表（用于Http全量请求、本地全量查询）
     * 对应需求2、6
     */
    protected fun replaceAll(
        newItems: List<TItem>,
        event: TEvent
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
     */
    protected fun prependTop(
        items: List<TItem>,
        event: TEvent
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
     */
    protected fun appendBottom(
        items: List<TItem>,
        event: TEvent
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
     * @param items 待插入的列表
     * @param mode 排序模式
     * @param longSelector LONG排序时使用的字段选择器（默认用timestamp）
     * @param duplicateDetector 去重规则（默认基于uid）
     * @param event 变更事件
     */
    protected fun insertOrdered(
        items: List<TItem>,
        mode: SortMode = defaultSortMode,
        longSelector: (TItem) -> Long = { it.getTimestamp() },
        duplicateDetector: (TItem, TItem) -> Boolean = { a, b -> a.getUid() == b.getUid() },
        event: TEvent
    ): List<TItem> = reducerLock.withLock {
        val next = _items.value.toMutableList()

        val newItems = items.filter { candidate ->
            !next.any { existing -> duplicateDetector(existing, candidate) }
        }

        if (newItems.isNotEmpty()) {
            // ✅ 正确：直接调用 SortUtil 的批量插入方法
            SortUtil.insertOrdered(newItems, next, mode, longSelector)
            _items.value = next
        }

        emitEvent(event)
        _items.value
    }

    /**
     * 移除匹配的项
     */
    protected fun remove(
        matcher: (TItem) -> Boolean,
        event: TEvent
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
     * @param resort 更新后是否重新排序
     * @param mode 排序模式（当resort=true时使用）
     * @param longSelector LONG排序时使用的字段选择器
     * @return true 表示有项被更新
     */
    protected fun updateByUid(
        uid: Long,
        updater: (TItem) -> TItem,
        event: TEvent,
        resort: Boolean = false,
        mode: SortMode = defaultSortMode,
        longSelector: (TItem) -> Long = { it.getTimestamp() }
    ): Boolean = reducerLock.withLock {
        val next = _items.value.toMutableList()
        val index = next.indexOfFirst { it.getUid() == uid }

        if (index >= 0) {
            next[index] = updater(next[index])

            if (resort) {
                SortUtil.insertOrdered(emptyList(), next, mode, longSelector)
            }

            _items.value = next
            emitEvent(event)
            true
        } else {
            false
        }
    }

    /**
     * 批量更新 - 基于 uid 映射
     */
    protected fun updateBatchByUid(
        updates: Map<Long, (TItem) -> TItem>,
        event: TEvent,
        resort: Boolean = false,
        mode: SortMode = defaultSortMode,
        longSelector: (TItem) -> Long = { it.getTimestamp() }
    ): Int = reducerLock.withLock {
        val next = _items.value.toMutableList()
        var updateCount = 0

        next.indices.forEach { index ->
            val uid = next[index].getUid()
            updates[uid]?.let { updater ->
                next[index] = updater(next[index])
                updateCount++
            }
        }

        if (updateCount > 0 && resort) {
            SortUtil.insertOrdered(emptyList(), next, mode, longSelector)
        }

        if (updateCount > 0) {
            _items.value = next
            emitEvent(event)
        }

        updateCount
    }

    /**
     * 清空列表
     */
    protected fun clear(event: TEvent): List<TItem> = reducerLock.withLock {
        _items.value = emptyList()
        emitEvent(event)
        _items.value
    }

}