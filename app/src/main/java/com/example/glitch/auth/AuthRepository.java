package com.example.glitch.auth;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.glitch.model.UserProfile;

/**
 * Contract for authentication and profile validation workflows.
 * Pattern: Repository abstraction over FirebaseAuth + Firestore profile checks.
 * Known issue: social login providers are intentionally out of scope in v1.
 */
public interface AuthRepository {

    /**
     * Attempts email/password login and validates profile permissions.
     */
    void login(@NonNull String email, @NonNull String password, @NonNull AuthCallback callback);

    /**
     * Logs out current Firebase session and clears local profile cache.
     */
    void logout();

    /**
     * Validates currently authenticated user's profile document.
     */
    void validateCurrentSession(@NonNull AuthCallback callback);

    /**
     * Returns currently cached user profile, if any.
     */
    @Nullable
    UserProfile getCurrentProfile();

    interface AuthCallback {
        void onResult(boolean success, @Nullable UserProfile profile, @NonNull String message);
    }
}
