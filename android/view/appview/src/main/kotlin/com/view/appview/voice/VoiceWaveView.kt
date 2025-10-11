package com.view.appview.voice

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.animation.ValueAnimator
import android.graphics.Path
import android.os.Handler
import android.os.Parcelable
import android.util.Log
import android.view.Gravity
import com.view.appview.R
import com.view.appview.voice.VoiceWaveView.Companion.audioList
import java.util.*
import kotlin.math.min

class VoiceWaveView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    var bodyWaveList = LinkedList<Int>()
        private set
    var headerWaveList = LinkedList<Int>()
        private set
    var footerWaveList = LinkedList<Int>()
        private set

    private var waveList = LinkedList<Int>()

    companion object {
        val TAG = VoiceWaveView::class.simpleName
        val multiple = 50
        val audioList = listOf<Int>(
            5,
            10,
            20,
            40,

            60,
            60,
            80,
            80,
            100,
            80,
            80,
            60,
            60,

            40,
            20,
            10,
            5,
        )
    }

    fun init() {
        duration = 150
        // 0 ~ 3
        for (i in 0 until 4){
            addHeader(audioList[i])
        }
        for (i in 4 until 13){
            addBody(audioList[i])
        }
        for (i in 13 until 17){
            addFooter(audioList[i])
        }
        start()
    }

    fun setVolume(volume: Float){
        // 放大10倍
        val rate = volume * multiple

        for (i in 0 until headerWaveList.size){
            for (j in 0 until 4){
                headerWaveList[i] = min((audioList[j] * rate).toInt(), 100)
            }
        }
        for (i in 0 until bodyWaveList.size){
            for (j in 4 until 13){
                bodyWaveList[i] = min((audioList[j] * rate).toInt(), 100)
            }
        }
        for (i in 0 until footerWaveList.size){
            for (j in 13 until 17){
                footerWaveList[i] = min((audioList[j] * rate).toInt(), 100)
            }
        }

        val waveList = listOf(
            headerWaveList[0],
            headerWaveList[1],
            headerWaveList[2],
            headerWaveList[3],

            bodyWaveList[0],
            bodyWaveList[1],
            bodyWaveList[2],
            bodyWaveList[3],
            bodyWaveList[4],
            bodyWaveList[5],
            bodyWaveList[6],
            bodyWaveList[7],
            bodyWaveList[8],

            footerWaveList[0],
            footerWaveList[1],
            footerWaveList[2],
            footerWaveList[3],
        )

        Log.i(TAG, "volume: $waveList")
    }


    /**
     * 线间距 px
     */
    var lineSpace: Float = 10f
    /**
     * 线宽 px
     */
    var lineWidth: Float = 20f

    /**
     * 动画持续时间
     */
    var duration: Long = 200
    /**
     * 线颜色
     */
    var lineColor: Int = Color.BLUE
    var paintLine: Paint? = null
    var paintPathLine: Paint? = null

    private var valueAnimator = ValueAnimator.ofFloat(0f, 1f)

    private var valueAnimatorOffset: Float = 1f

    private var valHandler = Handler()
    val linePath = Path()

    @Volatile
    var isStart: Boolean = false
        private set

    /**
     * 跳动模式
     */
    var waveMode: WaveMode = WaveMode.UP_DOWN

    /**
     * 线条样式
     */
    var lineType: LineType = LineType.BAR_CHART

    /**
     * 显示位置
     */
    var showGravity: Int = Gravity.LEFT or Gravity.BOTTOM

    private var runnable: Runnable? = null


    init {
        attrs?.let {
            val typedArray = context.theme.obtainStyledAttributes(
                attrs,
                R.styleable.VoiceWaveView, 0, 0
            )

            lineWidth = typedArray.getDimension(R.styleable.VoiceWaveView_lineWidth, 20f)
            lineSpace = typedArray.getDimension(R.styleable.VoiceWaveView_lineSpace, 10f)
            duration = typedArray.getInt(R.styleable.VoiceWaveView_duration, 200).toLong()
            showGravity = typedArray.getInt(R.styleable.VoiceWaveView_android_gravity, Gravity.LEFT or Gravity.BOTTOM)
            lineColor = typedArray.getInt(R.styleable.VoiceWaveView_lineColor, Color.BLUE)
            val mode = typedArray.getInt(R.styleable.VoiceWaveView_waveMode, 0)
            when (mode) {
                0 -> waveMode = WaveMode.UP_DOWN
                1 -> waveMode = WaveMode.LEFT_RIGHT
            }

            val lType = typedArray.getInt(R.styleable.VoiceWaveView_lineType, 0)
            when (lType) {
                0 -> lineType = LineType.BAR_CHART
                1 -> lineType = LineType.LINE_GRAPH
            }

            typedArray.recycle()
        }

        paintLine = Paint()
        paintLine?.isAntiAlias = true
        paintLine?.strokeCap = Paint.Cap.ROUND

        paintPathLine = Paint()
        paintPathLine?.isAntiAlias = true
        paintPathLine?.style = Paint.Style.STROKE;
    }

    /**
     * 线的高度 0,100 百分数
     */
    fun addBody(num: Int) {
        checkNum(num)
        bodyWaveList.add(num)
    }

    /**
     * 头部线的高度 0,100 百分数
     */
    fun addHeader(num: Int) {
        checkNum(num)
        headerWaveList.add(num)
    }

    /**
     * 尾部线的高度 0,100 百分数
     */
    fun addFooter(num: Int) {
        checkNum(num)
        footerWaveList.add(num)
    }

    private fun checkNum(num: Int) {
        if (num < 0 || num > 100) {
            throw Exception("num must between 0 and 100")
        }
    }

    /**
     * 开始
     */
    fun start() {
        if (isStart) {
            return
        }
        isStart = true
        when (waveMode) {
            WaveMode.UP_DOWN -> {
                valueAnimator.duration = duration
                valueAnimator.repeatMode = ValueAnimator.REVERSE
                valueAnimator.repeatCount = ValueAnimator.INFINITE
                valueAnimator.addUpdateListener {
                    valueAnimatorOffset = it.animatedValue as Float
                    invalidate()
                }
                valueAnimator.start()
            }
            WaveMode.LEFT_RIGHT -> {
                runnable = object : Runnable {
                    override fun run() {
                        val last = bodyWaveList.pollLast()
                        if (last != null) {
                            bodyWaveList.addFirst(last)
                        }
                        invalidate()
                        valHandler.postDelayed(this, duration);
                    }
                }
                valHandler.post(runnable!!)
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        waveList.clear()
        waveList.addAll(headerWaveList)
        waveList.addAll(bodyWaveList)
        waveList.addAll(footerWaveList)

        linePath.reset()
        paintPathLine?.strokeWidth = lineWidth
        paintPathLine?.color = lineColor

        paintLine?.strokeWidth = lineWidth
        paintLine?.color = lineColor
        for (i in waveList.indices) {
            var startX = 0f
            var startY = 0f
            var endX = 0f
            var endY = 0f

            var offset = 1f
            if (i >= headerWaveList.size && i < (waveList.size - footerWaveList.size)) {
                //模式1 ，排除掉头尾
                offset = valueAnimatorOffset
            }

            val lineHeight = waveList[i] / 100.0 * measuredHeight * offset

            val absoluteGravity = Gravity.getAbsoluteGravity(showGravity, layoutDirection);

            when (absoluteGravity and Gravity.HORIZONTAL_GRAVITY_MASK) {
                Gravity.CENTER_HORIZONTAL -> {
                    val lineSize = waveList.size
                    val allLineWidth = lineSize * (lineSpace + lineWidth)
                    if (allLineWidth < measuredWidth) {
                        startX = (i * (lineSpace + lineWidth) + lineWidth / 2) + ((measuredWidth - allLineWidth) / 2)
                    } else {
                        startX = i * (lineSpace + lineWidth) + lineWidth / 2
                    }
                    endX = startX
                }

                Gravity.RIGHT -> {
                    val lineSize = waveList.size
                    val allLineWidth = lineSize * (lineSpace + lineWidth)
                    startX = if (allLineWidth < measuredWidth) {
                        (i * (lineSpace + lineWidth) + lineWidth / 2) + (measuredWidth - allLineWidth)
                    } else {
                        i * (lineSpace + lineWidth) + lineWidth / 2
                    }
                    endX = startX
                }

                Gravity.LEFT -> {
                    startX = i * (lineSpace + lineWidth) + lineWidth / 2
                    endX = startX
                }
            }


            when (showGravity and Gravity.VERTICAL_GRAVITY_MASK) {
                Gravity.TOP -> {
                    startY = 0f
                    endY = lineHeight.toFloat()
                }

                Gravity.CENTER_VERTICAL -> {
                    startY = (measuredHeight / 2 - lineHeight / 2).toFloat()
                    endY = (measuredHeight / 2 + lineHeight / 2).toFloat()
                }

                Gravity.BOTTOM -> {
                    startY = (measuredHeight - lineHeight).toFloat()
                    endY = measuredHeight.toFloat()
                }

            }
            if (lineType == LineType.BAR_CHART) {
                canvas.drawLine(
                    startX,
                    startY,
                    endX,
                    endY,
                    paintLine!!
                )
            }
            if (lineType == LineType.LINE_GRAPH) {
                if (i == 0) {
                    linePath.moveTo(startX, startY)
                    val pathEndX = endX + (lineWidth / 2) + (lineSpace / 2)
                    linePath.lineTo(pathEndX, endY)
                } else {
                    linePath.lineTo(startX, startY)
                    val pathEndX = endX + (lineWidth / 2) + (lineSpace / 2)
                    linePath.lineTo(pathEndX, endY)
                }
            }
        }
        if (lineType == LineType.LINE_GRAPH) {
            canvas.drawPath(linePath, paintPathLine!!)
        }
    }

    /**
     * 停止 onDestroy call
     */
    fun stop() {
        isStart = false
        if (runnable != null) {
            valHandler.removeCallbacks(runnable!!)
        }
        valueAnimator.cancel()
    }

    override fun onSaveInstanceState(): Parcelable? {
        // onSaveInstanceState
        return super.onSaveInstanceState()
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        // onRestoreInstanceState
        super.onRestoreInstanceState(state)
    }
}