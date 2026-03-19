package com.magicvector.manager.event

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
abstract class AbstractEventManager<TItem : SortItem, TEvent : Any>(
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
     * 置顶插入/更新（用于用户主动操作、Ws新消息）
     * 对应需求1、4
     *
     * @param item 待插入/更新的项
     * @param matcher 匹配规则（默认基于uid）
     * @param event 变更事件
     * @param sortAfterInsert 插入后是否重新排序
     * @param mode 排序模式（当sortAfterInsert=true时使用）
     * @param longSelector LONG排序时使用的字段选择器
     */
    protected fun upsertTop(
        item: TItem,
        matcher: (TItem) -> Boolean = { it.getUid() == item.getUid() },
        event: TEvent,
        sortAfterInsert: Boolean = false,
        mode: SortMode = defaultSortMode,
        longSelector: (TItem) -> Long = { it.getTimestamp() }): List<TItem> = reducerLock.withLock {
        val next = _items.value.toMutableList()
        val index = next.indexOfFirst(matcher)

        if (index >= 0) {
            // 更新已存在的项
            next[index] = item
        } else {
            // 插入新项到顶部
            next.add(0, item)
        }

        if (sortAfterInsert) {
            // 使用 SortUtil 的 insertOrdered 方法重新排序
            // 传入 emptyList() 因为 item 已经添加过了
            SortUtil.insertOrdered(emptyList(), next, mode, longSelector)
        }

        _items.value = next
        emitEvent(event)
        _items.value
    }

    /**
     * 置底插入（扩展功能，用于特殊场景）
     *
     * @param item 待插入/更新的项
     * @param matcher 匹配规则（默认基于uid）
     * @param event 变更事件
     * @param sortAfterInsert 插入后是否重新排序
     * @param mode 排序模式（当sortAfterInsert=true时使用）
     * @param longSelector LONG排序时使用的字段选择器
     */
    protected fun upsertBottom(
        item: TItem,
        matcher: (TItem) -> Boolean = { it.getUid() == item.getUid() },
        event: TEvent,
        sortAfterInsert: Boolean = false,
        mode: SortMode = defaultSortMode,
        longSelector: (TItem) -> Long = { it.getTimestamp() }
    ): List<TItem> = reducerLock.withLock {
        val next = _items.value.toMutableList()
        val index = next.indexOfFirst(matcher)

        if (index >= 0) {
            next[index] = item  // 更新
        } else {
            next.add(item)      // 插入底部
        }

        if (sortAfterInsert) {
            // ✅ 直接使用 SortUtil 的 insertOrdered 方法
            SortUtil.insertOrdered(emptyList(), next, mode, longSelector)
        }

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
     * 更新匹配的项
     *
     * @param matcher 匹配规则
     * @param updater 更新函数
     * @param event 变更事件
     * @param resort 更新后是否重新排序
     * @param mode 排序模式（当resort=true时使用）
     * @param longSelector LONG排序时使用的字段选择器
     * @return true 表示有项被更新
     */
    protected fun update(
        matcher: (TItem) -> Boolean,
        updater: (TItem) -> TItem,
        event: TEvent,
        resort: Boolean = false,
        mode: SortMode = defaultSortMode,
        longSelector: (TItem) -> Long = { it.getTimestamp() }
    ): Boolean = reducerLock.withLock {
        val next = _items.value.toMutableList()
        var updated = false

        next.indices.forEach { index ->
            if (matcher(next[index])) {
                next[index] = updater(next[index])
                updated = true
            }
        }

        if (updated) {
            if (resort) {
                // 使用 SortUtil 的 insertOrdered 方法重新排序
                SortUtil.insertOrdered(emptyList(), next, mode, longSelector)
            }
            _items.value = next
            emitEvent(event)
        }

        updated
    }

    /**
     * 批量更新（支持复杂操作）
     */
    protected fun batchUpdate(
        builder: (List<TItem>) -> List<TItem>,
        event: TEvent
    ): List<TItem> = reducerLock.withLock {
        _items.value = builder(_items.value)
        emitEvent(event)
        _items.value
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