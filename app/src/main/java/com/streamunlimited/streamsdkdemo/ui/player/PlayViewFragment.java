package com.streamunlimited.streamsdkdemo.ui.player;

import android.animation.LayoutTransition;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.DrawableRes;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.streamunlimited.remotebrowser.MimeType;
import com.streamunlimited.remotebrowser.PlayState;
import com.streamunlimited.remotebrowser.RandomMode;
import com.streamunlimited.remotebrowser.RepeatMode;
import com.streamunlimited.streamsdkdemo.R;
import com.streamunlimited.streamsdkdemo.callbacks.IPlayView;
import com.streamunlimited.streamsdkdemo.data.Action;
import com.streamunlimited.streamsdkdemo.data.Source;
import com.streamunlimited.streamsdkdemo.data.TrackControls;
import com.streamunlimited.streamsdkdemo.helper.CurrentTaskExecutor;
import com.streamunlimited.streamsdkdemo.helper.DeviceManager;
import com.streamunlimited.streamsdkdemo.helper.Devices;
import com.streamunlimited.streamsdkdemo.helper.StreamControlBroadcastReceiver;
import com.streamunlimited.streamsdkdemo.helper.StreamControlRemoteBrowserManager;
import com.streamunlimited.streamsdkdemo.ui.streamshare.IUnfollowListener;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static com.streamunlimited.streamsdkdemo.data.Action.VOLUME_STATUS_CHANGED;
import static com.streamunlimited.streamsdkdemo.helper.DeviceManager.ShareState.follow;

public class PlayViewFragment extends AbstractPlayViewFragment implements IUnfollowListener {

    private static final float ALPHA_DISABLED = 0.4f;

    private static String intToTimeString(int time) {
        String out = "";
        if (time == -1) {
            out = "00:00";
        } else {
            int min = time / 60;
            int sec = time % 60;
            String minString = ((min < 10) ? "0" + Integer.toString(min) : Integer.toString(min));
            String secString = ((sec < 10) ? "0" + Integer.toString(sec) : Integer.toString(sec));
            out = minString + ":" + secString;
        }
        return out;
    }

    private static String getStringFromBundleSafe(Bundle bndl, String key) {
        boolean ok = bndl.containsKey(key) && bndl.getString(key) != null;
        return ok ? bndl.getString(key) : "";
    }

    private static final String TAG = PlayViewFragment.class.getSimpleName();
    private static final boolean enableFavorites = false;

    private final DeviceManager _currentDeviceManager = Devices.instance(getActivity()).getCurrentDevice();
    private final CurrentTaskExecutor executor = new CurrentTaskExecutor();

    private ViewHolder holder;

    private MimeType _mimeType = MimeType.eMimeType_Unknown;
    private String _metaAlbum = "";
    private String _metaAlbumartUri = "";
    private String _currentMetaAlbumartUri = "";
    private String _metaArtist = "";
    private String _metaType = "";
    private String _contextPath = "";
    private String _title = "";
    private String _codec = "";
    private String _source = "";
    private String _serviceName = "";
    private TrackControls _availableControls = new TrackControls(0);
    private int _bitrate = 0;
    private int _playTime = 0;
    private boolean _playTimeUpdateFromApp = false;
    private AtomicInteger _playTimeUpdateCounter = new AtomicInteger(0);
    private int _samplerate = 0;
    private int _bps = 0;
    private int _channels = 0;
    private int _trackTime = 0;
    private int _volume = 0;
    private int _minVolume = 0;
    private int _maxVolume = 100;
    private int _volumeStep = 1;
    private int _instance = 0;
    private int _instancePrevious = 0;
    private boolean _mute = false;
    private PlayState _playState = PlayState.ePlayStateUndefined;
    private RandomMode _random = RandomMode.eRandomDisabled;
    private RepeatMode _repeat = RepeatMode.eRepeatUndefined;
    private Drawable _currentAlbumArt;

    public PlayViewFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup out = (ViewGroup)inflater.inflate(R.layout.play_view, container, false);
        setupView(out);
        setRetainInstance(true);
        setHasOptionsMenu(true);
        return out;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Activities containing this fragment must implement its callbacks.
        if (!(activity instanceof IPlayView)) {
            throw new IllegalStateException(getResources().getString(R.string.ex_illegal_state));
        }
        _callbacks = (IPlayView) activity;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (_playStatusBundle == null) return;

