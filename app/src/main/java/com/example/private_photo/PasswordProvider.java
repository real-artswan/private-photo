package com.example.private_photo;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

public class PasswordProvider {
    public interface PasswordProviderResultListener {
        void onPassword(@Nullable String password);
    }
    public static void getPassword(FragmentManager fm, PasswordProviderResultListener listener) {
        DialogFragment dialog = new InputPasswordDialogFragment(listener::onPassword);
        dialog.show(fm, null);
    }
}
