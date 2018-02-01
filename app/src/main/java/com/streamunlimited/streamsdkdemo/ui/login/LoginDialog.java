package com.streamunlimited.streamsdkdemo.ui.login;

import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.streamunlimited.streamsdkdemo.R;

import java.util.Arrays;
import java.util.List;

/**
 *
 */
public class LoginDialog extends DialogFragment {

    public static final String TAG = LoginDialog.class.getSimpleName();

    private final Context ctx;
    private final LoginTarget target;
    private final ILoginService callback;

    private ViewHolder holder;

    public LoginDialog(Context ctx, LoginTarget target, ILoginService callback) {
        this.ctx = ctx;
        this.target = target;
        this.callback = callback;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NO_TITLE, R.style.Tio_Dialog_Fullscreen);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = View.inflate(getActivity(), R.layout.dialog_login, container);
        holder = new ViewHolder(v);
        holder.configure(target);
        return v;
    }

    private class ViewHolder {

        ImageView logoImage;
        EditText usernameText;
        EditText passwordText;
        Button loginButton;

        ViewHolder(View v) {
            logoImage = v.findViewById(R.id.logo_img);
            usernameText = v.findViewById(R.id.username_txt);
            passwordText = v.findViewById(R.id.password_txt);
            loginButton = v.findViewById(R.id.login_btn);
            loginButton.setOnClickListener(login_onClick);
        }

        void configure(LoginTarget t) {
            switch (t) {
                case siriusxm:
                    logoImage.setImageResource(R.drawable.ic_sirius_hero);
                    break;
                case napster:
                    logoImage.setImageResource(R.drawable.ic_napster_hero);
                    break;
                default:
                    Log.w(TAG, "configure: unhandled case " + t);
                    break;
            }
        }

        String getUsername() {
            return usernameText.getText().toString().trim();
        }

        String getPassword() {
            return passwordText.getText().toString().trim();
        }

        boolean validate(String s) {
            if (s.isEmpty()) {
                Toast.makeText(ctx, getString(R.string.err_login_empty, target.toDisplayString()), Toast.LENGTH_LONG).show();
                return false;
            }
            return true;
        }

        void setAllEnabled(boolean value) {
            List<View> elements = Arrays.asList(usernameText, passwordText, loginButton);
            for (View el : elements) {
                el.setEnabled(value);
            }
        }

        private final View.OnClickListener login_onClick = (v) -> {
            String user = getUsername();
            if (!validate(user)) return;

            String pass = getPassword();
            if (!validate(pass)) return;

            setAllEnabled(false);

            if (callback != null) callback.onAccountCredentialsProvided(user, pass);
            LoginDialog.super.dismiss();
        };
    }
}