        boolean hasControls = _playStatusBundle.containsKey("availableControls") && _playStatusBundle.getSerializable("availableControls") != null;
        _availableControls = hasControls ?
                (TrackControls)_playStatusBundle.getSerializable("availableControls") :
                new TrackControls(0);

        _mimeType = MimeType.swigToEnum(_playStatusBundle.getInt("mimeType"));
        _metaAlbum = getStringFromBundleSafe(_playStatusBundle, "metaAlbum");
        _metaAlbumartUri = getStringFromBundleSafe(_playStatusBundle, "metaAlbumartUri");
        _metaArtist = getStringFromBundleSafe(_playStatusBundle, "metaArtist");
        _metaType = getStringFromBundleSafe(_playStatusBundle, "metaType");
        _title = getStringFromBundleSafe(_playStatusBundle, "title");
        _codec = getStringFromBundleSafe(_playStatusBundle, "codec");
        _bitrate = _playStatusBundle.getInt("bitrate");
        if (!_playTimeUpdateFromApp) {
            _playTime = _playStatusBundle.getInt("playTime");
        }
        _samplerate = _playStatusBundle.getInt("samplerate");
        _instancePrevious = _playStatusBundle.getInt("instance");
        _bps = _playStatusBundle.getInt("bps");
        _channels = _playStatusBundle.getInt("channels");
        _trackTime = _playStatusBundle.getInt("trackTime");
        _volume = _playStatusBundle.getInt("currentVolume");
        _maxVolume = _playStatusBundle.getInt("maxVolume");
        _minVolume = _playStatusBundle.getInt("minVolume");
        _volumeStep = _playStatusBundle.getInt("volumeStep");
        _liveStream = _playStatusBundle.getBoolean("liveStream");
        _playState = PlayState.swigToEnum(_playStatusBundle.getInt("playState"));
        _random = RandomMode.swigToEnum(_playStatusBundle.getInt("random"));
        _repeat = RepeatMode.swigToEnum(_playStatusBundle.getInt("repeat"));
        _source = getStringFromBundleSafe(_playStatusBundle, "source");
        _serviceName = getStringFromBundleSafe(_playStatusBundle, "serviceName");

        new Handler(Looper.getMainLooper()).post(this::fillMetadata);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        ViewGroup v = (ViewGroup)LayoutInflater.from(getActivity()).inflate(R.layout.play_view, null);
        setupView(v);
        ViewGroup root = (ViewGroup) getView();
        root.removeAllViews();
        root.addView(v);

