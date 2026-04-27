package com.example.glitch.auth;

import androidx.annotation.Nullable;

import com.example.glitch.model.UserProfile;

/**
 * In-memory holder for active user profile session state.
 * Pattern: Process-local singleton utility for lightweight session access.
 * Known issue: session is cleared when app process is killed.
 */
public final class SessionManager {
    private static UserProfile currentProfile;

    private SessionManager() {
    }

    /**
     * Stores the current authenticated profile in memory.
     *
     * @param profile active profile, or null to clear.
     */
    public static void setCurrentProfile(@Nullable UserProfile profile) {
        currentProfile = profile;
    }

    /**
     * Returns the in-memory authenticated profile.
     *
     * @return current profile, or null when no active session is cached.
     */
    @Nullable
    public static UserProfile getCurrentProfile() {
        return currentProfile;
    }

    /**
     * Clears in-memory session profile state.
     */
    public static void clear() {
        currentProfile = null;
    }
}
