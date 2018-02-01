package com.streamunlimited.streamsdkdemo.helper;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.streamunlimited.observer.RemoteBrowserManager;
import com.streamunlimited.remotebrowser.*;
import com.streamunlimited.streamsdkdemo.callbacks.ISueObserver;
import com.streamunlimited.streamsdkdemo.ui.login.LoginTarget;

import java.util.Arrays;
import java.util.List;

public class StreamControlRemoteBrowserManager extends RemoteBrowserManager {

    private static final String TAG = StreamControlRemoteBrowserManager.class.getSimpleName();
    private static final int LOGIN_DELAY_MS = 1000;
    private static final int LOGOUT_DELAY_MS = 200; // necessary for Stream's browser to realize we logged out
    private static final List<String> chromecastSettings = Arrays.asList("Settings", "Chromecast built-in settings");

    public StreamControlRemoteBrowserManager() {

        _browser = RemoteBrowser.createRemoteBrowser(this);

        if (isBrowserSet())
            Log.d(TAG, "browser initialization ok");
        else
            Log.e(TAG, "browser initialization failed");
    }

    @Override
    public void onClientDisconnected() {
        Log.d(TAG, "onClientDisconnected");

        if (deviceManager == null) {
            Log.e(TAG, "observer is NULL!");
            return;
        }

        deviceManager.onClientDisconnected();
    }

    @Override
    public void onViewTypeChanged(ViewType type) {
        if (deviceManager == null) {
            Log.e(TAG, "observer is NULL!");
            return;
        }

        switch (type) {
            case eTypeMessage:
                deviceManager.onMessage();
                break;
            case eTypeAlert:
                deviceManager.onAlert();
                break;
            case eTypeTransition:
                deviceManager.onTransition();
                break;
            case eTypeContextMenu:
                deviceManager.onContextMenu();
                break;
            default:
                // unused
                deviceManager.onViewTypeChanged(type);
                break;
        }
    }

    @Override
    public void onViewChanged() {
        if (deviceManager == null) {
            Log.e(TAG, "observer is NULL!");
            return;
        }

        deviceManager.onRemoteViewChanged();
    }

    @Override
    public void onContextMenuViewChanged() {
        Log.d(TAG, "onContextMenuViewChanged");

        if (deviceManager == null) {
            Log.e(TAG, "observer is NULL!");
            return;
        }

        deviceManager.onContextMenuViewChanged();
    }

    @Override
    public void onNumItemsChanged(int numItems) {
        Log.d(TAG, "onNumItemsChanged: " + numItems);

        if (deviceManager == null) {
            Log.e(TAG, "observer is NULL!");
            return;
        }

        deviceManager.onNumItemsChanged(numItems);
    }

    @Override
    public void onPlayStatusChanged(PlayStatus status) {
        Log.d(TAG, "onPlayStatusChanged: " + status);

        if (deviceManager == null) {
            Log.e(TAG, "observer is NULL!");
            return;
        }

        deviceManager.onPlayStatusChanged(status);
    }

    @Override
    public void onPlayTimeChanged(int time) {
//        Log.d(TAG, "onPlayTimeChanged: " + time);

        if (deviceManager == null) {
            Log.e(TAG, "observer is NULL!");
            return;
        }

        deviceManager.onPlayTimeChanged(time);
    }

    @Override
    public void onVolumeStatusChanged(VolumeStatus volumeStatus) {
        Log.d(TAG, "onVolumeStatusChanged: " + volumeStatus.get_currentVolume());

        if (deviceManager == null) {
            Log.e(TAG, "observer is NULL!");
            return;
        }

        deviceManager.onVolumeStatusChanged(volumeStatus);
    }

    @Override
    public void onStandbyChanged(StandbyState state) {
        switch (state) {
            case eStandbyStateUndefined:
                Log.d(TAG, "onStandbyChanged: eStandbyStateUndefined");
                break;

            case eStandbyStateOnline:
                Log.d(TAG, "onStandbyChanged: eStandbyStateOnline");
                break;

            case eStandbyStateNetworkStandby:
                Log.d(TAG, "onStandbyChanged: eStandbyStateNetworkStandby");
                break;

            case eStandbyStateStandby:
                Log.d(TAG, "onStandbyChanged: eStandbyStateStandby");
                break;

            case eStandbyStateOffline:
                Log.d(TAG, "onStandbyChanged: eStandbyStateOffline");
                break;
        }

        if (deviceManager == null) {
            Log.e(TAG, "observer is NULL!");
            return;
        }

        deviceManager.onStandbyStateChanged(state);
    }

