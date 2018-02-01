package com.streamunlimited.streamsdkdemo.callbacks;

import com.streamunlimited.remotebrowser.ViewType;

/**
 *
 */
public interface IBrowser {
    void onViewChanged();
    void onNumItemsChanged(int numItems);
    void onViewTypeChanged(ViewType type);
}
