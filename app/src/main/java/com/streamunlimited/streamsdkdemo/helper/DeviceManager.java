package com.streamunlimited.streamsdkdemo.helper;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.streamunlimited.remotebrowser.Alert;
import com.streamunlimited.remotebrowser.ConnectionState;
import com.streamunlimited.remotebrowser.ContextMenu;
import com.streamunlimited.remotebrowser.Message;
import com.streamunlimited.remotebrowser.PlayState;
import com.streamunlimited.remotebrowser.PlayStatus;
import com.streamunlimited.remotebrowser.RandomMode;
import com.streamunlimited.remotebrowser.RemoteBrowser;
import com.streamunlimited.remotebrowser.RepeatMode;
import com.streamunlimited.remotebrowser.StandbyState;
import com.streamunlimited.remotebrowser.ViewType;
import com.streamunlimited.remotebrowser.VolumeStatus;
import com.streamunlimited.streamsdkdemo.callbacks.IBrowser;
import com.streamunlimited.streamsdkdemo.callbacks.IConnection;
import com.streamunlimited.streamsdkdemo.callbacks.IContextMenu;
import com.streamunlimited.streamsdkdemo.callbacks.IPlayer;
import com.streamunlimited.streamsdkdemo.callbacks.ISueObserver;
import com.streamunlimited.streamsdkdemo.callbacks.IUtils;
import com.streamunlimited.streamsdkdemo.data.Action;
import com.streamunlimited.streamsdkdemo.data.DeviceRowEntry;
import com.streamunlimited.streamsdkdemo.data.TrackControls;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.streamunlimited.streamsdkdemo.data.Action.ALERT;
import static com.streamunlimited.streamsdkdemo.data.Action.CLIENT_DISCONNECTED;
import static com.streamunlimited.streamsdkdemo.data.Action.CLIENT_SHUTDOWN;
import static com.streamunlimited.streamsdkdemo.data.Action.MESSAGE;
import static com.streamunlimited.streamsdkdemo.data.Action.MUTE_CHANGED;
import static com.streamunlimited.streamsdkdemo.data.Action.OPEN_APP_FILTER;
import static com.streamunlimited.streamsdkdemo.data.Action.OPEN_LINK_FILTER;
import static com.streamunlimited.streamsdkdemo.data.Action.PLAY_PROGRESS_TOGGLE;
import static com.streamunlimited.streamsdkdemo.data.Action.PLAY_TIME_CHANGED;
import static com.streamunlimited.streamsdkdemo.data.Action.REPEAT_CHANGED;
import static com.streamunlimited.streamsdkdemo.data.Action.SHUFFLE_CHANGED;
import static com.streamunlimited.streamsdkdemo.data.Action.STANDBY_STATE_CHANGED;
import static com.streamunlimited.streamsdkdemo.data.Action.VOLUME_STATUS_CHANGED;
import static com.streamunlimited.streamsdkdemo.helper.DeviceManager.ShareState.follow;
import static com.streamunlimited.streamsdkdemo.helper.DeviceManager.ShareState.solo;
import static com.streamunlimited.streamsdkdemo.helper.OptionalBoolean.False;
import static com.streamunlimited.streamsdkdemo.helper.OptionalBoolean.Null;
import static com.streamunlimited.streamsdkdemo.helper.OptionalBoolean.True;

public class DeviceManager implements ISueObserver, IConnection {

    public enum ShareState {
        solo,
        master,
        follow
    }

    private static final String TAG = DeviceManager.class.getSimpleName();

    private final Message _message = new Message();
    private final Alert _alert = new Alert();
    private final ContextMenu _contextMenu = new ContextMenu();
    private final Context ctx;

    private int _pingTries = 0;

    public DeviceManager(Context ctx, DeviceRowEntry device) {
        this.ctx = ctx;
        Log.i(TAG, "ctor: " + device.getName());
        if (!browserManager.isBrowserSet()) {
            Log.e(TAG, "ctor: device assignment failed, browser is null");
            return;
        }
        _device = device;
        isAvailableForConnectAsync(); // connect automatically on initialization
    }

