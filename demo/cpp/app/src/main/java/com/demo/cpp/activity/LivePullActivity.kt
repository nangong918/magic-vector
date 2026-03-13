package com.demo.cpp.activity

import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.ToggleButton
import android.widget.Toast
import com.demo.cpp.R
import com.demo.cpp.base.BaseActivity

class LivePullActivity : BaseActivity(), CompoundButton.OnCheckedChangeListener {

    companion object {
        private const val DEFAULT_RTMP_URL = "rtmp://172.16.41.199/live/stream"
    }

    override val layoutId: Int
        get() = R.layout.activity_live_pull

    private var isPulling = false
    private var player: RtmpPlayer? = null
    private var surfaceHolder: SurfaceHolder? = null

    private lateinit var urlInput: EditText
    private lateinit var pullToggle: ToggleButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideActionBar()
        initView()
    }

    private fun initView() {
        urlInput = getView(R.id.et_rtmp_url)
        pullToggle = getView(R.id.btn_pull)
        pullToggle.setOnCheckedChangeListener(this)
        urlInput.setText(DEFAULT_RTMP_URL)

        val surfaceView = getView<SurfaceView>(R.id.surface_player)
        surfaceHolder = surfaceView.holder
    }

    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        if (buttonView.id != R.id.btn_pull) return
        if (isChecked) {
            startPull()
        } else {
            stopPull()
        }
    }

    private fun startPull() {
        val url = urlInput.text.toString().trim()
        if (TextUtils.isEmpty(url)) {
            showToast(getString(R.string.rtmp_url_empty))
            pullToggle.isChecked = false
            return
        }
        val holder = surfaceHolder
        if (holder == null) {
            showToast(getString(R.string.rtmp_surface_not_ready))
            pullToggle.isChecked = false
            return
        }
        val rtmpPlayer = player ?: MissingRtmpPlayer(this).also { player = it }
        val started = rtmpPlayer.start(url, holder)
        if (!started) {
            pullToggle.isChecked = false
            return
        }
        isPulling = true
    }

    private fun stopPull() {
        player?.stop()
        isPulling = false
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isPulling) {
            stopPull()
        }
        player?.release()
        player = null
    }

    override fun onViewClick(view: View) {
        // no-op
    }

    override fun onSelectedFile(filePath: String) {
        // no-op
    }
}

private interface RtmpPlayer {
    fun start(url: String, holder: SurfaceHolder): Boolean
    fun stop()
    fun release()
}

private class MissingRtmpPlayer(private val context: Context) : RtmpPlayer {
    override fun start(url: String, holder: SurfaceHolder): Boolean {
        Toast.makeText(
            context,
            context.getString(R.string.rtmp_player_missing),
            Toast.LENGTH_SHORT
        ).show()
        return false
    }

    override fun stop() {
        // no-op
    }

    override fun release() {
        // no-op
    }
}
