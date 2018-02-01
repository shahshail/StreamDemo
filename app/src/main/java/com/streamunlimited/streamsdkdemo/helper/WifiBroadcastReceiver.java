package com.streamunlimited.streamsdkdemo.helper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.streamunlimited.streamsdkdemo.callbacks.IWifiStateChanged;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */
public class WifiBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = WifiBroadcastReceiver.class.getSimpleName();

    private static WifiBroadcastReceiver _instance = null;
    public static WifiBroadcastReceiver instance() {
        if (_instance == null) {
            _instance = new WifiBroadcastReceiver();
        }
        return _instance;
    }

    private Map<String, IWifiStateChanged> _mapCallbacks = new ConcurrentHashMap<>();

    public void addCallback(IWifiStateChanged value) {
        String name = value.getClass().getName();
        _mapCallbacks.put(name, value);
        Log.d(TAG, "addCallback: " + name);
    }

    public void removeCallback(IWifiStateChanged value) {
        String name = value.getClass().getName();
        IWifiStateChanged tmp = _mapCallbacks.remove(name);
        if (tmp != null) {
            Log.d(TAG, "removeCallback: " + name);
        }
    }

    // events

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        boolean conn = _connected;

        if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION))
        {
            Log.v(TAG, "onReceive: NETWORK_STATE_CHANGED_ACTION");

            NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            NetworkInfo.State state = networkInfo.getState();

            if (state == NetworkInfo.State.CONNECTED) {
                conn = true;
            } else if (state == NetworkInfo.State.DISCONNECTED) {
                conn = false;
            }
        }

        Log.v(TAG, "onReceive conn: " + conn);

        if (conn != _connected) {
            _connected = conn;
            Log.d(TAG, "onReceive: network state changed, connected: " + _connected);

            Iterator<IWifiStateChanged> it0 = _mapCallbacks.values().iterator();
            while (it0.hasNext()) {
                IWifiStateChanged callback = it0.next();
                callback.onWifiStateChanged(_connected);
            }
        }
    }

    private String _ssid = "";
    public String getSSID() {
        return _ssid;
    }

    private boolean _connected = false;
    public boolean isConnected() {
        return _connected;
    }

}