    @Override
    public void onMuteChanged(boolean mute) {
        if (mute) {
            Log.d(TAG, "onMuteChanged: enabled");
        } else {
            Log.d(TAG, "onMuteChanged: disabled");
        }

        if (deviceManager == null) {
            Log.e(TAG, "observer is NULL!");
            return;
        }

        deviceManager.onMuteChanged(mute);
    }

    @Override
    public void onRepeatChanged(RepeatMode repeat) {
        switch (repeat) {
            case eRepeatNone:
                Log.d(TAG, "onRepeatChanged: eRepeatNone");
                break;

            case eRepeatOne:
                Log.d(TAG, "onRepeatChanged: eRepeatOne");
                break;

            case eRepeatAll:
                Log.d(TAG, "onRepeatChanged: eRepeatAll");
                break;

            case eRepeatUndefined:
                Log.d(TAG, "onRepeatChanged: eRepeatUndefined");
                break;
        }

        if (deviceManager == null) {
            Log.e(TAG, "observer is NULL!");
            return;
        }

        deviceManager.onRepeatChanged(repeat);
    }

    @Override
    public void onShuffleChanged(RandomMode shuffle) {
        switch (shuffle) {
            case eRandomDisabled:
                Log.d(TAG, "onShuffleChanged: eRandomDisabled");
                break;

            case eRandomEnabled:
                Log.d(TAG, "onShuffleChanged: eRandomEnabled");
                break;
        }

        if (deviceManager == null) {
            Log.e(TAG, "observer is NULL!");
            return;
        }

        deviceManager.onShuffleChanged(shuffle);
    }

    @Override
    public void onOpenApp(String app) {
        Log.d(TAG, "onOpenApp");

        if (deviceManager == null) {
            Log.e(TAG, "observer is NULL!");
            return;
        }

        deviceManager.onOpenApp(app);
    }

    @Override
    public void openLinkOnController(String link) {
        Log.d(TAG, "openLinkOnController");

        if (deviceManager == null) {
            Log.e(TAG, "observer is NULL!");
            return;
        }

        deviceManager.openLinkOnController(link);
    }

    public boolean isBrowserSet() {
        return super._browser != null;
    }

    private ISueObserver deviceManager;
    void setObserver(ISueObserver value) {
        deviceManager = value;
    }

    private void changeViewType(final ViewType viewType) {
        if (!isBrowserSet()) {
            Log.e(TAG, "browser is NULL!");
            return;
        }

        new Thread(() -> {
            if (browser().getViewType() != viewType) {
                Log.i(TAG, "Thread#changeViewType: " + viewType);
                browser().changeViewType(viewType);
            }
        }).start();
    }

    public void pause() {
        if (!isBrowserSet()) {
            Log.e(TAG, "browser is NULL!");
            return;
        }

        new Thread(() -> {
            Log.d(TAG, "pause");
            browser().pause();
        }).start();
    }

    public void setSeek2Time(final int position) {
        if (!isBrowserSet()) {
            Log.e(TAG, "browser is NULL!");
            return;
        }

        new Thread(() -> {
            Log.d(TAG, "seek2Time");
            browser().setSeek2Time(position);
        }).start();
    }

    public void previous() {
        if (!isBrowserSet()) {
            Log.e(TAG, "browser is NULL!");
            return;
        }

        changeViewType(ViewType.eTypePlay);

        new Thread(() -> {
            Log.d(TAG, "previous");
            browser().previous();
        }).start();
    }

    public void fastRewind() {
        if (!isBrowserSet()) {
            Log.e(TAG, "browser is NULL!");
            return;
        }

        changeViewType(ViewType.eTypePlay);

        new Thread(() -> {
            Log.d(TAG, "fastRewind");
            browser().fastRewind();
        }).start();
    }

    public void next() {
        if (!isBrowserSet()) {
            Log.e(TAG, "browser is NULL!");
            return;
        }

        changeViewType(ViewType.eTypePlay);

        new Thread(() -> {
            Log.d(TAG, "next");
            browser().next();
        }).start();
    }

    public void fastForward() {
        if (!isBrowserSet()) {
            Log.e(TAG, "browser is NULL!");
            return;
        }

        changeViewType(ViewType.eTypePlay);

        new Thread(() -> {
            Log.d(TAG, "fastForward");
            browser().fastForward();
        }).start();
    }

    public void stop() {
        if (!isBrowserSet()) {
            Log.e(TAG, "browser is NULL!");
            return;
        }

        new Thread(() -> {
            Log.d(TAG, "stop");
            browser().stop();
        }).start();

        changeViewType(ViewType.eTypeBrowse);
    }

