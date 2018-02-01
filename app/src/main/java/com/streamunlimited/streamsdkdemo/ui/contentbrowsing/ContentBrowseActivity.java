package com.streamunlimited.streamsdkdemo.ui.contentbrowsing;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.streamunlimited.remotebrowser.Alert;
import com.streamunlimited.remotebrowser.ContextMenu;
import com.streamunlimited.remotebrowser.Message;
import com.streamunlimited.remotebrowser.PlayState;
import com.streamunlimited.remotebrowser.PlayStatus;
import com.streamunlimited.remotebrowser.RemoteBrowser;
import com.streamunlimited.remotebrowser.RowEditType;
import com.streamunlimited.remotebrowser.RowEntryVector;
import com.streamunlimited.remotebrowser.StandbyState;
import com.streamunlimited.remotebrowser.StringVector;
import com.streamunlimited.remotebrowser.ViewType;
import com.streamunlimited.streamsdkdemo.R;
import com.streamunlimited.streamsdkdemo.StreamControlApp;
import com.streamunlimited.streamsdkdemo.callbacks.IBitmapCache;
import com.streamunlimited.streamsdkdemo.callbacks.IBrowser;
import com.streamunlimited.streamsdkdemo.callbacks.IConnection;
import com.streamunlimited.streamsdkdemo.callbacks.IPlayView;
import com.streamunlimited.streamsdkdemo.callbacks.IPlayer;
import com.streamunlimited.streamsdkdemo.callbacks.ITransition;
import com.streamunlimited.streamsdkdemo.callbacks.IUtils;
import com.streamunlimited.streamsdkdemo.callbacks.IWifiStateChanged;
import com.streamunlimited.streamsdkdemo.data.Action;
import com.streamunlimited.streamsdkdemo.data.BrowseRowEntry;
import com.streamunlimited.streamsdkdemo.data.ClientViewEnum;
import com.streamunlimited.streamsdkdemo.data.Source;
import com.streamunlimited.streamsdkdemo.helper.ContentFragmentTransaction;
import com.streamunlimited.streamsdkdemo.helper.DeviceManager;
import com.streamunlimited.streamsdkdemo.helper.Devices;
import com.streamunlimited.streamsdkdemo.helper.KeyboardManager;
import com.streamunlimited.streamsdkdemo.helper.StreamControlBroadcastReceiver;
import com.streamunlimited.streamsdkdemo.helper.StreamControlRemoteBrowserManager;
import com.streamunlimited.streamsdkdemo.helper.WifiBroadcastReceiver;
import com.streamunlimited.streamsdkdemo.ui.StreamAlertDialogBuilder;
import com.streamunlimited.streamsdkdemo.ui.contentbrowsing.contextmenubrowsing.ContextMenuActivity;
import com.streamunlimited.streamsdkdemo.ui.player.PlayViewFragment;
import com.streamunlimited.streamsdkdemo.ui.player.PlayViewWidgetFragment;
import com.streamunlimited.streamsdkdemo.ui.streamshare.FollowDialog;
import com.streamunlimited.streamsdkdemo.ui.streamshare.IUnfollowListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;

import static com.streamunlimited.streamsdkdemo.data.Action.STANDBY_STATE_CHANGED;

/**
 *
 */
