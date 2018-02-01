package com.streamunlimited.streamsdkdemo.helper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public abstract class StreamControlBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String deviceUuid = intent.getExtras().getString("deviceUuid");

        if (!Devices.isCurrentDevice(context, deviceUuid)) return;

        // TODO: run onReceiveFinished in Thread to reduce weight on UI thread
        onReceiveFinished(intent);
    }

    protected abstract void onReceiveFinished(Intent intent);
}
