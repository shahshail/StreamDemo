package com.streamunlimited.streamsdkdemo.data;

import android.content.Context;

import com.streamunlimited.streamsdkdemo.R;

import java.util.Arrays;
import java.util.List;

/**
 * Music sources are constant strings received from StreamConLib.
 */
public abstract class Source {

    public static final String display_sirius = "SiriusXM";
    public static final String display_napster = "Napster";
    public static final String display_tuneIn = "TuneIn";

    private static final String BLUETOOTH = "bluetooth";
    private static final String AIRPLAY = "airplay";
    private static final String TIDAL = "tidal";
    private static final String DEEZER = "deezer";
    private static final String SIRIUS = "sirius";
    private static final String TUNEIN = "tunein";
    private static final String SPOTIFY = "spotify";
    private static final String UPNP = "upnp";
    private static final String NAPSTER = "rhapsody";
    private static final String CHROMECAST = "googlecast";

    public static final List<String> streamshareBlacklist = Arrays.asList(CHROMECAST, AIRPLAY);

    public static final List<String> unseekableTracks = Arrays.asList(
            BLUETOOTH,
            AIRPLAY,
            NAPSTER,
            CHROMECAST);

    public static String getDisplayText(String service, String source) {
        String out = service.isEmpty() ? source : service;
        String o = out.toLowerCase();
        if (o.equals(CHROMECAST)) return "Chromecast built-in";
        if (o.equals(NAPSTER)) return display_napster;
        if (o.equals(TIDAL)) return "Tidal";
        if (o.equals(DEEZER)) return "Deezer";
        if (o.equals(SIRIUS)) return display_sirius;
        if (o.equals(TUNEIN)) return "TuneIn";
        if (o.equals(SPOTIFY)) return "Spotify";
        if (o.equals(AIRPLAY)) return "AirPlay";
        if (o.equals(UPNP)) return "My Music";
        if (o.equals(BLUETOOTH)) return "Bluetooth";
        return out;
    }

    public static String getEntryLabel(Context ctx, String name) {
        String o = name.toLowerCase();
        if (o.equals(UPNP)) return ctx.getString(R.string.my_music);
        if (o.equals("dhcp connect")) return "Connect";
        return name;
    }
}
