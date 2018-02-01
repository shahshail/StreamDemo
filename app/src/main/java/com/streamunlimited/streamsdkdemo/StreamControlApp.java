package com.streamunlimited.streamsdkdemo;

import android.app.Application;
import android.content.Context;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.net.wifi.WifiManager;
import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.core.CrashlyticsCore;
import com.streamunlimited.streamsdkdemo.helper.WifiBroadcastReceiver;
import io.fabric.sdk.android.Fabric;

import java.util.Arrays;
import java.util.List;

public class StreamControlApp extends Application {

    public static boolean isTabletMode(Context ctx) {
        return getScreenLayoutSize(ctx) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    public static List<String> getAcceptedClientManufacturers(Context ctx) {
        String[] manufacturers = ctx.getApplicationContext().getResources().getStringArray(R.array.manufacturers);
        return Arrays.asList(manufacturers);
    }

    private static int getScreenLayoutSize(Context ctx) {
        return ctx.getApplicationContext().getResources().getConfiguration().screenLayout &
                Configuration.SCREENLAYOUT_SIZE_MASK;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Crashlytics crashKit = new Crashlytics.Builder()
                .core(new CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build())
                .build();
        Fabric.with(this, crashKit, new Answers());

        IntentFilter f = new IntentFilter();
        f.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
        f.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        super.registerReceiver(WifiBroadcastReceiver.instance(), f);
    }

}
