package com.inappstory.sdk.ugc.picker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.inappstory.sdk.stories.utils.Sizes;
import com.inappstory.sdk.ugc.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

class FilePreviewsAdapter extends RecyclerView.Adapter<FilePreviewsHolder> {
    FilePicker picker;
    List<FilePicker.FileData> previews = new ArrayList<>();
    FilePreviewsCache cache = new FilePreviewsCache();
    OpenCameraClickCallback cameraCallback;
    FileClickCallback clickCallback;
    NoAccessCallback noAccessCallback;
    boolean hasFileAccess;
    boolean allowMultipleSelection;
    String galleryAccessText;
    int galleryFileMaxCount;
    String galleryFileLimitText;

    public FilePreviewsAdapter(Context context,
                               boolean hasFileAccess,
                               boolean allowMultipleSelection,
                               List<String> mimeTypes,
                               FileClickCallback clickCallback,
                               OpenCameraClickCallback cameraCallback,
                               NoAccessCallback noAccessCallback,
                               String galleryAccessText,
                               int galleryFileMaxCount,
                               String galleryFileLimitText) {
        this.noAccessCallback = noAccessCallback;
        this.galleryFileMaxCount = galleryFileMaxCount;
        this.galleryFileLimitText = galleryFileLimitText;
        this.galleryAccessText = galleryAccessText;
        this.cameraCallback = cameraCallback;
        this.clickCallback = clickCallback;
        this.hasFileAccess = hasFileAccess;
        this.allowMultipleSelection = allowMultipleSelection;
        this.picker = new PhotoVideoPicker();
        if (hasFileAccess) {
            previews.addAll(
                    picker.getImagesPath(
                            context,
                            new PickerFilter(30000000L, 30000L),
                            mimeTypes
                    )
            );
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

    @SuppressLint("DefaultLocale")
    private String convertLongToTime(long seconds) {
        long s = seconds % 60;
        long m = (seconds / 60) % 60;
        long h = (seconds / (60 * 60)) % 24;
        return seconds >= 3600 ? String.format("%02d:%02d:%02d", h, m, s) : String.format("%02d:%02d", m, s);
    }

    @Override
    public void onBindViewHolder(@NonNull FilePreviewsHolder holder, int position) {
        Integer intPos = Integer.valueOf(position - 1);
        if (position != 0) {
            if (hasFileAccess) {
                FilePicker.FileData data = previews.get(position - 1);
                String path = data.component1();
                Long duration = data.component2();
                holder.itemView.setSelected(activePositions.contains(intPos));
                ImageView iv = holder.itemView.findViewById(R.id.image);
                if (iv != null) {
                    holder.path = path;
                    cache.loadPreview(path, iv, duration != null);
                }
                TextView count = holder.itemView.findViewById(R.id.count);
                TextView videoDuration = holder.itemView.findViewById(R.id.videoDuration);
                if (duration != null) {
                    videoDuration.setVisibility(View.VISIBLE);
                    videoDuration.setText(convertLongToTime(duration / 1000));
                } else {
                    videoDuration.setVisibility(View.GONE);
                }
                if (activePositions.contains(intPos)) {
                    count.setText(Integer.toString(activePositions.indexOf(position - 1) + 1));
                    count.setVisibility(View.VISIBLE);
                } else {
                    count.setVisibility(View.GONE);
                }
                holder.itemView.setOnClickListener(v -> {
                    if (activePositions.contains(intPos)) {
                        clickCallback.unselect(path);
                        activePositions.remove(intPos);
                    } else {
                        if (activePositions.size() >= galleryFileMaxCount) {
                            Toast.makeText(
                                    holder.itemView.getContext(),
                                    galleryFileLimitText,
                                    Toast.LENGTH_SHORT
                            ).show();
                            return;
                        }
                        activePositions.add(intPos);
                        clickCallback.select(path);
                        if (!allowMultipleSelection) {
                            Iterator<Integer> i = activePositions.iterator();
                            while (i.hasNext()) {
                                Integer activePosition = i.next();
                                if (activePosition.intValue() != intPos.intValue()) {
                                    clickCallback.unselect(previews.get(activePosition).component1());
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
                cameraCallback.open();
            });
        }
    }


    @Override
    public int getItemCount() {
        return (hasFileAccess ? previews.size() : 1) + 1;
    }
}
