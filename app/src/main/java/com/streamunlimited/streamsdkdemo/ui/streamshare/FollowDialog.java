package com.streamunlimited.streamsdkdemo.ui.streamshare;

import android.app.DialogFragment;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import com.streamunlimited.streamsdkdemo.R;
import com.streamunlimited.streamsdkdemo.discovery.Discovery;
import com.streamunlimited.streamsdkdemo.helper.DeviceManager;

import java.util.List;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.streamunlimited.streamsdkdemo.helper.DeviceManager.ShareState.follow;

/**
 *
 */
public class FollowDialog extends DialogFragment {

    public static final String TAG = FollowDialog.class.getSimpleName();

    private final DeviceManager current;
    private final FollowAdapter adapter;
    private final List<IUnfollowListener> ungroupCallbacks;

    private ViewHolder holder;

    public FollowDialog(DeviceManager current, List<DeviceManager> items, List<IUnfollowListener> ungroupCallbacks) {
        this.current = current;
        adapter = new FollowAdapter(getActivity(), current, items);
        this.ungroupCallbacks = ungroupCallbacks;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NO_TITLE, R.style.Tio_Dialog_Fullscreen);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = View.inflate(getActivity(), R.layout.dialog_follow, container);
        holder = new ViewHolder(v);
        holder.configure(new LinearLayoutManager(getActivity()), adapter);
        return v;
    }

    private class ViewHolder {

        RecyclerView deviceList;
        Button unfollowButton;
        TextView titleText;
        TextView subtitleText;

        ViewHolder(View v) {
            deviceList = v.findViewById(R.id.device_list);
            unfollowButton = v.findViewById(R.id.unfollow_button);
            unfollowButton.setOnClickListener(unfollowButton_onClick);
            titleText = v.findViewById(R.id.title_txt);
            subtitleText = v.findViewById(R.id.subtitle_txt);
        }

        void configure(RecyclerView.LayoutManager m, RecyclerView.Adapter a) {
            titleText.setText(current.getDeviceRowEntry().getName());
            deviceList.setAdapter(a);
            deviceList.setLayoutManager(m);
            refresh();
        }

        private void refresh() {
            boolean follower = current.getShareState() == follow;
            unfollowButton.setVisibility(follower ? VISIBLE : GONE);
            subtitleText.setText(follower ? current.getDisplayStatus() : "");
        }

        private final View.OnClickListener unfollowButton_onClick = (v) -> {
            Discovery.instance(getActivity()).ungroup(current.getDeviceRowEntry());
            for (IUnfollowListener cb : ungroupCallbacks) {
                if (cb != null) cb.onUngroup();
            }
            refresh();
            adapter.notifyDataSetChanged();
        };
    }
}