    public void onForwardRewindActivated() {
        Intent i = buildDeviceEvent(PLAY_PROGRESS_TOGGLE);
        LocalBroadcastManager.getInstance(ctx).sendBroadcast(i);
    }

    public void connectAsync(String from) {
        String name = getDeviceRowEntry().getName();
        String uuid = getDeviceRowEntry().getUUID();
        String ip = getDeviceRowEntry().getIpAddress();
        int port = getDeviceRowEntry().getPort();

        Log.i(TAG, "connectAsync: officially marking request in-flight: " + name + " " + uuid);
        isInFlight = true;

        if (isConnected()) {
            Log.i(TAG, "connectAsync: already connected! " + name + " " + uuid + " " + ip);
            if (_managerConnectionCallback != null) _managerConnectionCallback.onConnectSuccess();
            isInFlight = false;
            return;
        }

        Log.i(TAG, "connectAsync: officially connecting! " + name
                + " " + uuid
                + ", ip: " + ip
                + ", port: " + port);
        try {
            final IConnection callback = this;

            DeviceConnectionAsyncTask t = new DeviceConnectionAsyncTask(callback, browserManager.browser());
            // Get a handler that can be used to post to the main thread, because AsyncTasks need to be run from there
            new Handler(ctx.getMainLooper()).post(() ->
                t.execute(_device.getIpAddress(), Integer.toString(_device.getPort()), "connect"));
        } catch (Exception ex) {
            Log.e(TAG, "connectAsync: " + ex.getMessage());
        }
    }

    void disconnectAsync(boolean reset) {
        if (isInFlight) {
            Log.i(TAG, "disconnectAsync: " + _device.getName() + "(" + _device.getIpAddress() + ")");
            isInFlight = false;
            return;
        }


        String name = getDeviceRowEntry().getName();
        String uuid = getDeviceRowEntry().getUUID();

        Log.i(TAG, "disconnectAsync: officially marking request in-flight: " + name + " " + uuid);
        isInFlight = true;

        if (!isConnected()) {
            Log.i(TAG, "disconnectAsync: already disconnected " + _device.getName() + "(" + _device.getIpAddress() + ")");
            browserManager.setObserver(null);
            clearAllCallbacks();
            isInFlight = false;
            return;
        }

        Log.i(TAG, "disconnectAsync: from device " + name + " " + uuid + ", reset? " + reset);
        try {
            new DeviceConnectionAsyncTask(this, browserManager.browser())
                .execute(_device.getIpAddress(),
                        Integer.toString(_device.getPort()),
                        reset ? "disconnect" : "disconnectNoReset");
        } catch (Exception ex) {
            Log.e(TAG, "disconnectAsync: " + ex.getMessage());
        }
    }

    public void browsePlayqueue(){
        getBrowser().browsePlayqueue();
    }

    public void dumpCallbacks() {
        Iterator<IBrowser> it0 = _mapBrowserCallbacks.values().iterator();
        while (it0.hasNext()) {
            IBrowser dmbc = it0.next();
            Log.i(TAG, "Browser subscriber: " + dmbc.getClass().getName());
        }

        Iterator<IPlayer> it1 = _mapPlayerCallbacks.values().iterator();
        while (it1.hasNext()) {
            IPlayer dmbc = it1.next();
            Log.d(TAG, "Player subscriber: " + dmbc.getClass().getName());
        }

        Iterator<IUtils> it2 = _mapUtilsCallbacks.values().iterator();
        while (it2.hasNext()) {
            IUtils dmbc = it2.next();
            Log.d(TAG, "Utils subscriber: " + dmbc.getClass().getName());
        }
    }

