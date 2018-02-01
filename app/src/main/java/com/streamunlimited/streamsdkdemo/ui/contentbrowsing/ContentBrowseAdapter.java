package com.streamunlimited.streamsdkdemo.ui.contentbrowsing;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.DrawableRes;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import android.widget.ImageView.ScaleType;
import com.streamunlimited.remotebrowser.EnumValueVector;
import com.streamunlimited.remotebrowser.RowEditType;
import com.streamunlimited.streamsdkdemo.R;
import com.streamunlimited.streamsdkdemo.callbacks.IBitmapCache;
import com.streamunlimited.streamsdkdemo.data.BrowseRowEntry;
import com.streamunlimited.streamsdkdemo.data.Source;

import java.util.List;

public class ContentBrowseAdapter extends ArrayAdapter<BrowseRowEntry> {

	interface PopupMenuCallbacks {
		boolean onMenuItemClick(int position);
	}

	private static void disable(View convertView) {
		convertView.setAlpha(ALPHA_DISABLED);
		convertView.setOnClickListener(null);
	}

	private static final String SEP = " | ";
	private static final String TAG = ContentBrowseAdapter.class.getSimpleName();

	private static final int TYPE_DEFAULT = 0;
	private static final int TYPE_HIDE = TYPE_DEFAULT + 1;
	private static final int TYPE_MAX_COUNT = TYPE_HIDE + 1;
	private static final float ALPHA_DISABLED = 0.45f;

	private List<BrowseRowEntry> _items;
	private Context _context;
	private PopupMenuCallbacks _callbacks;
	private ProgressBar _currentProgress;

	ContentBrowseAdapter(Context context, List<BrowseRowEntry> items, PopupMenuCallbacks callbacks) {
		super(context, R.layout.stream_list_view, items);
		this._context = context;
		this._items = items;
		this._callbacks = callbacks;
	}

    @Override
    public int getCount() {
        return _items.size();
    }

	@Override
	public int getViewTypeCount() {
		return TYPE_MAX_COUNT;
	}

	@Override
	public int getItemViewType(int position) {
		if (position >= this.getCount()) return -1;

		final BrowseRowEntry entry = _items.get(position);
		if (_context instanceof ContentBrowseActivity) {
			if (entry.isHeader() && entry.get_name().isEmpty()) return TYPE_HIDE;

			String path = ((ContentBrowseActivity)_context).getLastKnownPath();
            if (entry.isTioHidden(path)) return TYPE_HIDE;
		}

		return TYPE_DEFAULT;
	}

    @Override
	public View getView(int position, View convertView, ViewGroup parent) {
		BrowseRowEntry entry = _items.get(position);

		int type = getItemViewType(position);
		if (type == TYPE_HIDE) return LayoutInflater.from(_context).inflate(R.layout.item_gone, parent, false);

		ViewHolder holder;
		if (convertView == null) {
			convertView = LayoutInflater.from(_context).inflate(R.layout.row_browse, parent, false);
			holder = new ViewHolder(convertView);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder)convertView.getTag();
		}

		if (entry == null) return convertView;

		if (holder == null) holder = new ViewHolder(convertView); // I see NPE's without this
		holder.configure(_context, entry);

		if (entry.get_dirty()) {
			holder.clearAndShowSpinner();
			return convertView;
		}

		holder.showSpinner(false);

		String name = entry.get_name();
		name = Source.getEntryLabel(_context, name);

		int pipeIdx = name.lastIndexOf(SEP);

		holder.setRowName(pipeIdx == -1 ? name : name.substring(0, pipeIdx));

		String artist = entry.get_mediaData().get_artist();
		holder.setArtistName(pipeIdx == -1 ? artist : name.substring(pipeIdx + SEP.length(), name.length()));

		if (holder.contextMenuButton != null) {
			if (entry.hasContextMenu() ) {
				holder.contextMenuButton.setVisibility(View.VISIBLE);
				// deactivate Browse Arrow, take up same space as Context Menu
				holder.browseIntoArrow.setVisibility(View.GONE);
				holder.contextMenuButton.setOnClickListener(view -> _callbacks.onMenuItemClick(position));
			} else {
				holder.contextMenuButton.setVisibility(View.GONE);
			}
		}

