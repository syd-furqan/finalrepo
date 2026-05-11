package com.example.glitch.ui;

import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

final class GuardLanguageUiBinder {
    private GuardLanguageUiBinder() {
    }

    static void bind(@NonNull View root, @NonNull Fragment owner) {
        // Legacy guard status ribbon was removed from the shared shell.
        // Keep this method as a safe no-op so existing fragment calls still compile.
    }
}
