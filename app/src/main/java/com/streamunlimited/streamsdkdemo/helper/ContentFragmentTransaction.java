package com.streamunlimited.streamsdkdemo.helper;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;

import com.streamunlimited.streamsdkdemo.R;
import com.streamunlimited.streamsdkdemo.StreamControlApp;
import com.streamunlimited.streamsdkdemo.data.ClientStateEnum;
import com.streamunlimited.streamsdkdemo.ui.contentbrowsing.ContentBrowseTransitionFragment;
import com.streamunlimited.streamsdkdemo.ui.player.PlayViewFragment;
import com.streamunlimited.streamsdkdemo.ui.player.PlayViewWidgetFragment;

public class ContentFragmentTransaction {

    private FragmentTransaction _transaction;

    // CONSTRUCTOR
    public ContentFragmentTransaction(FragmentTransaction transaction) {
        this._transaction = transaction;
    }

    public FragmentTransaction removeFragment(Fragment fragment) {
        if (fragment != null) {
            _transaction.remove(fragment);
        }

        return _transaction;
    }

    public FragmentTransaction addFragments(Context ctx, ContentBrowseTransitionFragment browseFragment, PlayViewFragment playFragment, PlayViewWidgetFragment playWidgetFragment) {
        if (StreamControlApp.isTabletMode(ctx)) {
            _transaction.add(R.id.container_browse, browseFragment)
                    .addToBackStack(ClientStateEnum.eClientStateBrowse.toString())
                    .add(R.id.container_play_widget, playFragment);
        } else {
            _transaction.add(R.id.container_browse, browseFragment)
                    .add(R.id.container_browse, playFragment)
                    .add(R.id.container_play_widget, playWidgetFragment)
                    .hide(playWidgetFragment);
        }
        _transaction.hide(playFragment);

        return _transaction;
    }

    // ---

    public FragmentTransaction showFragment(Fragment fragment, boolean show) {
        if (fragment != null) {
            if (show) {
                _transaction.show(fragment);
            } else {
                _transaction.hide(fragment);
            }
        }
        return _transaction;
    }

    public int commitAllowingStateLoss() {
        return _transaction.commitAllowingStateLoss();
    }

    public int commit() {
        return _transaction.commit();
    }
}
