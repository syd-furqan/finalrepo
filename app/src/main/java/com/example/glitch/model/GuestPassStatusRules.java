package com.example.glitch.model;

import androidx.annotation.NonNull;

import java.util.Date;
import java.util.Locale;

/**
 * Shared guest-pass lifecycle rules used by repository and UI layers.
 */
public final class GuestPassStatusRules {
    private GuestPassStatusRules() {
    }

    public static boolean isTimeExpiredActive(@NonNull GuestPass pass) {
        if (!"active".equalsIgnoreCase(pass.getStatus())) {
            return false;
        }
        if (pass.getExpiresAt() == null) {
            return false;
        }
        Date now = new Date();
        Date expiresAt = pass.getExpiresAt().toDate();
        return !expiresAt.after(now);
    }

    /**
     * Returns true if the pass is in a terminal state where it no longer represents
     * an active or upcoming visitor engagement.
     */
    public static boolean isArchivedStatus(@NonNull String rawStatus) {
        String status = rawStatus.trim().toLowerCase(Locale.getDefault());
        // Terminal states: Guest has left, pass was cancelled, pass timed out before use, or access was denied.
        return "exited".equals(status) || "cancelled".equals(status) || "expired".equals(status) || "denied".equals(status);
    }

    /**
     * Returns true if the pass represents a guest currently authorized to be on-site
     * or scheduled to arrive.
     */
    public static boolean isInProgress(@NonNull String rawStatus) {
        String status = rawStatus.trim().toLowerCase(Locale.getDefault());
        return "active".equals(status) || "used".equals(status) || "overdue".equals(status);
    }

    public static boolean blocksStudentIssuance(@NonNull String rawStatus) {
        String status = rawStatus.trim().toLowerCase(Locale.getDefault());
        return "active".equals(status) || "overdue".equals(status);
    }

    public static boolean isShareable(@NonNull GuestPass pass) {
        // Only "active" passes (issued but not yet used) can be shared for scanning.
        return "active".equalsIgnoreCase(pass.getStatus());
    }
}