    public void stopScan() {
        if (!isBrowserSet()) {
            Log.e(TAG, "browser is NULL!");
            return;
        }

        new Thread(() -> {
            Log.d(TAG, "stopScan");
            browser().stopScan();
        }).start();
    }

    public void refresh() {
        if (!isBrowserSet()) {
            Log.e(TAG, "browser is NULL!");
            return;
        }

        final ViewType current = browser().getViewType();

        new Thread(() -> {
            Log.d(TAG, "refresh");
            browser().refresh();
        }).start();

        changeViewType(current);
    }

    public void addToFavorites() {
        if (!isBrowserSet()) {
            Log.e(TAG, "browser is NULL!");
            return;
        }

        changeViewType(ViewType.eTypePlay);

        new Thread(() -> {
            Log.d(TAG, "addToFavorites");
            browser().addToFavorites();
        }).start();
    }

    public void setRepeat(final RepeatMode repeat) {
        if (!isBrowserSet()) {
            Log.e(TAG, "browser is NULL!");
            return;
        }

        new Thread(() -> {
            Log.d(TAG, "setRepeat");
            browser().setRepeat(repeat);
        }).start();
    }

    public void setShuffle(final RandomMode random) {
        if (!isBrowserSet()) {
            Log.e(TAG, "browser is NULL!");
            return;
        }

        new Thread(() -> {
            Log.d(TAG, "setShuffle");
            browser().setShuffle(random);
        }).start();
    }

    public void setMute(final boolean mute) {
        if (!isBrowserSet()) {
            Log.e(TAG, "browser is NULL!");
            return;
        }

        new Thread(() -> {
            Log.d(TAG, "setMute");
            browser().setMute(mute);
        }).start();
    }

    public void setStandbyChanged(final StandbyState standbyState) {
        if (!isBrowserSet()) {
            Log.e(TAG, "browser is NULL!");
            return;
        }

        new Thread(() -> {
            Log.d(TAG, "setStandbyState");
            browser().setStandbyState(standbyState);
        }).start();
    }

    public void playItem(final int position) {
        if (!isBrowserSet()) {
            Log.e(TAG, "browser is NULL!");
            return;
        }

        changeViewType(ViewType.eTypePlay);

        new Thread(() -> {
            Log.d(TAG, "play item: " + position);
            browser().playItem(position);
        }).start();
    }

    public void playContextMenu(int position) {
        if (!isBrowserSet()) {
            Log.e(TAG, "browser is NULL!");
            return;
        }

        changeViewType(ViewType.eTypeBrowse);

        new Thread(() -> {
            Log.d(TAG, "play contextmenu: " + position);
            browser().playContextMenuItem(position);
        }).start();
    }

    public void browseItem(final int position) {
        if (!isBrowserSet()) {
            Log.e(TAG, "browser is NULL!");
            return;
        }

        changeViewType(ViewType.eTypeBrowse);

        new Thread(() -> {
            Log.i(TAG, "browse item: " + position);
            browser().browseItem(position);
        }).start();
    }

    public void browseIntoContextMenu(final int position) {
        if (!isBrowserSet()) {
            Log.e(TAG, "browser is NULL!");
            return;
        }

        new Thread(() -> {
            Log.d(TAG, "browse item: " + position);
            browser().browseIntoContextMenu(position);
        }).start();
    }

    public void invokeQuery(final int position, final String queryStr) {
        if (!isBrowserSet()) {
            Log.e(TAG, "browser is NULL!");
            return;
        }

        changeViewType(ViewType.eTypeBrowse);

        new Thread(() -> {
            Log.d(TAG, "invoke query item: " + position + ", query: " + queryStr);
            browser().invokeQuery(position, queryStr);
        }).start();
    }

    public void browseContextMenuItem(final int position) {
        if (!isBrowserSet()) {
            Log.e(TAG, "browser is NULL!");
            return;
        }

        new Thread(() -> {
            Log.d(TAG, "browse context menu item: " + position);
            browser().browseContextMenuItem(position);
        }).start();
    }

    public void browseParent() {
        if (!isBrowserSet()) {
            Log.e(TAG, "browser is NULL!");
            return;
        }

        changeViewType(ViewType.eTypeBrowse);

        new Thread(() -> {
            Log.d(TAG, "browse Parent");
            browser().browseParent();
        }).start();
    }

    public void browseContextMenuParent() {
        if (!isBrowserSet()) {
            Log.e(TAG, "browser is NULL!");
            return;
        }

        new Thread(() -> {
            Log.d(TAG, "browse Context Menu Parent");
            browser().browseContextMenuParent();
        }).start();
    }

