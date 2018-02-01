package com.streamunlimited.streamsdkdemo.ui.player;

import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.streamunlimited.remotebrowser.PlayState;
import com.streamunlimited.streamsdkdemo.R;
import com.streamunlimited.streamsdkdemo.StreamControlApp;
import com.streamunlimited.streamsdkdemo.data.Action;
import com.streamunlimited.streamsdkdemo.data.ClientViewEnum;
import com.streamunlimited.streamsdkdemo.data.TrackControls;
import com.streamunlimited.streamsdkdemo.helper.DeviceManager;
import com.streamunlimited.streamsdkdemo.helper.Devices;
import com.streamunlimited.streamsdkdemo.helper.StreamControlBroadcastReceiver;
import com.streamunlimited.streamsdkdemo.ui.contentbrowsing.ContentBrowseActivity;
import com.streamunlimited.streamsdkdemo.ui.streamshare.IUnfollowListener;

import java.util.Arrays;
import java.util.List;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static com.streamunlimited.streamsdkdemo.helper.DeviceManager.ShareState.follow;

public class PlayViewWidgetFragment extends AbstractPlayViewFragment implements IUnfollowListener {

    private ViewHolder holder;

    private String _metaAlbumartUri;
    private String _metaArtist;
    private String _title;
    private TrackControls _availableControls;
    private int _playTime;
    private int _trackTime;
    private PlayState _playState;
    private boolean _liveStream;

    private DeviceManager _currentDeviceManager;
    private static final float ALPHA_DISABLED = 0.4f;

    public PlayViewWidgetFragment() {
        _currentDeviceManager = Devices.instance(getActivity()).getCurrentDevice();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View out = inflater.inflate(R.layout.play_view_widget, container, false);
        holder = new ViewHolder(out);
        holder.configure(this, this, this);
        return out;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (_playStatusBundle != null && savedInstanceState != null) {

            _metaAlbumartUri = _playStatusBundle.getString("metaAlbumartUrl");
            _metaArtist = _playStatusBundle.getString("metaArtist");
            _availableControls = (TrackControls) _playStatusBundle.getSerializable("availableControls");
            _title = _playStatusBundle.getString("title");
            _playTime = _playStatusBundle.getInt("playTime");
            _trackTime = _playStatusBundle.getInt("trackTime");
            _liveStream = _playStatusBundle.getBoolean("liveStream");
            _playState = PlayState.swigToEnum(_playStatusBundle.getInt("playState"));

            fillMetadata();
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.album_art:    // fallthru
            case R.id.art_meta_layout:
                ((ContentBrowseActivity) getActivity()).setClientView(ClientViewEnum.eClientViewPlayScreen);
                break;
            default:
                super.onClick(v);
                break;
        }
    }

    @Override
    public void registerReceivers() {
        unregisterReceivers();
        LocalBroadcastManager mgr = LocalBroadcastManager.getInstance(getActivity());
        mgr.registerReceiver(_receivePlayStatus, new IntentFilter(Action.PLAY_STATUS_CHANGED));
        mgr.registerReceiver(_receivePlayTimeChanged, new IntentFilter(Action.PLAY_TIME_CHANGED));
        mgr.registerReceiver(_receiveProgressToggle, new IntentFilter(Action.PLAY_PROGRESS_TOGGLE));
    }

    @Override
    public void unregisterReceivers() {
        LocalBroadcastManager mgr = LocalBroadcastManager.getInstance(getActivity());
        mgr.unregisterReceiver(_receivePlayStatus);
        mgr.unregisterReceiver(_receivePlayTimeChanged);
        mgr.unregisterReceiver(_receiveProgressToggle);
    }

    private final StreamControlBroadcastReceiver _receivePlayStatus = new StreamControlBroadcastReceiver() {
        @Override
        protected void onReceiveFinished(Intent intent) {
            if (intent.getExtras() != null) {
                Bundle msg = intent.getExtras();

                _metaAlbumartUri = msg.getString("metaAlbumartUri");
                _metaArtist = msg.getString("metaArtist");
                _availableControls = (TrackControls) msg.getSerializable("availableControls");
                _title = msg.getString("title");
                _playTime = msg.getInt("playTime");
                _trackTime = msg.getInt("trackTime");
                _liveStream = msg.getBoolean("liveStream");
                _playState = PlayState.swigToEnum(msg.getInt("playState"));

                fillMetadata();
            }
        }
    };

    private final StreamControlBroadcastReceiver _receivePlayTimeChanged = new StreamControlBroadcastReceiver() {
        @Override
        protected void onReceiveFinished(Intent intent) {
            holder.setProgressVisible(false);
            if (intent.getExtras() != null) {
                Bundle msg = intent.getExtras();

                _playTime = msg.getInt("playTime");
                holder.setElapsedTime(_playTime);
            }
        }
    };

    private final StreamControlBroadcastReceiver _receiveProgressToggle = new StreamControlBroadcastReceiver() {
        @Override
        protected void onReceiveFinished(Intent intent) {
            holder.setProgressVisible(true);
        }
    };