    private void clearAllCallbacks() {
        _mapBrowserCallbacks.clear();
        _mapPlayerCallbacks.clear();
        _mapUtilsCallbacks.clear();
    }

    private Intent buildDeviceEvent(String action) {
        Intent out = new Intent(action);
        out.putExtra("deviceUuid", _device.getUUID());
        return out;
    }

    private boolean isInFlight = false;
    public boolean isConnecting() {
        return isInFlight;
    }

    private DeviceRowEntry _device;
    public DeviceRowEntry getDeviceRowEntry() {
        return _device;
    }
    void setDeviceRowEntry(DeviceRowEntry value) {
        _device = value;
    }

    private int _currVolume = -1;
    public int getCurrentVolume() {
        return _currVolume;
    }

    private int _minVolume = 0;
    public int getMinVolume() {
        return _minVolume;
    }

    private int _maxVolume = 100;
    public int getMaxVolume() {
        return _maxVolume;
    }

    private int _volumeStep = 1;
    public int getVolumeStep() {
        return _volumeStep;
    }

    private boolean _mute = false;
    public boolean getMute() {
        return _mute;
    }

    private PlayState _playState;
    public PlayState getPlayState() {
        return _playState;
    }

    private String _metadata;
    public String getMetadataString() {
        return _metadata;
    }

    private StandbyState _standbyState;
    public StandbyState getStandbyState() {
        return _standbyState;
    }

    private boolean _newPlaybackSelected = false;
    public boolean isNewPlaybackSelected() {
        return _newPlaybackSelected;
    }
    public void setNewPlaybackSelected(boolean value) {
        _newPlaybackSelected = value;
    }

    private IConnection _managerConnectionCallback;
    public void setConnectionCallback(IConnection value) {
        _managerConnectionCallback = value;
    }

    private final Map<String, IBrowser> _mapBrowserCallbacks = new ConcurrentHashMap<>();
    public void addManagerBrowserCallback(IBrowser value) {
        String klass = value.getClass().getName();
        _mapBrowserCallbacks.put(klass, value);
        Log.i(TAG, "addManagerBrowserCallback: " + _device.getName() + " " + klass);
    }
    public void removeManagerBrowserCallback(IBrowser value) {
        String klass = value.getClass().getName();
        boolean ok = _mapBrowserCallbacks.remove(klass) != null;
        if (ok) Log.i(TAG, "removeManagerBrowserCallback: " + _device.getName() + " " + klass);
    }

    private final Map<String, IPlayer> _mapPlayerCallbacks = new ConcurrentHashMap<>();
    public void addManagerPlayerCallback(IPlayer value) {
        String klass = value.getClass().getName();
        _mapPlayerCallbacks.put(klass, value);
//        Log.d(TAG, "Player subscriber added: " + klass);
    }
    public void removeManagerPlayerCallback(IPlayer value) {
        String klass = value.getClass().getName();
        boolean ok = _mapPlayerCallbacks.remove(klass) != null;
//        if (ok) Log.d(TAG, "Player subscriber removed: " + klass);
    }

    private final Map<String, IUtils> _mapUtilsCallbacks = new ConcurrentHashMap<>();
    public void addManagerUtilsCallback(IUtils value) {
        String klass = value.getClass().getName();
        _mapUtilsCallbacks.put(klass, value);
//        Log.d(TAG, "Utils subscriber added: " + klass);
    }
    public void removeManagerUtilsCallback(IUtils value) {
        String klass = value.getClass().getName();
        boolean ok = _mapUtilsCallbacks.remove(klass) != null;
//        if (ok) Log.d(TAG, "Utils subscriber removed: " + klass);
    }

    private boolean _isConnected = false;
    public boolean isConnected() {
        _isConnected = getBrowser().getConnectionState().equals(ConnectionState.eStateConnected);
        return _isConnected;
    }

