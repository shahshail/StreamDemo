package com.streamunlimited.streamsdkdemo.callbacks;

import com.streamunlimited.remotebrowser.RowEntryVector;

/**
 *
 */
public interface IBrowseTask {
    void onBrowseFinished(RowEntryVector rows, int startPos);
    void onBrowseTimedOut();
}
