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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FilePreviewsCache {
    private LruCache<String, Bitmap> memoryCache;

    ExecutorService executorService = Executors.newSingleThreadExecutor();

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
    }


    public void remove(String path, ImageView imageView) {

    }

    public interface FileLoadCallback {
        void onLoaded();
    }

    public void loadPreview(String path, ImageView imageView, boolean isVideo, FileLoadCallback callback) {
        if (isVideo) loadVideoThumbnail(path, imageView, callback);
        else loadBitmap(path, imageView, noCache, callback);
    }


    private void loadVideoThumbnail(String path, ImageView imageView, FileLoadCallback callback) {
        Bitmap bmp = getBitmap(path);
        if (bmp == null) {
            executorService.submit(() -> {
                Bitmap loaded = ThumbnailUtils.createVideoThumbnail(path,
                        MediaStore.Video.Thumbnails.MINI_KIND);
                memoryCache.put(path, loaded);
                try {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (callback != null) callback.onLoaded();
                        imageView.setImageBitmap(loaded);
                    });
                } catch (Exception e) {
                }
            });
        }
    }

    private void loadBitmap(String path, ImageView imageView, boolean noCache, FileLoadCallback callback) {
        Bitmap bmp = null;
        if (!noCache)
            bmp = getBitmap(path);
        if (bmp == null) {
            File file = new File(path);
            executorService.submit(() -> {
                Bitmap loaded = decodeFile(file);
                if (!noCache)
                    memoryCache.put(path, loaded);
                try {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (callback != null) callback.onLoaded();
                        imageView.setImageBitmap(loaded);
                    });
                } catch (Exception e) {
                }
            });
        }
    }

    private static Bitmap rotateImageIfRequired(Bitmap img, String filePath) throws IOException {

        ExifInterface ei = new ExifInterface(filePath);

        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        Log.e("exif_orientation", "" + orientation);
        switch (orientation) {
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
            final int REQUIRED_SIZE = Sizes.dpToPxExt(300);
            int width_tmp = o.outWidth, height_tmp = o.outHeight;
            int scale = 1;
            while (true) {
                if (width_tmp / 2 < REQUIRED_SIZE || height_tmp / 2 < REQUIRED_SIZE)
                    break;
                width_tmp /= 2;
                height_tmp /= 2;
                scale *= 2;
            }
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            FileInputStream fileInputStream = new FileInputStream(f);
            Bitmap bitmap = BitmapFactory.decodeStream(fileInputStream, null, o2);
            fileInputStream.close();
            return rotateImageIfRequired(bitmap, f.getAbsolutePath());
        } catch (Exception ignored) {

        }
        return null;
    }

    private Bitmap getBitmap(String path) {
        return memoryCache.get(path);
    }
}