		final RowEditType editType = entry.get_editType();
		switch (editType) {
			case eEditTypeCheckBox:
				holder.checkBox.setChecked((entry.get_editData().contentEquals("1")));
				holder.checkBox.setVisibility(View.VISIBLE);
				holder.setFolderVisibility(View.GONE);
				if (entry.isChromecastTos()) disable(convertView);
				return convertView;

			case eEditTypeIPAddress:	// fallthru
			case eEditTypeNumber:		// fallthru
			case eEditTypePassword:		// fallthru
			case eEditTypeString:		// fallthru
			case eEditTypeSlider:
				if (holder.itemValue != null) {
					holder.itemValue.setVisibility(View.VISIBLE);
					holder.itemValue.setText(entry.get_editData());
				}
				holder.setFolderVisibility(View.VISIBLE);
				holder.hideCheckbox();
				break;

			case eEditTypeRadioBox:
				if (holder.itemValue != null) {
					holder.itemValue.setVisibility(View.VISIBLE);

					final EnumValueVector values = entry.get_editEnum().get_values();
					int valueCnt = (int) values.size();
					String valueString = "";
					String selectedValueString = entry.get_editEnum().get_selected();

					for (int x = 0; x < valueCnt; x++) {
						if (values.get(x).get_value().compareTo(selectedValueString) == 0) {
							valueString = values.get(x).get_title();
						}
					}
					holder.itemValue.setText(valueString);
				}
				holder.setFolderVisibility(View.VISIBLE);
				holder.hideCheckbox();
				break;

			case eEditTypeExecute:	// fallthru
			case eEditTypeNone:		// fallthru
			default:
				holder.setFolderVisibility(View.VISIBLE);
				holder.hideCheckbox();
				break;
		}

		if (entry.isBrowsable() || entry.isPlayable()) {
			holder.hideCheckbox();
			boolean isFolder = false;
			switch (entry.get_itemIcon()) {
				case eItemIconCheckbox:
					holder.setFolderButton(android.R.drawable.checkbox_on_background);
					break;
				case eItemIconUPNP:
					holder.setFolderButton(R.drawable.ic_browse_server);
					break;
				case eItemIconSpotify:
					holder.setFolderButton(R.drawable.ic_browse_spotify);
					break;
				case eItemIconUSB:
					holder.setFolderButton(R.drawable.ic_browse_usb);
					break;
				case eItemIconBluetooth:
					holder.setFolderButton(R.drawable.ic_browse_bluetooth);
					break;
				case eItemIconAirPlay:
					holder.setFolderButton(R.drawable.ic_browse_airplay);
					break;
				case eItemIconFavorites:
					holder.setFolderButton(R.drawable.ic_browse_folder);
					isFolder = true;
					break;
				case eItemIconIPod:
					holder.setFolderButton(R.drawable.ic_browse_ipod);
					break;
				case eItemIconSirius:
					holder.setFolderButton(R.drawable.ic_browse_siriusxm);
					break;
				case eItemIconPandora:
					holder.setFolderButton(R.drawable.ic_browse_pandora);
					break;
				case eItemIconRhapsody:
					holder.setFolderButton(R.drawable.ic_browse_rhapsody);
					break;
				case eItemIconTuneIn:
					holder.setFolderButton(R.drawable.ic_browse_tunein);
					break;
				case eItemIconTidal:
					holder.setFolderButton(R.drawable.ic_browse_tidal);
					break;
				case eItemIconLineIn:
					holder.setFolderButton(R.drawable.ic_browse_linein);
					break;
				case eItemIconSettings:
					holder.setFolderButton(R.drawable.ic_browse_settings);
					break;
				case eItemIconMultiroom:
					holder.setFolderButton(R.drawable.ic_browse_suegrouping);
					break;
				case eItemIconRecentlyPlayed:
					holder.setFolderButton(R.drawable.ic_browse_recentlyplayed);
					break;
				case eItemIconCDRom:
					holder.setFolderButton(R.drawable.ic_browse_cdrom);
					break;
				case eItemIconIRadio:
					holder.setFolderButton(R.drawable.ic_browse_iradio);
					break;
				case eItemIconFolder:
					holder.setFolderButton(R.drawable.ic_browse_allfiles);
					break;
				case eItemIconPlaylists:
					holder.setFolderButton(R.drawable.ic_browse_playlist);
					break;
				case eItemIconAirable:
					holder.setFolderButton(R.drawable.ic_browse_deezer);
					break;
				case eItemIconNone:
					if (entry.isChromecast()) {
						holder.setFolderButton(R.drawable.ic_chromecast);
						break;
					}					// fallthru
				case eItemIconSamba:	// fallthru
				case eItemIconVideo:	// fallthru
				case eItemIconAudio:	// fallthru
				case eItemIconCount:	// fallthru
				case eItemIconFlickr:	// fallthru
				case eItemIconHome:		// fallthru
				case eItemIconHourglass:// fallthru
				case eItemIconNapster:	// fallthru
				case eItemIconPicture:	// fallthru
				default:
					holder.setFolderTransparent();
					isFolder = !entry.isPlayable() && entry.isBrowsable();

					String albumArtUri = entry.get_mediaData().get_albumArtUri();
					if (albumArtUri != null && !albumArtUri.isEmpty()) {
						Bitmap albumArt = null;

						if (_context instanceof IBitmapCache) {
							albumArt = ((IBitmapCache)_context).getBitmapFromMemCache(albumArtUri);
						}

						if (albumArt != null) {
							holder.setAlbumArt(albumArt);
							isFolder = false;
						}

					} else if (entry.isPlayable()) {
						holder.setFolderButton(R.drawable.ic_av_play);
					} else {
						holder.centerFolder();
						holder.setFolderButton(R.drawable.ic_browse_folder);
						isFolder = true;
					}
					break;
			}
			holder.setFolderVisibility(isFolder ? View.GONE : View.VISIBLE);
		} else {
			if (name.equals(_context.getString(R.string.favorites))) {
				holder.setFolderVisibility(View.VISIBLE);
				holder.setFolderButton(R.drawable.ic_favorites);
			} else {
				holder.setFolderVisibility(View.GONE);
			}
		}

