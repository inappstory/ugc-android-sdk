package com.inappstory.sdk.ugc.camerax

import android.widget.RelativeLayout
import android.graphics.drawable.GradientDrawable
import com.inappstory.sdk.ugc.camerax.CameraButton.OnAction
import com.inappstory.sdk.stories.utils.Sizes
import android.widget.FrameLayout
import android.view.ViewGroup
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.animation.ObjectAnimator
import android.animation.AnimatorInflater
import com.inappstory.sdk.ugc.R
import android.animation.AnimatorSet
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.Log
import android.view.View

class CameraButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : RelativeLayout(context, attrs, defStyle) {

    interface OnAction {
        fun onClick()
        fun onLongPressDown()
        fun onLongPressUp()
    }

    var started = false

    lateinit var actions: OnAction
    private var gradientDrawable: GradientDrawable
    private var animatedView: View
    private lateinit var mGestureDetector: GestureDetector

    init {
        val nonAnimatedGradientDrawable = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(
                Color.WHITE, Color.WHITE
            )
        )
        nonAnimatedGradientDrawable.shape = GradientDrawable.OVAL
        background = nonAnimatedGradientDrawable
        setPadding(Sizes.dpToPxExt(2), Sizes.dpToPxExt(2), Sizes.dpToPxExt(2), Sizes.dpToPxExt(2))
        background = nonAnimatedGradientDrawable
        animatedView = FrameLayout(context)
        animatedView.layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        gradientDrawable = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(
                Color.RED, Color.RED
            )
        )
        gradientDrawable.cornerRadius = 200.0f
        gradientDrawable.shape = GradientDrawable.RECTANGLE
        animatedView.background = gradientDrawable
        addView(animatedView)
        setGestureDetector(context)
    }



    private fun setGestureDetector(context: Context) {
        mGestureDetector = GestureDetector(context,
            object : SimpleOnGestureListener() {
                override fun onDown(e: MotionEvent): Boolean {
                    onLongPressed = false
                    return true
                }

                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {

                    actions.onClick()
                    return super.onSingleTapConfirmed(e)
                }

                override fun onLongPress(e: MotionEvent) {
                    onLongPressed = true
                    start()
                    actions.onLongPressDown()
                }
            })
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP ||
            event.action == MotionEvent.ACTION_CANCEL
        ) {
            if (onLongPressed) {
                actions.onLongPressUp()
                stop()
            }
        }
        return mGestureDetector.onTouchEvent(event)
    }

    private var onLongPressed = false

    fun start() {
        started = true
        val cornerAnimation = ObjectAnimator.ofFloat(gradientDrawable, "cornerRadius", 200.0f, 30f)
        val shiftAnimation = AnimatorInflater.loadAnimator(context, R.animator.cs_scale_down)
        shiftAnimation.setTarget(animatedView)
        val animatorSet = AnimatorSet()
        animatorSet.duration = 500
        animatorSet.playTogether(cornerAnimation, shiftAnimation)
        animatorSet.start()
    }

    fun stop() {
        if (!started) return
        started = false
        val cornerAnimation = ObjectAnimator.ofFloat(gradientDrawable, "cornerRadius", 30f, 200.0f)
        val shiftAnimation = AnimatorInflater.loadAnimator(context, R.animator.cs_scale_up)
        shiftAnimation.setTarget(animatedView)
        val animatorSet = AnimatorSet()
        animatorSet.duration = 500
        animatorSet.playTogether(cornerAnimation, shiftAnimation)
        animatorSet.start()
    }
}