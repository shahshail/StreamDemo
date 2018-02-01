package com.streamunlimited.streamsdkdemo.ui.devicebrowsing;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.streamunlimited.streamsdkdemo.R;
import com.streamunlimited.streamsdkdemo.data.DeviceRowEntry;
import com.streamunlimited.streamsdkdemo.helper.CurrentTaskExecutor;
import com.streamunlimited.streamsdkdemo.helper.DeviceManager;
import com.streamunlimited.streamsdkdemo.helper.Devices;

import java.util.List;

class DevicesAdapter extends ArrayAdapter<DeviceManager> {

    private static final String TAG = DevicesAdapter.class.getSimpleName();

    private final List<DeviceManager> _items;
    private final IRegisterable receivers;
    private final Context _context;

    private int _volume;
    private boolean _mute;

    DevicesAdapter(Context context, int textViewResourceId, List<DeviceManager> items, IRegisterable receivers) {
        super(context, textViewResourceId, items);
        this._context = context;
        this._items = items;
        this.receivers = receivers;
    }

    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {

        if (position >= _items.size()) return null;

        final DeviceManager item = _items.get(position);
        if (item == null) return null;

        DeviceRowEntry entry = item.getDeviceRowEntry();
        if (entry == null) return null;

        ViewHolder holder;
        if (convertView == null) {
            convertView = View.inflate(_context, R.layout.row_device, null);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder)convertView.getTag();
        }

        holder.configure(item, position, entry.getName(), item.getDisplayStatus());

        return convertView;
    }

    public void refresh(Activity ctx, List<DeviceManager> items) {
        ctx.runOnUiThread(() -> {
            _items.clear();
            _items.addAll(items);
            notifyDataSetChanged();
        });
    }

    private class ViewHolder {

        TextView primaryLabel;
        TextView secondaryLabel;
        ImageButton deviceButton;
        LinearLayout volumeGroup;
        SeekBar volumeSeekBar;
        ImageButton muteButton;
        RelativeLayout shadow;

        DeviceManager device;

        ViewHolder(View v) {
            primaryLabel = v.findViewById(R.id.device_name_friendly_text_view);
            secondaryLabel = v.findViewById(R.id.device_name_text_view);
            deviceButton = v.findViewById(R.id.device_image_button);
            volumeGroup = v.findViewById(R.id.device_row_volume_layout);
            volumeSeekBar = v.findViewById(R.id.device_row_volume_bar);
            muteButton = v.findViewById(R.id.device_row_mute_button);
            shadow = v.findViewById(R.id.row_device_shadow);
        }

        void configure(DeviceManager device, int position, String title, String subtitle) {
            this.device = device;
            volumeSeekBar.setMax((device.getMaxVolume() - device.getMinVolume()) / device.getVolumeStep());
            deviceButton.setTag(Integer.toString(position));

            primaryLabel.setText(title);
            primaryLabel.setSelected(true);
            if (secondaryLabel != null) secondaryLabel.setText(subtitle);

            if (volumeGroup != null && volumeSeekBar != null && device.getCurrentVolume() >= 0) {
                int max = (device.getMaxVolume() - device.getMinVolume()) / device.getVolumeStep();
                volumeSeekBar.setMax(max);

                _mute = device.getMute();
                if (_mute) {
                    volumeSeekBar.setProgress(0);
                    _volume = device.getCurrentVolume();
                } else {
                    int vol = (device.getCurrentVolume() - device.getMinVolume()) / device.getVolumeStep();
                    volumeSeekBar.setProgress(vol);
                }

                volumeSeekBar.setOnSeekBarChangeListener(volume_onSeekBarChanged);

                muteButton.setSelected(_mute);

                muteButton.setOnClickListener(mute_onClick);

                volumeSeekBar.setEnabled(true);
                volumeGroup.setEnabled(true);
                volumeGroup.setAlpha(1);
            } else {
                volumeSeekBar.setEnabled(false);
                volumeGroup.setEnabled(false);
                volumeGroup.setAlpha((float) 0.5);
            }

            if (shadow != null) shadow.setAlpha(0);
        }

        private final SeekBar.OnSeekBarChangeListener volume_onSeekBarChanged = new SeekBar.OnSeekBarChangeListener() {

            private CurrentTaskExecutor executor = new CurrentTaskExecutor();

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int realProgress = (progress * device.getVolumeStep()) + device.getMinVolume();
                if (fromUser && _volume != realProgress) {
                    _volume = realProgress;
                    executor.setCurrTask(() -> device.getBrowser().setVolume(_volume));
//                    Log.d(TAG, "onProgressChanged(): current value is " + _volume);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
//                Log.d(TAG, "onStartTrackingTouch(): current value is " + _volume);
                receivers.unregisterReceivers();
                executor.startExecution(50);

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
//                Log.d(TAG, "onStopTrackingTouch(): current value is " + _volume);
                executor.stopExecution();
                new Thread(() -> {
                    device.getBrowser().setVolume(_volume);
                    receivers.registerReceivers();
                }).start();
            }
        };

        private final View.OnClickListener mute_onClick = view -> {
            _mute = !_mute;
            muteButton.setSelected(_mute);
            if (_mute) {
                volumeSeekBar.setProgress(0);
            } else {
                int vol = (_volume - device.getMinVolume()) / device.getVolumeStep();
                volumeSeekBar.setProgress(vol);
            }
            device.getBrowser().setMute(_mute);
        };
    }

}
