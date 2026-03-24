package com.magicvector.manager.realtime


import android.graphics.Bitmap
import androidx.fragment.app.FragmentActivity
import com.magicvector.domain.model.mixLLM.McpSwitch
import com.magicvector.domain.bo.AgentChatBO
import kotlinx.coroutines.Job

/**
 * 实时聊天 MVI Intent
 */
sealed class RealtimeChatIntent {
    data class Initialize(
        val chatActivity: FragmentActivity,
        val agentChatBo: AgentChatBO?,
        val initNetworkRunnable: () -> Job,
        val onVideoFrame: ((Bitmap) -> Unit)? = null
    ) : RealtimeChatIntent()

    data class BindChannel(val agentId: Long) : RealtimeChatIntent()
    data class SendTextMessage(val message: String) : RealtimeChatIntent()
    data object StartVadCall : RealtimeChatIntent()
    data object StopVadCall : RealtimeChatIntent()
    data object DestroyVadCall : RealtimeChatIntent()
    data class SendMcpSwitch(val mcpSwitch: McpSwitch) : RealtimeChatIntent()
    data object RefreshMessages : RealtimeChatIntent()
    data object LoadMoreMessages : RealtimeChatIntent()
    data object ClearCache : RealtimeChatIntent()
    data class SendVideoFrame(val bitmap: Bitmap) : RealtimeChatIntent()
}