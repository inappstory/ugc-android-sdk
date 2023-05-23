package com.inappstory.sdk.ugc.picker;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;

import androidx.exifinterface.media.ExifInterface;

import android.graphics.Paint;
import android.media.ThumbnailUtils;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
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

    private final int MAX_THREADS = 6;

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

    public void loadPreview(String path, ImageView imageView, boolean unavailable, boolean isVideo) {
        if (isVideo) loadVideoThumbnail(path, imageView, unavailable);
        else loadBitmap(path, imageView, unavailable, noCache);
    }


    private void loadVideoThumbnail(String path, ImageView imageView, boolean unavailable) {
        Bitmap bmp = getBitmap(path);
        if (bmp == null) {

            executorService.submit(() -> {
                Bitmap loaded;
                if (unavailable) {
                    loaded = toGrayscale(ThumbnailUtils.createVideoThumbnail(path,
                            MediaStore.Video.Thumbnails.MINI_KIND));
                } else {
                    loaded = ThumbnailUtils.createVideoThumbnail(path,
                            MediaStore.Video.Thumbnails.MINI_KIND);
                }
                synchronized (memCacheLock) {
                    memoryCache.put(path, loaded);
                }
                try {
                    new Handler(Looper.getMainLooper()).post(() -> imageView.setImageBitmap(loaded));
                } catch (Exception ignored) {
                }
            });
        }
    }

    private void loadBitmap(String path, ImageView imageView, boolean noCache, boolean unavailable) {
        Bitmap bmp = null;
        if (!noCache)
            bmp = getBitmap(path);
        if (bmp == null) {
            if (noCache) {
                File file = new File(path);
                executorService.submit(() -> {
                    Bitmap loaded = unavailable ?
                            toGrayscale(decodeFile(file)) : decodeFile(file);
                    try {

                        new Handler(Looper.getMainLooper()).post(() ->
                                imageView.setImageBitmap(loaded));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            } else {
                addPriorityTask(path, imageView);
            }
        }
    }

    private Bitmap toGrayscale(Bitmap bmpOriginal) {
        if (bmpOriginal == null) return null;
        int width, height;
        height = bmpOriginal.getHeight();
        width = bmpOriginal.getWidth();

        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        Canvas c = new Canvas(bmpGrayscale);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bmpOriginal, 0, 0, paint);
        return bmpGrayscale;
    }

    private int getExifInformation(String filePath) throws IOException {
        ExifInterface ei = new ExifInterface(filePath);
        return ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
    }

    private Bitmap rotateImageIfRequired(Bitmap img, String filePath) throws IOException {
        int orientation = getExifInformation(filePath);
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

    private final Object queueLock = new Object();

    HashMap<String, QueuedTask> tasks = new HashMap<>();

    private String getMaxPriorityTaskKey() {
        synchronized (queueLock) {
            if (tasks.isEmpty()) return null;
            String minTaskKey = null;
            int minPriority = 99999;
            int startedCount = 0;
            for (String taskKey : tasks.keySet()) {
                QueuedTask cur = tasks.get(taskKey);
                if (cur == null) continue;
                if (cur.started) {
                    startedCount++;
                    continue;
                }
                if (startedCount >= MAX_THREADS) return null;
                if (minPriority > cur.priority) {
                    minPriority = cur.priority;
                    minTaskKey = taskKey;
                }
            }
            if (minTaskKey != null) {
                QueuedTask cur = tasks.get(minTaskKey);
                if (cur != null)
                    cur.started = true;
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
                    Bitmap loaded = decodeFile(new File(key));
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
                    } catch (Exception ignored) {
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

    private Bitmap decodeFile(File f) {
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(new FileInputStream(f), null, o);
            final int REQUIRED_SIZE = Sizes.dpToPxExt(200);
            int width_tmp = o.outWidth, height_tmp = o.outHeight;
            int scale = 1;
            while (width_tmp / 2 >= REQUIRED_SIZE || height_tmp / 2 >= REQUIRED_SIZE) {
                width_tmp /= 2;
                height_tmp /= 2;
                scale *= 2;
            }
            o.inJustDecodeBounds = false;
            o.inSampleSize = scale;
            Bitmap bitmap = BitmapFactory.decodeFile(f.getAbsolutePath(), o);
            return rotateImageIfRequired(bitmap, f.getAbsolutePath());
            //
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private final Object memCacheLock = new Object();

    private Bitmap getBitmap(String path) {
        synchronized (memCacheLock) {
            return memoryCache.get(path);
        }
    }
}
