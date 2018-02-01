package com.streamunlimited.streamsdkdemo.data;

import com.streamunlimited.remotebrowser.RowEntry;
import com.streamunlimited.remotebrowser.RowEntryAttributes;

/** An item representing a piece of content. */
public class BrowseRowEntry extends RowEntry {

    private boolean _isInFavorites = false;
    private boolean _dirty;

    public BrowseRowEntry() {}

    public BrowseRowEntry(RowEntry other) {
        super.assign(other);
    }

    public boolean get_dirty() {
        return _dirty;
    }

    public void set_dirty(boolean dirty) {
        _dirty = dirty;
    }

    @Override
    public String toString() {
        return get_name();
    }

    public boolean isUnknown() {
        return (((int) this.get_attrMask() & RowEntryAttributes.eAttrUnknown.swigValue()) > 0);
    }

    public boolean isInvokable() {
        return (((int) this.get_attrMask() & RowEntryAttributes.eAttrInvokable.swigValue()) > 0);
    }

    public boolean isEditable() {
        return (((int) this.get_attrMask() & RowEntryAttributes.eAttrEditable.swigValue()) > 0);
    }

    public boolean isPlayable() {
        return (((int) this.get_attrMask() & RowEntryAttributes.eAttrPlayable.swigValue()) > 0);
    }

    public boolean isBrowsable() {
        return (((int) this.get_attrMask() & RowEntryAttributes.eAttrBrowsable.swigValue()) > 0);
    }

    public boolean isHeader() {
        return (((int) this.get_attrMask() & RowEntryAttributes.eAttrHeader.swigValue()) > 0);
    }

    public boolean isQuery() {
        return (((int) this.get_attrMask() & RowEntryAttributes.eAttrQuery.swigValue()) > 0);
    }

    public boolean isDisabled() {
        return (((int) this.get_attrMask() & RowEntryAttributes.eAttrDisabled.swigValue()) > 0);
    }

    public boolean hasContextMenu() {
        return (((int) this.get_attrMask() & RowEntryAttributes.eAttrCtxMenu.swigValue()) > 0);
    }

    public boolean isChromecast() {
        return get_name().toLowerCase().contains("chromecast");
    }

    public void storedInFavorites(boolean isStored) {
        _isInFavorites = isStored;
    }

    public boolean isInFavorites() {
        return _isInFavorites;
    }

    public boolean isTioHidden(String path) {
        String name = get_name();
        switch (name) {
            case "Favorites": // fallthru
            case "Podcasts":
                return !path.equals(Source.display_tuneIn);
            case "Radio":
                return !path.equals(Source.display_napster);
            default:
                return Blacklist.contains(name);
        }
    }

    public boolean isChromecastTos() {
        return get_name().toLowerCase().equals("tos accepted");
    }
}
