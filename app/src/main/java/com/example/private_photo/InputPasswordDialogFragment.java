package com.example.private_photo;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.snackbar.Snackbar;

public class InputPasswordDialogFragment extends DialogFragment {
    public interface InputPasswordDialogListener {
        void onOk(@Nullable String password);
    }

    InputPasswordDialogListener listener;

    public InputPasswordDialogFragment(InputPasswordDialogListener listener) {
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        setCancelable(false);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        View view = inflater.inflate(R.layout.dialog_input_password, null);
        TextView inputPassword = view.findViewById(R.id.input_password);
        inputPassword.requestFocus();
        SwitchCompat toggleSavePassword = view.findViewById(R.id.switch_save_password);
        (new Handler()).postDelayed(() ->
                ((InputMethodManager)requireActivity()
                        .getSystemService(Context.INPUT_METHOD_SERVICE))
                        .showSoftInput(inputPassword, InputMethodManager.SHOW_IMPLICIT), //must be run in postDelayed to work here
                100
        );

        builder.setView(view)
                // Add action buttons
                .setPositiveButton(R.string.ok, (dialog, id) -> {
                    if (toggleSavePassword.isChecked()) {
                        try {
                            String encryptedPassword = PPEncryption.encryptPassword(inputPassword.getText().toString());
                            PreferenceManager
                                    .getDefaultSharedPreferences(getContext())
                                    .edit()
                                    .putString("password", encryptedPassword)
                                    .apply();
                        } catch (Exception e) {
                            e.printStackTrace();
                            Snackbar.make(this.requireView(), "Could not save password!", Snackbar.LENGTH_LONG)
                                    .setAction("Action", null).show();
                        }
                    }
                    listener.onOk(inputPassword.getText().toString());
                })
                .setNegativeButton(R.string.cancel, (dialog, id) -> {
                        listener.onOk(null);
                        InputPasswordDialogFragment.this.requireDialog().cancel();
                    }
                );
        return builder.create();
    }
}
