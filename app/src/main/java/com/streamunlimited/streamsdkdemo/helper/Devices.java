package com.streamunlimited.streamsdkdemo.helper;

import android.content.Context;
import android.util.Log;

import com.streamunlimited.streamsdkdemo.callbacks.IDeviceList;
import com.streamunlimited.streamsdkdemo.data.DeviceRowEntry;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Devices is a collection representing the available streamers on the network.
 */
public class Devices {

    private static final String TAG = Devices.class.getSimpleName();

    private static Devices _instance;
    public static synchronized Devices instance(Context ctx) {
        if (_instance == null) {
            _instance = new Devices(ctx);
        }
        return _instance;
    }

    /**
     * Checks if the UUID matches the one of the current device and returns boolean
     *
     * @param deviceUuid The UUID of the device to check
     * @return boolean Returns true if it is the same, otherwise false
     */
    static boolean isCurrentDevice(Context ctx, String deviceUuid) {
        DeviceManager d = Devices.instance(ctx)._currentDevice;
        return d != null &&
                deviceUuid != null &&
                d.getDeviceRowEntry().getUUID().compareTo(deviceUuid) == 0;
    }

    private final ConcurrentHashMap<String, DeviceManager> _deviceMap = new ConcurrentHashMap<>();
    private final ExecutorService pingThreadPool = Executors.newCachedThreadPool();
    private final Context ctx;

    private IDeviceList _observer;
    public void setObserver(IDeviceList value) {
        _observer = value;
    }

    private Devices(Context ctx) {
        this.ctx = ctx.getApplicationContext();
    }

    /**
     * Creates a DeviceManager object from the deviceRowEntry and then adds it to the container.
     *
     * @param deviceRowEntry is the DeviceRowEntry
     * @return the success of adding the new DeviceManager object
     */
    public synchronized boolean add(DeviceRowEntry deviceRowEntry) {
        String uuid = deviceRowEntry.getUUID();
        String name = deviceRowEntry.getName();

        boolean here = _deviceMap.containsKey(uuid);
        if (here) {
            update(deviceRowEntry);
            return false;
        }

        Log.i(TAG, "add: " + uuid + " " + name);
        _deviceMap.put(uuid, new DeviceManager(ctx, deviceRowEntry));
        if (_observer != null) _observer.onDeviceListChanged();
        return true;
    }

    public void update(DeviceRowEntry e) {
        String uuid = e.getUUID();
        String name = e.getName();
        Log.i(TAG, "update: " + uuid + " " + name);

        DeviceManager dm = _deviceMap.get(uuid);

        if (!dm.getDeviceRowEntry().equals(e)) {
            if (dm.getDeviceRowEntry().getMultiroomSupported()) {
                e.setMultiroomSupported(true);
            }
            dm.setDeviceRowEntry(e);
        }

        if (_observer != null) _observer.onDeviceListChanged();
    }

    public void remove(DeviceManager dm) {
        Log.d(TAG, "remove() DeviceManager name: " + dm.getDeviceRowEntry().getName() + " uuuid: " + dm.getDeviceRowEntry().getUUID());
        remove(dm.getDeviceRowEntry().getUUID());
    }

    public void remove(String uuid) {
        Log.d(TAG, "remove: uuuid=" + uuid);

        DeviceManager dm = _deviceMap.get(uuid);
        if (dm == null) return;

        boolean ok = dm.getBrowser().disconnect(true, true);
        Log.d(TAG, "remove: disconnect OK? " + ok);
        if (_observer != null) _observer.onDeviceListChanged();
    }

    public void clearAll() {
        resetAll();
        _deviceMap.clear();
    }

    private void resetAll() {
        for (DeviceManager dm : getAll()) {
            dm.reset(true);
        }
    }

    public void connectAll(String from) {
        ArrayList<DeviceManager> devices = getAll();
        String s;
        try {
            s = _currentDevice.getDeviceRowEntry().getUUID();
        } catch (NullPointerException e) {
            s = "n/a";
        }
        Log.i(TAG, "connectAll: " + devices.size() + ", current=" + s);

        // connect to the current device first, to recover as fast as possible
//        if(_currentDevice != null) {
//            _currentDevice.connectAsync();
//            devices.remove(_currentDevice);
//        }

        for (DeviceManager dm : devices) {
            dm.connectAsync(from + "/" + TAG);
        }
    }

