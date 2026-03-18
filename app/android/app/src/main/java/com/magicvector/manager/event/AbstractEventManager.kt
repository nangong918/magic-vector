package com.magicvector.manager.event

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

abstract class AbstractEventManager<TItem : Any, TEvent : Any>(
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

    protected fun snapshotItems(): List<TItem> = reducerLock.withLock { _items.value.toList() }

    protected fun replaceAllInternal(
        list: List<TItem>,
        event: TEvent
    ): List<TItem> = reducerLock.withLock {
        _items.value = list.toList()
        _events.tryEmit(event)
        _items.value
    }

    protected fun upsertTopInternal(
        item: TItem,
        matcher: (TItem) -> Boolean,
        event: TEvent
    ): List<TItem> = reducerLock.withLock {
        val next = _items.value.toMutableList()
        val index = next.indexOfFirst(matcher)
        if (index >= 0) {
            next[index] = item
        } else {
            next.add(0, item)
        }
        _items.value = next
        _events.tryEmit(event)
        _items.value
    }

    protected fun removeInternal(
        matcher: (TItem) -> Boolean,
        event: TEvent
    ): List<TItem> = reducerLock.withLock {
        _items.value = _items.value.filterNot(matcher)
        _events.tryEmit(event)
        _items.value
    }

    protected fun insertHistoryOrderedInternal(
        items: List<TItem>,
        comparator: Comparator<TItem>,
        duplicate: (TItem, TItem) -> Boolean,
        event: TEvent
    ): List<TItem> = reducerLock.withLock {
        val next = _items.value.toMutableList()
        items.forEach { candidate ->
            val exists = next.any { duplicate(it, candidate) }
            if (exists) {
                return@forEach
            }
            val insertIndex = next.binarySearch(candidate, comparator).let { index ->
                if (index >= 0) index else -(index + 1)
            }
            next.add(insertIndex, candidate)
        }
        _items.value = next
        _events.tryEmit(event)
        _items.value
    }

    protected fun clearInternal(event: TEvent): List<TItem> = reducerLock.withLock {
        _items.value = emptyList()
        _events.tryEmit(event)
        _items.value
    }
}
