package com.streamunlimited.streamsdkdemo.callbacks;

import com.streamunlimited.remotebrowser.PlayStatus;

/**
 *
 */
public interface IPlayer {
    void onPlayStatusChanged(PlayStatus status);
//    void onPlayTimeChanged(int time);
//    void onVolumeChanged(int volume);
//    void onMuteChanged(boolean mute);
//    void onRepeatChanged(RepeatMode repeat);
//    void onShuffleChanged(RandomMode shuffle);
}
