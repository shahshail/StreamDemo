package com.streamunlimited.streamsdkdemo.ui.contentbrowsing;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.support.annotation.StringRes;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.streamunlimited.remotebrowser.EditEnum;
import com.streamunlimited.remotebrowser.EnumValue;
import com.streamunlimited.remotebrowser.EnumValueVector;
import com.streamunlimited.remotebrowser.RowEditType;
import com.streamunlimited.remotebrowser.RowEntryVector;
import com.streamunlimited.streamsdkdemo.R;
import com.streamunlimited.streamsdkdemo.callbacks.ITransition;
import com.streamunlimited.streamsdkdemo.callbacks.IUtils;
import com.streamunlimited.streamsdkdemo.data.BrowseRowEntry;
import com.streamunlimited.streamsdkdemo.helper.DeviceManager;
import com.streamunlimited.streamsdkdemo.helper.Devices;
import com.streamunlimited.streamsdkdemo.ui.StreamAlertDialogBuilder;
import com.streamunlimited.streamsdkdemo.ui.login.LoginTarget;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public abstract class AbstractTransitionFragment extends Fragment implements IUtils {

    /**
     * A dummy implementation of the {@link ITransition} interface that does
     * nothing. Used only when this fragment is not attached to an activity.
     */
    private static ITransition sDummyCallbacks = new ITransition() {
        @Override
        public void onBrowseItemSelected(BrowseRowEntry entry) {
        }
        @Override
        public void onShowProgressDialog() {
        }
    };

    private static final String TAG = AbstractTransitionFragment.class.getSimpleName();
    // Under "Wireless Setup -> Manual" menu, we validate the network SSID and password are non-empty...
    private static final int wirelessSetup_manual_ssidIndex = 0;
    private static final int wirelessSetup_manual_keyIndex = 1;
    // ...and hard-code an override to the "Encryption" option's default value...
    private static final int wirelessSetup_manual_encryptionIndex = 2;
    // ...from "none" to WPA-PSK
    private static final String wpa_psk_json = "{\"networkProfileWirelessEncryption\":\"wpa_psk\",\"type\":\"networkProfileWirelessEncryption\"}";
    // Under "Wireless Setup -> Scan -> (pick network)" menu, we validate the password is non-empty.
    private static final int wirelessSetup_scan_keyIndex = 0;


    protected ITransition _callbacks = sDummyCallbacks; //< The fragment's current callback object, which is notified of list item clicks.

    private ScheduledExecutorService _scheduler;
    private ScheduledFuture<?> _scheduledFuture;

    protected DeviceManager _currentDeviceManager = Devices.instance(getActivity()).getCurrentDevice();

    protected boolean _itemClicked = false;
    protected boolean _paused = false;
    protected boolean _initialBrowseFinished = false;

    protected int _browserUpdateNr = 0;
    protected int _numItems;
    protected int _currDepth = 0;
    protected List<BrowseRowEntry> _items;

    abstract public void registerReceivers();

    abstract public void unregisterReceivers();

    abstract protected void runRefreshList();

    abstract protected void openContextMenuForItem(int itemNum);

    abstract protected ContentBrowseListInterface getFragment();

    abstract protected void hideItemProgressBar();

    abstract protected void playItem(int postion);

    abstract protected void setItem(int postion, String value);

    abstract protected void browseItem(int postion, BrowseRowEntry entry);

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Activities containing this fragment must implement its callbacks.
        if (!(activity instanceof ITransition)) {
            throw new IllegalStateException(getResources().getString(R.string.ex_illegal_state));
        }

        _callbacks = (ITransition) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        unregisterReceivers();
        try {
            Field childFragmentManager = Fragment.class.getDeclaredField("mChildFragmentManager");
            childFragmentManager.setAccessible(true);
            childFragmentManager.set(this, null);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        // Reset the active callbacks interface to the dummy implementation.
        _callbacks = sDummyCallbacks;
    }


    protected void startScheduler() {
        if (_scheduler == null) {
            _scheduler = Executors.newScheduledThreadPool(1);
        }
        if (_scheduledFuture == null) {
            _scheduledFuture = _scheduler.scheduleAtFixedRate(this::runRefreshList, 1000, 1000, TimeUnit.MILLISECONDS);
        }
    }

    protected void shutdownScheduler() {
        if (_scheduledFuture != null) {
            _scheduledFuture.cancel(true);
            _scheduledFuture = null;
        }
        if (_scheduler != null) {
            _scheduler.shutdownNow();
            _scheduler = null;
        }
    }

    protected void addItems(List<BrowseRowEntry> items, RowEntryVector rows, int startIndex) {
        if (rows != null) {
            Log.i(TAG, "addItems: " + rows.size() + " rows");

            int endPos = (int) (startIndex + rows.size());
            if (endPos > items.size()) {
                Log.e(TAG, "Size missmatch - expected at least: " + endPos + " found only: " + items.size());
                return;
            }
            for (int i = startIndex; i < endPos; i++) {
                BrowseRowEntry e = new BrowseRowEntry(rows.get(i - startIndex));
                items.set(i, e);
                items.get(i).set_dirty(false);
            }
        }
    }

    protected void addDirtyItems(List<BrowseRowEntry> items, int numItems) {
        for (int i = items.size(); i < numItems; i++) {
            BrowseRowEntry dummy = new BrowseRowEntry();
            dummy.set_dirty(true);
            items.add(dummy);
        }
    }

    public void onItemClick(View view, int position) {

        if (_itemClicked) return;

        if (_currentDeviceManager == null) {
            Log.e(TAG, "onItemClick: currentDeviceManager is null!");
            return;
        }

        if (!_currentDeviceManager.getBrowserManager().isBrowserSet()) {
            Log.e(TAG, "onItemClick: browser is null!");
            return;
        }

        if (_currentDeviceManager.getBrowser().isProcessing()) {
            Log.e(TAG, "onItemClick: browser is still in processing!");
            return;
        }

        if (_currentDeviceManager.getShareState() == DeviceManager.ShareState.follow) {
            showBrowseUnavailablePopup();
            return;
        }

        final List<BrowseRowEntry> items = getFragment().getItemList();
        if (position > items.size()) {
            return;
        }

        final BrowseRowEntry entry = items.get(position);
        final String name = entry.get_name();
        if ((name.equals("empty") && entry.get_dirty())) return;
        if (entry.isDisabled() || entry.isHeader()) return;

        final Activity a = getActivity();
        if (a instanceof ContentBrowseActivity) {
            if (name.equals("SiriusXM")) {
                Log.i(TAG, "onItemClick: setting sirius login");
                ((ContentBrowseActivity)a).setSiriusLogin();
            } else if (name.equals("Napster")) {
                Log.i(TAG, "onItemClick: setting napster login");
                ((ContentBrowseActivity)a).setNapsterLogin();
            } else {
                ((ContentBrowseActivity)a).notLoggingIn();
            }

            String p = ((ContentBrowseActivity)a).getLastKnownPath();
            if (p.contains("Manual") && name.equals("DHCP Connect")) {  // Wireless Setup -> Manual
                String ssid = items.get(wirelessSetup_manual_ssidIndex).get_editData();
                String password = items.get(wirelessSetup_manual_keyIndex).get_editData();
//                Log.i(TAG, "onItemClick: ssid=" + ssid + ", password=" + (password.isEmpty() ? "" : "*...*"));
                if (!validate(ssid, R.string.err_validate_ssid) ||
                    !validate(password, R.string.err_validate_wifi_password)) {
                    return;
                }
                setItem(wirelessSetup_manual_encryptionIndex, wpa_psk_json);
            }

            if (name.equals("Connect") && items.size() == 2) {  // Wireless Setup -> Scan
                String password = items.get(wirelessSetup_scan_keyIndex).get_editData();
                if (!validate(password, R.string.err_validate_wifi_password)) return;
            }
        }

        _itemClicked = true;

        ProgressBar progressBar = view.findViewById(R.id.browse_progress_bar);
        getFragment().getBrowseAdapter().changeProgressBar(progressBar);
        new java.util.Timer().schedule(
            new java.util.TimerTask() {
                @Override
                public void run() {
                    _callbacks.onShowProgressDialog();
                }
            },
            1000
        );

        if (name.equals("Logout")) {
            String source = _currentDeviceManager.getBrowser().getCurrentTitle();
            Log.i(TAG, "onItemClick: logout " + source);
            LoginTarget t = LoginTarget.byServiceName(source);
            if (t != LoginTarget.nil) {
                _currentDeviceManager.getBrowserManager().logout(t);
                Toast.makeText(a, getString(R.string.logged_out, source), Toast.LENGTH_SHORT).show();
                _itemClicked = false;
                return;
            }
        }

        if (entry.isEditable()) {
            // check special cases
            switch (entry.get_editType()) {
                case eEditTypeRadioBox:

                    final EditEnum opts = entry.get_editEnum();
                    final EnumValueVector vec = opts.get_values();
                    int selectedValue = 0;
                    int valueCnt = (int) vec.size();

                    String[] valueStrings = new String[valueCnt];
                    String selectedValueString = opts.get_selected();

                    for (int x = 0; x < valueCnt; x++) {
                        final EnumValue v = vec.get(x);
                        valueStrings[x] = v.get_title();

                        if (v.get_value().compareTo(selectedValueString) == 0) {
                            selectedValue = x;
                        }
//                        Log.i(TAG, "onItemClick: radio item title=" + v.get_title() +
//                                ", value=" + v.get_value() +
//                                ", path=" + v.get_path() +
//                                ", json=" + v.get_valueJson());
                    }

                    StreamAlertDialogBuilder radioBoxAlertBuilder = new StreamAlertDialogBuilder(a);
                    String dialogTitle = (name.lastIndexOf(":") > 0) ? name.substring(0, name.lastIndexOf(":")) : name;
                    DialogInterface.OnClickListener listener = (dialog, which) -> {
                        setItem(position, vec.get(which).get_valueJson());
                        dialog.dismiss();
                    };
                    ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(
                            a, R.layout.custom_single_checked_item, valueStrings);
                    radioBoxAlertBuilder.setTitle(dialogTitle).setSingleChoiceItems(adapter, selectedValue, listener);

                    // add dismiss listener after create to support Android Version < 17
                    AlertDialog radioBoxAlertDialog = radioBoxAlertBuilder.create();
                    radioBoxAlertDialog.getListView().setVerticalScrollBarEnabled(false);
                    radioBoxAlertDialog.setOnDismissListener(dialog -> hideItemProgressBar());
                    radioBoxAlertDialog.show();

                    _itemClicked = false;
                    return;
                case eEditTypeCheckBox:
                    CheckBox checkBox = view.findViewById(R.id.browse_checkbox);
                    checkBox.setChecked(!checkBox.isChecked());

                    setItem(position, String.valueOf(checkBox.isChecked()));

                    _itemClicked = false;
                    hideItemProgressBar();
                    return;
                case eEditTypePassword:     // fallthru
                case eEditTypeString:       // fallthru
                case eEditTypeSlider:       // fallthru
                case eEditTypeIPAddress:    // fallthru
                case eEditTypeNumber:
                    Log.e(TAG, "edit type: " + entry.get_editType().toString());
                    Log.e(TAG, "editData: " + entry.get_editData());

                    final StreamAlertDialogBuilder alertDialogBuilder = new StreamAlertDialogBuilder(a);
                    alertDialogBuilder.setTitle(name.replace(": " + entry.get_editData(), ""));
                    alertDialogBuilder.setNegativeButton(android.R.string.no, (dialog, which) -> {
                        hideItemProgressBar();
                        dialog.cancel();
                    });

                    if (entry.get_editType() == RowEditType.eEditTypeSlider) {
                        final double min = entry.get_editSlider().get_min();
                        final double max = entry.get_editSlider().get_max();
                        final double step = entry.get_editSlider().get_step();
                        final double currVal = Double.parseDouble(entry.get_editData());

                        // calculate factor to get rid of decimal places (Android slider
                        // is limited to step = 1)
                        final double stepFactor = 1 / step;

                        final View viewLayout = a.getLayoutInflater().inflate(R.layout.layout_alert_seekbar, a.findViewById(R.id.layout_alert_seekbar_root));

                        final TextView seekvalue = viewLayout.findViewById(R.id.alert_seekbar_seekvalue);

                        // Android seekbar has always a fixed min of 0, so shift values
                        // by min -> - min
                        // get rid of decimal places -> * stepFactor

                        final SeekBar slider = viewLayout.findViewById(R.id.alert_seekbar_slider);
                        slider.setMax((int) ((max - min) * stepFactor));
                        slider.setProgress((int) ((currVal - min) * stepFactor));

                        seekvalue.setText(String.valueOf(currVal));

                        final SeekBar.OnSeekBarChangeListener onSeekBarChange = new SeekBar.OnSeekBarChangeListener() {
                            @Override
                            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                                seekvalue.setText(String.valueOf(progress / stepFactor + min));
                            }

                            @Override
                            public void onStartTrackingTouch(SeekBar arg0) {}

                            @Override
                            public void onStopTrackingTouch(SeekBar seekBar) {}
                        };
                        slider.setOnSeekBarChangeListener(onSeekBarChange);

                        alertDialogBuilder.setView(viewLayout);
                        alertDialogBuilder.setPositiveButton(android.R.string.yes, (dialog, id1) -> {
                            setItem(position, seekvalue.getText().toString());
                            hideItemProgressBar();
                            dialog.dismiss();
                        });
                        alertDialogBuilder.setOnDismissListener(dialog -> hideItemProgressBar());
                    } else {
                        alertDialogBuilder.setEditText(entry.get_editData());
                        alertDialogBuilder.setPositiveButton(android.R.string.yes, (dialog, which) -> {
                            setItem(position, alertDialogBuilder.getEditTextValue().toString());
                            hideItemProgressBar();
                            dialog.dismiss();
                        });
                        alertDialogBuilder.setOnDismissListener(dialog -> hideItemProgressBar());
                    }

                    alertDialogBuilder.create().show();
                    _itemClicked = false;
                    return;
                default:
                    break;
            }
        }

        // check general cases
        if (entry.isPlayable()) {
            _currentDeviceManager.setNewPlaybackSelected(true);
            playItem(position);
        } else if (entry.isBrowsable() || entry.isInvokable()) {
            _callbacks.onBrowseItemSelected(entry);
            if (entry.isQuery()) {
                final StreamAlertDialogBuilder alertDialogBuilder = new StreamAlertDialogBuilder(a);
                alertDialogBuilder.setTitle(name);
                alertDialogBuilder.setNegativeButton(android.R.string.no, (dialog, which) -> {
                    hideItemProgressBar();
                    dialog.cancel();
                });
                alertDialogBuilder.setEditText("");
                alertDialogBuilder.setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    // save value and hide the progessbar
                    _currentDeviceManager.getBrowserManager().invokeQuery(position, alertDialogBuilder.getEditTextValue().toString());
                    hideItemProgressBar();
                    dialog.dismiss();
                });

                AlertDialog alertDialog = alertDialogBuilder.create();

                alertDialog.setOnDismissListener(dialog -> hideItemProgressBar());
                alertDialog.show();

            } else {
                browseItem(position, entry);
            }
            _initialBrowseFinished = false;
        } else { // must be a title
            hideItemProgressBar();
        }

        _itemClicked = false;
    }

    private boolean validate(String value, @StringRes int errMsgId) {
        boolean out = !value.isEmpty();
        if (!out) Toast.makeText(getActivity(), errMsgId, Toast.LENGTH_SHORT).show();
        return out;
    }

    private void showBrowseUnavailablePopup() {
        AlertDialog.Builder bld = new AlertDialog.Builder(getActivity(), R.style.Tio_Material_Alert);
        bld.setMessage(R.string.browsing_unavailable);
        bld.setPositiveButton(R.string.ok, null);
        bld.show();
    }
}