		convertView.setAlpha(entry.isDisabled() ? ALPHA_DISABLED : 1f);
		return convertView;
	}

	void changeProgressBar(ProgressBar progress) {
		if (_currentProgress != null && _currentProgress.getVisibility() != View.GONE) {
			_currentProgress.setVisibility(View.GONE);
		}
		_currentProgress = progress;
		_currentProgress.setVisibility(View.VISIBLE);
	}

	public void setProgressBarVisibility(final int visibility) {
		if (_currentProgress != null) {
			new Handler(Looper.getMainLooper()).post(() -> _currentProgress.setVisibility(visibility));
		}
	}

	private static class ViewHolder {
		TextView rowName;
		TextView artistName;
		TextView itemValue;
		ImageView folderButton;
		ProgressBar progressBar;
		CheckBox checkBox;
		ImageButton contextMenuButton;
		ImageView browseIntoArrow;
		LinearLayout background;

		ViewHolder(View v) {
			rowName = v.findViewById(R.id.name_text_view);
			artistName = v.findViewById(R.id.artist_text_view);
			itemValue = v.findViewById(R.id.item_value_text);
			progressBar = v.findViewById(R.id.browse_progress_bar);
			checkBox = v.findViewById(R.id.browse_checkbox);
			contextMenuButton = v.findViewById(R.id.context_menu_button);
			browseIntoArrow = v.findViewById(R.id.browse_arrow);
			background = v.findViewById(R.id.row_item_parent);
			folderButton = v.findViewById(R.id.folder_button);
		}

		void configure(Context _context, BrowseRowEntry entry) {
			int arrowVis = entry.isBrowsable() ? View.VISIBLE : View.GONE;
			if (browseIntoArrow != null) browseIntoArrow.setVisibility(arrowVis);

			try {
				if (entry.isHeader()) {
					rowName.setTextSize(12);
					rowName.setPadding(0, 10, 0, 10);
					background.setBackgroundColor(ContextCompat.getColor(_context, R.color.list_header_background));
					rowName.setSingleLine(false);
				} else {
					rowName.setTextSize(18);
					rowName.setPadding(0, 22, 0, 22);
					background.setBackgroundColor(ContextCompat.getColor(_context, R.color.list_background));
					browseIntoArrow.setVisibility(View.VISIBLE);
					rowName.setSingleLine(true);
				}

				folderButton.setImageResource(R.color.transparent);
				folderButton.setBackgroundResource(R.color.transparent);

				if (itemValue != null) itemValue.setVisibility(View.GONE);
			} catch (NullPointerException e) {
				Log.e(TAG, "configure: " + e.getMessage());
			}
		}

		void hideCheckbox() {
			if (checkBox != null) checkBox.setVisibility(View.GONE);
		}

		void showSpinner(boolean visible) {
			if (progressBar != null) progressBar.setVisibility(visible ? View.VISIBLE : View.GONE);
		}

		void setRowName(String value) {
			if (rowName == null) {
				Log.e(TAG, "setRowName: textview is null");
				return;
			}
			rowName.setText(value);
		}

		void setArtistName(String value) {
			if (artistName == null) {
				Log.e(TAG, "setArtistName: textview is null");
				return;
			}
			artistName.setText(value);
			artistName.setVisibility(value.isEmpty() ? View.GONE : View.VISIBLE);
		}

		void setFolderButton(@DrawableRes int value) {
			if (folderButton == null) return;
			folderButton.setImageResource(value);
		}

		void setFolderVisibility(int value) {
			if (folderButton == null) return;
			folderButton.setVisibility(value);
		}

		void setFolderTransparent() {
			if (folderButton == null) return;
			folderButton.setBackgroundResource(R.color.transparent);
			folderButton.setScaleType(ScaleType.FIT_CENTER);
		}

		void setAlbumArt(Bitmap albumArt) {
			if (folderButton == null) return;
			folderButton.setImageBitmap(albumArt);
		}

		void centerFolder() {
			if (folderButton == null) return;
			folderButton.setScaleType(ScaleType.CENTER);
		}

		void clearAndShowSpinner() {
			setRowName("");
			setArtistName("");
			showSpinner(true);
			hideCheckbox();
		}
	}

}
