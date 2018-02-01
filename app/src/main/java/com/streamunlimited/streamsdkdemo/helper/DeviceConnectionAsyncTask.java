package com.streamunlimited.streamsdkdemo.helper;

import android.os.AsyncTask;
import android.util.Log;

import com.streamunlimited.remotebrowser.RemoteBrowser;
import com.streamunlimited.streamsdkdemo.callbacks.IConnection;

public class DeviceConnectionAsyncTask extends AsyncTask<String, Void, Void> {

    private static final String TAG = DeviceConnectionAsyncTask.class.getSimpleName();

    private boolean _success = false;

    private String _connect;

    private IConnection _callback;
    private RemoteBrowser _browser;

    DeviceConnectionAsyncTask(IConnection callback, RemoteBrowser browser) {
        this._callback = callback;
        this._browser = browser;
    }

    @Override
    protected Void doInBackground(String... params) {
        _success = false;
        String ip = params[0];
        int port;
        try {
            port = Integer.parseInt(params[1]);
        } catch (NumberFormatException ex) {
            Log.e(TAG, "Cannot convert port number to integer: '" + params[1] + "'");
            return null;
        }
        _connect = params[2];

        switch (_connect) {
            case "connect":
                _success = _browser.connect(ip, port);
                break;
            case "disconnect":
                _success = _browser.disconnect(true, true);
                break;
            case "disconnectNoReset":
                _success = _browser.disconnect(true, false);
                break;
            default:
                break;
        }

        Log.d(TAG, "doInBackground: " + _connect + " result: " + _success);

        return null;
    }

    @Override
    protected void onPostExecute(Void unused) {
        Log.i(TAG, "onPostExecute: " + _connect + " " + _success);

        if (_connect.contains("disconnect")) {
            if (_callback == null) return;

            if (_success) {
                _callback.onDisconnectSuccess();
            } else {
                _callback.onDisconnectFailed();
            }
        } else if (_connect.contains("connect")) {
            if (_callback == null) return;

            if (_success) {
                _callback.onConnectSuccess();
            } else {
                _callback.onConnectFailed();
            }
        }
    }
}
