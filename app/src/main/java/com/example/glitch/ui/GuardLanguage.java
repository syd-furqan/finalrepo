package com.example.glitch.ui;

import androidx.annotation.NonNull;

import java.util.Locale;

enum GuardLanguage {
    EN("en"),
    UR("ur");

    private final String code;

    GuardLanguage(@NonNull String code) {
        this.code = code;
    }

    @NonNull
    String code() {
        return code;
    }

    @NonNull
    static GuardLanguage fromStored(@NonNull String raw) {
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if ("ur".equals(normalized)) {
            return UR;
        }
        return EN;
    }
}
