package com.inappstory.sdk.ugc.cache;

public interface ProgressCallback {
    void onProgress(long loadedSize, long totalSize);
}