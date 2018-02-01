package com.streamunlimited.streamsdkdemo.ui.contentbrowsing.contextmenubrowsing;

import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.TextView;

import com.streamunlimited.remotebrowser.Alert;
import com.streamunlimited.remotebrowser.ContextMenu;
import com.streamunlimited.remotebrowser.Message;
import com.streamunlimited.remotebrowser.RowEntryVector;
import com.streamunlimited.streamsdkdemo.R;
import com.streamunlimited.streamsdkdemo.callbacks.IBrowseTask;
import com.streamunlimited.streamsdkdemo.callbacks.IContextMenu;
import com.streamunlimited.streamsdkdemo.data.BrowseRowEntry;
import com.streamunlimited.streamsdkdemo.helper.ContentBrowseTask;
import com.streamunlimited.streamsdkdemo.helper.DeviceManager;
import com.streamunlimited.streamsdkdemo.helper.Devices;
import com.streamunlimited.streamsdkdemo.ui.contentbrowsing.AbstractTransitionFragment;
import com.streamunlimited.streamsdkdemo.ui.contentbrowsing.ContentBrowseActivity;
import com.streamunlimited.streamsdkdemo.ui.contentbrowsing.ContentBrowseListFragment;
import com.streamunlimited.streamsdkdemo.ui.contentbrowsing.ContentBrowseListInterface;

import java.util.ArrayList;


/**
 *
 */
public class ContextMenuFragment extends AbstractTransitionFragment implements IContextMenu {

    private static final String TAG = ContextMenuFragment.class.getSimpleName();

    private DeviceManager _currentDeviceManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        _currentDeviceManager = Devices.instance(getActivity()).getCurrentDevice();

