package com.example.glitch.ui;

import android.text.Editable;
import android.text.TextWatcher;

import androidx.annotation.NonNull;

import com.example.glitch.model.GuestIdentityPolicy;
import com.google.android.material.textfield.TextInputEditText;

/**
 * UI-only input formatters for guest identity fields.
 */
public final class GuestIdentityInputSupport {
    private GuestIdentityInputSupport() {
    }

    public static void attachCnicFormatter(@NonNull TextInputEditText input) {
        attachFormattingWatcher(input, true);
    }

    public static void attachVehiclePlateFormatter(@NonNull TextInputEditText input) {
        attachFormattingWatcher(input, false);
    }

    private static void attachFormattingWatcher(@NonNull TextInputEditText input, boolean cnic) {
        final boolean[] selfChange = {false};
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (selfChange[0]) {
                    return;
                }
                String raw = editable == null ? "" : editable.toString();
                String formatted = cnic
                        ? GuestIdentityPolicy.formatCnicForInput(raw)
                        : GuestIdentityPolicy.formatVehiclePlateForInput(raw);
                if (raw.equals(formatted)) {
                    return;
                }
                selfChange[0] = true;
                input.setText(formatted);
                input.setSelection(formatted.length());
                selfChange[0] = false;
            }
        });
    }
}
