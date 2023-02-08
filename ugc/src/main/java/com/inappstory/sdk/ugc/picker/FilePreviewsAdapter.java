package com.inappstory.sdk.ugc.picker;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.inappstory.sdk.stories.utils.Sizes;
import com.inappstory.sdk.ugc.R;

import java.util.ArrayList;
import java.util.List;

class FilePreviewsAdapter extends RecyclerView.Adapter<FilePreviewsHolder> {
    FilePicker picker;
    List<String> imagePath = new ArrayList<>();
    boolean isVideo;
    FilePreviewsCache cache = new FilePreviewsCache();
    OpenCameraClickCallback cameraCallback;
    FileClickCallback clickCallback;
    NoAccessCallback noAccessCallback;
    boolean hasFileAccess;

    public FilePreviewsAdapter(Context context,
                               boolean isVideo,
                               boolean hasFileAccess,
                               List<String> mimeTypes,
                               FileClickCallback clickCallback,
                               OpenCameraClickCallback cameraCallback,
                               NoAccessCallback noAccessCallback) {
        this.noAccessCallback = noAccessCallback;
        this.cameraCallback = cameraCallback;
        this.clickCallback = clickCallback;
        this.isVideo = isVideo;
        this.hasFileAccess = hasFileAccess;
        if (isVideo) {
            this.picker = new VideoPicker();
        } else {
            this.picker = new ImagePicker();
        }
        if (hasFileAccess) {
            imagePath.addAll(picker.getImagesPath(context, mimeTypes));
        }
    }


    @Override
    public void onViewRecycled(@NonNull FilePreviewsHolder holder) {
        super.onViewRecycled(holder);
        if (holder.path != null && cache != null) {
            cache.remove(holder.path);
        }
       /* ImageView iv = holder.itemView.findViewById(R.id.image);
        if (iv != null) {
         //   iv.setImageBitmap(null);
        }*/
    }

    @NonNull
    @Override
    public FilePreviewsHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v;
        if (viewType == -1) {
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.cs_file_camera_cell,
                    parent, false);
        } else if (viewType == -2) {
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.cs_file_no_access_cell,
                    parent, false);
        } else {
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.cs_file_picker_cell,
                    parent, false);
            v.setOnClickListener(clicked -> {
                activePos = viewType;
                clickCallback.select(imagePath.get(activePos - 1));
                notifyDataSetChanged();
            });
        }
        v.getLayoutParams().width = Sizes.getScreenSize(v.getContext()).x / 3;
        v.getLayoutParams().height = (16 * v.getLayoutParams().width) / 9;

        return new FilePreviewsHolder(v);
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) return -1;
        if (!hasFileAccess && position == 1) return -2;
        return position;
    }

    @Override
    public long getItemId(int position) {
        if (position == 0) return -1;
        return position;
    }

    public int activePos = -1;


    @Override
    public void onBindViewHolder(@NonNull FilePreviewsHolder holder, int position) {
        if (position != 0) {
            if (hasFileAccess) {
                holder.itemView.setSelected(position == activePos);
                ImageView iv = holder.itemView.findViewById(R.id.image);
                if (iv != null) {
                    holder.path = imagePath.get(position - 1);
                    cache.loadPreview(imagePath.get(position - 1), iv, isVideo);
                }
            } else {
                holder.itemView.setOnClickListener(v -> {
                    noAccessCallback.click();
                });
            }
        } else {
            holder.itemView.setOnClickListener(v -> {
                activePos = -1;
                clickCallback.unselect();
                cameraCallback.open(isVideo);
                notifyDataSetChanged();
            });
        }
    }


    @Override
    public int getItemCount() {
        return (hasFileAccess ? imagePath.size() : 1) + 1;
    }
}
