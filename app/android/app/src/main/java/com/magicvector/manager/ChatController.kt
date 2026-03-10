package com.magicvector.manager

import android.text.TextUtils
import android.util.Log
//import com.core.baseutil.date.DateUtils
import com.core.baseutil.sort.SortUtil
import com.data.domain.Do.ChatMessageDo
import com.data.domain.ao.chat.ChatItemAo
import com.data.domain.constant.chat.MessageTypeEnum
import com.data.domain.dto.ws.reponse.RealtimeChatTextResponse
import com.view.appview.recycler.UpdateRecyclerViewItem
import com.view.appview.recycler.UpdateRecyclerViewTypeEnum
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * ChatManager：将数据源绑定到Chat View上
 * chat信息来源：后端，本地
 * 后端：
 *  1.ChatRequest请求获取List
 *  2.Websocket收到的Chat消息（还是他妈流式的）
 * 本地：
 *  1.Room
 *  2.MMKV
 *
 *  todo 使用DiffUtil优化性能
 */
class ChatController(val agentId: String) {

    companion object {
        val TAG = ChatController::class.simpleName
        // 防止长会话导致内存无限增长；极端历史依赖 Room/HTTP 锚点分页回放。
        private const val MAX_IN_MEMORY_MESSAGES = 2000
    }

    private val lock = ReentrantLock()
    private val needUpdateQueue: ArrayDeque<UpdateRecyclerViewItem> = ArrayDeque()
    // 以 messageId 作为索引，避免 O(n) 全量遍历查重
    private val messageIdIndex: MutableMap<String, ChatItemAo> = mutableMapOf()

    fun getNeedUpdateList(): List<UpdateRecyclerViewItem>{
        return lock.withLock {
            val updateList = needUpdateQueue.toList()
            needUpdateQueue.clear()
            updateList
        }
    }

    // view
    private val viewChatMessageList: MutableList<ChatItemAo> = mutableListOf()
    // 私有保护，避免外部添加导致ids和views不统一
    fun getViewChatMessageList(): MutableList<ChatItemAo> {
        return lock.withLock { viewChatMessageList.toMutableList() }
    }

    // response -> view
    fun setResponsesToViews(responses: List<ChatMessageDo>){
        lock.withLock {
        if (responses.isEmpty()){
            Log.d(TAG, "response为空")
            return
        }
        else {
            Log.d(TAG, "response.size = ${responses.size}")
        }
        /*
            http请求一定是批量的，chat记录是timestamp越大越靠前。
            插入之后偏移是向后偏移，所以只需要确定[Math(position_min), list.size - 1]
         */
        var minPosition = if (viewChatMessageList.isEmpty()) {
            0
        } else {
            viewChatMessageList.size - 1
        }
        for (response in responses) {
            val messageId = response.id
            val exists = !messageId.isNullOrBlank() && messageIdIndex.containsKey(messageId)
            // 不存在：插入
            if (!exists) {
                val view = responseToView(response)
                // 降序二分查找适合的位置插入
                val insertPosition = SortUtil.descFindInsertPosition(view.getIndex(), viewChatMessageList)
                viewChatMessageList.add(insertPosition, view)
                if (!view.messageId.isNullOrBlank()) {
                    messageIdIndex[view.messageId!!] = view
                }
                // 最小左边界
                minPosition = insertPosition.coerceAtMost(minPosition)
            }
            // 存在：http请求的消息存在覆盖的情况，不需要更新view
        }
        trimInMemoryIfNeed()
        // 此处是插入之后的viewChatMessageList.size
        if (minPosition < viewChatMessageList.size - 1){
            val updateRecyclerViewItem = UpdateRecyclerViewItem()
            updateRecyclerViewItem.type = UpdateRecyclerViewTypeEnum.ID_TO_END_UPDATE
            updateRecyclerViewItem.idToEndUpdateId = viewChatMessageList[minPosition].messageId
            needUpdateQueue.add(updateRecyclerViewItem)
        }
        else {
            Log.d(TAG, "全部都存在：minPosition: $minPosition > viewChatMessageList.size - 1: ${viewChatMessageList.size - 1}")
        }
        }
    }

