package com.example.glitch.ui;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import com.example.glitch.auth.SessionManager;
import com.example.glitch.model.UserProfile;

public final class GuardLanguageHelper {
    private GuardLanguageHelper() {
    }

    @Nullable
    public static GuardLanguage currentGuardLanguage(@NonNull Context context) {
        UserProfile profile = SessionManager.getCurrentProfile();
        if (profile == null || !"guard".equalsIgnoreCase(profile.getRole())) {
            return null;
        }
        String uid = profile.getUid() == null ? "" : profile.getUid().trim();
        if (uid.isEmpty()) {
            return null;
        }
        return new GuardLanguageStore(context).getForGuard(uid);
    }

    public static void setCurrentGuardLanguage(@NonNull Context context, @NonNull GuardLanguage language) {
        UserProfile profile = SessionManager.getCurrentProfile();
        if (profile == null || !"guard".equalsIgnoreCase(profile.getRole())) {
            return;
        }
        String uid = profile.getUid() == null ? "" : profile.getUid().trim();
        if (uid.isEmpty()) {
            return;
        }
        new GuardLanguageStore(context).setForGuard(uid, language);
        applyLanguageIfNeeded(language);
    }

    public static void syncApplicationLocalesForCurrentGuard(@NonNull Context context) {
        GuardLanguage current = currentGuardLanguage(context);
        if (current == null) {
            return;
        }
        applyLanguageIfNeeded(current);
    }

    public static void resetApplicationLocaleToDefault() {
        applyLanguageIfNeeded(GuardLanguage.EN);
    }

    private static void applyLanguageIfNeeded(@NonNull GuardLanguage language) {
        LocaleListCompat appLocales = AppCompatDelegate.getApplicationLocales();
        String currentTag = appLocales.toLanguageTags();
        String nextTag = language.code();
        if (nextTag.equalsIgnoreCase(currentTag)) {
            return;
        }
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(nextTag));
    }
}