    public void goHome() {
        if (!isBrowserSet()) {
            Log.e(TAG, "browser is NULL!");
            return;
        }

        changeViewType(ViewType.eTypeBrowse);

        new Thread(() -> {
            Log.d(TAG, "goHome");
            browser().goHome();
        }).start();
    }

    public void getMessage(Message message) {
        if (!isBrowserSet()) {
            Log.e(TAG, "browser is NULL!");
            return;
        }

        Log.i(TAG, "getMessage: proxying to browser");
        browser().getMessage(message);
    }

    void getAlert(Alert alert) {
        if (!isBrowserSet()) {
            Log.e(TAG, "browser is NULL!");
            return;
        }

        Log.d(TAG, "getAlert");
        browser().getAlert(alert);
    }

    void getContextMenu(ContextMenu contextMenu) {
        if (!isBrowserSet()) {
            Log.e(TAG, "browser is NULL!");
            return;
        }

        Log.d(TAG, "getContextMenu");
        browser().getContextMenu(contextMenu);
    }

    public void confirmMessage(final Message message) {
        if (!isBrowserSet()) {
            Log.e(TAG, "browser is NULL!");
            return;
        }

        new Thread(() -> {
            Log.d(TAG, "confirmMessage");
            browser().confirmMessage(message);
        }).start();
    }

    public void cancelMessage(final Message message) {
        if (!isBrowserSet()) {
            Log.e(TAG, "browser is NULL!");
            return;
        }

        new Thread(() -> {
            Log.d(TAG, "cancelMessage");
            browser().cancelMessage(message);
        }).start();
    }

    public void login(LoginTarget t, String username, String password) {
        _browser.goHome();
        boolean ok = browse(t.toAccountsPath());
        if (!ok) Log.e(TAG, "login: problem browsing path " + t);

        ok = setEditable(t.getUsernameIndex(), username);
        if (!ok) Log.e(TAG, "login: problem setting username " + t);

        ok = setEditable(t.getPasswordIndex(), password);
        if (!ok) Log.e(TAG, "login: problem setting password " + t);

        ok = _browser.browseItem(t.getLoginIndex());
        if (!ok) Log.e(TAG, "login: problem adding account " + t);

        _browser.goHome();
        // navigating immediately to the source page crashes!
        new Handler(Looper.myLooper()).postDelayed(() -> browseItem(t.toRootIndex()), LOGIN_DELAY_MS);
    }

    public void logout(LoginTarget t) {
        _browser.goHome();
        boolean ok = browse(t.toAccountsPath());
        if (!ok) Log.e(TAG, "logout: problem browsing path " + t);
        browseItem(t.getLogoutIndex());
        new Handler(Looper.myLooper()).postDelayed(() -> _browser.goHome(), LOGOUT_DELAY_MS);
    }

    /** Clients should replace cached query results after the mutation. */
    boolean acceptChromecastTos() {
        try {
            goHome();
            browse(chromecastSettings);
            _browser.setItem(0, "true");
            goHome();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "acceptChromecastTos: " + e.getMessage());
            return false;
        }
    }

    /** Clients should cache the result of this lookup. */
    boolean isChromecastTosAccepted() {
        try {
            goHome();
            browse(chromecastSettings);
            RowEntry checkbox = _browser.getItems(0, 1).get(0);
            String val = checkbox.get_editData();
            Log.i(TAG, "isChromecastTosAccepted: " + val);
            goHome();
            return val.equals("1");
        } catch (Exception e) {
            Log.e(TAG, "isChromecastTosAccepted: " + e.getMessage());
        }
        return true;
    }

    private boolean setEditable(int idx, String value) {
        _browser.browseItem(idx);
        return _browser.setItem(idx, value);
    }

    private boolean browse(List<String> path) {
        if (path.isEmpty()) {
            Log.w(TAG, "browse: got empty path, nothing to do");
        }

        for (String s : path) {
            boolean found = false;
            RowEntryVector v = _browser.getItems(0, _browser.getNumItems());
            for (int i = 0; i < v.size(); i++) {
                RowEntry e = v.get(i);
                String name = e.get_name();
                if (name.equals(s)) {
                    found = true;
                    boolean ok = _browser.browseItem(i);
                    if (!ok) {
                        Log.e(TAG, "browse: failed! at " + i + " " + name);
                        return false;
                    }
                    break;
                }
            }
            if (!found) {
                Log.e(TAG, "browse: no entry with name " + s + " in vector " + v);
                return false;
            }
        }
        return true;
    }

}
