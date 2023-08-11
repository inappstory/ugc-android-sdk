package com.inappstory.sdk.ugc.editor

import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.widget.RelativeLayout
import com.inappstory.sdk.stories.utils.Sizes
import com.inappstory.sdk.ugc.IUGCReaderLoaderView
import com.inappstory.sdk.ugc.R
import kotlin.math.abs

class UGCLoadProgressBar(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) :
    View(context, attrs, defStyleAttr), IUGCReaderLoaderView {
    private var currentFrame = 0
    private var isIndeterminate = true
    private var progress = 0
    private var max = 100

    constructor(context: Context?) : this(context, null) {
        initSize()
    }


    private val strokeWidthDP = 4

    private val sizeDP = 36

    private var STROKE_WIDTH = Sizes.dpToPxExt(strokeWidthDP).toFloat()
    private var STROKE_SIZE_HALF = STROKE_WIDTH / 2

    private fun getColorPaint(resources: Resources): Paint? {
        if (COLOR_PAINT == null) {
            COLOR_PAINT = Paint()
            COLOR_PAINT!!.color =
                resources.getColor(R.color.cs_loaderColor)
            COLOR_PAINT!!.style = Paint.Style.STROKE
            COLOR_PAINT!!.strokeWidth = STROKE_WIDTH
            COLOR_PAINT!!.isAntiAlias = true
        }
        return COLOR_PAINT
    }

    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0) {
        initSize()
    }

    private fun initSize() {
        STROKE_WIDTH = Sizes.dpToPxExt(strokeWidthDP, context).toFloat()
        STROKE_SIZE_HALF = STROKE_WIDTH / 2
        val lp = RelativeLayout.LayoutParams(
            Sizes.dpToPxExt(sizeDP, context),
            Sizes.dpToPxExt(sizeDP, context)
        )
        lp.addRule(RelativeLayout.CENTER_IN_PARENT)
        layoutParams = lp
    }

    private var arcRect: RectF? = null

    init {
        initSize()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (arcRect == null) {
            arcRect = RectF(
                STROKE_SIZE_HALF, STROKE_SIZE_HALF,
                canvas.width - STROKE_SIZE_HALF,
                canvas.height - STROKE_SIZE_HALF
            )
        }
        if (isIndeterminate) {
            drawIndeterminate(canvas)
        } else {
            drawDeterminate(canvas)
        }
        invalidate()
    }

    private fun drawDeterminate(canvas: Canvas) {
        canvas.save()
        canvas.drawArc(
            arcRect!!,
            -90f,
            360 * (progress / 100f),
            false,
            getColorPaint(resources)!!
        )
        canvas.restore()
    }

    private fun drawIndeterminate(canvas: Canvas) {
        currentFrame++
        currentFrame %= 450
        val currentState = currentFrame % 90
        val value = if (currentState < 12 || currentState > 78) {
            0f
        } else if (currentState in 34..56) {
            1f
        } else if (currentState >= 57) {
            (currentState - 78f) / 22f //close
        } else {
            (currentState - 12f) / 22f //open
        }
        drawIndeterminateOutlineArc(canvas, 72f * (currentState % 90) / 90f, value)
    }

    private fun drawIndeterminateOutlineArc(canvas: Canvas, angle: Float, value: Float) {
        canvas.save()
        canvas.drawArc(
            arcRect!!,
            if (value > 0) -144 + angle else -144 + angle + 288 * (1 + value),
            288 * abs(value),
            false,
            getColorPaint(resources)!!
        )
        canvas.restore()
    }

    override fun setProgress(progress: Int, max: Int) {
        this.progress = progress
        this.max = max
    }

    override fun setIndeterminate(indeterminate: Boolean) {
        isIndeterminate = indeterminate
    }


    companion object {
        private var COLOR_PAINT: Paint? = null
    }

    override fun getView(context: Context): View {
        return this
    }
}