public class ContentBrowseActivity extends Activity implements
        IBitmapCache,
        ITransition,
        IPlayView,
        IBrowser,
        IPlayer,
        IUtils,
        IWifiStateChanged,
        IConnection {

    private static final String TAG = ContentBrowseActivity.class.getSimpleName();

    private final Queue<Message> _messageQueue = new LinkedList<>();

    private boolean _paused = false;
    private boolean _tearingDown = false;
    private boolean _messageDialogShowing = false;

    private ContentBrowseTransitionFragment _browseFragment;
    private PlayViewWidgetFragment _playWidgetFragment;
    private PlayViewFragment _playFragment;

    private Bundle _playStatusBundle;
    private PlayState _currentPlayState = PlayState.ePlayStateUndefined;
    private ClientViewEnum _currentClientView = ClientViewEnum.eClientViewBrowse;
    private DeviceManager _currentDeviceManager;

    private List<String> _searchTitles;
    private RelativeLayout _searchBar;
    private int _selectedSearchTitle;
    private boolean _searchBarVisibility;

    private int _currentPlaylistDepth;

    private ImageCache _imageCache;

    private boolean _contextOpen = false;
    private AlertDialog _dialog;
    private boolean canStreamShare = true;
    private Menu menu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_content_browse);
        setProgressBarIndeterminateVisibility(false);

        if (!StreamControlApp.isTabletMode(this))
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        _imageCache = new ImageCache(this, ((ActivityManager) getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass());

        _searchTitles = new ArrayList<>();
        _currentPlaylistDepth = 0;
        _selectedSearchTitle = 0;

        WifiBroadcastReceiver.instance().addCallback(this);

        _currentDeviceManager = Devices.instance(this).getCurrentDevice();

        if (_currentDeviceManager != null) {

            setClientView(_currentClientView);

            setActionbarTitleAndLogo();

            // shows the menu loader
            toggleActionBarProgress(true);

            initializeSearchBar();
            return;
        }

        setResult(RESULT_CANCELED);
        finish();
    }

    private void goPlayqueue() {
        _currentDeviceManager.browsePlayqueue();
        _currentPlaylistDepth = 1;
    }

    @Override
    public void onPause() {
        super.onPause();
        _paused = true;
        unregisterReceivers();
        _currentDeviceManager.setConnectionCallback(null);
        _searchTitles.clear();

        // if tearDown() wasn't called, the device goes to standby
        // so disconnect from all known devices
        if (!_tearingDown) {
            Log.i(TAG, "onPause: disconnecting");
            Devices.instance(this).disconnectAll();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.i(TAG, "onResume: paused? " + _paused);

        if (_paused) {
            if (!WifiBroadcastReceiver.instance().isConnected()) {
                Log.i(TAG, "onResume: paused but no wifi connection; aborting");
                tearDown(false);
                return;
            } else {
                // try to reconnect and call onConnectSuccess if successful
                toggleActionBarProgress(true);
                _currentDeviceManager.setConnectionCallback(this);
                Devices.instance(this).connectAll(TAG);
                _paused = false;
            }
        }
        _tearingDown = false;
        registerReceivers();
    }

    @Override
    protected void onDestroy() {
        _tearingDown = false;
        WifiBroadcastReceiver.instance().removeCallback(this);
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (StreamControlApp.isTabletMode(this)) {
            setContentView(R.layout.activity_content_browse);

            // Reset the fragments
            ContentFragmentTransaction initTransaction = new ContentFragmentTransaction(getFragmentManager().beginTransaction());
            initTransaction.removeFragment(_browseFragment);
            initTransaction.removeFragment(_playFragment);
            initTransaction.addFragments(this, _browseFragment, _playFragment, _playWidgetFragment);
            initTransaction.commitAllowingStateLoss();
        }

        _imageCache.interruptCaching();

        setClientView(_currentClientView);
    }

    // menu functions

    @Override
    public boolean onCreateOptionsMenu(Menu m) {
        getMenuInflater().inflate(R.menu.browse_menu, m);
        menu = m;
        m.setGroupVisible(R.id.netapi_action_group, false); // search menu
        return super.onCreateOptionsMenu(m);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
            case android.R.id.home:
                goBack();
                return true;
            case R.id.action_top:
                goHome();
                return true;
            case R.id.action_grouping:
                showStreamSharePopup();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem shareButton = menu.findItem(R.id.action_grouping);
        shareButton.setVisible(canStreamShare);
        return super.onPrepareOptionsMenu(menu);
    }

    private void enableStreamShare(boolean value) {
        canStreamShare = value;
        invalidateOptionsMenu();
    }

    private void showStreamSharePopup() {
        List<IUnfollowListener> callbacks = Arrays.asList(_playFragment, _playWidgetFragment, onUnfollow);
        new FollowDialog(_currentDeviceManager, Devices.instance(this).filter(_currentDeviceManager), callbacks)
                .show(getFragmentManager(), FollowDialog.TAG);
    }

    @Override
    public void onBackPressed() {
        goBack();
    }

    private void goBack() {
        if (!StreamControlApp.isTabletMode(this) && _currentClientView == ClientViewEnum.eClientViewPlayScreen) {
            setClientView(ClientViewEnum.eClientViewPlayWidget);
        } else {
            if (_currentDeviceManager.getBrowser().getCurrentDepth() == 0) {
                super.onBackPressed();
                tearDown(true);
            } else {
                // TODO: check if this if-statement is necessary
                if (_browseFragment != null && _browseFragment.isInitialBrowseFinished()) {
                    _browseFragment.setInitialBrowseFinished(false);

                    toggleActionBarProgress(true);
                    _currentDeviceManager.getBrowserManager().browseParent();
                    _currentPlaylistDepth --;
                }
            }
        }
    }

    private void goHome() {
        if (!StreamControlApp.isTabletMode(this) && _currentClientView == ClientViewEnum.eClientViewPlayScreen) {
            setClientView(ClientViewEnum.eClientViewPlayWidget);
        }
        toggleActionBarProgress(true);
        _currentDeviceManager.getBrowserManager().goHome();
    }

    private void setActionbarTitleAndLogo() {
        runOnUiThread(() -> {
            if (!StreamControlApp.isTabletMode(this) && _currentClientView == ClientViewEnum.eClientViewPlayScreen) {
                getActionBar().setTitle(R.string.title_back);
                getActionBar().setSubtitle(R.string.subtitle_back);
                return;
            }

            new UpdateToolbarTask().execute();
        });
    }

    public void toggleActionBarProgress(boolean show) {
        final boolean fShow = show;
        runOnUiThread(() -> setProgressBarIndeterminateVisibility(fShow));
    }

    private void initializeSearchBar() {
        Log.d(TAG, "initializeSearchBar()");

        final EditText searchText = findViewById(R.id.search_bar_textview);
        searchText.setOnEditorActionListener((v, actionId, event) -> {
            if ((actionId == EditorInfo.IME_ACTION_SEARCH )) {
                KeyboardManager.hideSoftKeyboard(ContentBrowseActivity.this);
                _currentDeviceManager.getBrowser().search(searchText.getText().toString(), _selectedSearchTitle);
                toggleActionBarProgress(true);
                return true;
            }
            return false;
        });

        _searchBar = findViewById(R.id.search_bar);

        ImageButton menuButton = findViewById(R.id.search_bar_menu_button);
        menuButton.setOnClickListener(v -> runOnUiThread(() -> {
            StreamAlertDialogBuilder dialogBuilder = new StreamAlertDialogBuilder(ContentBrowseActivity.this);
            dialogBuilder.removeTitle();

            dialogBuilder.setTitle(R.string.context_menu_title);

            CharSequence[] cs = _searchTitles.toArray(new CharSequence[_searchTitles.size()]);
            dialogBuilder.setSingleChoiceItems(cs, _selectedSearchTitle, (dialog, which) -> {
                searchText.setHint(_searchTitles.get(which));
                _selectedSearchTitle = which;
                dialog.dismiss();
            });


            dialogBuilder.setOnDismissListener(dialog -> {});
            AlertDialog diaglog = dialogBuilder.create();
            diaglog.show();
        }));


        ImageButton searchButton = findViewById(R.id.search_bar_search_button);
        searchButton.setOnClickListener(v -> {
            KeyboardManager.hideSoftKeyboard(ContentBrowseActivity.this);
            _currentDeviceManager.getBrowser().search(searchText.getText().toString(), _selectedSearchTitle);
            toggleActionBarProgress(true);
        });
    }

    public void updateSearchBar() {
        new Thread(() -> {
            _searchTitles.clear();
            StringVector titles = Devices.instance(this).getCurrentDevice().getBrowser().getSearchTitles();
            for (int i = 0; i < titles.size(); i++) {
                _searchTitles.add(titles.get(i));
            }

            runOnUiThread(() -> {
                if (_searchTitles.size() > 0) {
                    _searchBarVisibility = true;
                    ((EditText) findViewById(R.id.search_bar_textview)).setHint(_searchTitles.get(_selectedSearchTitle));
                } else {
                    _searchBarVisibility = false;
                }

                boolean wat = _searchBarVisibility && (_currentClientView != ClientViewEnum.eClientViewPlayScreen || StreamControlApp.isTabletMode(this));
                _searchBar.setVisibility(wat ? View.VISIBLE : View.GONE);
            });

        }).start();
    }

    // Broadcast Receiver functions

    private void registerReceivers() {
        unregisterReceivers();

        if (_currentDeviceManager != null) {
            _currentDeviceManager.addManagerBrowserCallback(this);
            _currentDeviceManager.addManagerPlayerCallback(this);
            _currentDeviceManager.addManagerUtilsCallback(this);
        }

        LocalBroadcastManager mgr = LocalBroadcastManager.getInstance(this);
        mgr.registerReceiver(_receivePlayStatus, new IntentFilter(Action.PLAY_STATUS_CHANGED));
        mgr.registerReceiver(_receiveDisconnect, new IntentFilter(Action.CLIENT_DISCONNECTED));
        mgr.registerReceiver(_receiveShutdown, new IntentFilter(Action.CLIENT_SHUTDOWN));
        mgr.registerReceiver(_receiveStandbyChanged, new IntentFilter(STANDBY_STATE_CHANGED));
        mgr.registerReceiver(_receiveOpenLink, new IntentFilter(Action.OPEN_LINK_FILTER));
        mgr.registerReceiver(_receiveAppLink, new IntentFilter(Action.OPEN_APP_FILTER));
    }

    private void unregisterReceivers() {
        if (_currentDeviceManager != null) {
            _currentDeviceManager.removeManagerBrowserCallback(this);
            _currentDeviceManager.removeManagerPlayerCallback(this);
            _currentDeviceManager.removeManagerUtilsCallback(this);
        }

        LocalBroadcastManager mgr = LocalBroadcastManager.getInstance(this);
        mgr.unregisterReceiver(_receivePlayStatus);
        mgr.unregisterReceiver(_receiveDisconnect);
        mgr.unregisterReceiver(_receiveShutdown);
        mgr.unregisterReceiver(_receiveStandbyChanged);
        mgr.unregisterReceiver(_receiveOpenLink);
        mgr.unregisterReceiver(_receiveAppLink);
    }

    private final StreamControlBroadcastReceiver _receiveOpenLink = new StreamControlBroadcastReceiver() {
        @Override
        protected void onReceiveFinished(Intent intent) {
            String _link = intent.getStringExtra("openLink");
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(_link));
            startActivity(browserIntent);
            finish();
        }
    };

    private final StreamControlBroadcastReceiver _receiveAppLink = new StreamControlBroadcastReceiver() {
        @Override
        protected void onReceiveFinished(Intent intent) {
            String _link = intent.getStringExtra("openApp");
            Intent i;
            PackageManager manager = getPackageManager();
            try {
                i = manager.getLaunchIntentForPackage(_link);
                if (i == null)
                    throw new PackageManager.NameNotFoundException();
                i.addCategory(Intent.CATEGORY_LAUNCHER);
                startActivity(i);
            } catch (PackageManager.NameNotFoundException e) {
                String _linkToMarket = "market://details?id=" + _link;
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(_linkToMarket));
                startActivity(browserIntent);
            }
            finish();
        }
    };

    private final StreamControlBroadcastReceiver _receiveStandbyChanged = new StreamControlBroadcastReceiver() {
        @Override
        protected void onReceiveFinished(Intent intent) {
            _playStatusBundle = intent.getExtras();
            StandbyState standbyState = StandbyState.swigToEnum(_playStatusBundle.getInt("standbyState"));
            if (standbyState == StandbyState.eStandbyStateStandby || standbyState == StandbyState.eStandbyStateOffline) {
                runOnUiThread(() -> {
                    tearDown(true);
                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.info_connection_standby), Toast.LENGTH_SHORT).show();
                });
            }
        }
    };

    private final StreamControlBroadcastReceiver _receivePlayStatus = new StreamControlBroadcastReceiver() {
        @Override
        protected void onReceiveFinished(Intent i) {
            _playStatusBundle = i.getExtras();
            PlayState state = PlayState.swigToEnum(_playStatusBundle.getInt("playState"));
            String source = getSource();
            Log.i(TAG, "_receivePlayStatus: " + state + " " + source);

            if (_currentPlayState == null || _currentPlayState != state || _currentDeviceManager.isNewPlaybackSelected()) {
                _currentPlayState = state;
                updateClientViewByPlayStatus();
            }

            boolean playing = state == PlayState.ePlayStatePlay || state == PlayState.ePlayStatePause;
            boolean shareable = !playing || !Source.streamshareBlacklist.contains(source);
            enableStreamShare(shareable);
        }
    };

    private final StreamControlBroadcastReceiver _receiveDisconnect = new StreamControlBroadcastReceiver() {
        @Override
        protected void onReceiveFinished(Intent intent) {
            runOnUiThread(() -> {
                tearDown(true);
                Toast.makeText(getApplicationContext(), getResources().getString(R.string.info_connection_terminated), Toast.LENGTH_SHORT).show();
            });
        }
    };

    private final StreamControlBroadcastReceiver _receiveShutdown = new StreamControlBroadcastReceiver() {
        @Override
        protected void onReceiveFinished(Intent intent) {
            tearDown(true);
        }
    };

    private void updateClientViewByPlayStatus() {
        Log.d(TAG, "updateClientViewByPlayStatus: " +
                _currentPlayState.toString() + " " +
                _currentClientView.toString());

        switch (_currentPlayState) {
            case ePlayStatePause:
            case ePlayStatePlay:
                if (_currentClientView == ClientViewEnum.eClientViewBrowse) {
                    setClientView(ClientViewEnum.eClientViewPlayScreen);
                } else if (_currentClientView == ClientViewEnum.eClientViewPlayWidget && _currentDeviceManager.isNewPlaybackSelected()) {
                    setClientView(ClientViewEnum.eClientViewPlayScreen);
                } else {
                    setClientView(_currentClientView);
                }
                break;

            case ePlayStateStop:
                setClientView(ClientViewEnum.eClientViewBrowse);
                break;

            default:
                break;
        }

        // Reset the newPlaybackSelected flag
        _currentDeviceManager.setNewPlaybackSelected(false);
    }

    /**
     * StateMachine for clientView to handle different layouts in tablet and smartphone mode
     */
    public void setClientView(ClientViewEnum view) {

        boolean isTablet = StreamControlApp.isTabletMode(this);
        if (_browseFragment == null && _playFragment == null && _playWidgetFragment == null) {

            // create fragments and add them to ContentFragmentTransaction
            _browseFragment = new ContentBrowseTransitionFragment();
            _browseFragment.registerReceivers();

            _playFragment = new PlayViewFragment();
            _playFragment.registerReceivers();

            if (!isTablet) {
                _playWidgetFragment = new PlayViewWidgetFragment();
                _playWidgetFragment.registerReceivers();
            }

            ContentFragmentTransaction initTransaction = new ContentFragmentTransaction(getFragmentManager().beginTransaction());
            initTransaction.addFragments(this, _browseFragment, _playFragment, _playWidgetFragment).commit();
        }

        Log.d(TAG, "setClientView() previous view = " + _currentClientView.toString());
        Log.d(TAG, "setClientView() current view = " + view.toString());
        ContentFragmentTransaction transaction = new ContentFragmentTransaction(getFragmentManager().beginTransaction());

        switch (view) {
            case eClientViewPlayScreen:
                transaction.showFragment(_browseFragment, isTablet);
                transaction.showFragment(_playWidgetFragment, false);
                transaction.showFragment(_playFragment, true);
                if (!isTablet && _searchBar != null) _searchBar.setVisibility(View.GONE);
                break;
            case eClientViewPlayWidget:
                if (_currentClientView != view) {
                    if (_currentClientView != ClientViewEnum.eClientViewBrowse) {
                        transaction.showFragment(_browseFragment, true);
                    }

                    transaction.showFragment(_playWidgetFragment, !isTablet);
                    transaction.showFragment(_playFragment, isTablet);
                }
                updateSearchBarVisibility();
                break;
            case eClientViewBrowse:
                transaction.showFragment(_browseFragment, true);
                transaction.showFragment(_playWidgetFragment, false);
                transaction.showFragment(_playFragment, isTablet);
                updateSearchBarVisibility();
                break;
            default:
                break;
        }

        _currentClientView = view;
        transaction.commitAllowingStateLoss();
        // call this only after setting _currentClientView
        setActionbarTitleAndLogo();
    }

    private void updateSearchBarVisibility() {
        if (_searchBar != null) _searchBar.setVisibility(_searchBarVisibility ? View.VISIBLE : View.GONE);
    }

    public void hideProgressDialog() {
        runOnUiThread(() -> {
            if (_dialog != null) _dialog.dismiss();
        });
    }

    public void tearDown(boolean resultOk) {
        Log.i(TAG, "tearDown: ok=" + resultOk + ", in flight? " + _tearingDown);
        if (!_tearingDown) {
            _tearingDown = true;
            ContentFragmentTransaction transaction = new ContentFragmentTransaction(getFragmentManager().beginTransaction());

            if (!StreamControlApp.isTabletMode(this)) transaction.removeFragment(_playWidgetFragment);

            transaction.removeFragment(_browseFragment);
            transaction.removeFragment(_playFragment);
            transaction.commit();

            setResult(resultOk ? RESULT_OK : RESULT_CANCELED);

            finish();
        }
    }

    private void dismissDialog(Message message, DialogInterface dialog, boolean positive) {
        StreamControlRemoteBrowserManager mgr = _currentDeviceManager.getBrowserManager();
        if (message == null) {
            message = new Message();
            mgr.getMessage(message);
        }
        if (positive) {
            mgr.confirmMessage(message);
        } else {
            mgr.cancelMessage(message);
        }
        _messageDialogShowing = false;
        dialog.dismiss();

        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(Action.MESSAGE));

        if (_browseFragment != null) _browseFragment.setInitialBrowseFinished(true);
    }

    private final IUnfollowListener onUnfollow = () -> {
        _currentPlayState = PlayState.ePlayStateStop;
        updateClientViewByPlayStatus();
    };

    //----------------------------------------
    // ITransition
    //----------------------------------------

    @Override
    public void onShowProgressDialog() {
        runOnUiThread(() -> {
            if (_currentDeviceManager.getBrowserManager().browser().isProcessing() && (_dialog == null || !_dialog.isShowing())) {
                StreamAlertDialogBuilder dialogBuilder = new StreamAlertDialogBuilder(ContentBrowseActivity.this);
                dialogBuilder.removeTitle();

                dialogBuilder.setNegativeButton(getResources().getString(android.R.string.cancel), (dialog, which) -> {
                    _currentDeviceManager.getBrowserManager().browser().cancel();
                    _dialog.dismiss();
                });

                dialogBuilder.setMessage("Loading...");
                dialogBuilder.setCancelable(false);
                _dialog = dialogBuilder.create();
                _dialog.show();
            }
        });
    }

    @Override
    public void onBrowseItemSelected(BrowseRowEntry entry) {
        if (_currentPlaylistDepth > 0) _currentPlaylistDepth++;
    }

    //----------------------------------------
    // IPlayView
    //----------------------------------------

    @Override
    public Bundle onPlayViewCreated() {
        if (_playStatusBundle != null) {
            _playStatusBundle.putInt("currentVolume", _currentDeviceManager.getCurrentVolume());
            _playStatusBundle.putInt("maxVolume", _currentDeviceManager.getMaxVolume());
            _playStatusBundle.putInt("minVolume", _currentDeviceManager.getMinVolume());
            _playStatusBundle.putInt("volumeStep", _currentDeviceManager.getVolumeStep());
        }
        return _playStatusBundle;
    }

    /** This function is only intendet to be called from Playview, so that Playview closes and the Playqueue is opened in the browseView */
    @Override
    public void browsePlayqueue() {
        goBack();
        goPlayqueue();
    }

    //----------------------------------------
    // IBrowser
    //----------------------------------------

    @Override
    public void onViewChanged() {
        _imageCache.interruptCaching();
        setActionbarTitleAndLogo();
    }

    @Override
    public void onNumItemsChanged(int numItems) {}

    @Override
    public void onViewTypeChanged(ViewType type) {}

    //----------------------------------------
    // IPlayer
    //----------------------------------------

    @Override
    public void onPlayStatusChanged(PlayStatus status) {}

    public void readMessageQueue() {
        Message msgTemp = null;

        try {
            msgTemp = _messageQueue.remove();
        } catch (NoSuchElementException ex) {
            Log.e(TAG, "readMessageQueue: " + ex.getMessage());
        }

        if (msgTemp == null) return;

        final Message msg = msgTemp;
        final String body = msg.get_messageText();
        Log.i(TAG, "readMessageQueue: " + "captionText: " + msg.get_captionText() + " | messageText: " + body);

        if (body.equals("Ignoring in stopped state control command: stop")) {
            return;
        }

        if (body.equals("Invalid user credentials")) { // manifests elsewhere as 0 or 1 rows loaded: swallow this alert and show a login popup there
            Log.w(TAG, "readMessageQueue: login required; aborting");
            return;
        }

        if (body.equals("Please enter a valid username, and try again.") || // Sirius
            body.equals("Please enter a valid password, and try again.") || // Sirius
            body.equals("Invalid Password") || // Sirius
            body.equals("The email address and password do not match an existing account. Please try again.") || // Napster
            body.equals("Invalid member. Check your Napster account configuration.")) {  // Napster
            Log.w(TAG, "readMessageQueue: invalid password, aborting.");
            _currentDeviceManager.getBrowserManager().goHome();
            Toast.makeText(this, R.string.err_login_password, Toast.LENGTH_LONG).show();
            return;
        }

        runOnUiThread(() -> {
            StreamAlertDialogBuilder bld = new StreamAlertDialogBuilder(ContentBrowseActivity.this);
            bld.removeTitle();


            String message = msg.get_captionText() + "\n" + body;

            if (msg.get_captionText().contains("Network Error")) {
                bld.setPositiveButton(getResources().getString(android.R.string.ok), (dialog, which) -> {
                    _messageQueue.clear();
                    _currentDeviceManager.getBrowserManager().cancelMessage(msg);
                    dismissDialog(null, dialog, false);
                    tearDown(true);
                });

                bld.setMessage(message);
            } else {
                if (body.contains("Playback Error")) {
                    message += "\n" + "Do you want to stop the playback?";

                    bld.setPositiveButton(getResources().getString(android.R.string.yes), (dialog, which) -> {
                        _messageQueue.clear();
                        _currentDeviceManager.getBrowserManager().cancelMessage(msg);
                        _currentDeviceManager.getBrowserManager().stop();
                        dismissDialog(null, dialog, false);
                    });
                    bld.setNegativeButton(getResources().getString(android.R.string.no), dialogClickListener);
                } else {
                    bld.setNegativeButton(getResources().getString(android.R.string.ok), dialogClickListener);
                }

                bld.setMessage(message);
            }

            AlertDialog dialog = bld.create();

            dialog.setOnDismissListener(dialog1 -> _messageDialogShowing = false);

            _messageDialogShowing = true;
            dialog.show();
        });
    }

    private final DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
        dismissDialog(null, dialog, false);
        readMessageQueue();
    };

    //----------------------------------------
    // IUtils
    //----------------------------------------

    @Override
    public void onMessage(DeviceManager senderDevice, Message message) {
        Log.i(TAG, "onMessage: sender=" + senderDevice.getDeviceRowEntry().getName() +
                   ", id=" + message.get_messageId() +
                   ", caption=" + message.get_captionText() +
                   ", message=" + message.get_messageText());

        _messageQueue.add(message);
        if (!_messageDialogShowing) readMessageQueue();
    }

    @Override
    public void onAlert(DeviceManager senderDevice, final Alert alert) {
        Log.d(TAG, "onAlert() Sender: " + senderDevice.getDeviceRowEntry().getName());
        Log.d(TAG, "onAlert() Id: " + alert.get_alertId());
        Log.d(TAG, "onAlert() Caption: " + alert.get_captionText());

        StreamAlertDialogBuilder bld = new StreamAlertDialogBuilder(ContentBrowseActivity.this);
        bld.removeTitle();

        final RowEntryVector rows = alert.get_alertItems();
        final CharSequence[] items = new CharSequence[(int) rows.size()];
        for (int i = 0; i < rows.size(); i++) {
            items[i] = rows.get(i).get_name();
        }

        bld.setTitle(alert.get_captionText());

        if (items.length <= 0) return;

        if (items.length <= 2) {
            bld.setNegativeButton(items[0], (dialog, which) -> {
                _currentDeviceManager.getBrowserManager().browseContextMenuItem(0);
                dialog.dismiss();
            });
            if (items.length == 2) {
                bld.setPositiveButton(items[1], (dialog, which) -> {
                    _currentDeviceManager.getBrowserManager().browseContextMenuItem(1);
                    dialog.dismiss();
                });
            }
            show(bld);
            return;
        }

        if (items.length > 2) {
            bld.setSingleChoiceItems(items, 0, (dialog, which) -> {
                final int index = which;
                BrowseRowEntry entry = new BrowseRowEntry(rows.get(which) );
                if (entry.isBrowsable() || entry.isInvokable()) {
                    _currentDeviceManager.getBrowserManager().browser().browseContextMenuItem(which);
                } else if (entry.isEditable()) {
                    switch (entry.get_editType()) {
                        case eEditTypeString:
                        case eEditTypePassword:
                            final StreamAlertDialogBuilder alertDialogBuilder = new StreamAlertDialogBuilder(ContentBrowseActivity.this);
                            alertDialogBuilder.setTitle(entry.get_name().replace(": " + entry.get_editData(), ""));
                            alertDialogBuilder.setNegativeButton(android.R.string.no, (dialog1, which1) -> dialog1.cancel());
                            if (entry.get_editType() == RowEditType.eEditTypePassword)
                                alertDialogBuilder.setPasswordMode(true);
                            alertDialogBuilder.setEditText(entry.get_editData());
                            alertDialogBuilder.setPositiveButton(android.R.string.yes, (dialog12, which12) -> {
                                // save value and hide the progessbar
                                _currentDeviceManager.getBrowser().setContextMenuItem(index, alertDialogBuilder.getEditTextValue().toString());
                                dialog12.dismiss();
                            });
                            alertDialogBuilder.setOnDismissListener(dialog13 -> {
                            });


                            alertDialogBuilder.create().show();
                            break;
                        default:
                            break;
                    }
                }
                dialog.dismiss();
            });
        }

        show(bld);
    }

    @Override
    public void onContextMenu(DeviceManager senderDevice, ContextMenu contextMenu) {
        //pop the ContextMenu
        if (contextMenu == null) {
            _tearingDown = false;
            return;
        }
        final String title = contextMenu.get_title();
        final int depth = contextMenu.get_depth();
        final int numItems = contextMenu.get_numItems();

        setContextOpen(true);
        this.runOnUiThread(() -> {
            Intent i = new Intent(getApplicationContext(), ContextMenuActivity.class);
            i.putExtra("title", title);
            i.putExtra("depth", depth);
            i.putExtra("numItems", numItems);
            startActivity(i);
        });
        _tearingDown = true;
    }

    private String getSource() {
        if (_playStatusBundle == null) return "n/a";
        String out = _playStatusBundle.getString("source");
        return (out == null || out.isEmpty()) ? "empty" : out;
    }

    public boolean isContextOpen() {
        return _contextOpen;
    }
    public void setContextOpen(boolean value) {
        _contextOpen = value;
    }

    private void show(StreamAlertDialogBuilder dialogBuilder) {
        dialogBuilder.setOnDismissListener(dialog -> {
            _messageDialogShowing = false;
            readMessageQueue();
        });
        dialogBuilder.create().show();

        _messageDialogShowing = true;
    }

    //----------------------------------------
    // IWifiStateChanged
    //----------------------------------------

    @Override
    public void onWifiStateChanged(boolean connected) {
        if (!_paused) {
            Log.i(TAG, "onWifiStateChanged: not paused, aborting");
            tearDown(false);
        }
    }

    //----------------------------------------
    // IConnection
    //----------------------------------------

    @Override
    public void onConnectSuccess() {
        toggleActionBarProgress(false);
        setClientView(_currentClientView);

        if (_playFragment != null) _playFragment.registerReceivers();

        if (!StreamControlApp.isTabletMode(this) && _playWidgetFragment != null) {
            _playWidgetFragment.registerReceivers();
        }
    }

    @Override
    public void onConnectFailed() {
        Log.i(TAG, "onConnectFailed: aborting");
        tearDown(false);
    }

    @Override
    public void onDisconnectSuccess() {
        Log.i(TAG, "onDisconnectSuccess");
    }

    @Override
    public void onDisconnectFailed() {
        Log.i(TAG, "onDisconnectFailed");
    }

    public void loadBitmap(String uri, ImageCache.CacheImageCallback callback) {
        _imageCache.loadBitmap(uri, callback);
    }

    public Bitmap getBitmapFromMemCache(String key) {
        return _imageCache.getBitmapFromMemCache(key);
    }

    private boolean wantSiriusLogin;
    public boolean isSiriusLogin() {
        return wantSiriusLogin;
    }
    public void setSiriusLogin() {
        wantNapsterLogin = false;
        wantSiriusLogin = true;
    }

    private boolean wantNapsterLogin;
    public boolean isNapsterLogin() {
        return wantNapsterLogin;
    }
    public void setNapsterLogin() {
        wantSiriusLogin = false;
        wantNapsterLogin = true;
    }

    public void notLoggingIn() {
        wantSiriusLogin = false;
        wantNapsterLogin = false;
    }

    private String lastKnownPath;
    public String getLastKnownPath() {
        return lastKnownPath;
    }

    private class UpdateToolbarTask extends AsyncTask<Void, Void, Void> {
        private String title;
        private String subtitle;
        private String subtitleParent;
        private int depth = 0;

        @Override
        protected Void doInBackground(Void... voids) {
            title = _currentDeviceManager.getDeviceRowEntry().getName();

            RemoteBrowser b = _currentDeviceManager.getBrowserManager().browser();
            subtitle = b.getCurrentTitle();
            subtitleParent = b.getParentTitle();
            depth = b.getCurrentDepth();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            getActionBar().setTitle(title);
            String s = (depth >= 2 ? "../" : "") + subtitleParent + "/" + subtitle;
            String st = subtitleParent.isEmpty() ? subtitle : s;
            st = st.replace("UPnP", "My Music");
            getActionBar().setSubtitle(st);
            lastKnownPath = subtitle;
//            Log.i(TAG, "UpdateToolbarTask#onPostExecute: " + title + " " + st);
        }

    }
}
