package com.inappstory.sdk.ugc.implementation;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.inappstory.sdk.AppearanceManager;
import com.inappstory.sdk.stories.ui.list.StoriesList;

public class StoriesListWithUGCEditor extends StoriesList {
    public StoriesListWithUGCEditor(@NonNull Context context) {
        super(context);
    }

    public StoriesListWithUGCEditor(@NonNull Context context, boolean isFavoriteList) {
        super(context, isFavoriteList);
    }

    public StoriesListWithUGCEditor(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public StoriesListWithUGCEditor(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setAppearanceManager(AppearanceManager appearanceManager) {
        appearanceManager.csHasUGC(true);
        super.setAppearanceManager(appearanceManager);
    }
}
