package com.inappstory.sdk.ugc.camera;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.inappstory.sdk.stories.utils.Sizes;
import com.inappstory.sdk.ugc.R;

public class CameraButton extends RelativeLayout {

    GradientDrawable gradientDrawable;
    View animatedView;
    public boolean isVideoButton = false;
    public CameraButton(@NonNull Context context) {
        super(context);
        init(context);
    }

    public CameraButton(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CameraButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(@NonNull Context context) {

        GradientDrawable nonAnimatedGradientDrawable = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{Color.WHITE, Color.WHITE});
        nonAnimatedGradientDrawable.setShape(GradientDrawable.OVAL);
        setBackground(nonAnimatedGradientDrawable);
        setPadding(Sizes.dpToPxExt(2), Sizes.dpToPxExt(2), Sizes.dpToPxExt(2), Sizes.dpToPxExt(2));
        setBackground(nonAnimatedGradientDrawable);
        animatedView = new FrameLayout(context);
        animatedView.setLayoutParams(
                new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
                gradientDrawable = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{Color.RED, Color.RED});
        gradientDrawable.setCornerRadius(200.0f);
        gradientDrawable.setShape(GradientDrawable.RECTANGLE);
        animatedView.setBackground(gradientDrawable);
        addView(animatedView);
    }


    @Override
    public void setOnClickListener(@Nullable OnClickListener l) {
        super.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isVideoButton) {
                    if (started) {
                        stop();
                    } else {
                        start();
                    }
                }
                l.onClick(view);
            }
        });
    }

    public void start() {
        started = true;
        ObjectAnimator cornerAnimation = ObjectAnimator.ofFloat(gradientDrawable, "cornerRadius", 200.0f, 30f);
        Animator shiftAnimation = AnimatorInflater.loadAnimator(getContext(), R.animator.cs_scale_down);
        shiftAnimation.setTarget(animatedView);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.setDuration(500);
        animatorSet.playTogether(cornerAnimation, shiftAnimation);
        animatorSet.start();
    }

    boolean started = false;

    public void stop() {
        started = false;
        ObjectAnimator cornerAnimation = ObjectAnimator.ofFloat(gradientDrawable, "cornerRadius", 30f, 200.0f);
        Animator shiftAnimation = AnimatorInflater.loadAnimator(getContext(), R.animator.cs_scale_up);
        shiftAnimation.setTarget(animatedView);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.setDuration(500);
        animatorSet.playTogether(cornerAnimation, shiftAnimation);
        animatorSet.start();
    }
}
