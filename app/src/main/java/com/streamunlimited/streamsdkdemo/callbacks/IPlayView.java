package com.streamunlimited.streamsdkdemo.callbacks;

import android.os.Bundle;

/**
 * A callback interface that all activities containing this fragment must
 * implement. This mechanism allows activities to be notified of item
 * selections.
 */
public interface IPlayView {
    /**
     * Callback for when an item has been selected.
     */
    Bundle onPlayViewCreated();
    void browsePlayqueue();
}
