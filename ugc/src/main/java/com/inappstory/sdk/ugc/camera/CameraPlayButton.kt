package com.inappstory.sdk.ugc.camera

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.animation.AnimatorSet


import android.animation.ObjectAnimator
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.widget.FrameLayout
import android.widget.RelativeLayout


class CameraPlayButton: View {


    var gradientDrawable: GradientDrawable? = null

    constructor(context: Context) : super(context) {
        initialize()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initialize()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initialize()
    }

    fun initialize() {
        /* addView(View(context).also {
            it.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            it.setBackgroundColor(Color.RED)
        })

         gradientDrawable = GradientDrawable()
         gradientDrawable?.cornerRadius = 30.0f
         gradientDrawable?.shape = GradientDrawable.RECTANGLE*/
    }

    override fun setOnClickListener(l: OnClickListener?) {
        if (started) {
            stop()
        } else {
            start()
        }
        super.setOnClickListener(l)
    }

    var started = false


    fun stop() {
        started = true
        val cornerAnimation = ObjectAnimator.ofFloat(gradientDrawable, "cornerRadius", 30f, 200.0f)

        val scaleDownX = ObjectAnimator.ofFloat(gradientDrawable, SCALE_X.name, 1f)
        val scaleDownY = ObjectAnimator.ofFloat(gradientDrawable, SCALE_Y.name, 1f)
        val animatorSet = AnimatorSet()
        animatorSet.duration = 500
        animatorSet.playTogether(cornerAnimation, scaleDownX, scaleDownY)
        animatorSet.start()
    }

    fun start() {
        started = true
        val cornerAnimation = ObjectAnimator.ofFloat(gradientDrawable, "cornerRadius", 200.0f, 30f)

        val scaleDownX = ObjectAnimator.ofFloat(gradientDrawable, SCALE_X.name, 0.5f)
        val scaleDownY = ObjectAnimator.ofFloat(gradientDrawable, SCALE_Y.name, 0.5f)
        val animatorSet = AnimatorSet()
        animatorSet.duration = 500
        animatorSet.playTogether(cornerAnimation, scaleDownX, scaleDownY)
        animatorSet.start()
    }
}