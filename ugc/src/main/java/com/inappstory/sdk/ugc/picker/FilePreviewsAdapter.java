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

    public FilePreviewsAdapter(Context context,
                               boolean isVideo,
                               List<String> mimeTypes,
                               FileClickCallback clickCallback,
                               OpenCameraClickCallback cameraCallback) {
        this.cameraCallback = cameraCallback;
        this.clickCallback = clickCallback;
        this.isVideo = isVideo;
        if (isVideo) {
            this.picker = new VideoPicker();
        } else {
            this.picker = new ImagePicker();
        }
        imagePath.addAll(picker.getImagesPath(context, mimeTypes));
    }

    @Override
    public void onViewRecycled(@NonNull FilePreviewsHolder holder) {
        super.onViewRecycled(holder);
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
        } else {
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.cs_file_picker_cell,
                    parent, false);
            v.setOnClickListener(clicked -> {
                activePos = viewType;
                clickCallback.select(imagePath.get(activePos - 1));
                notifyDataSetChanged();
            });
        }

        return new FilePreviewsHolder(v);
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) return -1;
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
        holder.itemView.getLayoutParams().width = Sizes.getScreenSize(holder.itemView.getContext()).x / 3;
        holder.itemView.getLayoutParams().height = (16 * holder.itemView.getLayoutParams().width) / 9;
        holder.itemView.requestLayout();
        if (position != 0) {
            holder.itemView.setSelected(position == activePos);
            ImageView iv = holder.itemView.findViewById(R.id.image);
            if (iv != null) {
                cache.loadPreview(imagePath.get(position - 1), iv, isVideo);
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
        return imagePath.size() + 1;
    }
}