    private OptionalBoolean isChromecastTosAccepted = Null;
    /** Caches the browser lookup. */
    public boolean getChromecastTosAccepted() {
        if (isChromecastTosAccepted == True) return true;
        if (isChromecastTosAccepted == False) return false;

        boolean out = getBrowserManager().isChromecastTosAccepted();
        isChromecastTosAccepted = out ? True : False;
        return out;
    }

    public void acceptChromecastTos() {
        boolean ok = getBrowserManager().acceptChromecastTos();
        Log.i(TAG, "acceptChromecastTos: " + ok);
        isChromecastTosAccepted = ok ? True : False;
    }

    boolean isAlive() {
        boolean out = ping();

        if (out) {
            _pingTries = 0;
        } else {
            _pingTries++;
            _isConnected = false;
        }

        Log.d(TAG, "_pingTries in isAlive() = " + _pingTries);

        return out;
    }

    public boolean isDead() {
//        Log.d(TAG, "_pingTries in isDead() = " + _pingTries);
        return _pingTries > 3;
    }

    public void reset(boolean hard) {
        Log.i(TAG, "reset: " + hard);
        _pingTries = 0;
        _currVolume = -1;

        if (hard) disconnectAsync(true);
    }

    public void isAvailableForConnectAsync() {
        new Thread(() -> {
            if (_managerConnectionCallback == null) {
                if (isAlive() && !isConnected()) {
                    Log.i(TAG, "Thread#isAvailableForConnectAsync: " + getDeviceRowEntry().getName() + " ...");
                    connectAsync(TAG);
                }
                return;
            }

            if (!(_managerConnectionCallback instanceof IAvailable)) return;

            if (!isAlive()) {
                ((IAvailable)_managerConnectionCallback).onConnectionNotAvailable();
                return;
            }

            ((IAvailable)_managerConnectionCallback).onConnectionAvailable();
        }).start();
    }

    private boolean ping() {
        return getBrowser().ping(getDeviceRowEntry().getIpAddress(), getDeviceRowEntry().getPort());
    }

    private final StreamControlRemoteBrowserManager browserManager = new StreamControlRemoteBrowserManager();
    public StreamControlRemoteBrowserManager getBrowserManager() {
        return browserManager;
    }
    public RemoteBrowser getBrowser() {
        return browserManager.browser();
    }

    public ShareState getShareState() {
        if (hasMaster()) return follow;
        if (hasFollowers()) return ShareState.master;
        return solo;
    }

    public String getDisplayStatus() {
        ShareState s = getShareState();

        if (s == solo) return "";

        if (s == follow && getMaster() != null) return "Following " + getMaster().getDeviceRowEntry().getName();

        List<String> names = new ArrayList<>();
        for (DeviceManager d : getFollowers()) {
            names.add(d.getDeviceRowEntry().getName());
        }
        StringBuilder bld = new StringBuilder("Streaming to ");
        int len = names.size();
        for (int i = 0; i < len; i++) {
            String name = names.get(i);
            if (i < len - 1) name += ", ";
            bld.append(name);
        }
        return bld.toString();
    }

    boolean hasMaster() {
        if (getMasterUUID().equals("") || !getDeviceRowEntry().getMultiroomSupported()) {
            return false;
        }

        return !_device.getUUID().equals(getMasterUUID());
    }

    String getMasterUUID() {
        return _device.getSourceUUID();
    }

    DeviceManager getMaster() {
        return Devices.instance(ctx).get(_device.getSourceUUID());
    }

    boolean hasFollowers() {
        List<DeviceManager> list = getFollowers();
        return list != null && list.size() > 0;
    }

    List<DeviceManager> getFollowers() {
        return Devices.instance(ctx).getByParent(_device.getUUID());
    }

    //----------------------------------------
    // IConnection
    //----------------------------------------

    @Override
    public void onConnectSuccess() {
        Log.i(TAG, "onConnectSuccess: " + _device.getName() + " (" + _device.getIpAddress() + ")");
        _isConnected = true;
        _pingTries = 0;

        browserManager.setObserver(this);
        browserManager.refresh();

        if (_managerConnectionCallback != null) _managerConnectionCallback.onConnectSuccess();

        isInFlight = false;
    }

