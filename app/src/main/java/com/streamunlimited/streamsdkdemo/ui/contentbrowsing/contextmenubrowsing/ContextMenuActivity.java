package com.streamunlimited.streamsdkdemo.ui.contentbrowsing.contextmenubrowsing;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.streamunlimited.streamsdkdemo.R;
import com.streamunlimited.streamsdkdemo.callbacks.IBitmapCache;
import com.streamunlimited.streamsdkdemo.callbacks.ITransition;
import com.streamunlimited.streamsdkdemo.data.BrowseRowEntry;
import com.streamunlimited.streamsdkdemo.helper.DeviceManager;
import com.streamunlimited.streamsdkdemo.helper.Devices;
import com.streamunlimited.streamsdkdemo.ui.contentbrowsing.ImageCache;

/**
 *
 */
public class ContextMenuActivity extends Activity implements ITransition, IBitmapCache {

    private static final String TAG = ContextMenuActivity.class.getSimpleName();

    private ContextMenuFragment _browseFragment;
    private DeviceManager _currentDeviceManager;
    private ImageCache _imageCache;
    private ViewHolder holder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.context_menu);
        setProgressBarIndeterminateVisibility(false);

        _imageCache = new ImageCache(this, ((ActivityManager) getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass());

        Intent i = getIntent();
        String title = i.getStringExtra("title");
        int numItems = i.getIntExtra("numItems", -1);

        _currentDeviceManager = Devices.instance(this).getCurrentDevice();

        if (_currentDeviceManager == null) {
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        holder = new ViewHolder(this);
        holder.configure(title);

        _browseFragment = new ContextMenuFragment();
        _browseFragment.registerReceivers();
        _browseFragment.setNumItems(numItems);

        getFragmentManager().beginTransaction()
                .add(R.id.content, _browseFragment)
                .commit();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        _browseFragment.onContextMenuViewChanged();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        _imageCache.interruptCaching();
    }

    @Override
    public void onBackPressed() {
        if (_currentDeviceManager.getBrowser().getCurrentContextMenuDepth() == 0) {
            closeContextMenu();
            return;
        }

        // TODO: check if this if-statement is necessary
        if (_browseFragment != null) {
            _currentDeviceManager.getBrowserManager().browseContextMenuParent();
        }
    }

    //----------------------------------------
    // ITransition
    //----------------------------------------

    @Override
    public void onBrowseItemSelected(BrowseRowEntry entry) {

    }

    @Override
    public void onShowProgressDialog() {

    }

    public void loadBitmap(String uri, ImageCache.CacheImageCallback callback) {
        _imageCache.loadBitmap(uri, callback);
    }

    public Bitmap getBitmapFromMemCache(String key) {
        return _imageCache.getBitmapFromMemCache(key);
    }

    public void closeContextMenu() {
        _currentDeviceManager.getBrowser().closeContextMenu();

        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.remove(_browseFragment);
        transaction.commit();

        setResult(RESULT_OK);

        finish();
    }

    private class ViewHolder {
        Button closeButton;
        TextView titleText;

        ViewHolder(Activity v) {
            closeButton = v.findViewById(R.id.close_btn);
            closeButton.setOnClickListener(clickListener);

            titleText = v.findViewById(R.id.title_txt);
        }

        void configure(String title) {
            titleText.setText(title);
        }

        private final View.OnClickListener clickListener = v -> closeContextMenu();

    }

}
