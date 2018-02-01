package com.streamunlimited.streamsdkdemo.ui.devicebrowsing;

import android.app.ActionBar;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.AbsListView;
import android.widget.TextView;

import com.streamunlimited.streamsdkdemo.R;
import com.streamunlimited.streamsdkdemo.StreamControlApp;
import com.streamunlimited.streamsdkdemo.discovery.Discovery;

public class DeviceOverviewActivity extends Activity {

    private static final String TAG = DeviceOverviewActivity.class.getSimpleName();

    private ViewHolder holder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);

        setTheme(R.style.Theme_SUE);
        setContentView(R.layout.activity_device_overview);

        if (!StreamControlApp.isTabletMode(this))
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        holder = new ViewHolder(this);
        configureToolbar();
        setAppVersion();
    }

    @Override
    protected void onDestroy() {
        Discovery.instance(this).stop();
        super.onDestroy();
    }

    private void setAppVersion() {
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            holder.setVersion(pInfo.versionName);
        } catch(PackageManager.NameNotFoundException e){
            Log.e(TAG, "Error getting version", e);
        }
    }

    private void configureToolbar() {
        ActionBar ab = getActionBar();
        ab.setDisplayShowTitleEnabled(false);
        ab.setDisplayShowHomeEnabled(false);
        ab.setDisplayUseLogoEnabled(false);
        ab.setDisplayShowCustomEnabled(true);
        ab.setCustomView(LayoutInflater.from(this).inflate(R.layout.toolbar, null));

    }

    private class ViewHolder {

        SwipeRefreshLayout swipeRefreshGroup;
        DeviceListFragment fragment;
        TextView versionText;

        ViewHolder(Activity v) {
            swipeRefreshGroup = v.findViewById(R.id.swipeRefreshLayout);
            swipeRefreshGroup.setColorSchemeResources(R.color.accent);
            swipeRefreshGroup.setOnRefreshListener(onRefresh);

            fragment = (DeviceListFragment) getFragmentManager().findFragmentById(R.id.deviceoverview_fragment);
            fragment.getListView().setOnScrollListener(onScroll);

            versionText = findViewById(R.id.app_version);
        }

        void setVersion(String value) {
            versionText.setText(getString(R.string.version, value));
        }

        private final SwipeRefreshLayout.OnRefreshListener onRefresh = () -> {
            fragment.refreshDeviceList(0);
            swipeRefreshGroup.postDelayed(() -> swipeRefreshGroup.setRefreshing(false), 2000);
        };

        private final AbsListView.OnScrollListener onScroll = new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {}

            @Override
            public void onScroll(AbsListView listView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                int topRowVerticalPosition = (listView == null || listView.getChildCount() == 0) ? 0 : listView.getChildAt(0).getTop();
                swipeRefreshGroup.setEnabled((topRowVerticalPosition >= 0));
            }
        };
    }
}
