package com.example.private_photo;

import android.content.Context;
import android.preference.PreferenceManager;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

public class PasswordProvider {

    public static void getPassword(FragmentManager fm, Context context, PPEncryption.PasswordProviderResultListener listener) {
        String encryptedPassword = PreferenceManager.getDefaultSharedPreferences(context).getString("password", null);
        if (encryptedPassword != null && !encryptedPassword.isEmpty()) {
            try {
                PPEncryption.decryptPassword(encryptedPassword, context, listener);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }
        DialogFragment dialog = new InputPasswordDialogFragment(listener::onPassword);
        dialog.show(fm, null);
    }
}
