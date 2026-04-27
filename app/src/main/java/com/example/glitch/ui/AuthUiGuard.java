package com.example.glitch.ui;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.glitch.R;
import com.example.glitch.auth.SessionManager;
import com.example.glitch.model.UserProfile;
import com.google.android.material.snackbar.Snackbar;

/**
 * Shared session guard for feature fragments to prevent blank content when session state is missing.
 * Pattern: UI-level auth precondition helper for fragments that require authenticated profile context.
 * Known issue: navigation fallback is fragment-host based and assumes MainActivity implements NavigationHost.
 */
public final class AuthUiGuard {
    private AuthUiGuard() {
    }

    @Nullable
    public static UserProfile requireProfile(@NonNull Fragment fragment) {
        UserProfile profile = SessionManager.getCurrentProfile();
        if (profile != null) {
            return profile;
        }
        if (!fragment.isAdded()) {
            return null;
        }
        View root = fragment.getView();
        if (root != null) {
            Snackbar.make(root, R.string.error_session_expired, Snackbar.LENGTH_SHORT).show();
        }
        if (fragment.requireActivity() instanceof NavigationHost) {
            ((NavigationHost) fragment.requireActivity()).showLogin(true);
        }
        return null;
    }
}
