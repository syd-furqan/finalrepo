package com.example.glitch.ui;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.glitch.model.UserProfile;

/**
 * Navigation host contract exposed by MainActivity to feature fragments.
 * Pattern: Activity-level navigator abstraction for fragment transactions.
 * Known issue: navigation stack is intentionally simple and not using NavComponent in v1.
 */
public interface NavigationHost {
    void showLogin(boolean clearBackStack);

    void showRoleHome(@NonNull UserProfile profile, boolean clearBackStack);

    void showFragment(@NonNull Fragment fragment, boolean addToBackStack);
}
