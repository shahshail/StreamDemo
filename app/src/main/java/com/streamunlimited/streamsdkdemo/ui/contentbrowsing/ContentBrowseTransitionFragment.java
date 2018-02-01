package com.streamunlimited.streamsdkdemo.ui.contentbrowsing;

import android.app.ProgressDialog;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;

import com.streamunlimited.remotebrowser.Alert;
import com.streamunlimited.remotebrowser.ContextMenu;
import com.streamunlimited.remotebrowser.Message;
import com.streamunlimited.remotebrowser.PlayStatus;
import com.streamunlimited.remotebrowser.RowEntryVector;
import com.streamunlimited.remotebrowser.ViewType;
import com.streamunlimited.streamsdkdemo.R;
import com.streamunlimited.streamsdkdemo.callbacks.IBrowseTask;
import com.streamunlimited.streamsdkdemo.callbacks.IBrowser;
import com.streamunlimited.streamsdkdemo.callbacks.IPlayer;
import com.streamunlimited.streamsdkdemo.data.BrowseRowEntry;
import com.streamunlimited.streamsdkdemo.data.Source;
import com.streamunlimited.streamsdkdemo.helper.ContentBrowseTask;
import com.streamunlimited.streamsdkdemo.helper.DeviceManager;
import com.streamunlimited.streamsdkdemo.helper.Devices;
import com.streamunlimited.streamsdkdemo.helper.WifiBroadcastReceiver;
import com.streamunlimited.streamsdkdemo.ui.login.LoginDialog;
import com.streamunlimited.streamsdkdemo.ui.login.LoginTarget;

import java.util.ArrayList;
import java.util.List;

/**
 * Waits for data to load and then swaps screens (ContentBrowseFragments)
 * for a fluent user experience without empty screens
 */
