package com.streamunlimited.streamsdkdemo.ui.devicebrowsing;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListFragment;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.support.v4.content.LocalBroadcastManager;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;
import android.widget.Toast;

import com.streamunlimited.remotebrowser.Alert;
import com.streamunlimited.remotebrowser.ContextMenu;
import com.streamunlimited.remotebrowser.Message;
import com.streamunlimited.remotebrowser.StandbyState;
import com.streamunlimited.streamsdkdemo.R;
import com.streamunlimited.streamsdkdemo.callbacks.IConnection;
import com.streamunlimited.streamsdkdemo.callbacks.IDeviceList;
import com.streamunlimited.streamsdkdemo.callbacks.IUtils;
import com.streamunlimited.streamsdkdemo.callbacks.IWifiStateChanged;
import com.streamunlimited.streamsdkdemo.data.Action;
import com.streamunlimited.streamsdkdemo.data.DeviceRowEntry;
import com.streamunlimited.streamsdkdemo.discovery.Discovery;
import com.streamunlimited.streamsdkdemo.helper.DeviceManager;
import com.streamunlimited.streamsdkdemo.helper.Devices;
import com.streamunlimited.streamsdkdemo.helper.WifiBroadcastReceiver;
import com.streamunlimited.streamsdkdemo.ui.StreamAlertDialogBuilder;
import com.streamunlimited.streamsdkdemo.ui.contentbrowsing.ContentBrowseActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DeviceListFragment
        extends
            ListFragment
        implements
        IDeviceList,
        IConnection.IAvailable,
        IUtils,
        IWifiStateChanged,
        IRegisterable,
            DialogInterface.OnDismissListener {

    private static final String TAG = DeviceListFragment.class.getSimpleName();
    private static final int refreshInterval = 30; // seconds

    private final List<DeviceManager> items = new ArrayList<>();

    private DeviceManager _deviceSelected;
    private DevicesAdapter adapter;
    private TextView _emptyView;

    private ScheduledExecutorService _scheduledRefresh;

    private ProgressDialog progressDialog;

    private boolean connecting = false;
    private boolean _contentBrowseStarted = false;
    private boolean _wifichanged = false;
    private boolean _progressDialogCanceled = false;

    public DeviceListFragment() {
        WifiBroadcastReceiver.instance().addCallback(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        adapter = new DevicesAdapter(getActivity(), R.id.deviceoverview_fragment, items, this);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        setListAdapter(adapter);

        Discovery.instance(getActivity()).start();

        setRetainInstance(true);
    }

    @Override
    public void onPause() {
        // If contentBrowseActivity wasn't started, the device goes to standby now
        // so disconnect from all devices to save battery life
        if (!_contentBrowseStarted) {
            Devices.instance(getActivity()).disconnectAll();
        }

        stopRefreshTask();
        unregisterReceivers();
        unregisterDeviceEvents();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

        if(!_contentBrowseStarted) {
            Devices.instance(getActivity()).connectAll(TAG);
        }

        // reset the contentBrowseStarted var
        _contentBrowseStarted = false;

        registerDeviceEvents();
        registerReceivers();
        refreshDeviceList(0);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        dismissProgressDialog();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        dismissProgressDialog();
        if (Devices.instance(getActivity()).getCurrentDevice() != null) {
            Devices.instance(getActivity()).getCurrentDevice().dumpCallbacks();
        }

        if (resultCode == Activity.RESULT_CANCELED) {
            return;
        }

        updateDeviceList();
    }

    //----------------------------------------
    // ListFragment
    //----------------------------------------

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.stream_list_view, container, false);
        _emptyView = view.findViewById(R.id.empty);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        getListView().setEmptyView(_emptyView);

        getListView().setEnabled(true);
        getListView().setHapticFeedbackEnabled(false);
        getListView().setFastScrollEnabled(false);

        getListView().setOnItemClickListener(list_onItemClick);

        super.onViewCreated(view, savedInstanceState);
    }

    //----------------------------------------
    // OnDismissListener
    //----------------------------------------

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (connecting && Devices.instance(getActivity()).getCurrentDevice() != null) {
            Devices.instance(getActivity()).getCurrentDevice().reset(true);
            connecting = false;

            _progressDialogCanceled = true;
            onConnectionNotAvailable();
        }
    }

    //----------------------------------------
    // IAvailable
    //----------------------------------------

    @Override
    public void onConnectionAvailable() {
        Log.i(TAG, "onConnectionAvailable");

        if (_deviceSelected.getChromecastTosAccepted()) {
            connectToDevice(_deviceSelected);
            return;
        }

        getActivity().runOnUiThread(this::showExplainerPopup);
    }

    @Override
    public void onConnectionNotAvailable() {
        dismissProgressDialog();
        Log.w(TAG, "onConnectionNotAvailable: device is not responding " + _deviceSelected.getDeviceRowEntry().getName());

        if (_deviceSelected.isDead()) {
            Log.w(TAG, "onConnectionNotAvailable: device is dead");
            Devices.instance(getActivity()).remove(_deviceSelected);
            new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(getActivity().getApplicationContext(), getResources().getString(R.string.info_device_dead), Toast.LENGTH_SHORT).show());
        } else {

            if (!_progressDialogCanceled) {
                new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(getActivity().getApplicationContext(), getResources().getString(R.string.info_device_noresponse), Toast.LENGTH_SHORT).show());
            } else {
                _progressDialogCanceled = false;
            }

            refreshDeviceList(0);
        }

        connecting = false;
    }

    @Override
    public void onConnectSuccess() {
        Log.i(TAG, "onConnectSuccess");

        if (!connecting) return;

        dismissProgressDialog();

        connecting = false;

        proceed(getActivity(), _deviceSelected);
    }

    private void proceed(Context ctx, DeviceManager deviceSelected) {
        Devices.instance(ctx).setCurrentDevice(deviceSelected);
        startContentBrowseActivity(deviceSelected.getDeviceRowEntry());
    }

    private void acceptTos(DeviceManager dm) {
        dm.acceptChromecastTos();
        proceed(getActivity(), dm);
    }

    private void showExplainerPopup() {
        dismissProgressDialog();
        connecting = false;
        new AlertDialog.Builder(getActivity(), R.style.Tio_Material_Alert)
                .setTitle(R.string.chromecast_tos_title)
                .setMessage(R.string.chromecast_tos_explainer_body)
                .setPositiveButton(R.string.show_me, (v, i) -> showChromecastTosPopup())
                .setNegativeButton(R.string.ignore, null)
                .show();
    }

    private void showChromecastTosPopup() {
        TextView t = new AlertDialog.Builder(getActivity(), R.style.Tio_Material_Alert)
                .setMessage(R.string.chromecast_tos)
                .setPositiveButton(R.string.accept, (v, i) -> acceptTos(_deviceSelected))
                .setNegativeButton(R.string.decline, null)
                .show()
                .findViewById(android.R.id.message);
        t.setMovementMethod(LinkMovementMethod.getInstance()); // Make the links clickable. Must be called after show
    }

    @Override
    public void onConnectFailed() {
        Log.i(TAG, "onConnectFailed");

        dismissProgressDialog();

        connecting = false;

        showConnectionFailedPopup(_deviceSelected);
        Discovery.instance(getActivity()).start();
    }

    @Override
    public void onDisconnectSuccess() {
        Log.i(TAG, "onDisconnectSuccess");
    }

    @Override
    public void onDisconnectFailed() {
        Log.i(TAG, "onDisconnectFailed");
    }

    public void refreshDeviceList(int initialDelay) {
        Devices.instance(getActivity()).setObserver(this);
        stopRefreshTask();
        if (WifiBroadcastReceiver.instance().isConnected()) {
            _scheduledRefresh = Executors.newSingleThreadScheduledExecutor();
            _scheduledRefresh.scheduleWithFixedDelay(onScheduledRefresh, initialDelay, refreshInterval, TimeUnit.SECONDS);
        }
    }

    private void tryToConnect(DeviceManager dm) {
        dm.setConnectionCallback(this);
        dm.isAvailableForConnectAsync();
    }

    private void clearDeviceList() {
        unregisterReceivers();
        unregisterDeviceEvents();

        // Prevent crash in certain cases, were getActivity is null on WiFi change
        if(getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                Devices.instance(getActivity()).clearAll();
                items.clear();
                adapter.notifyDataSetChanged();
            });
        }
    }

    private void stopRefreshTask() {
        if (_scheduledRefresh != null) {
            _scheduledRefresh.shutdownNow();
        }
        if (Discovery.instance(getActivity()).isRunning()) {
            Discovery.instance(getActivity()).stop();
        }
    }

    private void startContentBrowseActivity(DeviceRowEntry device) {
        if (!_contentBrowseStarted) {
            _contentBrowseStarted = true;

            Intent i = new Intent(getActivity().getApplicationContext(), ContentBrowseActivity.class);
            i.putExtra("title", device.getName());
            startActivityForResult(i, 1);
        }
    }

    private void connectToDevice(DeviceManager dm) {
        Log.d(TAG, "connectToDevice, connected: " + dm.isConnected());

        if (dm.isConnected()) {
            connecting = false;
            proceed(getActivity(), dm);
        } else {
            dm.connectAsync(TAG);
        }
    }

    private void showProgressDialog(final String text) {
        dismissProgressDialog();

        getActivity().runOnUiThread(() -> {
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage(text);
            progressDialog.setCancelable(false);
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.setOnDismissListener(DeviceListFragment.this);
            progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getResources().getString(R.string.action_cancel), (dialog, which) -> dialog.dismiss());
            progressDialog.show();
        });
    }

    private void dismissProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            getActivity().runOnUiThread(() -> progressDialog.dismiss());
        }
    }

    private void showConnectionFailedPopup(DeviceManager dm) {
        getActivity().runOnUiThread(() -> {
            String name = "";
            if (_deviceSelected != null) name = dm.getDeviceRowEntry().getName();

            new StreamAlertDialogBuilder(getActivity())
                    .setTitle(R.string.title_connection_failed)
                    .setMessage(getResources().getString(R.string.info_connection_failed) + " " + name)
                    .setPositiveButton(getResources().getString(R.string.action_retry), (dialog, which) -> tryToConnect(_deviceSelected))
                    .setNegativeButton(getResources().getString(android.R.string.cancel), null)
                    .show();
        });
    }

    private void updateDeviceList() {
        adapter.refresh(getActivity(), Devices.instance(getActivity()).toSortedGroupedList());
    }

    private void registerDeviceEvents() {
        for (final DeviceManager dm : Devices.instance(getActivity()).getAll()) {
            dm.addManagerUtilsCallback(this);
        }
    }

    private void unregisterDeviceEvents() {
        for (final DeviceManager dm : Devices.instance(getActivity()).getAll()) {
            dm.removeManagerUtilsCallback(this);
        }
    }

    //----------------------------------------
    // IRegisterable
    //----------------------------------------

    @Override
    public void registerReceivers() {
        unregisterReceivers();
        LocalBroadcastManager mgr = LocalBroadcastManager.getInstance(getActivity());
        mgr.registerReceiver(onBroadcast, new IntentFilter(Action.VOLUME_STATUS_CHANGED));
        mgr.registerReceiver(onBroadcast, new IntentFilter(Action.STANDBY_STATE_CHANGED));
        mgr.registerReceiver(onBroadcast, new IntentFilter(Action.PLAY_STATUS_CHANGED));
    }

    @Override
    public void unregisterReceivers() {
        LocalBroadcastManager mgr = LocalBroadcastManager.getInstance(getActivity());
        mgr.unregisterReceiver(onBroadcast);
    }

    //----------------------------------------
    // IDeviceList
    //----------------------------------------

    @Override
    public void onDeviceListChanged() {
        updateDeviceList();
    }

    //----------------------------------------
    // IUtils
    //----------------------------------------

    @Override
    public void onMessage(DeviceManager senderDevice, Message message) {
        Log.d(TAG, "onMessage() Sender: " + senderDevice.getDeviceRowEntry().getName() +
                "Id: " + message.get_messageId() +
                "Caption: " + message.get_captionText() +
                "Message: " + message.get_messageText());

        if (message.get_messageId().equals("Network Error")) {
            // do a hard reset in the DeviceManager
            senderDevice.reset(true);
        }
    }

    @Override
    public void onAlert(DeviceManager senderDevice, Alert alert) {
        Log.d(TAG, "onAlert() Sender: " + senderDevice.getDeviceRowEntry().getName() +
                   "Id: " + alert.get_alertId() +
                   "Caption: " + alert.get_captionText());
    }

    @Override
    public void onContextMenu(DeviceManager senderDevice, ContextMenu contextMenu) {
        Log.d(TAG, "onContextMenu() Sender: " + senderDevice.getDeviceRowEntry().getName() +
                    "Title: " + contextMenu.get_title() +
                    "Depth: " + contextMenu.get_depth() +
                    "numItems: " + contextMenu.get_numItems());
    }

    //----------------------------------------
    // IWifiStateChanged
    //----------------------------------------

    @Override
    public void onWifiStateChanged(boolean connected) {
        if (connected) {
            _wifichanged = true;
            refreshDeviceList(1);
        } else {
            stopRefreshTask();
            clearDeviceList();
        }
    }

    private final Runnable onScheduledRefresh = () -> {
        // if there is no userAction (for example grouping or clicking on an item)
        if (!connecting) {
            if (_wifichanged) {
                Log.d(TAG, "wifi state changed, restarting discovery...");
                Discovery.instance(getActivity()).restart();
                _wifichanged = false;
            } else {
                Discovery.instance(getActivity()).refresh();
            }
            Devices.instance(getActivity()).pingAll();
        }
    };

    private final AdapterView.OnItemClickListener list_onItemClick = new OnItemClickListener() {
        @Override
        public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
            if (connecting) {
                Log.w(TAG, "list_onItemClick: connecting, aborting");
                return;
            }

            if (position >= items.size()) {
                Log.w(TAG, "list_onItemClick: invalid position, aborting");
                return;
            }

            showProgressDialog(getResources().getString(R.string.info_loading));

            _deviceSelected = items.get(position);

            Log.i(TAG, "list_onItemClick: isConnected=" + _deviceSelected.isConnected());
            connecting = true;

            if (_deviceSelected.getStandbyState() == StandbyState.eStandbyStateStandby)
                _deviceSelected.getBrowserManager().setStandbyChanged(StandbyState.eStandbyStateOnline);

            // Ping device and wait for callback (async)
            tryToConnect(_deviceSelected);
        }
    };

    private final BroadcastReceiver onBroadcast = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            adapter.notifyDataSetChanged();
        }
    };

}
