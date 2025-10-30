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

/**
 * ChatManager
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
class ChatManager(val agentId: String) {

    companion object {
        val TAG = ChatManager::class.simpleName
    }

    private val needUpdateQueue: ArrayDeque<UpdateRecyclerViewItem> = ArrayDeque()
    fun getNeedUpdateList(): List<UpdateRecyclerViewItem>{
        val updateList = needUpdateQueue.toList()
        // 清空队列
        needUpdateQueue.clear()
        return updateList
    }

    // view
    private val viewChatMessageList: MutableList<ChatItemAo> = mutableListOf()
    // 私有保护，避免外部添加导致ids和views不统一
    fun getViewChatMessageList(): MutableList<ChatItemAo> {
        return viewChatMessageList
    }

    // response -> view
    fun setResponsesToViews(responses: List<ChatMessageDo>){
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
            // 只有 ChatItemAo 中不包含此条消息才添加 (http的消息是唯一的)
            var viewIndex = -1
            for (chatItemAo in viewChatMessageList){
                if (chatItemAo.messageId == response.id) {
                    viewIndex = viewChatMessageList.indexOf(chatItemAo)
                    break
                }
            }
            // 不存在：插入
            if (viewIndex < 0) {
                val view = responseToView(response)
                // 降序二分查找适合的位置插入
                val insertPosition = SortUtil.descFindInsertPosition(view.getIndex(), viewChatMessageList)
                viewChatMessageList.add(insertPosition, view)
                // 最小左边界
                minPosition = insertPosition.coerceAtMost(minPosition)
            }
            // 存在：http请求的消息存在覆盖的情况，不需要更新view
        }
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
        var viewIndex = -1
        // 存在检测：存在就覆盖，不存在就插入
        for (chatItemAo in viewChatMessageList){
            if (chatItemAo.messageId == ws.messageId) {
                viewIndex = viewChatMessageList.indexOf(chatItemAo)
                break
            }
        }
        // ChatItemAo 中不包含此条消息添加, 包含则覆盖
        // 不存在
        if (viewIndex < 0){

            // 创建新视图
            val view = wsToView(ws)
            // 降序二分查找适合的位置插入
            val insertPosition = SortUtil.descFindInsertPosition(view.getIndex(), viewChatMessageList)
            viewChatMessageList.add(insertPosition, view)
            // 更新runnable
            val updateRecyclerViewItem = UpdateRecyclerViewItem()
            updateRecyclerViewItem.type = UpdateRecyclerViewTypeEnum.SINGLE_ID_INSERT
            updateRecyclerViewItem.singleInsertId = view.messageId
            needUpdateQueue.add(updateRecyclerViewItem)
        }
        // 存在
        else {
            // 覆盖逻辑
            val view = viewChatMessageList[viewIndex]
            wsToExistView(
                ws = ws,
                ao = view
            )
            val updateRecyclerViewItem = UpdateRecyclerViewItem()
            updateRecyclerViewItem.type = UpdateRecyclerViewTypeEnum.SINGLE_ID_UPDATE
            updateRecyclerViewItem.singleUpdateId = view.messageId
            needUpdateQueue.add(updateRecyclerViewItem)
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
        viewChatMessageList.clear()
        needUpdateQueue.clear()
    }
}