    @Override
    public void onConnectFailed() {
        Log.i(TAG, "onConnectFailed: " + _device.getName() + "(" + _device.getIpAddress() + ")");
        _isConnected = false;
        browserManager.setObserver(null);
        if (_managerConnectionCallback != null) _managerConnectionCallback.onConnectFailed();

        isInFlight = false;
    }

    //----------------------------------------
    // ISueObserver
    //----------------------------------------

    /** The activity updates the toolbar.  The task does nothing. The transition fragment calls the SDK to reload row data. */
    @Override
    public void onRemoteViewChanged() {
        Log.i(TAG, "onRemoteViewChanged: " + _device.getName() + " running my browser callbacks");
        dumpCallbacks();
        Iterator<IBrowser> it0 = _mapBrowserCallbacks.values().iterator();
        while (it0.hasNext()) {
            IBrowser dmc = it0.next();
            dmc.onViewChanged();
        }
    }

    @Override
    public void onTransition() {}

//    @Override
//    public void onTimeout() {
//        browserManager.getBrowser().cancel();
//    }
//
//    @Override
//    public void onError() {}

    /** The activity does nothing.  The task updates its cache count. The transition fragment adds "dirty" items. */
    @Override
    public void onNumItemsChanged(int numItems) {
        Log.i(TAG, "onNumItemsChanged: " + _device.getName() + " " + numItems);

        Iterator<IBrowser> it0 = _mapBrowserCallbacks.values().iterator();
        while (it0.hasNext()) {
            IBrowser dmc = it0.next();
            dmc.onNumItemsChanged(numItems);
        }
    }

    @Override
    public void onContextMenuViewChanged() {
        Log.i(TAG, "onContextMenuViewChanged: " + _device.getName());
        Iterator<IUtils> it0 = _mapUtilsCallbacks.values().iterator();
        while (it0.hasNext()) {
            IUtils dmc = it0.next();
            if (dmc instanceof IContextMenu)
                ((IContextMenu)dmc).onContextMenuViewChanged();
        }
    }

    @Override
    public void onPlayStatusChanged(PlayStatus status) {
        Log.i(TAG, "onPlayStatusChanged: " + _device.getName() + " " + status.get_source());

        _playState = status.get_playState();
        _metadata = status.get_metaArtist() + " - " + status.get_title() + " - " + status.get_metaAlbum();

        Intent i = buildDeviceEvent(Action.PLAY_STATUS_CHANGED);

        // FIXME: extract this crap
        // setup the intent to broadcast data
        i.putExtra("mimeType", status.get_mimeType().swigValue());
        i.putExtra("metaAlbum", status.get_metaAlbum());
        i.putExtra("metaAlbumartUri", status.get_metaAlbumartUrl());
        i.putExtra("metaArtist", status.get_metaArtist());
        i.putExtra("metaGenre", status.get_metaGenre());
        i.putExtra("contextPath", status.get_contextPath());
        i.putExtra("metaType", status.get_metaType());
        i.putExtra("availableControls", new TrackControls(status.get_availableControls()));
        i.putExtra("source", status.get_source());
        i.putExtra("serviceName", status.get_serviceName());
        i.putExtra("title", status.get_title());
        i.putExtra("codec", status.get_codec());
        i.putExtra("bps", status.get_bps());
        i.putExtra("bitrate", status.get_bitrate());
        i.putExtra("channels", status.get_channels());
        i.putExtra("instance", status.get_instance());
        i.putExtra("queueIndex", status.get_queueIndex());
        i.putExtra("queueLength", status.get_queueLength());
        i.putExtra("playTime", status.get_playTime());
        i.putExtra("samplerate", status.get_samplerate());
        i.putExtra("trackTime", status.get_trackTime()); // We have a livestream state if the tracktime is 0
        i.putExtra("liveStream", status.get_trackTime() == 0);
        i.putExtra("playState", status.get_playState().swigValue());
        i.putExtra("random", status.get_random().swigValue());
        i.putExtra("repeat", status.get_repeat().swigValue());

        LocalBroadcastManager.getInstance(ctx).sendBroadcast(i);

        // notify subscriber classes
        Iterator<IPlayer> it0 = _mapPlayerCallbacks.values().iterator();
        while (it0.hasNext()) {
            IPlayer dmc = it0.next();
            dmc.onPlayStatusChanged(status);
        }
    }

