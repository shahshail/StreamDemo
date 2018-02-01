package com.streamunlimited.streamsdkdemo.helper;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.streamunlimited.remotebrowser.RemoteBrowser;
import com.streamunlimited.remotebrowser.RowEntryVector;
import com.streamunlimited.remotebrowser.ViewType;
import com.streamunlimited.streamsdkdemo.callbacks.IBrowseTask;
import com.streamunlimited.streamsdkdemo.callbacks.IBrowser;

/** ContentBrowseTask is only an IBrowser to update numItems. TODO: needed? */
public class ContentBrowseTask extends AsyncTask<Void, Void, Void> implements IBrowser {

    private static final String TAG = ContentBrowseTask.class.getSimpleName();
    private static final int MaxCache = 30;

    private int _numItems = MaxCache;

    private IBrowseTask _callback;
    private int _browserUpdateNr;
    private RowEntryVector _rows;
    private RemoteBrowser _currentDeviceBrowser;
    private DeviceManager _currentDeviceManager;
    private boolean _contextMenu;

    public ContentBrowseTask(Context ctx, IBrowseTask callback, int browserUpdateNr, boolean contextMenu) {
        Log.i(TAG, "ctor: sequence=" + browserUpdateNr);
        _callback = callback;
        _browserUpdateNr = browserUpdateNr;
        _currentDeviceManager = Devices.instance(ctx).getCurrentDevice();
        registerReceivers();
        _currentDeviceBrowser = _currentDeviceManager.getBrowser();
        _contextMenu = contextMenu;
    }

    @Override
    protected void onPostExecute(Void result) {
        if (_callback != null) _callback.onBrowseFinished(_rows, _browserUpdateNr);
        unregisterReceivers();
    }

    @Override
    protected Void doInBackground(Void... params) {
        if (_contextMenu) {
//            Log.d("Cache Items", "get items: from 0 to " + (MaxCache));
            _rows = _currentDeviceBrowser.getContextMenuItems(0, MaxCache);
        } else {
            _currentDeviceManager.onNumItemsChanged(_currentDeviceBrowser.getNumItems());
            int itemsToCache = MaxCache < _numItems ? MaxCache : _numItems;
//            Log.d("Cache Items", "get items: from 0 to " + (itemsToCache));
            _rows = _currentDeviceBrowser.getItems(0, itemsToCache);
        }
//        Log.d("Cache Items", "rows: " + _rows.size());
        return null;
    }

    @Override
    public void onViewChanged() {}

    @Override
    public void onNumItemsChanged(int numItems) {
        _numItems = numItems;
    }

    @Override
    public void onViewTypeChanged(ViewType type) {}

    private void registerReceivers() {
        unregisterReceivers();
        if (_currentDeviceManager != null) {
            _currentDeviceManager.addManagerBrowserCallback(this);
        }
    }

    private void unregisterReceivers() {
        if (_currentDeviceManager != null) {
            _currentDeviceManager.removeManagerBrowserCallback(this);
        }
    }

}
