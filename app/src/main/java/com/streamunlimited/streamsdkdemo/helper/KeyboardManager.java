package com.streamunlimited.streamsdkdemo.helper;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

/**
 * Created by Akos Hamori on 6/9/16.
 */
public class KeyboardManager {
    public static void hideSoftKeyboard(Activity activity) {
        InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        View view = activity.getCurrentFocus();
        if (view == null) {
            if (inputMethodManager.isAcceptingText())
                inputMethodManager.toggleSoftInput(InputMethodManager.HIDE_NOT_ALWAYS, 0);
        } else {
            if (view instanceof EditText)
                ((EditText) view).setText(((EditText) view).getText().toString()); // reset edit text bug on some keyboards bug
            inputMethodManager.hideSoftInputFromInputMethod(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
