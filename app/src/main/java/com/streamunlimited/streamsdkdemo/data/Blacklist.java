package com.streamunlimited.streamsdkdemo.data;

import java.util.Arrays;
import java.util.List;

/**
 *
 */
abstract class Blacklist {

    static boolean contains(String name) {
        return entries.contains(name.toLowerCase());
    }

    private static final List<String> entries = Arrays.asList(
            // sources
            "airplay",
            "multiroom",
            "vtuner",
            "usb&sd",
            "file system",
            // settings
            "enable default volume reset",
            "default volume reset time [sec]",
            "other settings",
            // wireless setup
            "encryption",
            "static",
            "ip",
            "mask",
            "gateway",
            "dns",
            "static connect",
            // wireless setup -> scan|dhcp -> connect
            "type",
            "wired",
            "wireless",
            "connected",
            "cable",
            "dhcp",
            // Spotify
            "user",
            "resume playback",
            "presets",
            "log out",
            "embedded library version",
            // Deezer
            "flow"  // FIXME: TIO-4015
            );
}
