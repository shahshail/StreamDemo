package com.streamunlimited.streamsdkdemo.ui;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v4.content.ContextCompat;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.text.method.SingleLineTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.streamunlimited.streamsdkdemo.R;

public class StreamAlertDialogBuilder extends Builder {

    private FrameLayout _parentView;
    private TextView _dialogTitle;
    private TextView _dialogMessage;
    private SeekBar _dialogSeekBar;
    private EditText _dialogEditText;
    private ListView _dialogListView;
    private View _titleView;

    public StreamAlertDialogBuilder(Context context) {
        super(context);

        _parentView = (FrameLayout) LayoutInflater.from(context).inflate(R.layout.alert_dialog_stream, null);
        _dialogMessage = _parentView.findViewById(R.id.alert_dialog_message);
        _dialogSeekBar = _parentView.findViewById(R.id.alert_dialog_seek_bar);
        _dialogEditText = _parentView.findViewById(R.id.alert_dialog_edit_text);
        _dialogListView = _parentView.findViewById(R.id.alert_dialog_single_choice_list);

        _titleView = LayoutInflater.from(context).inflate(R.layout.alert_dialog_title_bar, null);
        _dialogTitle = _titleView.findViewById(R.id.alert_dialog_title);
        this.setCustomTitle(_titleView);
    }

    @Override
    public android.app.AlertDialog create() {
        final android.app.AlertDialog alertDialog = super.create();

        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                int titleDividerId = getContext().getResources().getIdentifier("titleDivider", "id", "android");

                View titleDivider = alertDialog.findViewById(titleDividerId);
                if (titleDivider != null) {
                    titleDivider.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.transparent));
                }
            }
        });

        return alertDialog;
    }

    @Override
    public AlertDialog show() {
        AlertDialog dialog = super.show();

        // change divider line color
        int dividerId = dialog.getContext().getResources().getIdentifier("android:id/titleDivider", null, null);
        View divider = dialog.findViewById(dividerId);
        divider.setBackground(null);

        return dialog;
    }

    @Override
    public Builder setMessage(CharSequence message) {
        setElementVisibility(View.GONE);

        _dialogMessage.setVisibility(View.VISIBLE);
        _dialogMessage.setText(message);
        return super.setView(_parentView);
    }

    @Override
    public Builder setMessage(int messageId) {
        setElementVisibility(View.GONE);

        _dialogMessage.setVisibility(View.VISIBLE);
        _dialogMessage.setText(messageId);
        return super.setView(_parentView);
    }

    public Builder setEditText(CharSequence editText) {
        setElementVisibility(View.GONE);

        _dialogEditText.setVisibility(View.VISIBLE);
        _dialogEditText.setText(editText);
        return super.setView(_parentView);
    }

    public Builder setPasswordMode(boolean isPassword) {
        if (isPassword) {
            _dialogEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
            _dialogEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        } else {
            _dialogEditText.setTransformationMethod(SingleLineTransformationMethod.getInstance());
            _dialogEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL);
        }
        return super.setView(_parentView);
    }

    public CharSequence getEditTextValue() {
        return _dialogEditText.getText();
    }

    @Override
    public Builder setTitle(CharSequence title) {
        _dialogTitle.setText(title);
        return this.setCustomTitle(_titleView);
    }

    @Override
    public Builder setTitle(int titleId) {
        _dialogTitle.setText(titleId);
        return this.setCustomTitle(_titleView);
    }

    public Builder removeTitle() {
        return this.setCustomTitle(null);
    }

    @Override
    public Builder setView(View view) {
        _parentView.addView(view);
        return super.setView(_parentView);
    }

    private void setElementVisibility(int visibility) {
        _dialogMessage.setVisibility(visibility);
        _dialogSeekBar.setVisibility(visibility);
        _dialogEditText.setVisibility(visibility);
        _dialogListView.setVisibility(visibility);
    }
}
