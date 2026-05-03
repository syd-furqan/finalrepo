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
        return pass.getExpiresAt().toDate().before(new Date());
    }

    public static boolean isArchivedStatus(@NonNull String rawStatus) {
        String status = rawStatus.trim().toLowerCase(Locale.getDefault());
        return "expired".equals(status) || "cancelled".equals(status) || "denied".equals(status);
    }

    public static boolean isShareable(@NonNull GuestPass pass) {
        return !isArchivedStatus(pass.getStatus());
    }
}