    public void disconnectAll() {
        Log.i(TAG, "disconnectAll");
        for (DeviceManager dm : getAll()) {
            dm.disconnectAsync(false);
        }
    }

    public void dumpAll() {
        for (DeviceManager dm : getAll()) {
            Log.d(TAG, "dumpAll()  name: " + dm.getDeviceRowEntry().getName() + " " +
                "ip: " + dm.getDeviceRowEntry().getIpAddress() + " " +
                "uuid: " + dm.getDeviceRowEntry().getUUID() + " " +
                "suuid: " + dm.getDeviceRowEntry().getSourceUUID() + " " +
                "hash: " + dm.getDeviceRowEntry().hashCode());
        }
    }

    public DeviceManager get(String uuid) {
        Log.d(TAG, "DeviceManager get(" + uuid + ")");

        DeviceManager dm;
        dm = _deviceMap.get(uuid);

        return dm;
    }

    public DeviceManager get(int index) {
        return (DeviceManager) _deviceMap.values().toArray()[index];
    }

    public ArrayList<DeviceManager> getAll() {
        return new ArrayList<>(_deviceMap.values());
    }

    List<DeviceManager> getByParent(String uuid) {
        List<DeviceManager> list = new ArrayList<DeviceManager>();

        for (DeviceManager dm : getAll()) {
            if (dm.hasMaster()) {
                if (dm.getMasterUUID().equals(uuid)) {
                    list.add(dm);
                }
            }
        }

        return list;
    }

    public int size() {
        return _deviceMap.size();
    }

    /** Returns a sorted and grouped list of the container elements. */
    public List<DeviceManager> toSortedGroupedList() {
        List<DeviceManager> list = new ArrayList<>();
        List<DeviceManager> topLevelList = new ArrayList<>();

        // first collect the parent objects
        Iterator<DeviceManager> it0 = _deviceMap.values().iterator();
        while (it0.hasNext()) {
            DeviceManager dm = it0.next();
            if (dm.isAlive()) {
                if (!dm.hasMaster()) {
                    topLevelList.add(dm);
                } else if (dm.getMaster() == null || dm.getMaster().isDead()) {
                    topLevelList.add(dm);
                }
            }
        }

        // iterate over parent objects
        Iterator<DeviceManager> it1 = topLevelList.iterator();
        while (it1.hasNext()) {
            DeviceManager dm = it1.next();
            if (!dm.isDead()) {
                list.add(dm); // add parent objects to the return-list

                if (dm.hasFollowers()) {
                    List<DeviceManager> childList = dm.getFollowers(); // get child-list
                    for (DeviceManager dmChild : childList) {
                        if (!dmChild.isDead()) {
                            list.add(dmChild); // add child elements to the bottom of their parent
                        }
                    }
                }
            }
        }

        Log.d(TAG, "toSortedGroupedList() size: " + list.size());

        return list;
    }

    /** <code>toSortedGroupedList</code> with the <code>curr</code> item removed. */
    public List<DeviceManager> filter(DeviceManager curr) {
        List<DeviceManager> out = Devices.instance(ctx).toSortedGroupedList();
        for (int i = out.size() - 1; i >= 0; i--) {
            if (out.get(i) == curr) {
                out.remove(i);
            }
        }
        return out;
    }

    private DeviceManager _currentDevice;
    public DeviceManager getCurrentDevice() {
        return _currentDevice;
    }
    public void setCurrentDevice(DeviceManager value) {
        _currentDevice = value;
    }

    /**
     * Ping all devices in the container.
     * The isAlive() function will set the availability of the device via the ping result.
     * This function is Asynchronous
     */
    public void pingAll() {
        for (final DeviceManager dm : getAll()) {
            pingThreadPool.execute(() -> {
                if (dm != null) {
                    dm.isAlive();
                }
            });
        }
    }

}
