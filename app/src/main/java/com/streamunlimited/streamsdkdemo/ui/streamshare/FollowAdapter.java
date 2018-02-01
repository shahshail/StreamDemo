package com.streamunlimited.streamsdkdemo.ui.streamshare;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.streamunlimited.streamsdkdemo.R;
import com.streamunlimited.streamsdkdemo.discovery.Discovery;
import com.streamunlimited.streamsdkdemo.helper.CurrentTaskExecutor;
import com.streamunlimited.streamsdkdemo.helper.DeviceManager;

import java.util.ArrayList;
import java.util.List;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.streamunlimited.streamsdkdemo.helper.DeviceManager.ShareState.follow;
import static com.streamunlimited.streamsdkdemo.helper.DeviceManager.ShareState.master;
import static com.streamunlimited.streamsdkdemo.helper.DeviceManager.ShareState.solo;

/**
 *
 */
public class FollowAdapter extends RecyclerView.Adapter {

    private static final String TAG = FollowAdapter.class.getSimpleName();
    private static final float alphaDisabled = 0.45f;

    private final DeviceManager source;
    private final List<DeviceManager> items = new ArrayList<>();
    private final Context ctx;

    FollowAdapter(Context ctx, DeviceManager source, List<DeviceManager> items) {
        this.ctx = ctx;
        this.source = source;
        this.items.addAll(items);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_share_device, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ((ViewHolder)holder).bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private class ViewHolder extends RecyclerView.ViewHolder {

        TextView nameText;
        TextView statusText;
        Button addButton;
        Button removeButton;
        DeviceManager item;
        SeekBar volumeBar;
        ImageButton muteButton;

        ViewHolder(View v) {
            super(v);
            nameText = v.findViewById(R.id.name_txt);
            statusText = v.findViewById(R.id.status_txt);
            addButton = v.findViewById(R.id.addButton);
            addButton.setOnClickListener(linkButton_onClick);
            removeButton = v.findViewById(R.id.removeButton);
            removeButton.setOnClickListener(unlinkButton_onClick);
            volumeBar = v.findViewById(R.id.volume_bar);
            volumeBar.setOnSeekBarChangeListener(volumeBar_onChange);
            muteButton = v.findViewById(R.id.mute_btn);
            muteButton.setOnClickListener(muteButton_onClick);
        }

        void bind(DeviceManager item) {
            this.item = item;
            setName(item.getDeviceRowEntry().getName());
            setState(item.getShareState(), item.getDisplayStatus());
            setVolumePosition(item.getMute());
        }

        private void setVolumePosition(boolean muted) {
            muteButton.setSelected(muted);
            volumeBar.setProgress(muted ? 0 : getVolumePosition());
        }

        private int getVolumePosition() {
            return (item.getCurrentVolume() - item.getMinVolume()) / item.getVolumeStep();
        }

        private void setState(DeviceManager.ShareState value, String displayStatus) {
            setStatus(displayStatus);
            if (value == master || source.getShareState() == follow) {
                addButton.setVisibility(GONE);
                removeButton.setVisibility(GONE);
                nameText.setAlpha(alphaDisabled);
                statusText.setAlpha(alphaDisabled);
//                setEnabled(false);
                return;
            }
            nameText.setAlpha(1.0f);
            statusText.setAlpha(1.0f);
//            setEnabled(true);

            addButton.setVisibility(value == solo ? VISIBLE : GONE);
            removeButton.setVisibility(value == follow ? VISIBLE : GONE);
        }

        private void setName(String value) {
            nameText.setText(value);
        }

        private void setStatus(String value) {
            statusText.setText(value);
        }

        private final View.OnClickListener linkButton_onClick = view -> {
            Discovery.instance(ctx).group(item.getDeviceRowEntry(), source.getDeviceRowEntry());
            notifyDataSetChanged();
        };

        private final View.OnClickListener unlinkButton_onClick = view -> {
            Discovery.instance(ctx).ungroup(item.getDeviceRowEntry());
            notifyDataSetChanged();
        };

        private final View.OnClickListener muteButton_onClick = view -> {
            boolean mute = !item.getMute();
            Log.i(TAG, "muteButton_onClick: mute was " + !mute + ", now " + mute);
            setVolumePosition(mute);
            item.getBrowser().setMute(mute);
        };

        private final SeekBar.OnSeekBarChangeListener volumeBar_onChange = new SeekBar.OnSeekBarChangeListener() {

            private CurrentTaskExecutor executor = new CurrentTaskExecutor();

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) return;
                muteButton.setSelected(progress == 0);
                int level = item.getMinVolume() + (progress * item.getVolumeStep());
                if (item.getCurrentVolume() == level) return;

                executor.setCurrTask(() -> item.getBrowser().setVolume(level));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                executor.startExecution(50);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                executor.stopExecution();
                int level = item.getMinVolume() + (seekBar.getProgress() * item.getVolumeStep());
                new Thread(() -> item.getBrowser().setVolume(level)).start();
            }
        };
    }
}
