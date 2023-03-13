package com.inappstory.sdk.ugc.picker;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.inappstory.sdk.stories.utils.Sizes;
import com.inappstory.sdk.ugc.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

class FilePreviewsAdapter extends RecyclerView.Adapter<FilePreviewsHolder> {
    FilePicker picker;
    List<String> imagePath = new ArrayList<>();
    boolean isVideo;
    FilePreviewsCache cache = new FilePreviewsCache();
    OpenCameraClickCallback cameraCallback;
    FileClickCallback clickCallback;
    NoAccessCallback noAccessCallback;
    boolean hasFileAccess;
    boolean allowMultipleSelection;
    String galleryAccessText;

    public FilePreviewsAdapter(Context context,
                               boolean isVideo,
                               boolean hasFileAccess,
                               boolean allowMultipleSelection,
                               List<String> mimeTypes,
                               FileClickCallback clickCallback,
                               OpenCameraClickCallback cameraCallback,
                               NoAccessCallback noAccessCallback,
                               String galleryAccessText) {
        this.noAccessCallback = noAccessCallback;
        this.galleryAccessText = galleryAccessText;
        this.cameraCallback = cameraCallback;
        this.clickCallback = clickCallback;
        this.isVideo = isVideo;
        this.hasFileAccess = hasFileAccess;
        this.allowMultipleSelection = allowMultipleSelection;
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

    public List<Integer> activePositions = new ArrayList<>();




    @Override
    public void onBindViewHolder(@NonNull FilePreviewsHolder holder, int position) {
        Integer intPos = Integer.valueOf(position - 1);
        if (position != 0) {
            if (hasFileAccess) {
                holder.itemView.setSelected(activePositions.contains(intPos));
                ImageView iv = holder.itemView.findViewById(R.id.image);
                if (iv != null) {
                    holder.path = imagePath.get(position - 1);
                    cache.loadPreview(imagePath.get(position - 1), iv, isVideo);
                }
                TextView count = holder.itemView.findViewById(R.id.count);
                if (activePositions.contains(intPos)) {
                    count.setText(Integer.toString(activePositions.indexOf(position - 1) + 1));
                    count.setVisibility(View.VISIBLE);
                } else {
                    count.setVisibility(View.GONE);
                }
                holder.itemView.setOnClickListener(v -> {
                    if (activePositions.contains(intPos)) {
                        clickCallback.unselect(imagePath.get(position - 1));
                        activePositions.remove(intPos);
                    } else {
                        activePositions.add(intPos);
                        clickCallback.select(imagePath.get(position - 1));
                        if (!allowMultipleSelection) {
                            Iterator<Integer> i = activePositions.iterator();
                            while (i.hasNext()) {
                                Integer activePosition = i.next();
                                if (activePosition.intValue() != intPos.intValue()) {
                                    clickCallback.unselect(imagePath.get(activePosition));
                                    i.remove();
                                }
                            }
                        }
                    }
                    notifyDataSetChanged();
                });
            } else {
                TextView text = holder.itemView.findViewById(R.id.gallery_access_text);
                text.setText(galleryAccessText);
                holder.itemView.setOnClickListener(v -> {
                    noAccessCallback.click();
                });
            }
        } else {
            holder.itemView.setOnClickListener(v -> {
                cameraCallback.open(isVideo);
            });
        }
    }


    @Override
    public int getItemCount() {
        return (hasFileAccess ? imagePath.size() : 1) + 1;
    }
}