    private void fillMetadata() {

        holder.setPauseMode(_availableControls.isPausable(), _playState);
        holder.setPreviousEnabled(_availableControls.isPreviousAvailable());
        holder.setNextEnabled(_availableControls.isNextAvailable());
        holder.showTitle(_title);
        holder.showArtist(_metaArtist);
        holder.showTime(_liveStream, _trackTime, _playTime);

        holder.setState(_currentDeviceManager.getShareState());

        boolean isPlayPause = _playState == PlayState.ePlayStatePlay || _playState == PlayState.ePlayStatePause;
        if (isPlayPause) holder.setProgressVisible(false);

        boolean hasArt = _metaAlbumartUri != null && !_metaAlbumartUri.isEmpty();
        if (!hasArt) {
            holder.albumArt.setImageResource(R.drawable.ic_album_art);
            return;
        }

        if (getActivity() == null) return;
        boolean ok = holder.useCachedBitmap(((ContentBrowseActivity) getActivity()).getBitmapFromMemCache(_metaAlbumartUri));
        if (ok) return;

        ((ContentBrowseActivity) getActivity()).loadBitmap(_metaAlbumartUri, () -> {
            if (_metaAlbumartUri == null || getActivity() == null) return;
            holder.useCachedBitmap(((ContentBrowseActivity) getActivity()).getBitmapFromMemCache(_metaAlbumartUri));
        });
        // fallback
        holder.albumArt.setImageResource(R.drawable.ic_album_art);
    }

    @Override
    public void onUngroup() {

    }

    private static class ViewHolder {

        TextView titleText;
        TextView artistText;
        ProgressBar timeBar;
        ProgressBar spinner;
        ImageView albumArt;
        ImageButton stopButton;
        ImageButton playPauseButton;
        ImageButton prevButton;
        ImageButton nextButton;
        ViewGroup artGroup;

        final List<View> transportControls;

        ViewHolder(View v) {
            titleText = v.findViewById(R.id.title_info);
            artistText = v.findViewById(R.id.artist_info);
            timeBar = v.findViewById(R.id.time_bar);
            albumArt = v.findViewById(R.id.album_art);
            stopButton = v.findViewById(R.id.stop_button);
            playPauseButton = v.findViewById(R.id.play_pause_button);
            prevButton = v.findViewById(R.id.previous_rewind_button);
            nextButton = v.findViewById(R.id.next_forward_button);
            spinner = v.findViewById(R.id.progress_play_view);
            artGroup = v.findViewById(R.id.art_meta_layout);

            transportControls = Arrays.asList(prevButton, nextButton, stopButton, playPauseButton, timeBar);
        }

        void configure(View.OnClickListener onClick, View.OnLongClickListener onLongClick, View.OnTouchListener onTouch) {
            albumArt.setOnClickListener(onClick);
            playPauseButton.setOnClickListener(onClick);
            stopButton.setOnClickListener(onClick);
            prevButton.setOnClickListener(onClick);
            prevButton.setOnTouchListener(onTouch);
            prevButton.setOnLongClickListener(onLongClick);
            nextButton.setOnClickListener(onClick);
            nextButton.setOnTouchListener(onTouch);
            nextButton.setOnLongClickListener(onLongClick);
            artGroup.setOnClickListener(onClick);
        }

        void setState(DeviceManager.ShareState value) {
            for (View v : transportControls) {
                v.setVisibility(value == follow ? INVISIBLE : VISIBLE);
            }
        }

        void setElapsedTime(int value) {
            timeBar.setProgress(value);
        }

        void setProgressVisible(boolean value) {
            spinner.setVisibility(value ? View.VISIBLE : View.GONE);
        }

        void setPauseMode(boolean pausable, PlayState state) {
            if (!pausable) {
                playPauseButton.setVisibility(View.GONE);
                return;
            }

            playPauseButton.setVisibility(View.VISIBLE);
            @DrawableRes int imgRes = state == PlayState.ePlayStatePlay ?
                    R.drawable.selector_pause : R.drawable.selector_play;
            playPauseButton.setImageResource(imgRes);
        }

        void setPreviousEnabled(boolean value) {
            prevButton.setEnabled(value);
            prevButton.setAlpha(value ? 1f : ALPHA_DISABLED);
        }

        void setNextEnabled(boolean value) {
            nextButton.setEnabled(value);
            nextButton.setAlpha(value ? 1f : ALPHA_DISABLED);
        }

        void showTitle(String value) {
            if (value.isEmpty()) {
                titleText.setVisibility(View.GONE);
                return;
            }
            titleText.setVisibility(View.VISIBLE);
            titleText.setText(value);
        }

        void showArtist(String value) {
            if (value.isEmpty()) {
                artistText.setVisibility(View.GONE);
                return;
            }
            artistText.setVisibility(View.VISIBLE);
            artistText.setText(value);
        }

        void showTime(boolean liveStream, int trackTime, int playTime) {
            if (liveStream) {
                timeBar.setVisibility(View.INVISIBLE);
                return;
            }
            timeBar.setVisibility(View.VISIBLE);
            timeBar.setMax(trackTime);
            setElapsedTime(playTime);
        }


        private boolean useCachedBitmap(Bitmap b) {
            if (b == null) return false;

            albumArt.setImageBitmap(b);
            return true;
        }
    }

}
