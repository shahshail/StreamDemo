package com.streamunlimited.streamsdkdemo.callbacks;

import com.streamunlimited.observer.RemoteBrowserManager;

/**
 *
 */
public interface ISueObserver extends RemoteBrowserManager.Observer {
    void onTransition();
    void onRemoteViewChanged();
    void onContextMenuViewChanged();
    @Override void onNumItemsChanged(int numItems);
//    void onTimeout();
//    void onError();
}
