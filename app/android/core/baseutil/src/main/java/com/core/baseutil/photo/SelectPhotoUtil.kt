package com.core.baseutil.photo

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.widget.ImageView
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.core.baseutil.image.ImageManager
import java.util.concurrent.atomic.AtomicReference

object SelectPhotoUtil {

    val TAG: String = SelectPhotoUtil::class.java.simpleName

    /**
     * 从相册选择图像。
     * @param selectImageLauncher    用于启动图像选择的 ActivityResultLauncher。
     */
    @SuppressLint("IntentReset")
    fun selectImageFromAlbum(selectImageLauncher : ActivityResultLauncher<Intent>) {
        try {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            intent.type = "image/*"
            selectImageLauncher.launch(intent)
        } catch (e: Exception){
            Log.e(TAG, "selectImageFromAlbum: ", e)
        }
    }


    /**
     * 初始化用于从相册选择图像的 ActivityResultLauncher。
     * @param activity                  AppCompatActivity 上下文
     * @param imageView                 ImageView。
     * @param selectedImageUri          用于保存选中图像 URI 的 AtomicReference。[如果知识传递Uri的话，只是传递形参，并不是地址，无法改变Activity中Uri的值]
     *
     *  此处的uri是null，用于保存从相册中获取的uri
     *
     * @param imageManager                 用于将 URI 转换为 Bitmap 以便显示的 ImageUtil 实例。
     * @return 返回一个 ActivityResultLauncher<Intent>，可用于启动图像选择的 Intent。
    </Intent> */
    fun initActivityResultLauncher(
        activity: AppCompatActivity, imageView: ImageView, selectedImageUri: AtomicReference<Uri?>,
        imageManager: ImageManager, runnable: Runnable
    ): ActivityResultLauncher<Intent?> {
        return activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult? ->
            handleImageResult(
                result!!, activity, imageView,
                selectedImageUri, imageManager, runnable
            )
        }
    }

    /**
     * 处理图像选择活动的结果。
     * @param result            图像选择活动的结果，包括结果码和数据。
     * @param activity          AppCompatActivity 上下文。
     * @param imageView         ImageView。
     * @param selectedImageUri  用于保存选中图像 URI 的 AtomicReference。[如果知识传递Uri的话，只是传递形参，并不是地址，无法改变Activity中Uri的值]
     * @param imageManager         用于将 URI 转换为 Bitmap 以便显示的 ImageUtil 实例。
     */
    private fun handleImageResult(
        result: ActivityResult,
        activity: Activity?,
        imageView: ImageView,
        selectedImageUri: AtomicReference<Uri?>,
        imageManager: ImageManager,
        finishCallback: Runnable
    ) {
        // if (result.getResultCode() == Activity.RESULT_OK){};
        val data = result.data
        if (data != null) {
            val imageUri = data.data
            // 更新 Uri
            selectedImageUri.set(imageUri)
            val bitmap: Bitmap? = imageManager.uriToBitmapMediaStore(activity, imageUri)
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap)
                finishCallback.run()
            }
        }
    }

}