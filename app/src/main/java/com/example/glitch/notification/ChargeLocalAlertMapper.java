package com.example.glitch.notification;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

/**
 * Maps fine-case Firestore transitions into sponsor charge local alerts.
 */
public final class ChargeLocalAlertMapper {
    private ChargeLocalAlertMapper() {
    }

    @Nullable
    public static ChargeLocalAlertType resolve(
            @NonNull String changeType,
            @Nullable String previousStatus,
            @NonNull String currentStatus
    ) {
        String normalizedChange = normalize(changeType);
        String previous = normalize(previousStatus);
        String current = normalize(currentStatus);

        if ("added".equals(normalizedChange)) {
            return "issued".equals(current) ? ChargeLocalAlertType.CREATED : null;
        }
        if (!"modified".equals(normalizedChange) || previous.equals(current)) {
            return null;
        }
        if ("settled".equals(current)) {
            return ChargeLocalAlertType.PAID;
        }
        if ("waived".equals(current)) {
            return ChargeLocalAlertType.REMOVED;
        }
        return null;
    }

    @NonNull
    private static String normalize(@Nullable String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toLowerCase(Locale.getDefault());
    }
}