    @Override
    public void onPlayTimeChanged(int time) {
//        Log.d(TAG, "onPlayTimeChanged " + _device.getName() + " time: " + time);

        Intent i = buildDeviceEvent(PLAY_TIME_CHANGED);
        i.putExtra("playTime", time);
        LocalBroadcastManager.getInstance(ctx).sendBroadcast(i);

        //		Iterator<PlayerCallback> it0 = _mapPlayerCallbacks.values().iterator();
        //		while (it0.hasNext()) {
        //			PlayerCallback dmc = it0.next();
        //			dmc.onPlayTimeChanged(time);
        //		}
    }

    @Override
    public void onVolumeStatusChanged(VolumeStatus volumeStatus) {
        Log.d(TAG, "onVolumeStatusChanged " + _device.getName() + " volume: " + volumeStatus.get_currentVolume());

        _currVolume = volumeStatus.get_currentVolume();
        _maxVolume  = volumeStatus.get_maxVolume();
        _minVolume  = volumeStatus.get_minVolume();
        _volumeStep = volumeStatus.get_volumeStep();

        Intent i = buildDeviceEvent(VOLUME_STATUS_CHANGED);
        i.putExtra("currentVolume", _currVolume);
        i.putExtra("minVolume", _minVolume);
        i.putExtra("maxVolume", _maxVolume);
        i.putExtra("volumeStep", _volumeStep);
        LocalBroadcastManager.getInstance(ctx).sendBroadcast(i);
    }

    @Override
    public void onStandbyStateChanged(StandbyState state) {
        Log.d(TAG, "onStandbyStateChanged " + _device.getName() + " Standby State changed to: " + state);

        _standbyState = state;
        Intent i = buildDeviceEvent(STANDBY_STATE_CHANGED);
        i.putExtra("standbyState", state.swigValue());
        LocalBroadcastManager.getInstance(ctx).sendBroadcast(i);
    }

    @Override
    public void onMuteChanged(boolean mute) {
        Intent i = buildDeviceEvent(MUTE_CHANGED);
        i.putExtra("mute", mute);
        _mute = mute;
        LocalBroadcastManager.getInstance(ctx).sendBroadcast(i);
    }

    @Override
    public void onRepeatChanged(RepeatMode repeat) {
        Intent i = buildDeviceEvent(REPEAT_CHANGED);
        i.putExtra("repeat", repeat.swigValue());
        LocalBroadcastManager.getInstance(ctx).sendBroadcast(i);
    }

    @Override
    public void onShuffleChanged(RandomMode shuffle) {
        Intent i = buildDeviceEvent(SHUFFLE_CHANGED);
        i.putExtra("shuffle", shuffle.swigValue());
        LocalBroadcastManager.getInstance(ctx).sendBroadcast(i);
    }

    @Override
    public void onClientDisconnected() {
        Intent i = buildDeviceEvent(CLIENT_DISCONNECTED);
        LocalBroadcastManager.getInstance(ctx).sendBroadcast(i);
    }

    @Override
    public void onViewChanged() {} // onRemoteViewChanged already called instead, so do nothing

