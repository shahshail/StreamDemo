package com.streamunlimited.streamsdkdemo.callbacks;

import com.streamunlimited.remotebrowser.Alert;
import com.streamunlimited.remotebrowser.ContextMenu;
import com.streamunlimited.remotebrowser.Message;
import com.streamunlimited.streamsdkdemo.helper.DeviceManager;

/**
 *
 */
public interface IUtils {
    void onMessage(DeviceManager senderDevice, Message message);
    void onAlert(DeviceManager senderDevice, Alert alert);
//    void onClientDisconnected();
//    void onShutdown();
    void onContextMenu(DeviceManager senderDevice, ContextMenu contextMenu);
//    void onContextMenuViewChanged();
}
