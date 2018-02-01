package com.streamunlimited.streamsdkdemo.ui.contentbrowsing;

import android.app.ListFragment;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.streamunlimited.streamsdkdemo.R;
import com.streamunlimited.streamsdkdemo.data.BrowseRowEntry;

import java.util.ArrayList;
import java.util.List;

public class ContentBrowseListFragment extends ListFragment implements ContentBrowseListInterface {

	private static final String TAG = ContentBrowseListFragment.class.getSimpleName();

	private /*final*/ AbstractTransitionFragment _parent;

	public ContentBrowseListFragment(AbstractTransitionFragment parent, List<BrowseRowEntry> items) {
		_parent = parent;
		_items.addAll(items);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		_adapter = new ContentBrowseAdapter(getActivity(), _items, popupMenuCallbacks);
		super.setListAdapter(_adapter);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.stream_list_view, container, false);
		SwipeRefreshLayout  swipeRefreshLayoutView = view.findViewById(R.id.swipeRefreshLayout);
		swipeRefreshLayoutView.setEnabled(false);
		return view;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		changeEmptyTextVisibility();
		getListView().setDivider(new ColorDrawable(ContextCompat.getColor(getActivity(), R.color.dark_grey)));
	}

	@Override
	public void onListItemClick(ListView listView, View view, int position, long id) {
		super.onListItemClick(listView, view, position, id);
		_parent.onItemClick(view, position);
	}

	private ContentBrowseAdapter _adapter;
	public ContentBrowseAdapter getBrowseAdapter() {
		return _adapter;
	}

	private final List<BrowseRowEntry> _items = new ArrayList<>();
	public List<BrowseRowEntry> getItemList() {
		return _items;
	}

	private boolean showEmpty;
	public void setShowEmpty(boolean value) {
		showEmpty = value;
		if (getView() != null) changeEmptyTextVisibility();
	}

	private void changeEmptyTextVisibility() {
		TextView emptyTextView = getView().findViewById(R.id.empty);
		emptyTextView.setVisibility(showEmpty ? View.VISIBLE : View.GONE);
	}

	private final ContentBrowseAdapter.PopupMenuCallbacks popupMenuCallbacks = position -> {
		_parent.openContextMenuForItem(position);
		return true;
	};

}
