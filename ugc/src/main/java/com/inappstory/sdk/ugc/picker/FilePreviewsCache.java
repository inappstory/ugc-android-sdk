package com.inappstory.sdk.ugc.picker;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.media.ThumbnailUtils;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;

import com.inappstory.sdk.stories.utils.Sizes;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FilePreviewsCache {
    private LruCache<String, Bitmap> memoryCache;


    private static int MAX_THREADS = 6;
    ExecutorService executorService = Executors.newFixedThreadPool(MAX_THREADS);

    boolean noCache = false;

    public FilePreviewsCache(boolean noCache) {
        this.noCache = noCache;
        init();
    }

    public FilePreviewsCache() {
        init();
    }

    private void init() {
        if (noCache) return;
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = maxMemory / 16;
        memoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount() / 1024;
            }
        };

        handler.postDelayed(runnable, 100);
        handler.postDelayed(runnable2, 2000);
    }


    public void remove(String path) {
        synchronized (queueLock) {
            tasks.remove(path);
        }
    }

    public interface FileLoadCallback {
        void onLoaded();
    }

    public void loadPreview(String path, ImageView imageView, boolean isVideo) {
        if (isVideo) loadVideoThumbnail(path, imageView);
        else loadBitmap(path, imageView, noCache);
    }


    private void loadVideoThumbnail(String path, ImageView imageView) {
        Bitmap bmp = getBitmap(path);
        if (bmp == null) {

            executorService.submit(() -> {
                Bitmap loaded = ThumbnailUtils.createVideoThumbnail(path,
                        MediaStore.Video.Thumbnails.MINI_KIND);
                synchronized (memCacheLock) {
                    memoryCache.put(path, loaded);
                }
                try {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        imageView.setImageBitmap(loaded);
                    });
                } catch (Exception e) {
                }
            });
        }
    }

    private void loadBitmap(String path, ImageView imageView, boolean noCache) {
        Bitmap bmp = null;
        if (!noCache)
            bmp = getBitmap(path);
        if (bmp == null) {
            if (noCache) {
                File file = new File(path);
                executorService.submit(() -> {
                    Bitmap loaded = decodeFile(file, true);
                    try {
                        new Handler(Looper.getMainLooper()).post(() -> {
                            imageView.setImageBitmap(loaded);
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            } else {
                addPriorityTask(path, imageView);
            }
        }
    }

    public static int getExifInformation(String filePath) throws IOException {
        ExifInterface ei = new ExifInterface(filePath);
        return ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
    }

    private static Bitmap rotateImageIfRequired(Bitmap img, String filePath) throws IOException {
        int orientation = getExifInformation(filePath);
        Log.e("exif_orientation", "" + orientation);
        switch (orientation) {
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                return flipImage(img);
            case ExifInterface.ORIENTATION_ROTATE_90:
                return rotateImage(img, 90);
            case ExifInterface.ORIENTATION_ROTATE_180:
                return rotateImage(img, 180);
            case ExifInterface.ORIENTATION_ROTATE_270:
                return rotateImage(img, 270);
            default:
                return img;
        }
    }

    private class QueuedTask {

        QueuedTask(ImageView imageView) {
            this.imageView = imageView;
        }

        int priority = 0;
        ImageView imageView;
        boolean started = false;
    }

    private Object queueLock = new Object();

    HashMap<String, QueuedTask> tasks = new HashMap<>();

    private String getMaxPriorityTaskKey() {
        synchronized (queueLock) {
            if (tasks.isEmpty()) return null;
            String minTaskKey = null;
            int minPriority = 99999;
            int startedCount = 0;
            for (String taskKey : tasks.keySet()) {
                if (tasks.get(taskKey).started) {
                    startedCount++;
                    continue;
                }
                if (startedCount >= MAX_THREADS) return null;
                if (minPriority > tasks.get(taskKey).priority) {
                    minPriority = tasks.get(taskKey).priority;
                    minTaskKey = taskKey;
                }
            }
            if (minTaskKey != null) {
                tasks.get(minTaskKey).started = true;
            }
            return minTaskKey;
        }
    }

    Handler handler = new Handler();

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            checkAndLoadTask();
            handler.postDelayed(runnable, 100);
        }
    };

    Runnable runnable2 = new Runnable() {
        @Override
        public void run() {
            handler.postDelayed(runnable2, 2000);
            synchronized (memCacheLock) {
                executorService.shutdownNow();
                executorService = Executors.newFixedThreadPool(MAX_THREADS);
            }
        }
    };

    private void checkAndLoadTask() {
        String key = getMaxPriorityTaskKey();
        if (key != null) {
            synchronized (memCacheLock) {
                executorService.submit(() -> {
                    Bitmap loaded = decodeFile(new File(key), noCache);
                    QueuedTask task = tasks.get(key);
                    if (!noCache) {
                        memoryCache.put(key, loaded);
                        remove(key);
                    }
                    try {
                        new Handler(Looper.getMainLooper()).post(() -> {
                            if (task != null)
                                task.imageView.setImageBitmap(loaded);
                        });
                    } catch (Exception e) {
                    }
                });
            }
        }
    }

    private void addPriorityTask(String key, ImageView imageView) {
        synchronized (memCacheLock) {
            if (memoryCache.get(key) != null) return;
        }
        synchronized (queueLock) {
            for (QueuedTask task : tasks.values()) {
                task.priority++;
            }
            tasks.put(key, new QueuedTask(imageView));
        }
    }

    private static Bitmap flipImage(Bitmap img) {
        Matrix matrix = new Matrix();
        matrix.postScale(-1.0f, 1.0f);
        Bitmap rotatedImg = Bitmap.createBitmap(img, 0, 0,
                img.getWidth(), img.getHeight(), matrix, true);
        img.recycle();
        return rotatedImg;
    }

    private static Bitmap rotateImage(Bitmap img, int degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        Bitmap rotatedImg = Bitmap.createBitmap(img, 0, 0,
                img.getWidth(), img.getHeight(), matrix, true);
        img.recycle();
        return rotatedImg;
    }

    private Bitmap decodeFile(File f, boolean noCache) {
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(new FileInputStream(f), null, o);
            final int REQUIRED_SIZE = Sizes.dpToPxExt(200);
            int width_tmp = o.outWidth, height_tmp = o.outHeight;
            int scale = 1;
            while (true) {
                if (width_tmp / 2 < REQUIRED_SIZE && height_tmp / 2 < REQUIRED_SIZE)
                    break;
                width_tmp /= 2;
                height_tmp /= 2;
                scale *= 2;
            }
            o.inJustDecodeBounds = false;
            o.inSampleSize = scale;
            //  o.inBitmap
            //  FileInputStream fileInputStream = new FileInputStream(f);
            Bitmap bitmap = BitmapFactory.decodeFile(f.getAbsolutePath(), o);
            return rotateImageIfRequired(bitmap, f.getAbsolutePath());
            //
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }
        return null;
    }

    private Object memCacheLock = new Object();

    private Bitmap getBitmap(String path) {
        synchronized (memCacheLock) {
            return memoryCache.get(path);
        }
    }
}
