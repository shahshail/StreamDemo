package com.streamunlimited.streamsdkdemo.ui.contentbrowsing;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v4.util.LruCache;
import android.util.Log;

import com.streamunlimited.streamsdkdemo.R;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by Kyrill Priesner on 29.02.16.
 */
public class ImageCache {
    private LruCache<String, Bitmap> _imageMemCache;

    private static final String TAG = ImageCache.class.getSimpleName();

    private CacheImageTask _cacheImageTask;

    private int _corePoolSize = 1;
    private int _maximumPoolSize = 1;
    private int _keepAliveTime = 10;
    private BlockingQueue<Runnable> _workQueue = new LinkedBlockingQueue<Runnable>(80);
    private ThreadPoolExecutor _threadPoolExecutor = new ThreadPoolExecutor(_corePoolSize, _maximumPoolSize, _keepAliveTime, TimeUnit.SECONDS, _workQueue);

    private Context myContext;


    public ImageCache(Context _context, int memClass){
        myContext = _context;
        Log.d(TAG, "memory class: " + Integer.toString(memClass));
        final int maxCacheSize = 1024 * 1024 * memClass / 3;
        Log.d(TAG, "max cache size: " + maxCacheSize);
        _imageMemCache = new LruCache<String, Bitmap>(maxCacheSize);
    }

    public void interruptCaching() {
        if (_cacheImageTask != null) {
            _threadPoolExecutor.shutdownNow();
        }
    }

    // bitmap helper functions

    private void addBitmapToMemCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null && bitmap != null) {
            _imageMemCache.put(key, bitmap);
        }
    }

    public Bitmap getBitmapFromMemCache(String key) {
        return _imageMemCache.get(key);
    }

    public void loadBitmap(String uri, CacheImageCallback callback) {
        if (_threadPoolExecutor.isShutdown()) {
            _threadPoolExecutor = new ThreadPoolExecutor(_corePoolSize, _maximumPoolSize, _keepAliveTime, TimeUnit.SECONDS, _workQueue);
        }
        _cacheImageTask = new CacheImageTask(callback);
        _cacheImageTask.executeOnExecutor(_threadPoolExecutor, uri);
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {

            // Calculate ratios of height and width to requested height and
            // width
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);

            // Choose the smallest ratio as inSampleSize value, this will
            // guarantee
            // a final image with both dimensions larger than or equal to the
            // requested height and width.
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }
        return inSampleSize;
    }

    public static Bitmap decodeSampledBitmapFromURI(String uri, int reqWidth, int reqHeight) {
        if (uri == null) {
            return null;
        }
        try {
            final BitmapFactory.Options options = new BitmapFactory.Options();
            URL url = new URL(uri);
            // First decode with inJustDecodeBounds=true to check dimensions
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(url.openStream(), null, options);
            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
            options.inJustDecodeBounds = false;
            Bitmap unscaled = BitmapFactory.decodeStream(url.openStream(), null, options);

            final int height = options.outHeight;
            final int width = options.outWidth;

            if (width > height) {
                reqWidth = Math.round(((float) width / (float) height) * reqWidth);
            } else {
                reqHeight = Math.round(((float) height / (float) width) * reqWidth);
            }
            if (reqWidth > 0 && reqHeight > 0 && unscaled != null) {
                Bitmap scaled = Bitmap.createScaledBitmap(unscaled, reqWidth, reqHeight, true);
                if (unscaled.getByteCount() < scaled.getByteCount()) {
                    return unscaled;
                } else {
                    return scaled;
                }
            } else {
                return unscaled;
            }

        } catch (MalformedURLException e) {
            return null;
        } catch (IOException e) {
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    // image caching
    public interface CacheImageCallback {
        void onCacheImageFinished();
    }

    public class CacheImageTask extends AsyncTask<String, Void, Bitmap> {

        private String _uri;
        CacheImageCallback _callback;

        public CacheImageTask(CacheImageCallback callback) {
            _callback = callback;
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            _uri = params[0];
            if (getBitmapFromMemCache(_uri) == null) {
                Bitmap bmp = decodeSampledBitmapFromURI(_uri, myContext.getResources().getDimensionPixelSize(R.dimen.folder_size), myContext.getResources().getDimensionPixelSize(R.dimen.folder_size));
                addBitmapToMemCache(_uri, bmp);
                return bmp;
            } else {
                return getBitmapFromMemCache(_uri);
            }
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            _callback.onCacheImageFinished();
        }
    }
}
