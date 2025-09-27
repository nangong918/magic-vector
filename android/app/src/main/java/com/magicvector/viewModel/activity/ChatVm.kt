package com.magicvector.viewModel.activity

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.text.Editable
import android.text.TextWatcher
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import com.core.baseutil.permissions.GainPermissionCallback
import com.core.baseutil.permissions.PermissionUtil
import com.core.baseutil.photo.SelectPhotoUtil
import com.core.baseutil.ui.ToastUtils
import com.data.domain.ao.message.MessageContactItemAo
import com.data.domain.fragmentActivity.aao.ChatAAo
import com.view.appview.R
import com.view.appview.chat.ChatMessageAdapter
import com.view.appview.chat.OnChatMessageClick


class ChatVm(

) : ViewModel(){

    companion object {
        val TAG: String = ChatVm::class.java.name
    }

    //---------------------------AAo Ld---------------------------

    val aao = ChatAAo()

    fun initAAo(messageContactItemAo : MessageContactItemAo?){
        aao.messageContactItemAo = messageContactItemAo
    }

    lateinit var adapter : ChatMessageAdapter

    fun initAdapter(onChatMessageClick : OnChatMessageClick){
        adapter = ChatMessageAdapter(
            aao.chatMessageList,
            onChatMessageClick
        )
    }

    //---------------------------NetWork---------------------------



    //---------------------------Logic---------------------------

    fun getTextWatcher(): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s?.isNotEmpty() == true) {
                    aao.inputTextLd.value = s.toString()
                }
            }

            override fun afterTextChanged(s: Editable?) {
            }
        }
    }

    fun sendMessage() {
    }


    //===========selectImage

    private var selectImageLauncher: ActivityResultLauncher<Intent>? = null

    // 图片消息：3.初始化图片选择器
    fun initPictureSelectorLauncher(activity: FragmentActivity){
        selectImageLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ){
            result ->
            // 图片消息：3.1 获取图片uri
            val imageUri: Uri? = result.data?.data


            if (imageUri != null) {
                val currentTime = System.currentTimeMillis()
                val content = aao.inputTextLd.value

                // todo 发送图片
            }
            else {
                ToastUtils.showToastActivity(
                    activity,
                    activity.getString(R.string.send_image_failed)
                )
            }
        }
    }

    // 选择图片
    fun beginSelectPicture(activity: FragmentActivity){
        val mustPermissions = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
        )
        val optionalPermissions = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
        )

        PermissionUtil.requestPermissionSelectX(
            activity,
            mustPermissions,
            optionalPermissions,
            object : GainPermissionCallback {
                override fun allGranted() {
                    // 必要权限获取成功
                    if (selectImageLauncher != null){
                        SelectPhotoUtil.selectImageFromAlbum(selectImageLauncher!!);
                    }
                }

                override fun notGranted(notGrantedPermissions: Array<String?>?) {
                    // 必要权限被拒绝
                    ToastUtils.showToastActivity(
                        activity,
                        activity.getString(R.string.gain_permission_failed)
                    )
                }

                override fun always() {

                }

            }
        )
    }

}