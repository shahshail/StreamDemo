package com.streamunlimited.streamsdkdemo.ui.login;

import android.util.Log;

import com.streamunlimited.streamsdkdemo.data.Source;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 */
public enum LoginTarget {
    nil,
    siriusxm,
    napster;

    public static final String TAG = LoginTarget.class.getSimpleName();

    public static LoginTarget byServiceName(String name) {
        switch (name) {
            case Source.display_napster: return napster;
            case Source.display_sirius: return siriusxm;
            default:
                return nil;
        }
    }

    public String toDisplayString() {
        switch (this) {
            case siriusxm: return "SiriusXM";
            case napster: return "Napster";
            default: return "";
        }
    }

    public List<String> toAccountsPath() {
        List<String> root = new ArrayList<>(Arrays.asList("Settings", "Other Settings", "Accounts"));
        switch (this) {
            case siriusxm:
                root.add("Sirius");
                return root;
            case napster:
                root.add("Napster");
                return root;
            default:
                Log.w(TAG, "toAccountsPath: unhandled case " + this);
                return new ArrayList<>();
        }
    }

    public int toRootIndex() {
        switch (this) {
            case siriusxm:
                return 12;
            case napster:
                return 13;
            default:
                Log.w(TAG, "toRootIndex: unhandled case " + this);
                return -1;
        }
    }

    public int getUsernameIndex() {
        switch (this) {
            case siriusxm: // fallthru
            case napster:
                return 0;
            default:
                Log.w(TAG, "getUsernameIndex: unhandled case " + this);
                return -1;
        }
    }


    public int getPasswordIndex() {
        switch (this) {
            case siriusxm: // fallthru
            case napster:
                return 1;
            default:
                Log.w(TAG, "getPasswordIndex: unhandled case " + this);
                return -1;
        }
    }


    public int getLoginIndex() {
        switch (this) {
            case siriusxm: // fallthru
            case napster:
                return 3;
            default:
                Log.w(TAG, "getLoginIndex: unhandled case " + this);
                return 2;
        }
    }

    public int getLogoutIndex() {
        switch (this) {
            case siriusxm: // fallthru
            case napster:
                return 2;
            default:
                Log.w(TAG, "getLogoutIndex: unhandled case " + this);
                return 0;
        }
    }
}

