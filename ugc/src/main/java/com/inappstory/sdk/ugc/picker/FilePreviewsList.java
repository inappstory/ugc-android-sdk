package com.inappstory.sdk.ugc.picker;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class FilePreviewsList extends RecyclerView {
    public FilePreviewsList(@NonNull Context context) {
        super(context);
        init(context);
    }

    public FilePreviewsList(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public FilePreviewsList(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(@NonNull Context context) {
        setLayoutManager(new GridLayoutManager(context, 3,
                GridLayoutManager.VERTICAL,
                false));
    }

    public void load(boolean isVideo,
                     boolean hasFileAccess,
                     boolean allowMultipleSelection,
                     List<String> mimeTypes,
                     FileClickCallback clickCallback,
                     OpenCameraClickCallback cameraCallback,
                     NoAccessCallback noAccessCallback,
                     String galleryAccessText) {
        FilePreviewsAdapter adapter = new FilePreviewsAdapter(getContext(),
                isVideo,
                hasFileAccess,
                allowMultipleSelection,
                mimeTypes,
                clickCallback,
                cameraCallback,
                noAccessCallback,
                galleryAccessText
        );
        adapter.setHasStableIds(true);
        setAdapter(adapter);
    }


}
