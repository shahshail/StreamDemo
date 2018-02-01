package com.streamunlimited.streamsdkdemo.data;

import com.streamunlimited.remotebrowser.AvailableControls;

import java.io.Serializable;

/**
 * Created by SJA on 09/10/2014.
 */
public class TrackControls implements Serializable {
    long _availableControls = 0;

    public TrackControls(long availableControls) {
        this._availableControls = availableControls;
    }

    // playData availableControls helper functions
    public boolean isPausable() {
        return (((int) _availableControls & AvailableControls.eControlPause.swigValue()) > 0);
    }

    public boolean isNextAvailable() {
        return (((int) _availableControls & AvailableControls.eControlNext.swigValue()) > 0);
    }

    public boolean isPreviousAvailable() {
        return (((int) _availableControls & AvailableControls.eControlPrevious.swigValue()) > 0);
    }

    public boolean isSeekTimeAvailable() {
        return (((int) _availableControls & AvailableControls.eControlSeekTime.swigValue()) > 0);
    }

    public boolean isSeekTrackAvailable() {
        return (((int) _availableControls & AvailableControls.eControlSeekTrack.swigValue()) > 0);
    }

    public boolean isLikeAvailable() {
        return (((int) _availableControls & AvailableControls.eControlLike.swigValue()) > 0);
    }

    public boolean isDislikeAvailable() {
        return (((int) _availableControls & AvailableControls.eControlDislike.swigValue()) > 0);
    }
}
