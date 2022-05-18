package com.inappstory.sdk.ugc.picker;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

class FilePreviewsHolder extends RecyclerView.ViewHolder {

    boolean isActive = false;
    String path;

    public FilePreviewsHolder(@NonNull View itemView) {
        super(itemView);
    }
}
