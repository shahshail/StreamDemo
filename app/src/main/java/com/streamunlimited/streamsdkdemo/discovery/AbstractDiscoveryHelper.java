package com.streamunlimited.streamsdkdemo.discovery;

import android.content.Context;

import com.streamunlimited.streamsdkdemo.discovery.Discovery.DeviceAddedCallbacks;

public abstract class AbstractDiscoveryHelper {

    final String serviceType;
    final DeviceAddedCallbacks callbacks;
    final Context ctx;

    AbstractDiscoveryHelper(Context ctx, String serviceType, DeviceAddedCallbacks callbacks) {
        this.serviceType = serviceType;
        this.callbacks = callbacks;
        this.ctx = ctx.getApplicationContext();
    }

    public abstract void startDiscovery();

    public abstract void stopDiscovery();

    public abstract void restartDiscovery();
}
