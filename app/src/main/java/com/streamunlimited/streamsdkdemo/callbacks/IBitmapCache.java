package com.streamunlimited.streamsdkdemo.callbacks;

import android.graphics.Bitmap;

/**
 *
 */
public interface IBitmapCache {
    Bitmap getBitmapFromMemCache(String key);
}
