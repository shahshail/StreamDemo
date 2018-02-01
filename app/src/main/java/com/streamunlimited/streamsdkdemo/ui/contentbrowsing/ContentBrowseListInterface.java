package com.streamunlimited.streamsdkdemo.ui.contentbrowsing;

import android.widget.AbsListView;

import com.streamunlimited.streamsdkdemo.data.BrowseRowEntry;

import java.util.List;

/**
 * Created by sebastian on 9/2/14.
 */
public interface ContentBrowseListInterface {
    ContentBrowseAdapter getBrowseAdapter();
    List<BrowseRowEntry> getItemList();
    AbsListView getListView();
}
