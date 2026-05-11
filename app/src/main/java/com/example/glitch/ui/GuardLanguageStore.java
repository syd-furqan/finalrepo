package com.example.glitch.ui;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

final class GuardLanguageStore {
    private static final String PREFS_NAME = "guard_language_prefs";
    private static final String KEY_PREFIX = "guard_lang_";

    private final SharedPreferences prefs;

    GuardLanguageStore(@NonNull Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    @NonNull
    GuardLanguage getForGuard(@NonNull String uid) {
        String key = KEY_PREFIX + uid.trim();
        String stored = prefs.getString(key, GuardLanguage.EN.code());
        return GuardLanguage.fromStored(stored == null ? "" : stored);
    }

    void setForGuard(@NonNull String uid, @NonNull GuardLanguage language) {
        String key = KEY_PREFIX + uid.trim();
        prefs.edit().putString(key, language.code()).apply();
    }
}
