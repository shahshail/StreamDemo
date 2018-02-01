package com.streamunlimited.streamsdkdemo.ui.player;

import android.app.Activity;
import android.app.Fragment;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;

import com.streamunlimited.streamsdkdemo.R;
import com.streamunlimited.streamsdkdemo.callbacks.IPlayView;
import com.streamunlimited.streamsdkdemo.helper.Devices;
import com.streamunlimited.streamsdkdemo.helper.StreamControlRemoteBrowserManager;

public abstract class AbstractPlayViewFragment extends Fragment implements OnClickListener, OnTouchListener, OnLongClickListener {

    /**
     * A dummy implementation of the {@link IPlayView} interface that does
     * nothing. Used only when this fragment is not attached to an activity.
     */
    private static IPlayView sDummyCallbacks = new IPlayView() {
        @Override
        public Bundle onPlayViewCreated() {
            return null;
        }

        @Override
        public void browsePlayqueue() {

        }
    };

    protected Bundle _playStatusBundle;

    protected boolean _liveStream = false;

    protected boolean _isForwardPress = false;
    protected boolean _isRewindPress = false;

    /**
     * The fragment's current callback object, which is notified of list item
     * clicks.
     */
    protected IPlayView _callbacks = sDummyCallbacks;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        registerReceivers();
        _playStatusBundle = _callbacks.onPlayViewCreated();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        _playStatusBundle = _callbacks.onPlayViewCreated();
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
    public void onResume() {
        registerReceivers();
        super.onResume();
    }

    @Override
    public void onPause() {
        unregisterReceivers();
        super.onPause();
    }

    protected abstract void registerReceivers();

    protected abstract void unregisterReceivers();

    @Override
    public void onClick(View v) {
        getActivity().findViewById(R.id.progress_play_view).setVisibility(View.VISIBLE);
        StreamControlRemoteBrowserManager mgr = Devices.instance(getActivity()).getCurrentDevice().getBrowserManager();

        switch (v.getId()) {
            case R.id.play_pause_button:
                mgr.pause();
                break;
            case R.id.stop_button:
                mgr.stop();
                break;
            case R.id.previous_rewind_button:
                mgr.previous();
                break;
            case R.id.next_forward_button:
                mgr.next();
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (v.getId() == R.id.next_forward_button) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_UP:
                    if (_isForwardPress) {
                        Devices.instance(getActivity()).getCurrentDevice().getBrowserManager().stopScan();
                        _isForwardPress = false;
                        v.setPressed(false);
                        return true;
                    } else {
                        return false;
                    }
                default:
                    return false;
            }
        } else if (v.getId() == R.id.previous_rewind_button) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_UP:
                    if (_isRewindPress) {
                        Devices.instance(getActivity()).getCurrentDevice().getBrowserManager().stopScan();
                        _isRewindPress = false;
                        v.setPressed(false);
                        return true;
                    } else {
                        return false;
                    }
                default:
                    return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean onLongClick(View v) {
        int id = v.getId();
        if (id == R.id.next_forward_button) {
            Devices.instance(getActivity()).getCurrentDevice().onForwardRewindActivated();
            if (!_liveStream) {
                Devices.instance(getActivity()).getCurrentDevice().getBrowserManager().fastForward();
                _isForwardPress = true;
            }
            return true;
        } else if (id == R.id.previous_rewind_button) {
            Devices.instance(getActivity()).getCurrentDevice().onForwardRewindActivated();
            if (!_liveStream) {
                Devices.instance(getActivity()).getCurrentDevice().getBrowserManager().fastRewind();
                _isRewindPress = true;
            }
            return true;
        } else {
            return false;
        }
    }
}