public class ContentBrowseTransitionFragment extends AbstractTransitionFragment implements
		IBrowser,
		IPlayer {

	private static final String TAG = ContentBrowseTransitionFragment.class.getSimpleName();

	private ContentBrowseListFragment _listBrowseFragment;
	private int _checkLastIndex = 0;

	private Menu _menu;
	private ProgressDialog progressDialog;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		super._currentDeviceManager = Devices.instance(getActivity()).getCurrentDevice();
		super._items = new ArrayList<>();
	}

	@Override
	public void onPause() {
		super.onPause();
		_paused = true;

        _items.clear();

		unregisterReceivers();
		shutdownScheduler();
		if (_currentDeviceManager != null) {
			_items.addAll(_listBrowseFragment.getItemList());
			getChildFragmentManager()
					.beginTransaction()
					.remove(_listBrowseFragment)
					.commit();
		}

		dismissProgressDialog();
	}

	@Override
	public void onResume() {
		super.onResume();

		final boolean wifiUp = WifiBroadcastReceiver.instance().isConnected();
		if (wifiUp) {

			_listBrowseFragment = new ContentBrowseListFragment(this, _items);

			getChildFragmentManager().beginTransaction().add(R.id.browse_fragment_container, _listBrowseFragment).commit();

			showCurrentFragment(_items.isEmpty());

			setHasOptionsMenu(true);
			setRetainInstance(true);

			registerReceivers();

			if (!((ContentBrowseActivity) getActivity()).isContextOpen())
				_currentDeviceManager.getBrowserManager().refresh();

			if (_paused) {
				startScheduler();
			}
		} else if (!_currentDeviceManager.isConnected() && !_currentDeviceManager.isConnecting()) {
			Log.w(TAG, "onResume: not connected to selected device, reconnecting...");
			_currentDeviceManager.connectAsync(TAG);
		}

		dismissProgressDialog();
		((ContentBrowseActivity) getActivity()).setContextOpen(false);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_browse_transition, container, false);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		_menu = menu;
		_menu.clear();
		inflater.inflate(R.menu.browse_menu, _menu);

		menu.setGroupVisible(R.id.netapi_action_group, false);

		super.onCreateOptionsMenu(_menu, inflater);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		if (!_paused) {
			unregisterReceivers();
			_listBrowseFragment = new ContentBrowseListFragment(this,
					_listBrowseFragment != null ? _listBrowseFragment.getItemList() : _items);

			getChildFragmentManager()
					.beginTransaction()
					.replace(R.id.browse_fragment_container, _listBrowseFragment)
					.commitAllowingStateLoss();

			showCurrentFragment(_items.isEmpty());

			registerReceivers();
		}
	}

	protected void runRefreshList() {
		AbsListView currentListView = _listBrowseFragment.getListView();
		int startIndex = currentListView.getFirstVisiblePosition();
		int lastIndex = currentListView.getLastVisiblePosition();
		if (lastIndex == -1) {
			return;
		}

		boolean dirty = false;
		// see if any of the currently visible items are dirty
		final List<BrowseRowEntry> entries = _listBrowseFragment.getItemList();

		for (int i = startIndex; i < lastIndex + 1; i++) {
			if (entries.get(i).get_dirty()) {
				startIndex = i;
				dirty = true;
				break;
			}
		}

		final ContentBrowseAdapter a = _listBrowseFragment.getBrowseAdapter();

		if (lastIndex != _checkLastIndex) {
			_checkLastIndex = lastIndex;
			for (int i = startIndex; i < lastIndex + 1; i++) {
				BrowseRowEntry entry = entries.get(i);
				if (entry.get_mediaData().get_albumArtUri() != null && !entry.get_mediaData().get_albumArtUri().isEmpty()) {
					((ContentBrowseActivity) getActivity()).loadBitmap(entry.get_mediaData().get_albumArtUri(), a::notifyDataSetChanged);
				}
			}
		}

		if (dirty) {
			if (!_currentDeviceManager.getBrowser().isProcessing() && _initialBrowseFinished) {
				int itemsToCache = 15 < _numItems - startIndex ? 15 : _numItems - startIndex;

				RowEntryVector rows = _currentDeviceManager.getBrowser().getItems(startIndex, itemsToCache);
				if (rows.size() == itemsToCache) {
					addItems(entries, rows, startIndex);
					getActivity().runOnUiThread(a::notifyDataSetChanged);
				}
			}
		}
	}

	public boolean isInitialBrowseFinished() {
		return _initialBrowseFinished;
	}

	public void setInitialBrowseFinished(boolean _initialBrowseFinished) {
		this._initialBrowseFinished = _initialBrowseFinished;
	}

    protected void showCurrentFragment(boolean empty) {
		getChildFragmentManager().beginTransaction().show(_listBrowseFragment).commit();
		_listBrowseFragment.setShowEmpty(empty);
    }

	protected void hideItemProgressBar() {
		if (_listBrowseFragment != null && _listBrowseFragment.getBrowseAdapter() != null) {
			_listBrowseFragment.getBrowseAdapter().setProgressBarVisibility(View.GONE);
		}
	}

	//----------------------------------------
	// parent -> IUtils
	//----------------------------------------

	@Override
	public void onMessage(DeviceManager senderDevice, Message message) {
		hideItemProgressBar();
		((ContentBrowseActivity) getActivity()).hideProgressDialog();
	}

	@Override
	public void onContextMenu(DeviceManager senderDevice, ContextMenu contextMenu) {}

	@Override
	public void onAlert(DeviceManager senderDevice, Alert alert) {}

	//----------------------------------------
	// IBrowser
	//----------------------------------------

	@Override
	public void onViewChanged() {
		Log.i(TAG, "onViewChanged: reloading rows...");
		_browserUpdateNr++;
		_checkLastIndex = -1;
        _items.clear();
		new ContentBrowseTask(getActivity(), onRowsLoaded, _browserUpdateNr, false).execute();
	}

	@Override
	public void onNumItemsChanged(int numItems) {
		addDirtyItems(_items, numItems);
		_numItems = numItems;
	}

	@Override
	public void onViewTypeChanged(ViewType type) {
		Log.d(TAG, "onViewTypeChanged: " + type.toString());
	}

	//----------------------------------------
	// IPlayer
	//----------------------------------------

	@Override
	public void onPlayStatusChanged(PlayStatus status) {
		Log.d(TAG, "onPlayStatusChanged");
		hideItemProgressBar();
	}

	private void showProgressDialog(final String text) {
		dismissProgressDialog();

		getActivity().runOnUiThread(() -> progressDialog = ProgressDialog.show(getActivity(), "", text, true));
	}

	private void dismissProgressDialog() {
		if (progressDialog != null && progressDialog.isShowing()) {
			getActivity().runOnUiThread(() -> progressDialog.dismiss());
		}
	}

	private void showFastScrollBar() {
		if (_listBrowseFragment != null && _listBrowseFragment.getListView() != null) {
			if (_numItems > 50) {
				_listBrowseFragment.getListView().setFastScrollEnabled(true);
				_listBrowseFragment.getListView().setFastScrollAlwaysVisible(true);
			} else {
				_listBrowseFragment.getListView().setFastScrollEnabled(false);
				_listBrowseFragment.getListView().setFastScrollAlwaysVisible(false);
			}
		}
	}

	protected void openContextMenuForItem(int itemNum) {
		showProgressDialog("Loading...");
		_currentDeviceManager.getBrowserManager().browseIntoContextMenu(itemNum);
	}

	//----------------------------------------
	// AbstractTransitionFragment
	//----------------------------------------

	@Override
	protected ContentBrowseListInterface getFragment() {
		return _listBrowseFragment;
	}

	@Override
	protected void playItem(int position) {
		_currentDeviceManager.getBrowserManager().playItem(position);
	}

	@Override
	protected void browseItem(int position, BrowseRowEntry entry) {
		// TODO: check for special cases here
		Log.i(TAG, "browseItem: " + position + " " + entry.get_name());
		_currentDeviceManager.getBrowserManager().browseItem(position);
	}

	@Override
	protected void setItem(int position, String value) {
		_currentDeviceManager.getBrowser().setItem(position, value);
	}

	@Override
	public void registerReceivers() {
		unregisterReceivers();
		if (_currentDeviceManager != null) {
			_currentDeviceManager.addManagerBrowserCallback(this);
			_currentDeviceManager.addManagerPlayerCallback(this);
			_currentDeviceManager.addManagerUtilsCallback(this);
		}
	}

	@Override
	public void unregisterReceivers() {
		if (_currentDeviceManager != null) {
			_currentDeviceManager.removeManagerBrowserCallback(this);
			_currentDeviceManager.removeManagerPlayerCallback(this);
			_currentDeviceManager.removeManagerUtilsCallback(this);
		}
	}

	private final IBrowseTask onRowsLoaded = new IBrowseTask() {
		@Override
		public void onBrowseFinished(RowEntryVector rows, int browserUpdateNr) {
			Log.i(TAG, "onRowsLoaded: " + rows.size() + " " + browserUpdateNr);
			if (getActivity() == null) {
				Log.e(TAG, "onRowsLoaded: activity is null, aborting");
				return;
			}

			// hides the menu loader
			((ContentBrowseActivity)getActivity()).toggleActionBarProgress(false);
			hideItemProgressBar();
			((ContentBrowseActivity)getActivity()).hideProgressDialog();

			if (browserUpdateNr != _browserUpdateNr) {
				Log.w(TAG, "onRowsLoaded: sequence number(?) mismatch, aborting");
				return;
			}


			if (rows.size() == 0 && ((ContentBrowseActivity)getActivity()).isSiriusLogin()) {
				Log.w(TAG, "onRowsLoaded: " + rows.size() + ", assuming SiriusXM login");
				showLoginPopup(LoginTarget.siriusxm);
				return;
			}

			if (rows.size() == 1 && ((ContentBrowseActivity)getActivity()).isNapsterLogin()) {
				Log.w(TAG, "onRowsLoaded: " + rows.size() + ", assuming Napster login");
				showLoginPopup(LoginTarget.napster);
				return;
			}

			try {
				_currDepth = Devices.instance(getActivity()).getCurrentDevice().getBrowser().getCurrentDepth();

				if (rows.size() > 0) ((ContentBrowseActivity)getActivity()).updateSearchBar();

				ContentBrowseTransitionFragment.super.addItems(_items, rows, 0);

				_initialBrowseFinished = true;

				final List<BrowseRowEntry> entries = _listBrowseFragment.getItemList();
				entries.clear();
				entries.addAll(_items);

				String source = _currentDeviceManager.getBrowserManager().browser().getCurrentTitle();
				// add a logout button for some services
				if (source.equals(Source.display_napster) ||
					source.equals(Source.display_sirius)) {
					BrowseRowEntry e = new BrowseRowEntry();
					e.set_name("Logout");
					entries.add(e);
				}

				getActivity().runOnUiThread(() -> {
					_listBrowseFragment.getBrowseAdapter().notifyDataSetChanged();
					getActivity().setProgressBarIndeterminateVisibility(false);
				});

				showCurrentFragment(_items.isEmpty());

				shutdownScheduler();
				startScheduler();

				_listBrowseFragment.getListView().setSelection(0);
				showFastScrollBar();
			} catch (Exception e) {
				Log.e(TAG, "onBrowseFinished: " + e.getMessage());
			}
		}

		@Override
		public void onBrowseTimedOut() {
			if (getActivity() != null) {
				// hides the menu loader
				((ContentBrowseActivity)getActivity()).toggleActionBarProgress(false);
				hideItemProgressBar();
				((ContentBrowseActivity)getActivity()).tearDown(true);
			}
		}

	};

	private void showLoginPopup(LoginTarget t) {
		_currentDeviceManager.getBrowser().browseParent(); // transition out of the (empty SiriusXM|Napster "account not configured") state
		new LoginDialog(getActivity(), t, (username, password) -> {
			Log.i(TAG, "login callback: " + username + " " + password);
			_currentDeviceManager.getBrowserManager().login(t, username, password);
		}).show(getFragmentManager(), LoginDialog.TAG);
	}

}