    @Override
    public void onViewTypeChanged(ViewType type) {
        Log.d(TAG, _device.getName() + " onViewTypeChanged(): " + type);

        Iterator<IBrowser> it0 = _mapBrowserCallbacks.values().iterator();
        while (it0.hasNext()) {
            IBrowser dmc = it0.next();
            dmc.onViewTypeChanged(type);
        }
    }

    @Override
    public void onMessage() {
        if (isInFlight || !isConnected()) {
            Log.w(TAG, "onMessage: disconnected (or in-flight), aborting!");
            return;
        }

        getBrowserManager().getMessage(_message);
        if (_message != null &&
            !_message.get_captionText().equals("") &&
            !_message.get_messageText().equals("")) {
            Iterator<IUtils> it0 = _mapUtilsCallbacks.values().iterator();
            while (it0.hasNext()) {
                IUtils dmc = it0.next();
                dmc.onMessage(this, _message);
            }

            Intent i = buildDeviceEvent(MESSAGE);
            LocalBroadcastManager.getInstance(ctx).sendBroadcast(i);
        }
    }

    @Override
    public void onAlert() {
        getBrowserManager().getAlert(_alert);
        if (_alert != null && !_alert.get_captionText().equals("") && _alert.get_alertItems().size() >= 0) {
            Iterator<IUtils> it0 = _mapUtilsCallbacks.values().iterator();
            while (it0.hasNext()) {
                IUtils dmc = it0.next();
                dmc.onAlert(this, _alert);
            }

            Intent i = buildDeviceEvent(ALERT);
            LocalBroadcastManager.getInstance(ctx).sendBroadcast(i);
        }
    }

    @Override
    public void onContextMenu() {
        getBrowserManager().getContextMenu(_contextMenu);
        if (_contextMenu != null) {
            Iterator<IUtils> it0 = _mapUtilsCallbacks.values().iterator();
            while (it0.hasNext()) {
                IUtils dmc = it0.next();
                dmc.onContextMenu(this, _contextMenu);
            }
        }
    }

    @Override
    public void onShutdown() {
        Intent i = buildDeviceEvent(CLIENT_SHUTDOWN);
        LocalBroadcastManager.getInstance(ctx).sendBroadcast(i);
    }

    @Override
    public void onOpenApp(String app) {
        String link;
        switch (app) {
            case "app://spotify":
                Log.d(TAG, "open Spotify App");
                link = "com.spotify.music";
                break;

            case "app://deezer":
                Log.d(TAG, "open Deezer App");
                link = "deezer.android.app";
                break;

            case "app://tunein":
                Log.d(TAG, "open TuneIn App");
                link = "tunein.player";
                break;

            case "app://googleplaymusic":
                Log.d(TAG, "open Google Play Music App");
                link = "com.google.android.music";
                break;

            case "app://pandora":
                Log.d(TAG, "open Pandora App");
                link = "com.pandora.android";
                break;

            default:
                Log.d(TAG, "open App Undefined: " + app);
                // do not call the broadcastIntent Code after the switch-case
                return;
        }

        Intent i = buildDeviceEvent(OPEN_APP_FILTER);
        i.putExtra("openApp", link);
        LocalBroadcastManager.getInstance(ctx).sendBroadcast(i);
    }

    @Override
    public void openLinkOnController(String link) {
        if (link.isEmpty()) {
            Log.d(TAG, "openLinkOnController: got empty argument, aborting");
            return;
        }

        Log.d(TAG, "openLinkOnController: " + link);

        Intent i = buildDeviceEvent(OPEN_LINK_FILTER);
        i.putExtra("openLink", link);
        LocalBroadcastManager.getInstance(ctx).sendBroadcast(i);
    }

    @Override
    public void onDisconnectSuccess() {
        Log.i(TAG, "onDisconnectSuccess: officially marking flight completed");
        isInFlight = false;
    }

    @Override
    public void onDisconnectFailed() {
        Log.i(TAG, "onDisconnectFailed: officially marking flight completed");
        isInFlight = false;
    }
}
