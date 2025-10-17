package com.magicvector.manager

import android.util.Log
import com.core.baseutil.date.DateUtils
import com.core.baseutil.sort.SortUtil
import com.data.domain.Do.ChatMessageDo
import com.data.domain.ao.chat.ChatItemAo
import com.data.domain.constant.chat.MessageTypeEnum
import com.data.domain.dto.ws.RealtimeChatTextResponse
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
 */
class ChatManager(val agentId: String) {

    companion object {
        val TAG = ChatManager::class.simpleName
    }

    val needUpdateQueue: ArrayDeque<UpdateRecyclerViewItem> = ArrayDeque()

    // view
    val viewChatMessageList: MutableList<ChatItemAo> = mutableListOf()

    // response -> view
    fun responsesToViews(responses: List<ChatMessageDo>){
        /*
            http请求一定是批量的，chat记录是timestamp越大越靠前。
            插入之后偏移是向后偏移，所以只需要确定[Math(position_min), list.size - 1]
         */
        var minPosition = viewChatMessageList.size - 1
        for (response in responses) {
            // 只有 ChatItemAo 中不包含此条消息才添加 (http的消息是唯一的)
            var isExist = false
            for (viewChatMessage in viewChatMessageList){
                if (viewChatMessage.messageId == response.id){
                    isExist = true
                    break
                }
            }
            if (!isExist) {
                val view = responseToView(response)
                // 降序二分查找适合的位置插入
                val insertPosition = SortUtil.descFindInsertPosition(view.getIndex(), viewChatMessageList)
                viewChatMessageList.add(insertPosition, view)
                // 最小左边界
                minPosition = insertPosition.coerceAtMost(minPosition)
            }
        }
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
            DateUtils.yyyyMMddHHmmssToString(response.chatTime)
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

    // ws -> view
    fun wssToViews(wss: List<RealtimeChatTextResponse>){
        var minPosition = viewChatMessageList.size - 1
        for (ws in wss) {
            // ChatItemAo 中不包含此条消息添加, 包含则覆盖
            var isExist = false
            var viewIndex = -1
            for (view in viewChatMessageList) {
                if (view.messageId == ws.messageId) {
                    isExist = true
                    viewIndex = viewChatMessageList.indexOf(view)
                    break
                }
            }
            if (!isExist) {
                val view = wsToView(ws)
                // 降序二分查找适合的位置插入
                val insertPosition = SortUtil.descFindInsertPosition(view.getIndex(), viewChatMessageList)
                viewChatMessageList.add(insertPosition, view)
                // 最小左边界
                minPosition = insertPosition.coerceAtMost(minPosition)
            }
            else {
                val view = wsToView(ws)
                viewChatMessageList[viewIndex] = view
                val updateRecyclerViewItem = UpdateRecyclerViewItem()
                updateRecyclerViewItem.type = UpdateRecyclerViewTypeEnum.SINGLE_ID_UPDATE
                updateRecyclerViewItem.singleUpdateId = view.messageId
                needUpdateQueue.add(updateRecyclerViewItem)
            }
        }
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

    private fun wsToView(ws: RealtimeChatTextResponse): ChatItemAo{
        val ao = ChatItemAo()
        // view
        // todo 暂时不支持发送图片
//        ao.vo.imgUrl = ""
        ao.vo.content = ws.content
        ao.vo.time = runCatching {
            DateUtils.yyyyMMddHHmmssToString(ws.chatTime)
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

    fun clear(){
        viewChatMessageList.clear()
        needUpdateQueue.clear()
    }
}