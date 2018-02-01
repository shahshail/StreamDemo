package com.streamunlimited.streamsdkdemo.data;

/**
 * Actions are keys for intent (event) broadcasts.
 */
public abstract class Action {
    public static final String PLAY_STATUS_CHANGED = "play_status_changed";
    public static final String PLAY_TIME_CHANGED = "play_time_changed";
    public static final String VOLUME_STATUS_CHANGED = "volumeStatus_changed";
    public static final String MUTE_CHANGED = "mute_changed";
    public static final String SHUFFLE_CHANGED = "shuffle_changed";
    public static final String REPEAT_CHANGED = "repeat_changed";
    public static final String PLAY_PROGRESS_TOGGLE = "play_progress_toggle";
    public static final String CLIENT_DISCONNECTED = "client_disconnected";
    public static final String CLIENT_SHUTDOWN = "client_shutdown";
    public static final String STANDBY_STATE_CHANGED = "standbyState_changed";
    public static final String OPEN_LINK_FILTER = "openLink_filter";
    public static final String OPEN_APP_FILTER = "openApp_filter";
    public static final String MESSAGE = "message";
    public static final String ALERT = "alert";
}
