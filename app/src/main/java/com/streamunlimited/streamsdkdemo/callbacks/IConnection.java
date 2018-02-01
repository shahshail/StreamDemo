package com.streamunlimited.streamsdkdemo.callbacks;

/**
 *
 */
public interface IConnection {
    void onConnectSuccess();
    void onConnectFailed();
    void onDisconnectSuccess();
    void onDisconnectFailed();

    /**
     *
     */
    interface IAvailable extends IConnection {
        void onConnectionAvailable();
        void onConnectionNotAvailable();
    }
}