        if (_playStatusBundle != null) new Handler(Looper.getMainLooper()).post(this::fillMetadata);
    }

    @Override
    public void onClick(View v) {
        StreamControlRemoteBrowserManager mgr = Devices.instance(getActivity()).getCurrentDevice().getBrowserManager();
        int id = v.getId();
        switch (id) {
            case R.id.mute_button:
                holder.setMute(!_mute, getMaxVolume(), getVol());
                mgr.setMute(!_mute);
                _mute = !_mute;
                break;
            case R.id.favorite_button:
                if (enableFavorites) mgr.addToFavorites();
                break;
            case R.id.repeat_button:
                switch (_repeat) {
                    case eRepeatNone:
                        mgr.setRepeat(RepeatMode.eRepeatOne);
                        break;
                    case eRepeatOne:
                        mgr.setRepeat(RepeatMode.eRepeatAll);
                        break;
                    case eRepeatAll:    // fallthru
                    default:
                        mgr.setRepeat(RepeatMode.eRepeatNone);
                        break;
                }
                break;
            case R.id.shuffle_button:
                switch (_random) {
                    case eRandomDisabled:
                        mgr.setShuffle(RandomMode.eRandomEnabled);
                        break;
                    case eRandomEnabled:
                    default:
                        mgr.setShuffle(RandomMode.eRandomDisabled);
                        break;
                }
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
        mgr.registerReceiver(_receiveVolumeStatusChanged, new IntentFilter(Action.VOLUME_STATUS_CHANGED));
        mgr.registerReceiver(_receiveMuteChanged, new IntentFilter(Action.MUTE_CHANGED));
        mgr.registerReceiver(_receiveShuffleChanged, new IntentFilter(Action.SHUFFLE_CHANGED));
        mgr.registerReceiver(_receiveRepeatChanged, new IntentFilter(Action.REPEAT_CHANGED));
        mgr.registerReceiver(_receiveProgressToggle, new IntentFilter(Action.PLAY_PROGRESS_TOGGLE));
    }

    @Override
    public void unregisterReceivers() {
        LocalBroadcastManager mgr = LocalBroadcastManager.getInstance(getActivity());
        mgr.unregisterReceiver(_receivePlayStatus);
        mgr.unregisterReceiver(_receivePlayTimeChanged);
        mgr.unregisterReceiver(_receiveVolumeStatusChanged);
        mgr.unregisterReceiver(_receiveMuteChanged);
        mgr.unregisterReceiver(_receiveShuffleChanged);
        mgr.unregisterReceiver(_receiveRepeatChanged);
        mgr.unregisterReceiver(_receiveProgressToggle);
    }

    private void setupView(ViewGroup view) {
        view.setOnTouchListener((v, event) -> true);

        holder = new ViewHolder(view);
        holder.configure(enableFavorites, this, this, this);
        holder.configureVolumeBar(getMaxVolume(), volumeStatus_changed);
        holder.configureTimeBar(timeBar_changed, (v, motionEvent) -> !_availableControls.isSeekTimeAvailable());
        holder.setMute(_mute, getMaxVolume(), getVol());

        LayoutTransition lt = new LayoutTransition();
        lt.enableTransitionType(LayoutTransition.CHANGING | LayoutTransition.CHANGE_APPEARING | LayoutTransition.CHANGE_DISAPPEARING);
        view.setLayoutTransition(lt);
    }

    private int getVol() {
        return (_volume - _minVolume) / _volumeStep;
    }

    private int getMaxVolume() {
        return (_maxVolume - _minVolume) / _volumeStep;
    }

    private void reset() {
        refresh(new Bundle());
    }

    private void fillMetadata() {
        final boolean following = _currentDeviceManager.getShareState() == follow;
        holder.hideTransportControls(following);

        boolean seekable = !Source.unseekableTracks.contains(_source.toLowerCase());
        holder.setLiveStreamMode(_liveStream, seekable, _playTime, _trackTime, following);
        holder.setPauseMode(_availableControls.isPausable(), isPlayOrPauseState(), _playState, following);
        holder.setPreviousMode(_availableControls.isPreviousAvailable(), this);
        holder.setNextMode(_availableControls.isNextAvailable(), this);

        boolean hasMeta = !_metaAlbum.isEmpty() || !_metaArtist.isEmpty() || !_metaType.isEmpty();
        holder.setMetadataVisible(hasMeta);

        holder.showTitle(_title);
        holder.showSource(_serviceName, _source);
        holder.showArtist(_metaArtist);
        holder.showAlbum(_metaAlbum);
        holder.setElapsedTime(_playTime);

        if (hasMeta) holder.setMetadataVisible(true);

        if (isPlayOrPauseState()) holder.setProgressVisible(false);

        holder.setShuffle(_random);
        holder.setRepeat(_repeat);
        holder.setVolume(_mute, getMaxVolume(), getVol());

        boolean artChanged = !_currentMetaAlbumartUri.equals(_metaAlbumartUri) || _instance > _instancePrevious;
        boolean hasArt = !_metaAlbumartUri.isEmpty() && _currentAlbumArt != null;
        if (!artChanged && hasArt) {
            holder.setAlbumArt(_currentAlbumArt);
            return;
        }

        if (!artChanged) return;

        _instancePrevious = _instance;
        if (!_metaAlbumartUri.startsWith("http")) {
            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> holder.albumArt.setImageResource(R.drawable.ic_album_art));
            return;
        }

        new FetchArtTask(_metaAlbumartUri).execute();
    }

    private boolean isPlayOrPauseState() {
        return _playState == PlayState.ePlayStatePlay || _playState == PlayState.ePlayStatePause;
    }

    private final StreamControlBroadcastReceiver _receivePlayStatus = new StreamControlBroadcastReceiver() {
        @Override
        protected void onReceiveFinished(Intent intent) {
            if (intent.getExtras() != null ) {
                refresh(intent.getExtras());
            }
        }
    };

    private void refresh(Bundle b) {
        _availableControls = b.containsKey("availableControls") && b.getSerializable("availableControls") != null ?
            (TrackControls) b.getSerializable("availableControls") :
            new TrackControls(0);

        _metaAlbum = getStringFromBundleSafe(b, "metaAlbum");
        _metaAlbumartUri = getStringFromBundleSafe(b, "metaAlbumartUri");
        _metaArtist = getStringFromBundleSafe(b, "metaArtist");
        _metaType = getStringFromBundleSafe(b, "metaType");
        _contextPath = getStringFromBundleSafe(b, "contextPath");
        _title = getStringFromBundleSafe(b, "title");
        _codec = getStringFromBundleSafe(b, "codec");
        _bitrate = b.getInt("bitrate");
        _bps = b.getInt("bps");
        _samplerate = b.getInt("samplerate");
        _channels = b.getInt("channels");
        _trackTime = b.getInt("trackTime");
        _liveStream = b.getBoolean("liveStream");
        _mimeType = MimeType.swigToEnum(b.getInt("mimeType"));
        _playState = PlayState.swigToEnum(b.getInt("playState"));
        _random = RandomMode.swigToEnum(b.getInt("random"));
        _repeat = RepeatMode.swigToEnum(b.getInt("repeat"));
        _instance = b.getInt("instance");
        _source = getStringFromBundleSafe(b, "source");
        _serviceName = getStringFromBundleSafe(b, "serviceName");

        if (_playState == PlayState.ePlayStatePause && !_playTimeUpdateFromApp) {
            _playTime = b.getInt("playTime");
        }

        fillMetadata();
    }

    private final StreamControlBroadcastReceiver _receivePlayTimeChanged = new StreamControlBroadcastReceiver() {
        @Override
        protected void onReceiveFinished(Intent intent) {
            holder.setProgressVisible(false);
            if (intent.getExtras() != null) {
                Bundle msg = intent.getExtras();

                if (!_playTimeUpdateFromApp) _playTime = msg.getInt("playTime");
                holder.timeBar.setProgress(_playTime);

                holder.setElapsedTime(_playTime);
            }
        }
    };

    private final StreamControlBroadcastReceiver _receiveVolumeStatusChanged = new StreamControlBroadcastReceiver() {
        @Override
        protected void onReceiveFinished(Intent intent) {
            if (intent.getExtras() != null) {
                Bundle msg = intent.getExtras();
                _volume = msg.getInt("currentVolume");
                _minVolume = msg.getInt("minVolume");
                _maxVolume = msg.getInt("maxVolume");
                _volumeStep = msg.getInt("volumeStep");
                holder.setVolume(_mute, getMaxVolume(), getVol());
            }
        }
    };

    private final StreamControlBroadcastReceiver _receiveMuteChanged = new StreamControlBroadcastReceiver() {
        @Override
        protected void onReceiveFinished(Intent intent) {
            if (intent.getExtras() != null) {
                Bundle msg = intent.getExtras();
                _mute = msg.getBoolean("mute");
                holder.setMute(_mute, getMaxVolume(), getVol());
            }
        }
    };

    private final StreamControlBroadcastReceiver _receiveShuffleChanged = new StreamControlBroadcastReceiver() {
        @Override
        protected void onReceiveFinished(Intent intent) {
            if (intent.getExtras() != null) {
                Bundle msg = intent.getExtras();
                _random = RandomMode.swigToEnum(msg.getInt("shuffle"));
                holder.setShuffle(_random);
            }
        }
    };

    private final StreamControlBroadcastReceiver _receiveRepeatChanged = new StreamControlBroadcastReceiver() {
        @Override
        protected void onReceiveFinished(Intent intent) {
            if (intent.getExtras() != null) {
                Bundle msg = intent.getExtras();
                _repeat = RepeatMode.swigToEnum(msg.getInt("repeat"));
                holder.setRepeat(_repeat);
            }
        }
    };

    private final StreamControlBroadcastReceiver _receiveProgressToggle = new StreamControlBroadcastReceiver() {
        @Override
        protected void onReceiveFinished(Intent intent) {
            holder.setProgressVisible(true);
        }

    };

    private final OnSeekBarChangeListener timeBar_changed = new OnSeekBarChangeListener() {
        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            holder.setProgressVisible(true);
            _playTimeUpdateFromApp = true;
            _playTimeUpdateCounter.incrementAndGet();
            _playTime = seekBar.getProgress();
            Devices.instance(getActivity()).getCurrentDevice().getBrowserManager().setSeek2Time(_playTime);
            new Handler().postDelayed(() -> {
                // set _playTimeUpdateFromApp to false 1000 seconds after last _playTime change by user
                if (_playTimeUpdateCounter.get() <= 1)
                    _playTimeUpdateFromApp = false;
                _playTimeUpdateCounter.decrementAndGet();
            }, 1000);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        }
    };

    private final OnSeekBarChangeListener volumeStatus_changed = new OnSeekBarChangeListener() {

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            int realProgress = (progress * _volumeStep) + _minVolume;
            if (fromUser && _volume != realProgress) {
                _volume = realProgress;
                executor.setCurrTask(() -> Devices.instance(getActivity()).getCurrentDevice().getBrowser().setVolume(_volume));
//                Log.d(TAG, "PlayViewFragmentVolumeSlider onProgressChanged(): current value is " + _volume);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            //            Log.d(TAG, "onStartTrackingTouch(): current value is " + _volume);
            LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(_receiveVolumeStatusChanged);
            executor.startExecution(50);
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
//            Log.d(TAG, "PlayViewFragmentVolumeSlider onStopTrackingTouch(): current value is " + _volume);
            executor.stopExecution();
            new Thread(() -> {
                Devices.instance(getActivity()).getCurrentDevice().getBrowser().setVolume(_volume);
                LocalBroadcastManager.getInstance(getActivity()).registerReceiver(_receiveVolumeStatusChanged, new IntentFilter(VOLUME_STATUS_CHANGED));
            }).start();
        }
    };

    @Override
    public void onUngroup() {
        reset();
    }

    private class FetchArtTask extends AsyncTask<Void, Void, Drawable> {

        final String uri;

        FetchArtTask(String uri) {
            this.uri = uri;
        }

        @Override
        protected Drawable doInBackground(Void... params) {
            Drawable albumArtDrawable = null;
            try {
                InputStream is = (InputStream) new URL(uri).getContent();
                albumArtDrawable = Drawable.createFromStream(is, "albumArt");
            } catch (IOException e) {
                Log.e(TAG, "<Task>fillMetadata: " + e.getMessage());
            }
            return albumArtDrawable;
        }

        @Override
        protected void onPostExecute(Drawable result) {
            holder.setAlbumArt(result);
            _currentMetaAlbumartUri = uri;
            _currentAlbumArt = result;
        }
    }

    private static class ViewHolder {

        TextView titleText;
        TextView artistText;
        TextView albumText;
        TextView trackLengthText;
        TextView elapsedTimeText;
        TextView ofText;
        TextView sourceText;

        ViewGroup sourceGroup;
        ViewGroup metadataLayout;
        ViewGroup timeGroup;

        ImageButton playPauseButton;
        ImageButton stopButton;
        ImageButton favoriteButton;
        ImageButton muteButton;
        ImageButton repeatButton;
        ImageButton shuffleButton;
        ImageButton previousRewindButton;
        ImageButton nextForwardButton;

        ProgressBar spinner;
        SeekBar volumeBar;
        SeekBar timeBar;

        ImageView albumArt;

        final List<View> transportControls;

        ViewHolder(View v) {
            titleText = v.findViewById(R.id.title_info);
            artistText = v.findViewById(R.id.artist_info);
            albumText = v.findViewById(R.id.album_info);

            sourceGroup = v.findViewById(R.id.source_container);
            sourceText = v.findViewById(R.id.source_text);

            playPauseButton = v.findViewById(R.id.play_pause_button);
            stopButton = v.findViewById(R.id.stop_button);
            favoriteButton = v.findViewById(R.id.favorite_button);
            muteButton = v.findViewById(R.id.mute_button);
            repeatButton = v.findViewById(R.id.repeat_button);
            shuffleButton = v.findViewById(R.id.shuffle_button);
            previousRewindButton = v.findViewById(R.id.previous_rewind_button);
            nextForwardButton = v.findViewById(R.id.next_forward_button);

            volumeBar = v.findViewById(R.id.volume_bar);
            timeBar = v.findViewById(R.id.time_bar);

            spinner = v.findViewById(R.id.progress_play_view);

            metadataLayout = v.findViewById(R.id.metadata_layout);

            albumArt = v.findViewById(R.id.album_art);

            trackLengthText = v.findViewById(R.id.text_time_full);
            elapsedTimeText = v.findViewById(R.id.text_time);
            ofText = v.findViewById(R.id.text_of);
            timeGroup = v.findViewById(R.id.text_time_container);

            transportControls = Arrays.asList(shuffleButton, previousRewindButton, nextForwardButton, stopButton, playPauseButton, repeatButton, timeBar);
        }

        void configure(boolean enableFavorites, View.OnClickListener onClick, View.OnLongClickListener onLongClick, OnTouchListener onTouch) {

            if (!enableFavorites) favoriteButton.setVisibility(View.GONE);

            playPauseButton.setOnClickListener(onClick);
            stopButton.setOnClickListener(onClick);
            favoriteButton.setOnClickListener(onClick);
            muteButton.setOnClickListener(onClick);
            repeatButton.setOnClickListener(onClick);
            shuffleButton.setOnClickListener(onClick);
            previousRewindButton.setOnClickListener(onClick);
            previousRewindButton.setOnTouchListener(onTouch);
            previousRewindButton.setOnLongClickListener(onLongClick);
            nextForwardButton.setOnClickListener(onClick);
            nextForwardButton.setOnTouchListener(onTouch);
            nextForwardButton.setOnLongClickListener(onLongClick);
        }

        void hideTransportControls(boolean value) {
            for (View v : transportControls) {
                v.setVisibility(value ? INVISIBLE : VISIBLE);
            }
        }

        void configureVolumeBar(int maxVolume, OnSeekBarChangeListener onChange) {
            volumeBar.setMax(maxVolume);
            volumeBar.setOnSeekBarChangeListener(onChange);
        }

        void configureTimeBar(OnSeekBarChangeListener onChange, OnTouchListener onTouch) {
            timeBar.setOnSeekBarChangeListener(onChange);
            timeBar.setOnTouchListener(onTouch);
        }

        void showTitle(String title) {
            if (title.isEmpty()) {
                titleText.setVisibility(INVISIBLE);
                return;
            }

            titleText.setVisibility(VISIBLE);
            titleText.setText(title);
        }

        void enableStop(boolean value) {
            stopButton.setEnabled(value);
            stopButton.setAlpha(value ? 1f : ALPHA_DISABLED);
        }

        void showSource(String service, String source) {
            if (source.isEmpty()) {
                sourceGroup.setVisibility(INVISIBLE);
                enableStop(false);
                return;
            }

            enableStop(true);
            sourceText.setText(Source.getDisplayText(service, source));
            sourceGroup.setVisibility(VISIBLE);
        }

        void showArtist(String artist) {
            if (artist.isEmpty()) {
                artistText.setVisibility(View.GONE);
                return;
            }
            artistText.setText(artist);
            artistText.setVisibility(VISIBLE);
        }

        void showAlbum(String album) {
            if (album.isEmpty()) {
                albumText.setVisibility(View.GONE);
                return;
            }
            albumText.setVisibility(VISIBLE);
            albumText.setText(album);
        }

        void setVolume(boolean mute, int max, int value) {
            volumeBar.setMax(max);
            volumeBar.setProgress(mute ? 0 : value);
        }

        void setMute(boolean mute, int max, int vol) {
            muteButton.setSelected(mute);
            setVolume(mute, max, vol);
        }

        void setRepeat(RepeatMode mode) {
            switch (mode) {
                case eRepeatAll:
                    repeatButton.setImageResource(R.drawable.selector_repeat_selected);
                    break;
                case eRepeatNone:
                    repeatButton.setImageResource(R.drawable.selector_repeat);
                    break;
                case eRepeatOne:
                    repeatButton.setImageResource(R.drawable.selector_repeat_one);
                    break;
                default:
                    repeatButton.setImageResource(R.drawable.selector_repeat_one);
                    break;
            }
        }

        void setShuffle(RandomMode mode) {
            switch (mode) {
                case eRandomDisabled:
                    shuffleButton.setImageResource(R.drawable.selector_shuffle_disabled);
                    break;
                case eRandomEnabled:    // fallthru
                default:
                    shuffleButton.setImageResource(R.drawable.selector_shuffle_enabled);
                    break;
            }
        }

        void setProgressVisible(boolean value) {
            if (spinner == null) return;
            spinner.setVisibility(value ? VISIBLE : View.GONE);
        }

        void setMetadataVisible(boolean value) {
            metadataLayout.setVisibility(value ? VISIBLE : View.GONE);
        }

        void setAlbumArt(Drawable albumArtImage) {
            if (albumArt != null) albumArt.setImageDrawable(albumArtImage);
        }

        void showTimeText(boolean value) {
            timeGroup.setVisibility(value ? VISIBLE : View.GONE);
            elapsedTimeText.setVisibility(value ? VISIBLE : View.GONE);
            ofText.setVisibility(value ? VISIBLE : View.GONE);
            trackLengthText.setVisibility(value ? VISIBLE : View.GONE);
        }

        void setElapsedTime(int value) {
            elapsedTimeText.setText(intToTimeString(value));
            elapsedTimeText.setVisibility(VISIBLE);
        }

        void setLiveStreamMode(boolean liveStream, boolean seekable, int playTime, int maxTime, boolean following) {
            showTimeText(!liveStream);

            shuffleButton.setEnabled(!liveStream);
            shuffleButton.setAlpha(liveStream ? ALPHA_DISABLED : 1f);

            repeatButton.setEnabled(!liveStream);
            repeatButton.setAlpha(liveStream ? ALPHA_DISABLED : 1f);

            final boolean hideTimeBar = liveStream || following;
            timeBar.setVisibility(hideTimeBar ? INVISIBLE : VISIBLE);
            timeBar.setEnabled(seekable);
            timeBar.setMax(liveStream ? 0 : maxTime);
            timeBar.setProgress(playTime);

            if (!liveStream) setTrackLength(maxTime);
        }

        void setTrackLength(int value) {
            if (trackLengthText == null) return;
            trackLengthText.setText(intToTimeString(value));
            ofText.setVisibility(VISIBLE);
            trackLengthText.setVisibility(VISIBLE);
        }

        void setPauseMode(boolean pausable, boolean playOrPauseState, PlayState state, boolean following) {
//            Log.i(TAG, "setPauseMode: " + pausable + " " + playOrPauseState + " " + state);
            final boolean hidePlayPause = !pausable || !playOrPauseState || following;
            if (hidePlayPause) {
                playPauseButton.setVisibility(View.GONE);
                return;
            }

            playPauseButton.setVisibility(VISIBLE);
            @DrawableRes int imgRes = state == PlayState.ePlayStatePlay ?
                    R.drawable.selector_pause : R.drawable.selector_play;
            playPauseButton.setImageResource(imgRes);
        }

        void setPreviousMode(boolean value, View.OnLongClickListener onLongClick) {
            previousRewindButton.setEnabled(value);
            previousRewindButton.setAlpha(value ? 1f : ALPHA_DISABLED);
            previousRewindButton.setOnLongClickListener(onLongClick);
        }

        void setNextMode(boolean value, View.OnLongClickListener onLongClick) {
            nextForwardButton.setEnabled(value);
            nextForwardButton.setAlpha(value ? 1f : ALPHA_DISABLED);
            nextForwardButton.setOnLongClickListener(onLongClick);
        }

    }

}