    private fun responseToView(response: ChatMessageDo): ChatItemAo{
        val ao = ChatItemAo()
        // view
        // todo 暂时不支持发送图片
//        ao.vo.imgUrl = response.imgUrl
        ao.vo.content = response.content
        ao.vo.time = runCatching {
//            DateUtils.yyyyMMddHHmmssToString(response.chatTime)
            response.chatTime
        }.getOrElse {
            // 记录异常信息（可选）
            Log.e(TAG, "时间转换失败")
            ""
        }
        ao.vo.viewType = response.role
        ao.vo.messageType = MessageTypeEnum.TEXT.value

        // data
        ao.senderId = response.agentId
        ao.receiverId = response.userId
        ao.messageId = response.id
        ao.timestamp = response.chatTimestamp

        return ao
    }

    // ws -> view (ws只会一个一个插入，不存在list的情况)
    fun setWsToViews(ws: RealtimeChatTextResponse){
        lock.withLock {
        val messageId = ws.messageId
        val existView = if (messageId.isNullOrBlank()) {
            null
        } else {
            messageIdIndex[messageId]
        }
        // ChatItemAo 中不包含此条消息添加, 包含则覆盖
        // 不存在
        if (existView == null){

            // 创建新视图
            val view = wsToView(ws)
            // 降序二分查找适合的位置插入
            val insertPosition = SortUtil.descFindInsertPosition(view.getIndex(), viewChatMessageList)
            viewChatMessageList.add(insertPosition, view)
            if (!view.messageId.isNullOrBlank()) {
                messageIdIndex[view.messageId!!] = view
            }
            // 更新runnable
            val updateRecyclerViewItem = UpdateRecyclerViewItem()
            updateRecyclerViewItem.type = UpdateRecyclerViewTypeEnum.SINGLE_ID_INSERT
            updateRecyclerViewItem.singleInsertId = view.messageId
            needUpdateQueue.add(updateRecyclerViewItem)
            trimInMemoryIfNeed()
        }
        // 存在
        else {
            // 覆盖逻辑
            wsToExistView(ws = ws, ao = existView)
            val updateRecyclerViewItem = UpdateRecyclerViewItem()
            updateRecyclerViewItem.type = UpdateRecyclerViewTypeEnum.SINGLE_ID_UPDATE
            updateRecyclerViewItem.singleUpdateId = existView.messageId
            needUpdateQueue.add(updateRecyclerViewItem)
        }
        }
    }

    private fun wsToView(ws: RealtimeChatTextResponse): ChatItemAo{
        val ao = ChatItemAo()
        // view
        // todo 暂时不支持发送图片
//        ao.vo.imgUrl = ""
        ao.vo.content = ws.content
        ao.vo.time = runCatching {
//            DateUtils.yyyyMMddHHmmssToString(ws.chatTime)
            ws.chatTime
        }.getOrElse {
            // 记录异常信息（可选）
            Log.e(TAG, "时间转换失败")
            ""
        }
        ao.vo.viewType = ws.role
        ao.vo.messageType = MessageTypeEnum.TEXT.value


        // data
        ao.senderId = ws.agentId
        ao.receiverId = ws.userId
        ao.messageId = ws.messageId
        ao.timestamp = ws.timestamp

        return ao
    }

    private fun wsToExistView(ws: RealtimeChatTextResponse, ao: ChatItemAo){
        // view
        // todo 暂时不支持发送图片
//        ao.vo.imgUrl = ""
        val existContent = if (TextUtils.isEmpty(ao.vo.content)) {
            ""
        } else {
            ao.vo.content
        }
        ao.vo.content = existContent + ws.content
        ao.vo.time = runCatching {
//            DateUtils.yyyyMMddHHmmssToString(ws.chatTime)
            ws.chatTime
        }.getOrElse {
            // 记录异常信息（可选）
            Log.e(TAG, "时间转换失败")
            ""
        }
        ao.vo.viewType = ws.role
        ao.vo.messageType = MessageTypeEnum.TEXT.value

        // data
        ao.senderId = ws.agentId
        ao.receiverId = ws.userId
        ao.messageId = ws.messageId
        ao.timestamp = ws.timestamp
    }

    fun clear(){
        lock.withLock {
            viewChatMessageList.clear()
            needUpdateQueue.clear()
            messageIdIndex.clear()
        }
    }

    private fun trimInMemoryIfNeed() {
        if (viewChatMessageList.size <= MAX_IN_MEMORY_MESSAGES) {
            return
        }
        val removeCount = viewChatMessageList.size - MAX_IN_MEMORY_MESSAGES
        repeat(removeCount) {
            val removed = viewChatMessageList.removeLast()
            val id = removed.messageId
            if (!id.isNullOrBlank()) {
                messageIdIndex.remove(id)
            }
        }
    }
}