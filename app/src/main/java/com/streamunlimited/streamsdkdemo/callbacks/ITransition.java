package com.streamunlimited.streamsdkdemo.callbacks;

import com.streamunlimited.streamsdkdemo.data.BrowseRowEntry;

/** All activities containing this fragment must implement. */
public interface ITransition {
    /** Callback for when a browse item has been selected. */
    void onBrowseItemSelected(BrowseRowEntry entry);
    /** Callback to show dialog when browsing takes longer then 1 second */
    void onShowProgressDialog();
}
