package com.example.glitch;

/**
 * Startup/session decision policy used by MainActivity bootstrap flow.
 * Pattern: Stateless policy helper to keep host navigation rules unit-testable.
 * Known issue: policy intentionally prioritizes explicit login entry on fresh launches.
 */
public final class MainActivityStartupPolicy {
    private MainActivityStartupPolicy() {
    }

    /**
     * Determines whether activity startup should force a fresh login flow.
     *
     * @param hasSavedInstanceState true when Android is restoring prior activity state.
     * @return true when startup must discard stale state and go to login.
     */
    public static boolean shouldForceFreshLogin(boolean hasSavedInstanceState) {
        return !hasSavedInstanceState;
    }

    /**
     * Determines whether current fragment state should be redirected to login.
     *
     * @param isLoginScreenVisible true when login UI is already being shown.
     * @param hasSessionProfile true when in-memory session profile exists.
     * @return true when app should route back to login.
     */
    public static boolean shouldRedirectToLogin(boolean isLoginScreenVisible, boolean hasSessionProfile) {
        return !isLoginScreenVisible && !hasSessionProfile;
    }
}
