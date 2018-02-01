package com.streamunlimited.streamsdkdemo.discovery;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.util.Log;

import com.streamunlimited.streamsdkdemo.StreamControlApp;
import com.streamunlimited.streamsdkdemo.data.DeviceRowEntry;
import com.streamunlimited.streamsdkdemo.discovery.Discovery.DeviceAddedCallbacks;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

public class JmdnsDiscoveryHelper extends AbstractDiscoveryHelper implements ServiceListener {

    private static final String TAG = JmdnsDiscoveryHelper.class.getSimpleName();
    private static final String NAME = "tio";

    private static MulticastLock _lock;
    private static JmDNS _jmdns;

    private final Executor _singleThreadEx = Executors.newSingleThreadExecutor();

    JmdnsDiscoveryHelper(Context ctx, String serviceType, DeviceAddedCallbacks callbacks) {
        super(ctx, serviceType, callbacks);
        createJmDNS();
    }

    //----------------------------------------
    // ServiceListener
    //----------------------------------------

    @Override
    public void serviceAdded(ServiceEvent serviceInfo) {
    }

    @Override
    public void serviceRemoved(ServiceEvent serviceInfo) {
        Log.i(TAG, "serviceRemoved: " + serviceInfo.getName());
        final ServiceInfo info = serviceInfo.getInfo();
        _singleThreadEx.execute(() -> callbacks.onDeviceRemoved(info.getName()));
    }

    @Override
    public void serviceResolved(ServiceEvent serviceInfo) {
        // TODO Resolve entry first before calling callback
        Log.i(TAG, "service resolved: " + serviceInfo.getName());
        final ServiceInfo info = serviceInfo.getInfo();
        resolveService(info);
    }

    //----------------------------------------
    // AbstractDiscoveryHelper
    //----------------------------------------

    @Override
    public void startDiscovery() {
        registerServiceListeners(serviceType);
        for (final ServiceInfo info : _jmdns.list(serviceType, 2000)) {
            resolveService(info);
        }
    }

    @Override
    public void stopDiscovery() {
        if (_jmdns != null) _jmdns.removeServiceListener(serviceType, this);
        _jmdns = null;
        if (_lock != null) {
            _lock.release();
            _lock = null;
        }
    }

    @Override
    public void restartDiscovery() {
        stopDiscovery();
        createJmDNS();
        startDiscovery();
    }



    private void createJmDNS() {
        WifiManager mgr = getWifiManager();
        _lock = mgr.createMulticastLock("mdns_discovery");
        _lock.setReferenceCounted(true);
        _lock.acquire();
        try {
            int intIp = mgr.getDhcpInfo().ipAddress;
            byte[] ipAddress = BigInteger.valueOf(intIp).toByteArray();
            byte[] ipAddressInv = new byte[ipAddress.length];
            for (int i = 0; i < ipAddress.length; ++i) {
                ipAddressInv[i] = ipAddress[ipAddress.length - i - 1];
            }
            if (ipAddressInv.length < 4) {
                Log.e(TAG, "createJmDNS: IP Address length should be minimum 4, current: " + ipAddressInv.length);
                return;
            }
            InetAddress wifiAddress = InetAddress.getByAddress(ipAddressInv);
            _jmdns = JmDNS.create(wifiAddress, NAME);
            Log.i(TAG, "createJmDNS: wifiAddress=" + wifiAddress + ", name=" + _jmdns.getName() + ", hostName=" + _jmdns.getHostName());
        } catch (IOException e) {
            Log.e(TAG, "createJmDNS: " + e.getMessage());
        }
    }

    private void registerServiceListeners(String serviceType) {
        if (_jmdns == null || _lock == null) createJmDNS();
        if (_jmdns != null) {
            _jmdns.addServiceListener(serviceType, this);
            Log.i(TAG, "registerServiceListeners");
        }
    }
    private void resolveService(final ServiceInfo info) {
        try {
            boolean tmp = info.getPropertyString("ip").isEmpty() &&
                          getSsid().toLowerCase().contains("s800-softap");
            final String ip = tmp ? "192.168.200.1" : info.getPropertyString("ip");
            final int port = info.getPort();

            String manufacturer = info.getPropertyString("manufacturer").toLowerCase();
            if (!StreamControlApp.getAcceptedClientManufacturers(ctx).contains(manufacturer)) {
                Log.w(TAG, "resolveService: unrecognized manufacturer, aborting! " + manufacturer);
                return;
            }
            final String name = info.getPropertyString("name");
            final String uuid = info.getPropertyString("uuid");
            boolean foo = name != null &&
                          uuid != null &&
                          !ip.isEmpty();
            if (!foo) {
                Log.e(TAG, "resolveService: expected property string missing; name=" + name + ", uuid=" + uuid + ", ip=" + ip);
                return;
            }

            DeviceRowEntry e = new DeviceRowEntry(ip, port, name, uuid);
//            dumpProperties(info, ip, port);

            if (serviceType.startsWith("_sueGrouping")) {
                e.setMultiroomSupported(true);
            } else if (serviceType.startsWith("_sueS800Device")) {
                e.setMultiroomSupported(false);
            }

            callbacks.onDeviceAdded(e);
        } catch (NullPointerException npe) {
            Log.e(TAG, "service resolved did not match our expected property strings");
        }
    }

    private void dumpProperties(ServiceInfo info, String ip, int port) {
        Log.i(TAG, "JMDNS --------------------------------------------------");
        Log.i(TAG, "JMDNS new device by service: " + serviceType + ", ip: " + ip + ", port: " + port);

        // get properties
        List<String> propertyList = Collections.list(info.getPropertyNames());
        String s = "";
        for (String name : propertyList) {
            s += name + ", ";
        }
        Log.i(TAG, "JMDNS propertyNames: " + s);

        for (String name : propertyList) {
            Log.i(TAG, "JMDNS " + name + ": " + info.getPropertyString(name));
        }

        // transcoder
        if (!propertyList.contains("transcoder")) {
            Log.i(TAG, "JMDNS transcoder: no transcoder property found");
        }
    }

    private WifiManager getWifiManager() {
        return (WifiManager) ctx.getApplicationContext().getSystemService(android.content.Context.WIFI_SERVICE);
    }

    private String getSsid() {
        return getWifiManager().getConnectionInfo().getSSID();
    }
}