        _items = new ArrayList<>();
        updateContextMenu();
    }

    @Override
    public void onPause() {
        super.onPause();
        _paused = true;

        _items.clear();

        unregisterReceivers();
        shutdownScheduler();
        if (_currentDeviceManager != null ) {
            _items.addAll(listFragment.getItemList());
            getChildFragmentManager().beginTransaction().remove(listFragment).commit();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        listFragment = new ContentBrowseListFragment(this, _items);

        getChildFragmentManager().beginTransaction().add(R.id.browse_fragment_container, listFragment).commit();

        listFragment.setShowEmpty(_items.isEmpty());

        setHasOptionsMenu(true);
        setRetainInstance(true);

        registerReceivers();

        if (_paused) {
            startScheduler();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_browse_transition, container, false);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if( !_paused) {
            unregisterReceivers();

            listFragment = new ContentBrowseListFragment(this, listFragment.getItemList());

            getChildFragmentManager().beginTransaction().replace(R.id.browse_fragment_container, listFragment).commitAllowingStateLoss();
            listFragment.setShowEmpty(_items.isEmpty());

            registerReceivers();
        }
    }

    //----------------------------------------
    // AbstractTransitionFragment
    //----------------------------------------

    @Override
    public void registerReceivers() {
        unregisterReceivers();
        if (_currentDeviceManager != null) {
            _currentDeviceManager.addManagerUtilsCallback(this);
        }
    }

    @Override
    public void unregisterReceivers() {
        if (_currentDeviceManager != null) {
            _currentDeviceManager.removeManagerUtilsCallback(this);
        }
    }

    @Override
    protected void playItem(int position) {
        Log.i(TAG, "playItem: " + position);
        _currentDeviceManager.getBrowserManager().playContextMenu(position);
    }

    @Override
    protected void browseItem(int position, BrowseRowEntry entry) {
        _currentDeviceManager.getBrowserManager().browseContextMenuItem(position);
    }

    @Override
    protected void setItem(int position, String value) {
        _currentDeviceManager.getBrowser().setContextMenuItem(position, value);
    }

    //----------------------------------------
    // IUtils
    //----------------------------------------

    @Override
    public void onMessage(DeviceManager senderDevice, Message message) {}

    @Override
    public void onAlert(DeviceManager senderDevice, Alert alert) {}

    @Override
    public void onContextMenu(DeviceManager senderDevice, ContextMenu contextMenu) {
        if (contextMenu == null) {
            terminate();
            return;
        }

        updateContextMenu();
    }

    protected void runRefreshList() {
        final ContentBrowseListInterface currFrag = listFragment;
        AbsListView currentListView = currFrag.getListView();
        int startIndex = currentListView.getFirstVisiblePosition();
        int lastIndex = currentListView.getLastVisiblePosition();
        if (lastIndex == -1) {
            return;
        }

        boolean dirty = false;
        // see if any of the currently visible items are dirty
        for (int i = startIndex; i < lastIndex + 1; i++) {
            if (currFrag.getItemList().get(i).get_dirty()) {
                startIndex = i;
                dirty = true;
                break;
            }
        }

        for (int i = startIndex; i < lastIndex + 1; i++) {
            BrowseRowEntry entry = currFrag.getItemList().get(i);
            if (entry.get_mediaData().get_albumArtUri() != null && !entry.get_mediaData().get_albumArtUri().isEmpty()) {
                ((ContextMenuActivity) getActivity()).loadBitmap(entry.get_mediaData().get_albumArtUri(),
                        () -> currFrag.getBrowseAdapter().notifyDataSetChanged());
            }
        }

        if (dirty) {
            if (!_currentDeviceManager.getBrowser().isProcessing() && _initialBrowseFinished) {
                int itemsToCache = 15 < _numItems - startIndex ? 15 : _numItems - startIndex;

                RowEntryVector rows = _currentDeviceManager.getBrowser().getItems(startIndex, itemsToCache);
                if (rows.size() == itemsToCache) {
                    addItems(currFrag.getItemList(), rows, startIndex);
                    getActivity().runOnUiThread(() -> currFrag.getBrowseAdapter().notifyDataSetChanged());
                }
            }
        }
    }

    public void setNumItems(int value) {
        super._numItems = value;
    }

    //----------------------------------------
    // IContextMenu
    //----------------------------------------

    @Override
    public void onContextMenuViewChanged() {
        updateContextMenu();
    }

    public void updateContextMenu() {
        _browserUpdateNr++;

        _items.clear();

        ContextMenu contextMenu = new ContextMenu();
        _currentDeviceManager.getBrowser().getContextMenu(contextMenu);
        _currentDeviceManager.getBrowser().getCurrentContextMenuDepth(); // FIXME: unused value, but the native call could have side effects...

        if (contextMenu.get_depth() < 0) {
            terminate();
            return;
        }

        _numItems = contextMenu.get_numItems();
        _currDepth = contextMenu.get_depth();
        final String title = contextMenu.get_title();

        getActivity().runOnUiThread(() -> {
            TextView titleView = getActivity().findViewById(R.id.title_txt);
            titleView.setText(title);
        });

        addDirtyItems(_items, _numItems);

        new ContentBrowseTask(getActivity(), new IBrowseTask() {
            @Override
            public void onBrowseFinished(RowEntryVector rows, int browserUpdateNr) {
                Log.i(TAG, "IBrowseTask#onBrowseFinished: " + rows.size() + " rows, seq=" + browserUpdateNr);
                if (getActivity() != null) hideItemProgressBar();

                try {
                    if (browserUpdateNr == _browserUpdateNr && getActivity() != null) {
                        _currDepth = Devices.instance(getActivity()).getCurrentDevice().getBrowser().getCurrentContextMenuDepth();

                        addItems(_items, rows, 0);

                        _initialBrowseFinished = true;

                        listFragment.getItemList().clear();
                        listFragment.getItemList().addAll(_items);
                        getActivity().runOnUiThread(() -> {
                            listFragment.getBrowseAdapter().notifyDataSetChanged();
                            getActivity().setProgressBarIndeterminateVisibility(false);
                        });

                        listFragment.setShowEmpty(_items.isEmpty());

                        shutdownScheduler();
                        startScheduler();

                        try {
                            listFragment.getListView().setFastScrollEnabled(_numItems > 50);
                        } catch (IllegalStateException e) {
                            Log.e(TAG, "IllegalStateException: " + e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "onBrowseFinished: " + e.getMessage());
                }
            }

            @Override
            public void onBrowseTimedOut() {
                if (getActivity() != null) {
                    hideItemProgressBar();
                    ((ContentBrowseActivity) getActivity()).tearDown(true);
                }
            }

        }, _browserUpdateNr, true).execute();
    }

    private void terminate() {
        _currDepth = -1;
        _numItems = -1;
        _items.clear();
        getActivity().finish();
    }

    protected boolean movePlaylistItems(int[] items, int plId) {
        return false;
    }

    protected void openContextMenuForItem(int itemNum) {
        _currentDeviceManager.getBrowserManager().browseIntoContextMenu(itemNum);
    }

    protected void hideItemProgressBar() {
        if (listFragment != null &&
            listFragment.getBrowseAdapter() != null) {
            listFragment.getBrowseAdapter().setProgressBarVisibility(View.GONE);
        }
    }

    private ContentBrowseListFragment listFragment;
    protected ContentBrowseListInterface getFragment() {
        return listFragment;
    }

}
