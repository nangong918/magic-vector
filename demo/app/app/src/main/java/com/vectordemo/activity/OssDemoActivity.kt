package com.vectordemo.activity

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import com.vectordemo.ui.theme.VectorDemoTheme
import com.vectordemo.ui.view.oss.OssDemoScreen
import com.vectordemo.utils.activity.BaseComponentActivity
import com.vectordemo.viewModel.oss.OssDemoViewModel

class OssDemoActivity : BaseComponentActivity() {

    private val vm: OssDemoViewModel by viewModels { OssDemoViewModel.factory() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val ui by vm.uiState.collectAsState()
            LaunchedEffect(ui.toast) {
                ui.toast?.let { msg ->
                    Toast.makeText(this@OssDemoActivity, msg, Toast.LENGTH_LONG).show()
                    vm.consumeToast()
                }
            }
            var pickedUri by remember { mutableStateOf<Uri?>(null) }
            var replaceContext by remember { mutableStateOf<Pair<Long, String>?>(null) }
            val pickMain = rememberLauncherForActivityResult(
                ActivityResultContracts.PickVisualMedia()
            ) { uri -> pickedUri = uri }
            val pickReplace = rememberLauncherForActivityResult(
                ActivityResultContracts.PickVisualMedia()
            ) { uri ->
                val ctx = replaceContext
                if (uri != null && ctx != null) {
                    vm.replaceFile(ctx.first, ctx.second, uri)
                }
                replaceContext = null
            }
            VectorDemoTheme {
                OssDemoScreen(
                    ui = ui,
                    pickedUri = pickedUri,
                    onBack = { finish() },
                    onPickMainImage = {
                        pickMain.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onUploadClick = { vm.uploadImage(pickedUri) },
                    onRefreshBuckets = { vm.refreshBuckets() },
                    onToggleBucket = { vm.toggleBucket(it) },
                    onDownload = { url, name -> vm.saveImageToGallery(url, name) },
                    onRequestReplacePick = { bucket, fileId ->
                        replaceContext = fileId to bucket
                        pickReplace.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onDeleteFile = { bucket, fileId -> vm.deleteFile(fileId, bucket) }
                )
            }
        }
    }
}
