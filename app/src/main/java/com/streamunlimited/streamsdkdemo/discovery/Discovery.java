package com.streamunlimited.streamsdkdemo.discovery;

import android.content.Context;
import android.util.Log;

import com.streamunlimited.streamsdkdemo.data.DeviceRowEntry;
import com.streamunlimited.streamsdkdemo.helper.Devices;
import com.streamunlimited.streamsdkdemo.helper.StreamControlRemoteBrowserManager;
import com.streamunlimited.streamsdkdemo.helper.WifiBroadcastReceiver;

public class Discovery {

    interface DeviceAddedCallbacks {
        void onDeviceAdded(DeviceRowEntry entry);
        void onDeviceRemoved(String uuid);
    }

    private static String getGroupSourceUUID(String ip, int port) {
        return (new StreamControlRemoteBrowserManager()).browser().getGroup(ip, port);
    }

    private static final String TAG = Discovery.class.getSimpleName();

    private static final String TypeStreamApiGroupping = "_sueGrouping._tcp.local.";
    private static final String TypeStreamApiS800Device = "_sueS800Device._tcp.local.";

    private static Discovery _instance;
    public static synchronized Discovery instance(Context ctx) {
        if (_instance == null) {
            _instance = new Discovery(ctx);
        }
        return _instance;
    }

    private final Context ctx;
    private final JmdnsDiscoveryHelper _jmdnsHelperGroupping;
    private final JmdnsDiscoveryHelper _jmdnsHelperS800Device;

    private Discovery(Context ctx) {
        this.ctx = ctx.getApplicationContext();
        _jmdnsHelperGroupping = new JmdnsDiscoveryHelper(ctx, TypeStreamApiGroupping, _callbacks);
        _jmdnsHelperS800Device = new JmdnsDiscoveryHelper(ctx, TypeStreamApiS800Device, _callbacks);
    }

    public void start() {
        _isRunning = true;

        if (!WifiBroadcastReceiver.instance().isConnected()) {
            Log.w(TAG, "start: wifi not connected, aborting");
            return;
        }
        _jmdnsHelperGroupping.startDiscovery();
        _jmdnsHelperS800Device.startDiscovery();
    }

    public void stop() {
        _jmdnsHelperGroupping.stopDiscovery();
        _jmdnsHelperS800Device.stopDiscovery();
        _isRunning = false;
    }

    public void restart() {
        if (!WifiBroadcastReceiver.instance().isConnected()) {
            Log.w(TAG, "restart: wifi not connected, aborting");
            return;
        }
        _jmdnsHelperGroupping.restartDiscovery();
        _jmdnsHelperS800Device.restartDiscovery();
    }

    public void refresh() {
        if (!WifiBroadcastReceiver.instance().isConnected()) {
            Log.w(TAG, "refresh: wifi not connected, aborting");
            return;
        }
    }

    private boolean _isRunning = false;
    public boolean isRunning() {
        return _isRunning;
    }

    public void group(DeviceRowEntry receiver, DeviceRowEntry source) {
        if (!receiver.getMultiroomSupported() || !source.getMultiroomSupported()) {
            Log.e(TAG, "group: multiroom is not supported" +
                    " receiver=" + receiver.getName() + " " + receiver.getMultiroomSupported() +
                    ", source " +source.getName() + " " + source.getMultiroomSupported());
            return;
        }

        Log.i(TAG, "group: receiver=" + receiver.getName() + ", source=" + source.getName());
        receiver.setSourceUUID(source.getUUID());

        try {
            Devices.instance(ctx).get(receiver.getUUID()).getBrowser().stop();
            Devices.instance(ctx).get(source.getUUID()).getBrowser().group(receiver.getIpAddress(), receiver.getPort(), source.getUUID(), source.getIpAddress(), source.getName());
            Devices.instance(ctx).update(receiver);
        } catch (NullPointerException npe) {
            Log.e(TAG, "group: receiver=" + receiver.getUUID() + ", source=" + source.getUUID());
        }
    }

    public void ungroup(DeviceRowEntry entry) {
        if (entry.getMultiroomSupported()) {
            entry.setSourceUUID(entry.getUUID());

            try {
                Devices.instance(ctx).get(entry.getUUID()).getBrowser().ungroup(entry.getIpAddress(), entry.getPort());
                Devices.instance(ctx).update(entry);
            } catch (NullPointerException npe) {
                Log.w(TAG, "npe: DeviceManager of the device: " + entry.getName() + " (" + entry.getUUID() + ")");
            }
        } else {
            Log.e(TAG, "Error when ungrouping: multiroom is not supported.");
            Log.e(TAG, "Error when ungrouping: " + entry.getName() + " multiroom: " + entry.getMultiroomSupported());
        }
    }

    private void addEntry(DeviceRowEntry entry) {
        String sourceUUID = getGroupSourceUUID(entry.getIpAddress(), entry.getPort());
        Log.d(TAG, "addEntryJmDNS entry name: " + entry.getName() + " uuid: " + entry.getUUID() + " parent uuid: " + sourceUUID);
        entry.setSourceUUID(sourceUUID);

        Devices.instance(ctx).add(entry);
    }

    private void removeEntry(String uuid) {
        Devices.instance(ctx).remove(uuid);
    }

    private final DeviceAddedCallbacks _callbacks = new DeviceAddedCallbacks() {
        @Override
        public void onDeviceAdded(DeviceRowEntry entry) {
            Log.d(TAG, "entry onDeviceAdded: " + entry.getName() + " " + entry.getUUID() + " " + entry.getIpAddress());
            addEntry(entry);
        }

        @Override
        public void onDeviceRemoved(String uuid) {
            Log.d(TAG, "entry onDeviceRemoved: " + uuid);
            removeEntry(uuid);
        }
    };
}
